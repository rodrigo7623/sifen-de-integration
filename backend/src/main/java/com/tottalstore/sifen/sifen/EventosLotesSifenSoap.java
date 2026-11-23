package com.tottalstore.sifen.sifen;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
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
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Cliente SOAP 1.2 (TLS mutuo, mismo {@link SSLContext} que {@link EnviadorSifenSoapService}) de
 * {@code siRecepEvento}, {@code siRecepLoteDE} y {@code siResultLoteDE}. Las respuestas se leen por
 * {@code local-name()} para no depender del prefijo que use el servidor.
 */
@Component
@ConditionalOnProperty(name = "sifen.envio.modo", havingValue = "real")
public class EventosLotesSifenSoap implements EventosLotesSifenService {

    private static final Logger log = LoggerFactory.getLogger(EventosLotesSifenSoap.class);
    static final String NS_SIFEN = "http://ekuatia.set.gov.py/sifen/xsd";
    static final String LOTE_RECIBIDO = "0300";
    static final String EVENTO_REGISTRADO = "0600";

    private final HttpClient httpClient;
    private final Duration timeout;
    private final SifenEnvioHistorial historial;
    private final String urlEvento;
    private final String urlLote;
    private final String urlConsultaLote;

    public EventosLotesSifenSoap(
            SSLContext sifenSslContext,
            SifenEnvioHistorial historial,
            @Value("${sifen.envio.url-evento}") String urlEvento,
            @Value("${sifen.envio.url-lote}") String urlLote,
            @Value("${sifen.envio.url-consulta-lote}") String urlConsultaLote,
            @Value("${sifen.envio.timeout-segundos:30}") long timeoutSegundos) {
        this.timeout = Duration.ofSeconds(timeoutSegundos);
        this.httpClient = HttpClient.newBuilder().sslContext(sifenSslContext).connectTimeout(timeout).build();
        this.historial = historial;
        this.urlEvento = urlEvento;
        this.urlLote = urlLote;
        this.urlConsultaLote = urlConsultaLote;
    }

    // --- siRecepEvento -----------------------------------------------------------------------------

    @Override
    public RespuestaEvento enviarEvento(long idEvento, String gGroupGesEveFirmado, Ambiente ambiente) {
        String dId = dId();
        String sobre = sobre("<rEnviEventoDe xmlns=\"" + NS_SIFEN + "\"><dId>" + dId + "</dId><dEvReg>"
                + sinNamespaceRepetido(gGroupGesEveFirmado) + "</dEvReg></rEnviEventoDe>");
        RespuestaEvento respuesta;
        try {
            HttpResponse<String> r = httpClient.send(peticion(urlEvento, sobre), HttpResponse.BodyHandlers.ofString());
            log.info("SIFEN siRecepEvento respondió HTTP {} para el evento {}: {}", r.statusCode(), idEvento, truncar(r.body()));
            respuesta = r.statusCode() == 200 ? parsearEvento(r.body())
                    : new RespuestaEvento(false, "ERROR_HTTP", "HTTP " + r.statusCode() + " del SIFEN: " + truncar(r.body()), null, false);
        } catch (HttpConnectTimeoutException | ConnectException | SSLHandshakeException e) {
            respuesta = new RespuestaEvento(false, "ERROR_CONEXION", "No se pudo conectar con el SIFEN: " + e, null, false);
        } catch (Exception e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            // Puede haber llegado: el reintento con el mismo evento devuelve 4003 (duplicado) si ya estaba.
            respuesta = RespuestaEvento.sinConfirmar("ERROR_TIMEOUT", "El SIFEN no respondió al evento (" + e + ")");
        }
        historial.registrar(null, ambiente, urlEvento, dId,
                "EVENTO " + idEvento + " " + (respuesta.aprobado() ? "APROBADO" : respuesta.sinConfirmar() ? "SIN_CONFIRMAR" : "RECHAZADO"),
                respuesta.codigo(), respuesta.mensaje(), null);
        return respuesta;
    }

