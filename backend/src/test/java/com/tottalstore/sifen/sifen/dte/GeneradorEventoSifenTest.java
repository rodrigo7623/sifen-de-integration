package com.tottalstore.sifen.sifen.dte;

import static org.assertj.core.api.Assertions.assertThat;

import com.tottalstore.sifen.firma.GeneradorEventoService.Inutilizacion;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/** Eventos del emisor (Manual Técnico v150, 11.5 y 11.5.1): estructura y firma del rEve. */
class GeneradorEventoSifenTest {

    private static final String NS = DeXmlBuilder.NS_SIFEN;
    private static final String NS_DSIG = "http://www.w3.org/2000/09/xmldsig#";
    private static final LocalDateTime FIRMA = LocalDateTime.of(2026, 11, 23, 10, 15, 30);
    private static final String CDC = "01800160967001001000000122026112319012345678";

    private X509Certificate certificado;
    private GeneradorEventoSifen generador;

    @BeforeEach
    void setUp() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (InputStream in = getClass().getResourceAsStream("/firma-prueba.p12")) {
            keyStore.load(in, "prueba123".toCharArray());
        }
        PrivateKey clave = (PrivateKey) keyStore.getKey("prueba", "prueba123".toCharArray());
        certificado = (X509Certificate) keyStore.getCertificate("prueba");
        generador = new GeneradorEventoSifen(clave, certificado);
    }

    @Test
    void laCancelacionLlevaElCdcYElMotivoDentroDelREveFirmado() throws Exception {
        Document doc = parsear(generador.cancelacion(17, FIRMA, CDC, "Error en el precio del ítem"));

        Element raiz = doc.getDocumentElement();
        assertThat(raiz.getLocalName()).isEqualTo("gGroupGesEve");
        assertThat(raiz.getNamespaceURI()).isEqualTo(NS);
        Element rEve = (Element) doc.getElementsByTagNameNS(NS, "rEve").item(0);
        assertThat(rEve.getAttribute("Id")).isEqualTo("17");
        assertThat(rEve.getParentNode().getLocalName()).isEqualTo("rGesEve");
        assertThat(texto(doc, "dFecFirma")).isEqualTo("2026-11-23T10:15:30");
        assertThat(texto(doc, "dVerFor")).isEqualTo("150");
        Element can = (Element) doc.getElementsByTagNameNS(NS, "rGeVeCan").item(0);
        assertThat(can.getParentNode().getLocalName()).isEqualTo("gGroupTiEvt");
        assertThat(texto(doc, "Id")).isEqualTo(CDC);
        assertThat(texto(doc, "mOtEve")).isEqualTo("Error en el precio del ítem");

        // La firma es hermana siguiente del rEve, como la del DE dentro del rDE.
        Node firma = rEve.getNextSibling();
        assertThat(firma.getLocalName()).isEqualTo("Signature");
        assertThat(firma.getNamespaceURI()).isEqualTo(NS_DSIG);
    }

    @Test
    void laFirmaDelEventoEsValidaYReferenciaAlIdDelREve() throws Exception {
        Document doc = parsear(generador.cancelacion(42, FIRMA, CDC, "Operación duplicada"));
        Element rEve = (Element) doc.getElementsByTagNameNS(NS, "rEve").item(0);
        rEve.setIdAttribute("Id", true);

        Node nodoFirma = doc.getElementsByTagNameNS(NS_DSIG, "Signature").item(0);
        DOMValidateContext contexto = new DOMValidateContext(certificado.getPublicKey(), nodoFirma);
        XMLSignature firma = XMLSignatureFactory.getInstance("DOM").unmarshalXMLSignature(contexto);

        assertThat(firma.validate(contexto)).isTrue();
        assertThat(firma.getSignedInfo().getReferences().get(0).getURI()).isEqualTo("#42");
    }

    @Test
    void laInutilizacionCompletaConCerosEstablecimientoPuntoYNumeros() throws Exception {
        Document doc = parsear(generador.inutilizacion(5, FIRMA,
                new Inutilizacion("04309652", "1", "1", 12, 12, 1, "Documento rechazado que no se reenvía")));

        assertThat(doc.getElementsByTagNameNS(NS, "rGeVeInu").getLength()).isEqualTo(1);
        assertThat(texto(doc, "dNumTim")).isEqualTo("04309652");
        assertThat(texto(doc, "dEst")).isEqualTo("001");
        assertThat(texto(doc, "dPunExp")).isEqualTo("001");
        assertThat(texto(doc, "dNumIn")).isEqualTo("0000012");
        assertThat(texto(doc, "dNumFin")).isEqualTo("0000012");
        assertThat(texto(doc, "iTiDE")).isEqualTo("1");
        assertThat(texto(doc, "mOtEve")).isEqualTo("Documento rechazado que no se reenvía");
    }

    private static Document parsear(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        return factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    private static String texto(Document doc, String nombre) {
        return doc.getElementsByTagNameNS(NS, nombre).item(0).getTextContent();
    }
}
