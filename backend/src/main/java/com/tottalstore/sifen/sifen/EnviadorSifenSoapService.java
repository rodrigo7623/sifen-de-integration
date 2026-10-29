package com.tottalstore.sifen.sifen;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import javax.net.ssl.SSLContext;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Cliente SOAP real del servicio de recepción síncrona del SIFEN (operación {@code SiRecepDE}),
 * con autenticación TLS mutua vía el certificado cargado en {@link SifenKeystoreConfig}.
 *
 * <p>Recibe el {@code <rDE>} ya armado y firmado por {@code GeneradorDteSifen} (Fases 3 y 4) y lo
 * envía dentro de {@code <xDE>} sin modificarlo: cualquier cambio invalidaría la firma.
 *
 * <p>Estructura del sobre y de la respuesta tomadas literalmente del Manual Técnico del SIFEN v150,
 * sección 7.4 (ejemplos de request/response de {@code SiRecepDE}).
 */
@Component
@ConditionalOnProperty(name = "sifen.envio.modo", havingValue = "real")
public class EnviadorSifenSoapService implements EnviadorSifenService {

    private static final Logger log = LoggerFactory.getLogger(EnviadorSifenSoapService.class);

    private final HttpClient httpClient;
    private final Duration timeout;
    private final EnviadorSifenConfigRepository enviadorSifenConfigRepository;
    private final SifenEnvioHistorial historial;

    public EnviadorSifenSoapService(
            SSLContext sifenSslContext,
            EnviadorSifenConfigRepository enviadorSifenConfigRepository,
            SifenEnvioHistorial historial,
            @Value("${sifen.envio.timeout-segundos:30}") long timeoutSegundos) {
        this.timeout = Duration.ofSeconds(timeoutSegundos);
        this.httpClient = HttpClient.newBuilder()
                .sslContext(sifenSslContext)
                .connectTimeout(timeout)
                .build();
        this.enviadorSifenConfigRepository = enviadorSifenConfigRepository;
        this.historial = historial;
    }

    @Override
    public RespuestaSifenResult enviar(UUID facturaId, String xmlFirmado, Ambiente ambiente) {
        String url = obtenerUrlEndpoint();
        String dId = String.valueOf(System.currentTimeMillis());
        String sobreRequest = construirSobreSoap(dId, xmlFirmado);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(timeout)
                    .header("Content-Type", "application/soap+xml; charset=utf-8")
                    .POST(HttpRequest.BodyPublishers.ofString(sobreRequest, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("SIFEN respondió HTTP {} para dId={}", response.statusCode(), dId);

            if (response.statusCode() != 200) {
                String mensaje = "HTTP " + response.statusCode() + " del SIFEN: "
                        + truncar(response.body(), 500);
                historial.registrar(facturaId, ambiente, url, dId, "ERROR_CONEXION", null, mensaje, null);
                return new RespuestaSifenResult(false, "ERROR_HTTP", mensaje, null);
            }

            RespuestaSifenResult resultado = parsearRespuesta(response.body());
            historial.registrar(
                    facturaId,
                    ambiente,
                    url,
                    dId,
                    resultado.aprobado() ? "APROBADO" : "RECHAZADO",
                    resultado.codigo(),
                    resultado.descripcion(),
                    resultado.cdc());
            return resultado;
        } catch (Exception e) {
            // Cualquier falla de red/TLS/parseo se traduce en un rechazo con motivo claro (en vez de
            // burbujear como 500 genérico): la factura queda en RECHAZADO con el motivo real, y el
            // flujo de "Reabrir" ya existente sirve para reintentar sin cambios.
            log.warn("Fallo al conectar con el SIFEN real ({})", url, e);
            String mensaje = "No se pudo conectar/procesar contra el SIFEN: " + e;
            historial.registrar(facturaId, ambiente, url, dId, "ERROR_CONEXION", null, mensaje, null);
            return new RespuestaSifenResult(false, "ERROR_CONEXION", mensaje, null);
        }
    }

    private String obtenerUrlEndpoint() {
        return enviadorSifenConfigRepository.findAll().stream()
                .findFirst()
                .map(EnviadorSifenConfig::getUrlEndpoint)
                .orElseThrow(() -> new IllegalStateException(
                        "No hay configuración de enviador_sifen con la URL del web service del SIFEN"));
    }

    /** Sobre SOAP 1.2 según el ejemplo literal del Manual Técnico, sección 7.4. Package-private para
     * poder testear el armado del sobre sin red real. */
    String construirSobreSoap(String dId, String rDeFirmado) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\">"
                + "<soap:Header/>"
                + "<soap:Body>"
                + "<rEnviDe xmlns=\"http://ekuatia.set.gov.py/sifen/xsd\">"
                + "<dId>" + dId + "</dId>"
                + "<xDE>" + rDeFirmado + "</xDE>"
                + "</rEnviDe>"
                + "</soap:Body>"
                + "</soap:Envelope>";
    }

    /** Extrae dEstRes/dCodRes/dMsgRes/id(CDC) de un {@code rRetEnviDe} usando local-name() por XPath,
     * para no depender de qué prefijo de namespace use el servidor (el ejemplo del manual usa "ns2",
     * pero eso no está garantizado). */
    RespuestaSifenResult parsearRespuesta(String xmlRespuesta) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        Document doc = factory.newDocumentBuilder()
                .parse(new ByteArrayInputStream(xmlRespuesta.getBytes(StandardCharsets.UTF_8)));

        XPath xpath = XPathFactory.newInstance().newXPath();
        String dEstRes = textoDe(xpath, doc, "//*[local-name()='dEstRes']");
        String dCodRes = textoDe(xpath, doc, "//*[local-name()='dCodRes']");
        String dMsgRes = textoDe(xpath, doc, "//*[local-name()='dMsgRes']");
        String cdc = textoDe(xpath, doc, "//*[local-name()='rProtDe']/*[local-name()='dId']");

        boolean aprobado = dEstRes != null && dEstRes.toLowerCase().startsWith("aprobado");
        String descripcion = (dEstRes != null ? dEstRes : "Sin dEstRes en la respuesta")
                + (dMsgRes != null ? " — " + dMsgRes : "");
        return new RespuestaSifenResult(aprobado, dCodRes, descripcion, aprobado ? cdc : null);
    }

    private String textoDe(XPath xpath, Document doc, String expresion) throws Exception {
        Element elemento = (Element) xpath.evaluate(expresion, doc, XPathConstants.NODE);
        return elemento != null ? elemento.getTextContent() : null;
    }

    private String truncar(String texto, int max) {
        if (texto == null) return null;
        return texto.length() > max ? texto.substring(0, max) + "…" : texto;
    }
}