    /**
     * El gGroupGesEve se serializa como documento propio, con {@code xmlns} del SIFEN; dentro de
     * rEnviEventoDe ese namespace ya está declarado (y el manual pide uno solo por raíz, 7.2.2). Quitarlo
     * no cambia el XML firmado: la firma cubre el rEve con exclusive C14N.
     */
    static String sinNamespaceRepetido(String gGroupGesEve) {
        return gGroupGesEve.replaceFirst("^(<gGroupGesEve)\\s+xmlns=\"" + NS_SIFEN.replace(".", "\\.") + "\"", "$1");
    }

    RespuestaEvento parsearEvento(String xml) throws Exception {
        Document doc = parsear(xml);
        XPath x = XPathFactory.newInstance().newXPath();
        String estado = texto(x, doc, "//*[local-name()='gResProcEVe']/*[local-name()='dEstRes']");
        String protocolo = texto(x, doc, "//*[local-name()='gResProcEVe']/*[local-name()='dProtAut']");
        String codigo = texto(x, doc, "//*[local-name()='gResProc']/*[local-name()='dCodRes']");
        String mensaje = texto(x, doc, "//*[local-name()='gResProc']/*[local-name()='dMsgRes']");
        if (codigo == null) codigo = texto(x, doc, "//*[local-name()='dCodRes']");
        if (mensaje == null) mensaje = texto(x, doc, "//*[local-name()='dMsgRes']");
        boolean aprobado = estado != null && estado.toLowerCase().startsWith("aprobado");
        String descripcion = (estado != null ? estado + " — " : "") + (mensaje != null ? mensaje : "sin mensaje");
        return new RespuestaEvento(aprobado, codigo, descripcion, protocolo, false);
    }

    // --- siRecepLoteDE -----------------------------------------------------------------------------

    @Override
    public RespuestaLote enviarLote(List<String> rDeFirmados, Ambiente ambiente) {
        String dId = dId();
        RespuestaLote respuesta;
        try {
            String sobre = sobre("<rEnvioLote xmlns=\"" + NS_SIFEN + "\"><dId>" + dId + "</dId><xDE>"
                    + comprimirLote(rDeFirmados) + "</xDE></rEnvioLote>");
            HttpResponse<String> r = httpClient.send(peticion(urlLote, sobre), HttpResponse.BodyHandlers.ofString());
            log.info("SIFEN siRecepLoteDE respondió HTTP {} (dId={}, {} DE)", r.statusCode(), dId, rDeFirmados.size());
            respuesta = r.statusCode() == 200 ? parsearLote(r.body())
                    : new RespuestaLote(false, "ERROR_HTTP", "HTTP " + r.statusCode() + " del SIFEN: " + truncar(r.body()), null, false);
        } catch (HttpConnectTimeoutException | ConnectException | SSLHandshakeException e) {
            respuesta = new RespuestaLote(false, "ERROR_CONEXION", "No se pudo conectar con el SIFEN: " + e, null, false);
        } catch (Exception e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            respuesta = RespuestaLote.sinConfirmar("ERROR_TIMEOUT",
                    "El SIFEN no respondió al envío del lote (" + e + "): hay que verificar cada documento por su CDC");
        }
        historial.registrar(null, ambiente, urlLote, dId,
                "LOTE " + (respuesta.recibido() ? "RECIBIDO " + respuesta.numeroLote() : respuesta.sinConfirmar() ? "SIN_CONFIRMAR" : "NO_RECIBIDO"),
                respuesta.codigo(), respuesta.mensaje(), null);
        return respuesta;
    }

