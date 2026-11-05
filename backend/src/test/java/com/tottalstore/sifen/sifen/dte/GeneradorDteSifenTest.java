package com.tottalstore.sifen.sifen.dte;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tottalstore.sifen.clientes.Cliente;
import com.tottalstore.sifen.common.BusinessException;
import com.tottalstore.sifen.configuracion.ConfiguracionEmisor;
import com.tottalstore.sifen.configuracion.ConfiguracionEmisorRepository;
import com.tottalstore.sifen.establecimientos.Establecimiento;
import com.tottalstore.sifen.establecimientos.PuntoExpedicion;
import com.tottalstore.sifen.configuracion.ConfiguracionEmisorService;
import com.tottalstore.sifen.facturacion.CondicionPago;
import com.tottalstore.sifen.facturacion.EstadoDte;
import com.tottalstore.sifen.facturacion.FacturaElectronica;
import com.tottalstore.sifen.facturacion.FacturaPdfGenerator;
import com.tottalstore.sifen.facturacion.ItemFactura;
import com.tottalstore.sifen.facturacion.MotivoNota;
import com.tottalstore.sifen.facturacion.TipoDocumento;
import com.tottalstore.sifen.facturacion.kude.KudeDatos;
import com.tottalstore.sifen.facturacion.kude.KudeDatosFactory;
import com.tottalstore.sifen.sifen.RespuestaSifen;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import com.tottalstore.sifen.firma.DteFirmado;
import com.tottalstore.sifen.shared.CondicionIva;
import com.tottalstore.sifen.shared.TasaIva;
import com.tottalstore.sifen.sifen.Ambiente;
import com.tottalstore.sifen.sifen.CdcGenerator;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Generación real del DE: estructura según el Manual Técnico v150, firma XMLDSig verificable con la
 * clave pública del certificado y QR. Usa un certificado autofirmado de prueba
 * ({@code src/test/resources/firma-prueba.p12}), no el del contribuyente.
 */
class GeneradorDteSifenTest {

    private static final String NS = DeXmlBuilder.NS_SIFEN;
    private static final String NS_DSIG = "http://www.w3.org/2000/09/xmldsig#";

    private ConfiguracionEmisorRepository repositorio;
    private X509Certificate certificado;
    private GeneradorDteSifen generador;

    @BeforeEach
    void setUp() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (InputStream in = getClass().getResourceAsStream("/firma-prueba.p12")) {
            keyStore.load(in, "prueba123".toCharArray());
        }
        PrivateKey clave = (PrivateKey) keyStore.getKey("prueba", "prueba123".toCharArray());
        certificado = (X509Certificate) keyStore.getCertificate("prueba");

