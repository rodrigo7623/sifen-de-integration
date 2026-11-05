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
                "https://sifen-test.set.gov.py/de/ws/consultas/consulta.wsdl",
                30);
    }

    @Test
    void construyeElSobreSoapConDIdYElRDeFirmadoSinModificarDentroDeXDE() throws Exception {
        EnviadorSifenSoapService service = nuevoServicio();
        String rDe = "<rDE xmlns=\"http://ekuatia.set.gov.py/sifen/xsd\"><dVerFor>150</dVerFor><DE Id=\"X\">contenido</DE></rDE>";

        String sobre = service.construirSobreSoap("10000011111111", rDe);

        assertThat(sobre).contains("xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\"");
        assertThat(sobre).contains("<rEnviDe xmlns=\"http://ekuatia.set.gov.py/sifen/xsd\">");
        assertThat(sobre).contains("<dId>10000011111111</dId>");
        assertThat(sobre).contains("<xDE>" + rDe + "</xDE>");
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

    // --- BUG-006: timeout sin confirmar y consulta por CDC (siConsDE) ---

    @Test
    void unTimeoutEsperandoLaRespuestaNoEsUnRechazoSinoUnEnvioSinConfirmar() throws Exception {
        // Servidor local que acepta el envío y nunca contesta: el SIFEN pudo haber recibido el DE.
        com.sun.net.httpserver.HttpServer servidor = com.sun.net.httpserver.HttpServer.create(
                new java.net.InetSocketAddress("127.0.0.1", 0), 0);
        java.util.concurrent.CountDownLatch liberar = new java.util.concurrent.CountDownLatch(1);
        servidor.createContext("/", intercambio -> {
            try {
                liberar.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            intercambio.close();
        });
        servidor.start();
        try {
            RespuestaSifenResult resultado = servicioContra("http://127.0.0.1:" + servidor.getAddress().getPort() + "/de", 1)
                    .enviar(java.util.UUID.randomUUID(), "<rDE/>", Ambiente.TEST);

            assertThat(resultado.aprobado()).isFalse();
            assertThat(resultado.sinConfirmar()).isTrue();
            assertThat(resultado.codigo()).isEqualTo("ERROR_TIMEOUT");
        } finally {
            liberar.countDown();
            servidor.stop(0);
        }
    }

    @Test
    void siNoSePudoConectarElDeNuncaSalioYEsUnRechazo() throws Exception {
        int puertoCerrado;
        try (java.net.ServerSocket socket = new java.net.ServerSocket(0)) {
            puertoCerrado = socket.getLocalPort();
        }

        RespuestaSifenResult resultado = servicioContra("http://127.0.0.1:" + puertoCerrado + "/de", 5)
                .enviar(java.util.UUID.randomUUID(), "<rDE/>", Ambiente.TEST);

        assertThat(resultado.sinConfirmar()).isFalse();
        assertThat(resultado.codigo()).isEqualTo("ERROR_CONEXION");
    }

    @Test
    void construyeElSobreDeLaConsultaPorCdc() throws Exception {
        String sobre = nuevoServicio().construirSobreConsultaDe("123", "01" + "4".repeat(42));

        assertThat(sobre).contains("<rEnviConsDeRequest xmlns=\"http://ekuatia.set.gov.py/sifen/xsd\">");
        assertThat(sobre).contains("<dId>123</dId>");
        assertThat(sobre).contains("<dCDC>01" + "4".repeat(42) + "</dCDC>");
    }

    @Test
    void laConsultaConCodigo0422SignificaQueElSifenTieneElDocumento() throws Exception {
        String cdc = "01" + "4".repeat(42);
        RespuestaSifenResult resultado = nuevoServicio().parsearConsultaDe(respuestaConsulta("0422", "CDC encontrado"), cdc);

        assertThat(resultado.aprobado()).isTrue();
        assertThat(resultado.cdc()).isEqualTo(cdc);
    }

    @Test
    void laConsultaConCodigo0420SignificaQueElDocumentoNoExiste() throws Exception {
        RespuestaSifenResult resultado = nuevoServicio().parsearConsultaDe(
                respuestaConsulta("0420", "Documento No Existe en SIFEN o ha sido Rechazado"), "01" + "4".repeat(42));

        assertThat(resultado.aprobado()).isFalse();
        assertThat(resultado.sinConfirmar()).isFalse();
    }

    @Test
    void otroCodigoDeLaConsultaNoPermiteConcluirNada() throws Exception {
        RespuestaSifenResult resultado = nuevoServicio().parsearConsultaDe(
                respuestaConsulta("0160", "XML Mal Formado."), "01" + "4".repeat(42));

        assertThat(resultado.sinConfirmar()).isTrue();
    }

    private EnviadorSifenSoapService servicioContra(String url, long timeoutSegundos) throws Exception {
        EnviadorSifenConfig config = new EnviadorSifenConfig();
        config.setUrlEndpoint(url);
        EnviadorSifenConfigRepository repositorio = mock(EnviadorSifenConfigRepository.class);
        org.mockito.Mockito.when(repositorio.findAll()).thenReturn(java.util.List.of(config));
        return new EnviadorSifenSoapService(
                SSLContext.getDefault(), repositorio, mock(SifenEnvioHistorial.class), url, timeoutSegundos);
    }

    private String respuestaConsulta(String codigo, String mensaje) {
        return "<env:Envelope xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\"><env:Body>"
                + "<ns2:rEnviConsDeResponse xmlns:ns2=\"http://ekuatia.set.gov.py/sifen/xsd\">"
                + "<ns2:dFecProc>2026-11-03T10:00:00-03:00</ns2:dFecProc>"
                + "<ns2:dCodRes>" + codigo + "</ns2:dCodRes>"
                + "<ns2:dMsgRes>" + mensaje + "</ns2:dMsgRes>"
                + "</ns2:rEnviConsDeResponse></env:Body></env:Envelope>";
    }
}