    /**
     * Protocolo de procesamiento del lote (Schema XML 5A): un {@code <rLoteDE>} con los {@code <rDE>}
     * firmados tal cual (cada uno con su namespace), una sola declaración XML, comprimido en zip y en
     * base64 (BSch03 xDE).
     */
    static String comprimirLote(List<String> rDeFirmados) throws IOException {
        StringBuilder lote = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><rLoteDE>");
        for (String rDe : rDeFirmados) {
            lote.append(rDe.replaceFirst("^<\\?xml[^>]*\\?>", ""));
        }
        lote.append("</rLoteDE>");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            zip.putNextEntry(new ZipEntry("lote.xml"));
            zip.write(lote.toString().getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return Base64.getEncoder().encodeToString(bytes.toByteArray());
    }

    RespuestaLote parsearLote(String xml) throws Exception {
        Document doc = parsear(xml);
        XPath x = XPathFactory.newInstance().newXPath();
        String codigo = texto(x, doc, "//*[local-name()='dCodRes']");
        String mensaje = texto(x, doc, "//*[local-name()='dMsgRes']");
        String numero = texto(x, doc, "//*[local-name()='dProtConsLote']");
        boolean recibido = LOTE_RECIBIDO.equals(codigo) && numero != null && !numero.isBlank() && !"0".equals(numero);
        return new RespuestaLote(recibido, codigo, mensaje != null ? mensaje : "sin mensaje", recibido ? numero : null, false);
    }

    // --- siResultLoteDE ----------------------------------------------------------------------------

    @Override
    public ResultadoLote consultarLote(String numeroLote, Ambiente ambiente) {
        String dId = dId();
        String sobre = sobre("<rEnviConsLoteDe xmlns=\"" + NS_SIFEN + "\"><dId>" + dId + "</dId><dProtConsLote>"
                + numeroLote + "</dProtConsLote></rEnviConsLoteDe>");
        ResultadoLote resultado;
        try {
            HttpResponse<String> r = httpClient.send(peticion(urlConsultaLote, sobre), HttpResponse.BodyHandlers.ofString());
            log.info("SIFEN siResultLoteDE respondió HTTP {} para el lote {}", r.statusCode(), numeroLote);
            resultado = r.statusCode() == 200 ? parsearResultadoLote(r.body())
                    : new ResultadoLote("ERROR_HTTP", "HTTP " + r.statusCode() + " del SIFEN: " + truncar(r.body()), List.of(), true);
        } catch (Exception e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            resultado = new ResultadoLote("ERROR_CONEXION", "No se pudo consultar el lote en el SIFEN: " + e, List.of(), true);
        }
        historial.registrar(null, ambiente, urlConsultaLote, dId, "CONSULTA_LOTE " + numeroLote,
                resultado.codigo(), resultado.mensaje() + " (" + resultado.documentos().size() + " DE)", null);
        return resultado;
    }

    ResultadoLote parsearResultadoLote(String xml) throws Exception {
        Document doc = parsear(xml);
        XPath x = XPathFactory.newInstance().newXPath();
        String codigo = texto(x, doc, "//*[local-name()='dCodResLot']");
        String mensaje = texto(x, doc, "//*[local-name()='dMsgResLot']");
        List<ResultadoDe> documentos = new ArrayList<>();
        NodeList grupos = (NodeList) x.evaluate("//*[local-name()='gResProcLote']", doc, XPathConstants.NODESET);
        for (int i = 0; i < grupos.getLength(); i++) {
            Node g = grupos.item(i);
            String cdc = texto(x, g, "./*[local-name()='id']");
            String estado = texto(x, g, "./*[local-name()='dEstRes']");
            String cod = texto(x, g, "./*[local-name()='gResProc'][1]/*[local-name()='dCodRes']");
            String msg = texto(x, g, "./*[local-name()='gResProc'][1]/*[local-name()='dMsgRes']");
            boolean aprobado = estado != null && estado.toLowerCase().startsWith("aprobado");
            documentos.add(new ResultadoDe(cdc, aprobado, estado, cod,
                    (estado != null ? estado : "Sin dEstRes") + (msg != null ? " — " + msg : "")));
        }
        return new ResultadoLote(codigo, mensaje != null ? mensaje : "sin mensaje", documentos, false);
    }

    // --- Comunes -----------------------------------------------------------------------------------

    private static String dId() {
        return String.valueOf(System.currentTimeMillis());
    }

    static String sobre(String cuerpo) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\"><soap:Header/><soap:Body>"
                + cuerpo + "</soap:Body></soap:Envelope>";
    }

    private HttpRequest peticion(String url, String sobre) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(timeout)
                .header("Content-Type", "application/soap+xml; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(sobre, StandardCharsets.UTF_8))
                .build();
    }

    private static Document parsear(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        return factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    private static String texto(XPath x, Object contexto, String expresion) throws Exception {
        Element e = (Element) x.evaluate(expresion, contexto, XPathConstants.NODE);
        return e != null ? e.getTextContent() : null;
    }

    private static String truncar(String texto) {
        if (texto == null) return null;
        return texto.length() > 500 ? texto.substring(0, 500) + "…" : texto;
    }
}
