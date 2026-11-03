package com.tottalstore.sifen.sifen;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
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
 * Cliente SOAP real del WS de consulta de RUC del SIFEN ({@code siConsRUC}, Manual Técnico v150,
 * sección 9.6), con el mismo TLS mutuo que {@link EnviadorSifenSoapService}.
 *
 * <p>Sirve como prueba de conectividad de punta a punta contra el SIFEN que no depende del XML real
 * del DE ni de la firma XMLDSig (Fases 3 y 4): el request es solo {@code dId} + RUC, y cualquier
 * código de respuesta (0500/0501/0502) confirma que el certificado, el TLS mutuo y el sobre SOAP
 * son aceptados por el SIFEN.
 */
@Component
@ConditionalOnProperty(name = "sifen.envio.modo", havingValue = "real")
public class ConsultaRucSifenService {

    private static final Logger log = LoggerFactory.getLogger(ConsultaRucSifenService.class);
    private static final String NS_SIFEN = "http://ekuatia.set.gov.py/sifen/xsd";

    private final HttpClient httpClient;
    private final Duration timeout;
    private final String url;
    private final SifenEnvioHistorial historial;
    private final EnviadorSifenConfigRepository enviadorSifenConfigRepository;

    public ConsultaRucSifenService(
            SSLContext sifenSslContext,
            SifenEnvioHistorial historial,
            EnviadorSifenConfigRepository enviadorSifenConfigRepository,
            @Value("${sifen.envio.url-consulta-ruc}") String url,
            @Value("${sifen.envio.timeout-segundos:30}") long timeoutSegundos) {
        this.timeout = Duration.ofSeconds(timeoutSegundos);
        this.httpClient = HttpClient.newBuilder()
                .sslContext(sifenSslContext)
                .connectTimeout(timeout)
                .build();
        this.url = url;
        this.historial = historial;
        this.enviadorSifenConfigRepository = enviadorSifenConfigRepository;
    }

    public ConsultaRucResult consultar(String ruc) {
        String rucSinDv = sinDigitoVerificador(ruc);
        String dId = String.valueOf(System.currentTimeMillis());
        Ambiente ambiente = ambienteActual();

        ConsultaRucResult resultado;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(timeout)
                    .header("Content-Type", "application/soap+xml; charset=utf-8")
                    .POST(HttpRequest.BodyPublishers.ofString(construirSobreSoap(dId, rucSinDv), StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("SIFEN siConsRUC respondió HTTP {} para dId={}", response.statusCode(), dId);

            resultado = response.statusCode() == 200
                    ? parsearRespuesta(response.body())
                    : ConsultaRucResult.sinRespuesta(
                            "ERROR_HTTP", "HTTP " + response.statusCode() + " del SIFEN"
                                    + response.headers().firstValue("Location").map(l -> " (redirige a " + l + ")").orElse("")
                                    + ": " + truncar(response.body(), 500));
        } catch (Exception e) {
            log.warn("Fallo la consulta de RUC contra el SIFEN ({})", url, e);
            resultado = ConsultaRucResult.sinRespuesta(
                    "ERROR_CONEXION", "No se pudo conectar/procesar contra el SIFEN: " + e);
        }

        historial.registrar(
                null,
                ambiente,
                url,
                dId,
                "CONSULTA_RUC " + (resultado.respondioSifen() ? "RESPONDIDA" : "ERROR_CONEXION"),
                resultado.codigo(),
                "RUC " + rucSinDv + ": " + resultado.mensaje(),
                null);
        return resultado;
    }

    /** El WS espera el RUC sin dígito verificador (RSch03, 5-8 caracteres). */
    static String sinDigitoVerificador(String ruc) {
        if (ruc == null || ruc.isBlank()) {
            throw new IllegalArgumentException("El RUC a consultar es obligatorio");
        }
        String base = ruc.trim().split("-")[0];
        if (base.length() < 5 || base.length() > 8) {
            throw new IllegalArgumentException("El RUC a consultar debe tener entre 5 y 8 caracteres sin el dígito verificador");
        }
        return base;
    }

    /** Sobre SOAP 1.2 con {@code rEnviConsRUC} (Schema XML 15, siConsRUC_v150.xsd). */
    String construirSobreSoap(String dId, String rucSinDv) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\">"
                + "<soap:Header/>"
                + "<soap:Body>"
                + "<rEnviConsRUC xmlns=\"" + NS_SIFEN + "\">"
                + "<dId>" + dId + "</dId>"
                + "<dRUCCons>" + rucSinDv + "</dRUCCons>"
                + "</rEnviConsRUC>"
                + "</soap:Body>"
                + "</soap:Envelope>";
    }

    /** Extrae {@code rResEnviConsRUC} (Schema XML 16) y, si vino, {@code xContRUC} (Schema XML 17),
     * por local-name() para no depender del prefijo de namespace que use el servidor. */
    ConsultaRucResult parsearRespuesta(String xmlRespuesta) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        Document doc = factory.newDocumentBuilder()
                .parse(new ByteArrayInputStream(xmlRespuesta.getBytes(StandardCharsets.UTF_8)));

        XPath xpath = XPathFactory.newInstance().newXPath();
        String facturador = texto(xpath, doc, "dRUCFactElec");
        return new ConsultaRucResult(
                true,
                texto(xpath, doc, "dCodRes"),
                texto(xpath, doc, "dMsgRes"),
                texto(xpath, doc, "dRUCCons"),
                texto(xpath, doc, "dRazCons"),
                texto(xpath, doc, "dCodEstCons"),
                texto(xpath, doc, "dDesEstCons"),
                facturador != null ? "S".equalsIgnoreCase(facturador.trim()) : null);
    }

    private String texto(XPath xpath, Document doc, String elemento) throws Exception {
        Element nodo = (Element) xpath.evaluate("//*[local-name()='" + elemento + "']", doc, XPathConstants.NODE);
        return nodo != null ? nodo.getTextContent() : null;
    }

    private Ambiente ambienteActual() {
        return enviadorSifenConfigRepository.findAll().stream()
                .findFirst()
                .map(EnviadorSifenConfig::getAmbiente)
                .orElse(Ambiente.TEST);
    }

    private String truncar(String texto, int max) {
        if (texto == null) return null;
        return texto.length() > max ? texto.substring(0, max) + "…" : texto;
    }
}
