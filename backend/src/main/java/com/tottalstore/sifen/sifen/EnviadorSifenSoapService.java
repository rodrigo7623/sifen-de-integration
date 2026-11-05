package com.tottalstore.sifen.sifen;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
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
 * Cliente SOAP real del servicio de recepción síncrona del SIFEN (operación {@code SiRecepDE}) y de
 * la consulta de DE por CDC ({@code siConsDE}), con autenticación TLS mutua vía el certificado
 * cargado en {@link SifenKeystoreConfig}.
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
    private static final String NS_SIFEN = "http://ekuatia.set.gov.py/sifen/xsd";
    /** siConsDE: dCodRes 0422 = CDC encontrado; 0420 = el DE no existe en el SIFEN o fue rechazado. */
    static final String CDC_ENCONTRADO = "0422";
    static final String CDC_INEXISTENTE = "0420";

    private final HttpClient httpClient;
    private final Duration timeout;
    private final EnviadorSifenConfigRepository enviadorSifenConfigRepository;
    private final SifenEnvioHistorial historial;
    private final String urlConsultaDe;

    public EnviadorSifenSoapService(
            SSLContext sifenSslContext,
            EnviadorSifenConfigRepository enviadorSifenConfigRepository,
            SifenEnvioHistorial historial,
            @Value("${sifen.envio.url-consulta-de}") String urlConsultaDe,
            @Value("${sifen.envio.timeout-segundos:30}") long timeoutSegundos) {
        this.timeout = Duration.ofSeconds(timeoutSegundos);
        this.httpClient = HttpClient.newBuilder()
                .sslContext(sifenSslContext)
                .connectTimeout(timeout)
                .build();
        this.enviadorSifenConfigRepository = enviadorSifenConfigRepository;
        this.historial = historial;
        this.urlConsultaDe = urlConsultaDe;
    }

    @Override
    public RespuestaSifenResult enviar(UUID facturaId, String xmlFirmado, Ambiente ambiente) {
        String url = obtenerUrlEndpoint();
        String dId = String.valueOf(System.currentTimeMillis());

        HttpResponse<String> response;
        try {
            response = httpClient.send(peticion(url, construirSobreSoap(dId, xmlFirmado)), HttpResponse.BodyHandlers.ofString());
        } catch (HttpConnectTimeoutException | ConnectException | SSLHandshakeException e) {
            // No se llegó a establecer la conexión: el DE nunca salió, así que es un rechazo y el flujo
            // de "Reabrir" sirve para reintentar.
            log.warn("No se pudo conectar con el SIFEN real ({})", url, e);
            String mensaje = "No se pudo conectar con el SIFEN: " + e;
            historial.registrar(facturaId, ambiente, url, dId, "ERROR_CONEXION", null, mensaje, null);
            return new RespuestaSifenResult(false, "ERROR_CONEXION", mensaje, null);
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            // BUG-006: timeout esperando la respuesta o conexión cortada después de enviar. El SIFEN
            // pudo haber recibido (y aprobado) el DE: no se marca como rechazo, se consulta por CDC.
            log.warn("Envío al SIFEN sin respuesta ({})", url, e);
            String mensaje = "El SIFEN no respondió a tiempo (" + e + "): hay que verificar el estado del documento";
            historial.registrar(facturaId, ambiente, url, dId, "SIN_CONFIRMAR", null, mensaje, null);
            return RespuestaSifenResult.sinConfirmar("ERROR_TIMEOUT", mensaje);
        }
        log.info("SIFEN respondió HTTP {} para dId={}", response.statusCode(), dId);

        if (response.statusCode() != 200) {
            String mensaje = "HTTP " + response.statusCode() + " del SIFEN: " + truncar(response.body(), 500);
            historial.registrar(facturaId, ambiente, url, dId, "ERROR_CONEXION", null, mensaje, null);
            return new RespuestaSifenResult(false, "ERROR_HTTP", mensaje, null);
        }

        try {
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
            // El SIFEN contestó 200 pero no se pudo leer la respuesta: tampoco se sabe si aprobó.
            log.warn("Respuesta del SIFEN ilegible para dId={}", dId, e);
            String mensaje = "No se pudo leer la respuesta del SIFEN (" + e + "): hay que verificar el estado del documento";
            historial.registrar(facturaId, ambiente, url, dId, "SIN_CONFIRMAR", null, mensaje, null);
            return RespuestaSifenResult.sinConfirmar("RESPUESTA_ILEGIBLE", mensaje);
        }
    }

    /** {@code siConsDE} (Manual Técnico v150, sección 9.3). Es de solo lectura: no tiene efectos en el SIFEN. */
    @Override
    public RespuestaSifenResult consultarDe(UUID facturaId, String cdc, Ambiente ambiente) {
        String dId = String.valueOf(System.currentTimeMillis());
        RespuestaSifenResult resultado;
        try {
            HttpResponse<String> response = httpClient.send(
                    peticion(urlConsultaDe, construirSobreConsultaDe(dId, cdc)), HttpResponse.BodyHandlers.ofString());
            log.info("SIFEN siConsDE respondió HTTP {} para dId={}", response.statusCode(), dId);
            resultado = response.statusCode() == 200
                    ? parsearConsultaDe(response.body(), cdc)
                    : RespuestaSifenResult.sinConfirmar(
                            "ERROR_HTTP", "HTTP " + response.statusCode() + " del SIFEN al consultar el DE: "
                                    + truncar(response.body(), 500));
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.warn("Falló la consulta de DE contra el SIFEN ({})", urlConsultaDe, e);
            resultado = RespuestaSifenResult.sinConfirmar(
                    "ERROR_CONEXION", "No se pudo consultar el documento en el SIFEN: " + e);
        }

        String estado = resultado.aprobado() ? "CONSULTA_DE ENCONTRADO"
                : resultado.sinConfirmar() ? "CONSULTA_DE ERROR_CONEXION" : "CONSULTA_DE INEXISTENTE";
        historial.registrar(facturaId, ambiente, urlConsultaDe, dId, estado, resultado.codigo(),
                "CDC " + cdc + ": " + resultado.descripcion(), resultado.cdc());
        return resultado;
    }

    private HttpRequest peticion(String url, String sobre) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(timeout)
                .header("Content-Type", "application/soap+xml; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(sobre, StandardCharsets.UTF_8))
                .build();
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
                + "<rEnviDe xmlns=\"" + NS_SIFEN + "\">"
                + "<dId>" + dId + "</dId>"
                + "<xDE>" + rDeFirmado + "</xDE>"
                + "</rEnviDe>"
                + "</soap:Body>"
                + "</soap:Envelope>";
    }

    /** Sobre SOAP 1.2 con {@code rEnviConsDeRequest} (siConsDE_v150.xsd). */
    String construirSobreConsultaDe(String dId, String cdc) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\">"
                + "<soap:Header/>"
                + "<soap:Body>"
                + "<rEnviConsDeRequest xmlns=\"" + NS_SIFEN + "\">"
                + "<dId>" + dId + "</dId>"
                + "<dCDC>" + cdc + "</dCDC>"
                + "</rEnviConsDeRequest>"
                + "</soap:Body>"
                + "</soap:Envelope>";
    }

    /** Extrae dEstRes/dCodRes/dMsgRes/id(CDC) de un {@code rRetEnviDe} usando local-name() por XPath,
     * para no depender de qué prefijo de namespace use el servidor (el ejemplo del manual usa "ns2",
     * pero eso no está garantizado). */
    RespuestaSifenResult parsearRespuesta(String xmlRespuesta) throws Exception {
        Document doc = parsearXml(xmlRespuesta);
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

    /** {@code rEnviConsDeResponse}: 0422 → el SIFEN tiene el DE (aprobado); 0420 → no existe, se puede
     * reenviar sin riesgo de duplicarlo; cualquier otro código no permite concluir nada. */
    RespuestaSifenResult parsearConsultaDe(String xmlRespuesta, String cdc) throws Exception {
        Document doc = parsearXml(xmlRespuesta);
        XPath xpath = XPathFactory.newInstance().newXPath();
        String dCodRes = textoDe(xpath, doc, "//*[local-name()='dCodRes']");
        String dMsgRes = textoDe(xpath, doc, "//*[local-name()='dMsgRes']");
        String descripcion = dMsgRes != null ? dMsgRes : "Sin dMsgRes en la respuesta";

        if (CDC_ENCONTRADO.equals(dCodRes)) {
            return new RespuestaSifenResult(true, dCodRes, descripcion, cdc);
        }
        if (CDC_INEXISTENTE.equals(dCodRes)) {
            return new RespuestaSifenResult(false, dCodRes, descripcion, null);
        }
        return RespuestaSifenResult.sinConfirmar(dCodRes, descripcion);
    }

    private Document parsearXml(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        return factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
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
