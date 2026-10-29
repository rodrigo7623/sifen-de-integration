package com.tottalstore.sifen.sifen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import javax.net.ssl.SSLContext;
import org.junit.jupiter.api.Test;

/**
 * Pruebas del armado del sobre y del parseo de la respuesta de {@code siConsRUC}, sin red real.
 * Estructura tomada del Manual Técnico del SIFEN v150, sección 9.6 (Schemas XML 15, 16 y 17).
 */
class ConsultaRucSifenServiceTest {

    private ConsultaRucSifenService nuevoServicio() throws Exception {
        return new ConsultaRucSifenService(
                SSLContext.getDefault(),
                mock(SifenEnvioHistorial.class),
                mock(EnviadorSifenConfigRepository.class),
                "https://sifen-test.set.gov.py/de/ws/consultas/consulta-ruc.wsdl",
                30);
    }

    @Test
    void construyeElSobreConDIdYRucSinDigitoVerificador() throws Exception {
        String sobre = nuevoServicio().construirSobreSoap("1730000000000", "80012345");

        assertThat(sobre).contains("xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\"");
        assertThat(sobre).contains("<rEnviConsRUC xmlns=\"http://ekuatia.set.gov.py/sifen/xsd\">");
        assertThat(sobre).contains("<dId>1730000000000</dId>");
        assertThat(sobre).contains("<dRUCCons>80012345</dRUCCons>");
    }

    @Test
    void quitaElDigitoVerificadorYValidaLongitud() {
        assertThat(ConsultaRucSifenService.sinDigitoVerificador("80012345-6")).isEqualTo("80012345");
        assertThat(ConsultaRucSifenService.sinDigitoVerificador(" 4309652 ")).isEqualTo("4309652");
        assertThatThrownBy(() -> ConsultaRucSifenService.sinDigitoVerificador("123-4"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ConsultaRucSifenService.sinDigitoVerificador(" "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void parseaRucEncontrado_0502_conElContenedorDeDatos() throws Exception {
        String xml = "<env:Envelope xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\">"
                + "<env:Body>"
                + "<ns2:rResEnviConsRUC xmlns:ns2=\"http://ekuatia.set.gov.py/sifen/xsd\">"
                + "<ns2:dCodRes>0502</ns2:dCodRes>"
                + "<ns2:dMsgRes>RUC encontrado</ns2:dMsgRes>"
                + "<ns2:xContRUC>"
                + "<ns2:dRUCCons>80012345</ns2:dRUCCons>"
                + "<ns2:dRazCons>EMPRESA DE PRUEBA S.A.</ns2:dRazCons>"
                + "<ns2:dCodEstCons>ACT</ns2:dCodEstCons>"
                + "<ns2:dDesEstCons>ACTIVO</ns2:dDesEstCons>"
                + "<ns2:dRUCFactElec>S</ns2:dRUCFactElec>"
                + "</ns2:xContRUC>"
                + "</ns2:rResEnviConsRUC>"
                + "</env:Body>"
                + "</env:Envelope>";

        ConsultaRucResult resultado = nuevoServicio().parsearRespuesta(xml);

        assertThat(resultado.respondioSifen()).isTrue();
        assertThat(resultado.codigo()).isEqualTo("0502");
        assertThat(resultado.ruc()).isEqualTo("80012345");
        assertThat(resultado.razonSocial()).isEqualTo("EMPRESA DE PRUEBA S.A.");
        assertThat(resultado.codigoEstado()).isEqualTo("ACT");
        assertThat(resultado.facturadorElectronico()).isTrue();
    }

    @Test
    void parseaRucSinPermiso_0501_sinContenedor() throws Exception {
        String xml = "<env:Envelope xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\">"
                + "<env:Body>"
                + "<ns2:rResEnviConsRUC xmlns:ns2=\"http://ekuatia.set.gov.py/sifen/xsd\">"
                + "<ns2:dCodRes>0501</ns2:dCodRes>"
                + "<ns2:dMsgRes>RUC sin permiso consulta WS</ns2:dMsgRes>"
                + "</ns2:rResEnviConsRUC>"
                + "</env:Body>"
                + "</env:Envelope>";

        ConsultaRucResult resultado = nuevoServicio().parsearRespuesta(xml);

        assertThat(resultado.respondioSifen()).isTrue();
        assertThat(resultado.codigo()).isEqualTo("0501");
        assertThat(resultado.razonSocial()).isNull();
        assertThat(resultado.facturadorElectronico()).isNull();
    }

    @Test
    void rechazaRespuestasConDoctype() throws Exception {
        String xml = "<?xml version=\"1.0\"?><!DOCTYPE x [<!ENTITY e SYSTEM \"file:///etc/passwd\">]><x>&e;</x>";

        assertThatThrownBy(() -> nuevoServicio().parsearRespuesta(xml)).isInstanceOf(Exception.class);
    }
}