        repositorio = mock(ConfiguracionEmisorRepository.class);
        when(repositorio.findAll()).thenReturn(List.of(emisorCompleto()));
        generador = new GeneradorDteSifen(repositorio, new CdcGenerator(), clave, certificado,
                "0001", "ABCD0000000000000000000000000000");
    }

    @Test
    void generaUnRDeConLaEstructuraDelManualYElCdcComoIdDelDe() throws Exception {
        DteFirmado dte = generador.generar(factura(), Ambiente.TEST);
        Document doc = parsear(dte.xmlFirmado());
        Element rDE = doc.getDocumentElement();

        assertThat(dte.xmlFirmado()).startsWith("<rDE xmlns=\"" + NS + "\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
                + " xsi:schemaLocation=\"" + NS + " siRecepDE_v150.xsd\">");
        assertThat(rDE.getLocalName()).isEqualTo("rDE");
        assertThat(rDE.getNamespaceURI()).isEqualTo(NS);
        assertThat(nombresHijos(rDE)).containsExactly("dVerFor", "DE", "Signature", "gCamFuFD");
        assertThat(texto(doc, "dVerFor")).isEqualTo("150");

        Element de = (Element) doc.getElementsByTagNameNS(NS, "DE").item(0);
        assertThat(dte.cdc()).hasSize(44);
        assertThat(de.getAttribute("Id")).isEqualTo(dte.cdc());
        assertThat(nombresHijos(de)).containsExactly(
                "dDVId", "dFecFirma", "dSisFact", "gOpeDE", "gTimb", "gDatGralOpe", "gDtipDE", "gTotSub");
        assertThat(texto(doc, "dDVId")).isEqualTo(dte.cdc().substring(43));
        assertThat(texto(doc, "dCodSeg")).isEqualTo(dte.codigoSeguridad());
        // El CDC contiene establecimiento-punto-número de la factura (no los de una variable de entorno).
        assertThat(dte.cdc().substring(11, 24)).isEqualTo("0020030000042");
        assertThat(texto(doc, "dNumDoc")).isEqualTo("0000042");
    }

    @Test
    void enAmbienteDeTestElNombreDelEmisorEsElLiteralExigidoPorElManual() throws Exception {
        Document test = parsear(generador.generar(factura(), Ambiente.TEST).xmlFirmado());
        Document produccion = parsear(generador.generar(factura(), Ambiente.PRODUCCION).xmlFirmado());

        assertThat(texto(test, "dNomEmi")).isEqualTo("DE generado en ambiente de prueba - sin valor comercial ni fiscal");
        assertThat(texto(produccion, "dNomEmi")).isEqualTo("TOTTAL STORE");
    }

    @Test
    void unaOcurrenciaDeGActEcoPorActividadConLaPrincipalPrimero() throws Exception {
        ConfiguracionEmisor emisor = emisorCompleto();
        emisor.getActividadesEconomicas().add(new com.tottalstore.sifen.configuracion.ActividadEconomicaEmisor(
                emisor, "46900", "Venta al por mayor", false, 0));
        emisor.getActividadesEconomicas().add(new com.tottalstore.sifen.configuracion.ActividadEconomicaEmisor(
                emisor, "47521", "Ferretería", true, 1));
        when(repositorio.findAll()).thenReturn(List.of(emisor));

        Document doc = parsear(generador.generar(factura(), Ambiente.TEST).xmlFirmado());

        var codigos = doc.getElementsByTagNameNS(NS, "cActEco");
        assertThat(codigos.getLength()).isEqualTo(2);
        assertThat(codigos.item(0).getTextContent()).isEqualTo("47521");
        assertThat(codigos.item(1).getTextContent()).isEqualTo("46900");
    }

    @Test
    void calculaTotalesEIvaComoIndicaElManual() throws Exception {
        Document doc = parsear(generador.generar(factura(), Ambiente.TEST).xmlFirmado());

        // 2 × 11.000 al 10% + 1 × 21.000 al 5% + 1 × 5.000 exento
        assertThat(texto(doc, "dSub10")).isEqualTo("22000");
        assertThat(texto(doc, "dSub5")).isEqualTo("21000");
        assertThat(texto(doc, "dSubExe")).isEqualTo("5000");
        assertThat(texto(doc, "dTotGralOpe")).isEqualTo("48000");
        assertThat(texto(doc, "dIVA10")).isEqualTo("2000");
        assertThat(texto(doc, "dIVA5")).isEqualTo("1000");
        assertThat(texto(doc, "dTotIVA")).isEqualTo("3000");
        assertThat(texto(doc, "dBaseGrav10")).isEqualTo("20000");
        assertThat(texto(doc, "dBaseGrav5")).isEqualTo("20000");
        assertThat(texto(doc, "dMonTiPag")).isEqualTo("48000");
        assertThat(doc.getElementsByTagNameNS(NS, "gCamItem").getLength()).isEqualTo(3);
    }

    @Test
    void laFirmaEsValidaConLaClavePublicaDelCertificadoYNoTieneSaltosDeLinea() throws Exception {
        DteFirmado dte = generador.generar(factura(), Ambiente.TEST);
        Document doc = parsear(dte.xmlFirmado());
        Element de = (Element) doc.getElementsByTagNameNS(NS, "DE").item(0);
        de.setIdAttribute("Id", true);

        Node nodoFirma = doc.getElementsByTagNameNS(NS_DSIG, "Signature").item(0);
        DOMValidateContext contexto = new DOMValidateContext(certificado.getPublicKey(), nodoFirma);
        XMLSignature firma = XMLSignatureFactory.getInstance("DOM").unmarshalXMLSignature(contexto);

        assertThat(firma.validate(contexto)).isTrue();
        assertThat(firma.getSignedInfo().getReferences().get(0).getURI()).isEqualTo("#" + dte.cdc());
        assertThat(dte.xmlFirmado()).doesNotContain("&#13;").doesNotContain("\n").doesNotStartWith("<?xml");
        assertThat(dte.xmlFirmado()).contains("<Signature xmlns=\"" + NS_DSIG + "\">");
    }

    @Test
    void elQrIncluyeElDigestValueDeLaFirmaYApuntaAlAmbienteDeTest() throws Exception {
        DteFirmado dte = generador.generar(factura(), Ambiente.TEST);
        Document doc = parsear(dte.xmlFirmado());
        String digest = doc.getElementsByTagNameNS(NS_DSIG, "DigestValue").item(0).getTextContent();

        String qr = texto(doc, "dCarQR");

        assertThat(qr).startsWith("https://ekuatia.set.gov.py/consultas-test/qr?nVersion=150&Id=" + dte.cdc());
        assertThat(qr).contains("&dRucRec=80012345&dTotGralOpe=48000&dTotIVA=3000&cItems=3"
                + "&DigestValue=" + CodigoQrSifen.hex(digest) + "&IdCSC=0001&cHashQR=");
        assertThat(qr).doesNotContain("ABCD0000000000000000000000000000");
    }

    @Test
    void elHashDelQrCoincideConElEjemploDelManual() {
        // Manual Técnico v150, sección 13.8.4.
        String qr = CodigoQrSifen.generar(Ambiente.PRODUCCION,
                "01444444017001001001452822017012515873260988",
                "2017-01-25T09:35:17", "88899990", "300000", "27272", 2,
                "yzGYhUx1/XYYzksWB+fPR3Qc50c=", "0001", "ABCD0000000000000000000000000000");

        assertThat(qr).isEqualTo("https://ekuatia.set.gov.py/consultas/qr?nVersion=150"
                + "&Id=01444444017001001001452822017012515873260988"
                + "&dFeEmiDE=323031372d30312d32355430393a33353a3137"
                + "&dRucRec=88899990&dTotGralOpe=300000&dTotIVA=27272&cItems=2"
                + "&DigestValue=797a4759685578312f5859597a6b7357422b6650523351633530633d"
                + "&IdCSC=0001&cHashQR=97ddbb3c1e7d65af03a70ffe21f2b34846ab1c89e0566c35222086766b7374ed");
    }

    @Test
    void receptorConsumidorFinalSeInformaConCedula() throws Exception {
        FacturaElectronica factura = factura();
        factura.getCliente().setRuc("1234567");
        factura.getCliente().setCondicionIva(CondicionIva.CONSUMIDOR_FINAL);

        Document doc = parsear(generador.generar(factura, Ambiente.TEST).xmlFirmado());

        assertThat(texto(doc, "iNatRec")).isEqualTo("2");
        assertThat(texto(doc, "iTiOpe")).isEqualTo("2");
        assertThat(texto(doc, "dNumIDRec")).isEqualTo("1234567");
        assertThat(doc.getElementsByTagNameNS(NS, "dRucRec").getLength()).isZero();
    }

    @Test
    void escapaCaracteresEspecialesEnLasDescripciones() throws Exception {
        FacturaElectronica factura = factura();
        factura.getItems().get(0).setDescripcion("Llave <12mm> & tuerca");

        DteFirmado dte = generador.generar(factura, Ambiente.TEST);

        assertThat(dte.xmlFirmado()).contains("Llave &lt;12mm&gt; &amp; tuerca");
        assertThat(texto(parsear(dte.xmlFirmado()), "dDesProSer")).isEqualTo("Llave <12mm> & tuerca");
    }

    @Test
    void noGeneraElDeSiFaltanDatosDelEmisor() {
        ConfiguracionEmisor incompleto = emisorCompleto();
        incompleto.setTimbradoNumero(null);
        incompleto.setActividadEconomicaCodigo(" ");
        when(repositorio.findAll()).thenReturn(List.of(incompleto));

        assertThatThrownBy(() -> generador.generar(factura(), Ambiente.TEST))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("timbrado")
                .hasMessageContaining("actividad económica");
    }

    @Test
    void elKudeSeArmaDesdeElXmlFirmadoConNumeroTimbradoCdcYQr() throws Exception {
        FacturaElectronica factura = factura();
        DteFirmado dte = generador.generar(factura, Ambiente.TEST);

        KudeDatos kude = KudeDatosFactory.desdeXml(dte.xmlFirmado());

        assertThat(kude.desdeXmlFirmado()).isTrue();
        assertThat(kude.numero()).isEqualTo("002-003-0000042");
        assertThat(kude.timbrado()).isEqualTo("12345678");
        assertThat(kude.timbradoInicio()).isEqualTo("01/10/2026");
        assertThat(kude.emisorRuc()).isEqualTo("4309652-2");
        assertThat(kude.fechaEmision()).isEqualTo("29/10/2026 10:00:00");
        assertThat(kude.receptorDocumento()).isEqualTo("80012345-0");
        assertThat(kude.cdc()).isEqualTo(dte.cdc());
        assertThat(kude.cdcAgrupado()).hasSize(44 + 10);
        assertThat(kude.urlQr()).startsWith("https://ekuatia.set.gov.py/consultas-test/qr?");
        assertThat(kude.ambienteTest()).isTrue();
        assertThat(kude.items()).hasSize(3);
        assertThat(kude.items().get(0).gravado10()).isEqualByComparingTo("22000");
        assertThat(kude.items().get(1).gravado5()).isEqualByComparingTo("21000");
        assertThat(kude.items().get(2).exentas()).isEqualByComparingTo("5000");
        assertThat(kude.totalIva()).isEqualByComparingTo("3000");

        RespuestaSifen respuesta = new RespuestaSifen();
        respuesta.setXmlFirmado(dte.xmlFirmado());
        factura.setRespuestaSifen(respuesta);
        factura.setEstadoDte(EstadoDte.APROBADO);
        byte[] pdf = new FacturaPdfGenerator(mock(ConfiguracionEmisorService.class)).generar(factura);

        try (PDDocument doc = Loader.loadPDF(pdf)) {
            String texto = new PDFTextStripper().getText(doc);
            assertThat(texto)
                    .contains("FACTURA ELECTRÓNICA")
                    .contains("N° 002-003-0000042")
                    .contains("Timbrado N° 12345678")
                    .contains("CDC: " + kude.cdcAgrupado())
                    .contains("https://ekuatia.set.gov.py/consultas-test/")
                    .contains("TOTAL IVA: 3.000")
                    .contains("REPRESENTACIÓN GRÁFICA")
                    .doesNotContain("Sin validez");
        }
    }

    @Test
    void laNotaDeCreditoLlevaMotivoYDocumentoAsociadoSinCondicionNiTipoDeTransaccion() throws Exception {
        String cdcFactura = "01043096522002003000004212026102911234567892";
        FacturaElectronica factura = factura();
        factura.setCdc(cdcFactura);
        FacturaElectronica nota = factura();
        nota.setTipoDoc(TipoDocumento.NC);
        nota.setCondicionPago(null);
        nota.setMotivoEmision(MotivoNota.DEVOLUCION);
        nota.setFacturaAsociada(factura);
        nota.setNumeroDocumento(7);

        DteFirmado dte = generador.generar(nota, Ambiente.TEST);
        Document doc = parsear(dte.xmlFirmado());

        // CDC: tipo de documento 05 (sección 10.1) y numeración propia de la nota.
        assertThat(dte.cdc()).startsWith("05");
        assertThat(texto(doc, "dNumDoc")).isEqualTo("0000007");
        assertThat(texto(doc, "iTiDE")).isEqualTo("5");
        assertThat(texto(doc, "dDesTiDE")).isEqualTo("Nota de crédito electrónica");

        Element de = (Element) doc.getElementsByTagNameNS(NS, "DE").item(0);
        assertThat(nombresHijos(de)).containsExactly(
                "dDVId", "dFecFirma", "dSisFact", "gOpeDE", "gTimb", "gDatGralOpe", "gDtipDE", "gTotSub", "gCamDEAsoc");
        Element gDtipDE = (Element) doc.getElementsByTagNameNS(NS, "gDtipDE").item(0);
        assertThat(nombresHijos(gDtipDE)).containsExactly("gCamNCDE", "gCamItem", "gCamItem", "gCamItem");
        assertThat(texto(doc, "iMotEmi")).isEqualTo("2");
        assertThat(texto(doc, "dDesMotEmi")).isEqualTo("Devolución");
        // Rechazos 1501 y D011: las notas no informan condición de la operación ni tipo de transacción.
        assertThat(doc.getElementsByTagNameNS(NS, "gCamCond").getLength()).isZero();
        assertThat(doc.getElementsByTagNameNS(NS, "iTipTra").getLength()).isZero();
        assertThat(doc.getElementsByTagNameNS(NS, "gCamFE").getLength()).isZero();

        Element asoc = (Element) doc.getElementsByTagNameNS(NS, "gCamDEAsoc").item(0);
        assertThat(nombresHijos(asoc)).containsExactly("iTipDocAso", "dDesTipDocAso", "dCdCDERef");
        assertThat(texto(doc, "iTipDocAso")).isEqualTo("1");
        assertThat(texto(doc, "dDesTipDocAso")).isEqualTo("Electrónico");
        assertThat(texto(doc, "dCdCDERef")).isEqualTo(cdcFactura);

        KudeDatos kude = KudeDatosFactory.desdeXml(dte.xmlFirmado());
        assertThat(kude.tipoDocumento()).isEqualTo("Nota de crédito electrónica");
        assertThat(kude.motivoEmision()).isEqualTo("Devolución");
        assertThat(kude.cdcDocumentoAsociado()).isEqualTo(cdcFactura);
        assertThat(kude.condicion()).isNull();

        RespuestaSifen respuesta = new RespuestaSifen();
        respuesta.setXmlFirmado(dte.xmlFirmado());
        nota.setRespuestaSifen(respuesta);
        nota.setEstadoDte(EstadoDte.APROBADO);
        byte[] pdf = new FacturaPdfGenerator(mock(ConfiguracionEmisorService.class)).generar(nota);
        try (PDDocument pdfDoc = Loader.loadPDF(pdf)) {
            String texto = new PDFTextStripper().getText(pdfDoc);
            assertThat(texto)
                    .contains("NOTA DE CRÉDITO ELECTRÓNICA")
                    .contains("Motivo de emisión: Devolución")
                    .contains("Documento asociado: Factura electrónica · CDC " + kude.cdcAgrupadoAsociado())
                    .contains("Consulte la validez de esta Nota de crédito electrónica")
                    .doesNotContain("Condición de venta");
        }
    }

    @Test
    void tipoDeContribuyenteDelReceptorSegunElRango80Millones() {
        assertThat(DeXmlBuilder.tipoContribuyenteReceptor("80012345")).isEqualTo("2");
        assertThat(DeXmlBuilder.tipoContribuyenteReceptor("4309652")).isEqualTo("1");
    }

    private FacturaElectronica factura() {
        Establecimiento establecimiento = new Establecimiento();
        establecimiento.setId(UUID.randomUUID());
        establecimiento.setCodigo("002");
        PuntoExpedicion punto = new PuntoExpedicion();
        punto.setId(UUID.randomUUID());
        punto.setCodigo("003");
        punto.setEstablecimiento(establecimiento);

        Cliente cliente = new Cliente();
        cliente.setRuc("80012345-0");
        cliente.setRazonSocial("Cliente de prueba S.A.");
        cliente.setCondicionIva(CondicionIva.RESPONSABLE_IVA);

        FacturaElectronica factura = new FacturaElectronica();
        factura.setId(UUID.randomUUID());
        factura.setEstablecimiento(establecimiento);
        factura.setPuntoExpedicion(punto);
        factura.setCliente(cliente);
        factura.setCondicionPago(CondicionPago.CONTADO);
        factura.setNumeroDocumento(42);
        factura.setFechaEmision(Instant.parse("2026-10-29T13:00:00Z"));
        factura.getItems().add(item("Taladro", 2, "11000", TasaIva.DIEZ));
        factura.getItems().add(item("Repuesto", 1, "21000", TasaIva.CINCO));
        factura.getItems().add(item("Servicio exento", 1, "5000", TasaIva.EXENTA));
        return factura;
    }

    private ItemFactura item(String descripcion, int cantidad, String precio, TasaIva tasa) {
        ItemFactura item = new ItemFactura();
        item.setDescripcion(descripcion);
        item.setCantidad(cantidad);
        item.setPrecioUnitario(new BigDecimal(precio).setScale(2));
        item.setTasaIva(tasa);
        return item;
    }

    private ConfiguracionEmisor emisorCompleto() {
        ConfiguracionEmisor e = new ConfiguracionEmisor();
        e.setRucBase("4309652");
        e.setDvRuc(2);
        e.setTipoContribuyente(1);
        e.setRazonSocial("TOTTAL STORE");
        e.setDireccion("Avda. Mariscal López");
        e.setNumeroCasa("1234");
        e.setDepartamentoCodigo("11");
        e.setDepartamentoDescripcion("CENTRAL");
        e.setCiudadCodigo("6106");
        e.setCiudadDescripcion("SAN LORENZO");
        e.setTelefono("021123456");
        e.setEmail("ventas@tottalstore.com");
        e.setActividadEconomicaCodigo("47521");
        e.setActividadEconomicaDescripcion("Comercio al por menor de artículos de ferretería");
        e.setTimbradoNumero("12345678");
        e.setTimbradoFechaInicio(LocalDate.of(2026, 10, 1));
        return e;
    }

    private static Document parsear(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        return factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    private static String texto(Document doc, String elemento) {
        Node nodo = doc.getElementsByTagNameNS(NS, elemento).item(0);
        return nodo != null ? nodo.getTextContent() : null;
    }

    private static List<String> nombresHijos(Element padre) {
        List<String> nombres = new java.util.ArrayList<>();
        for (Node n = padre.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                nombres.add(n.getLocalName());
            }
        }
        return nombres;
    }
}
