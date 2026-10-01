package com.tottalstore.sifen.sifen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import javax.net.ssl.SSLContext;
import org.junit.jupiter.api.Test;

/**
 * Pruebas del armado del sobre SOAP y del parseo de la respuesta de {@code SiRecepDE}, sin red
 * real. Los XML de respuesta son los ejemplos literales del Manual Técnico del SIFEN v150, sección
 * 7.4 (uno de rechazo, tal cual el manual; uno de aprobación, construido con la misma estructura).
 */
class EnviadorSifenSoapServiceTest {

    private EnviadorSifenSoapService nuevoServicio() throws Exception {
        return new EnviadorSifenSoapService(
                SSLContext.getDefault(),
                mock(EnviadorSifenConfigRepository.class),
                mock(SifenEnvioHistorial.class),
                new CdcGenerator(),
                new EmisorConfig("4309652-2", 1, "001", "001"),
                30);
    }

    @Test
    void envuelveElContenidoEnRDeYDeConElCdcComoId() throws Exception {
        EnviadorSifenSoapService service = nuevoServicio();

        String de = service.envolverEnDe("0144444401700100100145282201701251587326098",
                "<algo>placeholder</algo>");

        assertThat(de).isEqualTo(
                "<rDE xmlns=\"http://ekuatia.set.gov.py/sifen/xsd\">"
                        + "<DE Id=\"0144444401700100100145282201701251587326098\">"
                        + "<algo>placeholder</algo>"
                        + "</DE></rDE>");
    }

    @Test
    void construyeElSobreSoapConDIdYElXmlDelDeAdentroDeXDE() throws Exception {
        EnviadorSifenSoapService service = nuevoServicio();

        String sobre = service.construirSobreSoap("10000011111111", "<DE Id=\"X\">contenido</DE>");

        assertThat(sobre).contains("xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\"");
        assertThat(sobre).contains("<rEnviDe xmlns=\"http://ekuatia.set.gov.py/sifen/xsd\">");
        assertThat(sobre).contains("<dId>10000011111111</dId>");
        assertThat(sobre).contains("<xDE><DE Id=\"X\">contenido</DE></xDE>");
    }

    @Test
    void parseaUnaRespuestaDeRechazo_EjemploLiteralDelManual() throws Exception {
        // Ejemplo literal del Manual Técnico v150, pág. 37.
        String xml = "<env:Envelope xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\">"
                + "<env:Header/>"
                + "<env:body>"
                + "<ns2:rRetEnviDe xmlns:ns2=\"http://ekuatia.set.gov.py/sifen/xsd\">"
                + "<ns2:rProtDe>"
                + "<ns2:dId>00000000000000000000000000000000000000000000</ns2:dId>"
                + "<ns2:dFecProc>2019-06-03T12:00:00</ns2:dFecProc>"
                + "<ns2:dDigVal>0000000000000000000000000000</ns2:dDigVal>"
                + "<ns2:gResProc>"
                + "<ns2:dEstRes>Rechazado</ns2:dEstRes>"
                + "<ns2:dProtAut>0000000000</ns2:dProtAut>"
                + "<ns2:dCodRes>0160</ns2:dCodRes>"
                + "<ns2:dMsgRes>XML malformado</ns2:dMsgRes>"
                + "</ns2:gResProc>"
                + "</ns2:rProtDe>"
                + "</ns2:rRetEnviDe>"
                + "</env:body>"
                + "</env:Envelope>";

        EnviadorSifenSoapService service = nuevoServicio();
        RespuestaSifenResult resultado = service.parsearRespuesta(xml);

        assertThat(resultado.aprobado()).isFalse();
        assertThat(resultado.codigo()).isEqualTo("0160");
        assertThat(resultado.descripcion()).contains("Rechazado").contains("XML malformado");
        assertThat(resultado.cdc()).isNull();
    }

    @Test
    void parseaUnaRespuestaDeAprobacionYExtraeElCdc() throws Exception {
        String cdcEsperado = "0144444401700100100145282201170125158732260988";
        String xml = "<env:Envelope xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\">"
                + "<env:body>"
                + "<ns2:rRetEnviDe xmlns:ns2=\"http://ekuatia.set.gov.py/sifen/xsd\">"
                + "<ns2:rProtDe>"
                + "<ns2:dId>" + cdcEsperado + "</ns2:dId>"
                + "<ns2:dFecProc>2026-09-22T12:00:00</ns2:dFecProc>"
                + "<ns2:dDigVal>ABC</ns2:dDigVal>"
                + "<ns2:gResProc>"
                + "<ns2:dEstRes>Aprobado</ns2:dEstRes>"
                + "<ns2:dProtAut>1234567890</ns2:dProtAut>"
                + "<ns2:dCodRes>0260</ns2:dCodRes>"
                + "<ns2:dMsgRes>Autorizado</ns2:dMsgRes>"
                + "</ns2:gResProc>"
                + "</ns2:rProtDe>"
                + "</ns2:rRetEnviDe>"
                + "</env:body>"
                + "</env:Envelope>";

        EnviadorSifenSoapService service = nuevoServicio();
        RespuestaSifenResult resultado = service.parsearRespuesta(xml);

        assertThat(resultado.aprobado()).isTrue();
        assertThat(resultado.codigo()).isEqualTo("0260");
        assertThat(resultado.cdc()).isEqualTo(cdcEsperado);
    }
}
