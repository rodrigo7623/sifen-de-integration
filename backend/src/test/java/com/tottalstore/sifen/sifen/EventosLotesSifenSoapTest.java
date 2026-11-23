package com.tottalstore.sifen.sifen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.tottalstore.sifen.sifen.EventosLotesSifenService.RespuestaEvento;
import com.tottalstore.sifen.sifen.EventosLotesSifenService.RespuestaLote;
import com.tottalstore.sifen.sifen.EventosLotesSifenService.ResultadoLote;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.zip.ZipInputStream;
import javax.net.ssl.SSLContext;
import org.junit.jupiter.api.Test;

/** Armado del lote y lectura de las respuestas de siRecepEvento, siRecepLoteDE y siResultLoteDE. */
class EventosLotesSifenSoapTest {

    private static final String NS = "xmlns:ns2=\"http://ekuatia.set.gov.py/sifen/xsd\"";

    private EventosLotesSifenSoap cliente() throws Exception {
        return new EventosLotesSifenSoap(SSLContext.getDefault(), mock(SifenEnvioHistorial.class),
                "https://e", "https://l", "https://c", 30);
    }

    private static String sobre(String cuerpo) {
        return "<env:Envelope xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\"><env:Body>" + cuerpo
                + "</env:Body></env:Envelope>";
    }

