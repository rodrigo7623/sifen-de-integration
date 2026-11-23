package com.tottalstore.sifen.sifen.dte;

import com.tottalstore.sifen.firma.GeneradorEventoService;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.xml.parsers.DocumentBuilderFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Eventos del emisor según el Manual Técnico v150, 11.5 (Schema XML 19, Evento_v150.xsd) y 11.5.1.
 * Estructura: {@code gGroupGesEve > rGesEve > (rEve[Id] + Signature)}, con {@code rEve} =
 * {@code dFecFirma, dVerFor, gGroupTiEvt (rGeVeCan | rGeVeInu)}.
 *
 * <p>Usa el mismo {@link FirmadorXmlDsig} que el DE: referencia al {@code Id} del {@code rEve},
 * enveloped + exclusive C14N, RSA-SHA256; la {@code Signature} queda como hermana siguiente del
 * {@code rEve}, igual que la del DE dentro del {@code rDE}.
 */
@Component
@ConditionalOnProperty(name = "sifen.envio.modo", havingValue = "real")
public class GeneradorEventoSifen implements GeneradorEventoService {

    static final String NS_XSI = "http://www.w3.org/2001/XMLSchema-instance";
    private static final String XMLNS = "http://www.w3.org/2000/xmlns/";
    private static final String SCHEMA_LOCATION = "http://ekuatia.set.gov.py/sifen/xsd siRecepEvento_v150.xsd";
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final FirmadorXmlDsig firmador;

    public GeneradorEventoSifen(PrivateKey sifenPrivateKey, X509Certificate sifenCertificado) {
        this.firmador = new FirmadorXmlDsig(sifenPrivateKey, sifenCertificado);
    }

    @Override
    public String cancelacion(long idEvento, LocalDateTime fechaFirma, String cdc, String motivo) {
        Evento evento = new Evento(idEvento, fechaFirma);
        Element can = evento.el(evento.gGroupTiEvt, "rGeVeCan");
        evento.el(can, "Id", cdc);
        evento.el(can, "mOtEve", motivo);
        return evento.firmar();
    }

    @Override
    public String inutilizacion(long idEvento, LocalDateTime fechaFirma, Inutilizacion d) {
        Evento evento = new Evento(idEvento, fechaFirma);
        Element inu = evento.el(evento.gGroupTiEvt, "rGeVeInu");
        evento.el(inu, "dNumTim", d.timbrado());
        evento.el(inu, "dEst", ceros(d.establecimiento(), 3));
        evento.el(inu, "dPunExp", ceros(d.puntoExpedicion(), 3));
        evento.el(inu, "dNumIn", ceros(String.valueOf(d.desde()), 7));
        evento.el(inu, "dNumFin", ceros(String.valueOf(d.hasta()), 7));
        evento.el(inu, "iTiDE", String.valueOf(d.tipoDocumento()));
        evento.el(inu, "mOtEve", d.motivo());
        return evento.firmar();
    }

    static String ceros(String valor, int largo) {
        return String.format("%" + largo + "s", valor).replace(' ', '0');
    }

    private final class Evento {
        private final Document doc;
        private final Element rGesEve;
        private final String id;
        private final Element gGroupTiEvt;

        private Evento(long idEvento, LocalDateTime fechaFirma) {
            try {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setNamespaceAware(true);
                doc = factory.newDocumentBuilder().newDocument();
            } catch (Exception e) {
                throw new IllegalStateException("No se pudo armar el XML del evento " + idEvento, e);
            }
            id = String.valueOf(idEvento);
            Element raiz = doc.createElementNS(DeXmlBuilder.NS_SIFEN, "gGroupGesEve");
            raiz.setAttributeNS(XMLNS, "xmlns:xsi", NS_XSI);
            raiz.setAttributeNS(NS_XSI, "xsi:schemaLocation", SCHEMA_LOCATION);
            doc.appendChild(raiz);
            // El prefijo xsi se declara también en rGesEve: el SIFEN lo procesa por separado (sin esto
            // responde 0160 "XML mal formado" sin llegar a leer el Id del evento).
            rGesEve = el(raiz, "rGesEve");
            rGesEve.setAttributeNS(XMLNS, "xmlns:xsi", NS_XSI);
            rGesEve.setAttributeNS(NS_XSI, "xsi:schemaLocation", SCHEMA_LOCATION);
            Element rEve = el(rGesEve, "rEve");
            rEve.setAttribute("Id", id);
            rEve.setIdAttribute("Id", true);
            el(rEve, "dFecFirma", FECHA_HORA.format(fechaFirma));
            el(rEve, "dVerFor", "150");
            gGroupTiEvt = el(rEve, "gGroupTiEvt");
        }

        private Element el(Element padre, String nombre) {
            Element e = doc.createElementNS(DeXmlBuilder.NS_SIFEN, nombre);
            padre.appendChild(e);
            return e;
        }

        private Element el(Element padre, String nombre, String texto) {
            Element e = el(padre, nombre);
            e.setTextContent(texto);
            return e;
        }

        private String firmar() {
            firmador.firmar(rGesEve, id);
            return GeneradorDteSifen.serializar(doc);
        }
    }
}