    @Test
    void elLoteEsUnZipConUnRLoteDeQueContieneLosRDeSinSuDeclaracion() throws Exception {
        String base64 = EventosLotesSifenSoap.comprimirLote(List.of(
                "<?xml version=\"1.0\"?><rDE xmlns=\"x\"><a/></rDE>", "<rDE xmlns=\"x\"><b/></rDE>"));

        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(base64)))) {
            assertThat(zip.getNextEntry().getName()).isEqualTo("lote.xml");
            String contenido = new String(zip.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(contenido).isEqualTo("<?xml version=\"1.0\" encoding=\"UTF-8\"?><rLoteDE>"
                    + "<rDE xmlns=\"x\"><a/></rDE><rDE xmlns=\"x\"><b/></rDE></rLoteDE>");
        }
    }

    @Test
    void eventoAprobadoDevuelveElProtocolo() throws Exception {
        RespuestaEvento r = cliente().parsearEvento(sobre("<ns2:rRetEnviEventoDe " + NS + "><ns2:dFecProc>2026-11-23T10:00:00-03:00</ns2:dFecProc>"
                + "<ns2:gResProcEVe><ns2:dEstRes>Aprobado</ns2:dEstRes><ns2:dProtAut>1234567890</ns2:dProtAut><ns2:id>17</ns2:id>"
                + "<ns2:gResProc><ns2:dCodRes>0600</ns2:dCodRes><ns2:dMsgRes>Evento registrado correctamente</ns2:dMsgRes></ns2:gResProc>"
                + "</ns2:gResProcEVe></ns2:rRetEnviEventoDe>"));

        assertThat(r.aprobado()).isTrue();
        assertThat(r.codigo()).isEqualTo("0600");
        assertThat(r.protocolo()).isEqualTo("1234567890");
        assertThat(r.mensaje()).contains("Evento registrado correctamente");
    }

    @Test
    void eventoRechazadoDevuelveElCodigoDelMotivo() throws Exception {
        RespuestaEvento r = cliente().parsearEvento(sobre("<ns2:rRetEnviEventoDe " + NS + "><ns2:gResProcEVe>"
                + "<ns2:dEstRes>Rechazado</ns2:dEstRes><ns2:id>18</ns2:id><ns2:gResProc><ns2:dCodRes>4003</ns2:dCodRes>"
                + "<ns2:dMsgRes>CDC ya se encuentra con el mismo evento solicitado</ns2:dMsgRes></ns2:gResProc>"
                + "</ns2:gResProcEVe></ns2:rRetEnviEventoDe>"));

        assertThat(r.aprobado()).isFalse();
        assertThat(r.sinConfirmar()).isFalse();
        assertThat(r.codigo()).isEqualTo("4003");
    }

    @Test
    void loteRecibidoDevuelveElNumeroDeLote() throws Exception {
        RespuestaLote r = cliente().parsearLote(sobre("<ns2:rResEnviLoteDe " + NS + "><ns2:dFecProc>2026-11-23T10:00:00</ns2:dFecProc>"
                + "<ns2:dCodRes>0300</ns2:dCodRes><ns2:dMsgRes>Lote recibido con éxito</ns2:dMsgRes>"
                + "<ns2:dProtConsLote>987654321</ns2:dProtConsLote><ns2:dTpoProces>0</ns2:dTpoProces></ns2:rResEnviLoteDe>"));

        assertThat(r.recibido()).isTrue();
        assertThat(r.numeroLote()).isEqualTo("987654321");
    }

    @Test
    void loteNoEncoladoNoTieneNumero() throws Exception {
        RespuestaLote r = cliente().parsearLote(sobre("<ns2:rResEnviLoteDe " + NS + "><ns2:dCodRes>0301</ns2:dCodRes>"
                + "<ns2:dMsgRes>Lote no encolado para procesamiento</ns2:dMsgRes><ns2:dProtConsLote>0</ns2:dProtConsLote>"
                + "</ns2:rResEnviLoteDe>"));

        assertThat(r.recibido()).isFalse();
        assertThat(r.numeroLote()).isNull();
        assertThat(r.codigo()).isEqualTo("0301");
    }

    @Test
    void resultadoDelLoteTraeElEstadoDeCadaDe() throws Exception {
        ResultadoLote r = cliente().parsearResultadoLote(sobre("<ns2:rResEnviConsLoteDe " + NS + ">"
                + "<ns2:dFecProc>2026-11-23T10:05:00</ns2:dFecProc><ns2:dCodResLot>0362</ns2:dCodResLot>"
                + "<ns2:dMsgResLot>Procesamiento de lote concluido</ns2:dMsgResLot>"
                + "<ns2:gResProcLote><ns2:id>CDC1</ns2:id><ns2:dEstRes>Aprobado</ns2:dEstRes><ns2:dProtAut>111</ns2:dProtAut>"
                + "<ns2:gResProc><ns2:dCodRes>0260</ns2:dCodRes><ns2:dMsgRes>Autorización del DE satisfactoria</ns2:dMsgRes></ns2:gResProc></ns2:gResProcLote>"
                + "<ns2:gResProcLote><ns2:id>CDC2</ns2:id><ns2:dEstRes>Rechazado</ns2:dEstRes>"
                + "<ns2:gResProc><ns2:dCodRes>1101</ns2:dCodRes><ns2:dMsgRes>Número de timbrado inválido</ns2:dMsgRes></ns2:gResProc></ns2:gResProcLote>"
                + "</ns2:rResEnviConsLoteDe>"));

        assertThat(r.concluido()).isTrue();
        assertThat(r.documentos()).hasSize(2);
        assertThat(r.documentos().get(0).cdc()).isEqualTo("CDC1");
        assertThat(r.documentos().get(0).aprobado()).isTrue();
        assertThat(r.documentos().get(1).aprobado()).isFalse();
        assertThat(r.documentos().get(1).codigo()).isEqualTo("1101");
        assertThat(r.documentos().get(1).mensaje()).contains("Número de timbrado inválido");
    }

    @Test
    void loteEnProcesamientoNoEstaConcluido() throws Exception {
        ResultadoLote r = cliente().parsearResultadoLote(sobre("<ns2:rResEnviConsLoteDe " + NS + "><ns2:dFecProc/>"
                + "<ns2:dCodResLot>0361</ns2:dCodResLot><ns2:dMsgResLot>Lote en procesamiento</ns2:dMsgResLot></ns2:rResEnviConsLoteDe>"));

        assertThat(r.concluido()).isFalse();
        assertThat(r.codigo()).isEqualTo(ResultadoLote.EN_PROCESAMIENTO);
        assertThat(r.documentos()).isEmpty();
    }
}
