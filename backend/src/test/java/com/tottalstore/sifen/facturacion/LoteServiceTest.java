package com.tottalstore.sifen.facturacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tottalstore.sifen.auditoria.AuditoriaService;
import com.tottalstore.sifen.auth.CurrentUser;
import com.tottalstore.sifen.common.BusinessException;
import com.tottalstore.sifen.facturacion.dto.LoteResponse;
import com.tottalstore.sifen.sifen.Ambiente;
import com.tottalstore.sifen.sifen.EventosLotesSifenService;
import com.tottalstore.sifen.sifen.EventosLotesSifenService.RespuestaLote;
import com.tottalstore.sifen.sifen.EventosLotesSifenService.ResultadoDe;
import com.tottalstore.sifen.sifen.EventosLotesSifenService.ResultadoLote;
import com.tottalstore.sifen.sifen.LoteSifen;
import com.tottalstore.sifen.sifen.LoteSifenRepository;
import com.tottalstore.sifen.sifen.RespuestaSifen;
import com.tottalstore.sifen.sifen.RespuestaSifenResult;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LoteServiceTest {

    @Mock private FacturaRepository facturaRepository;
    @Mock private FacturaService facturaService;
    @Mock private LoteSifenRepository loteSifenRepository;
    @Mock private EventosLotesSifenService eventosLotesSifenService;
    @Mock private AuditoriaService auditoriaService;
    @Mock private CurrentUser currentUser;

    private LoteService servicio;

    @BeforeEach
    void setUp() {
        servicio = new LoteService(facturaRepository, facturaService, loteSifenRepository, eventosLotesSifenService,
                auditoriaService, currentUser);
        lenient().when(facturaService.ambienteActual()).thenReturn(Ambiente.TEST);
        lenient().when(loteSifenRepository.save(any())).thenAnswer(i -> {
            LoteSifen l = i.getArgument(0);
            if (l.getId() == null) l.setId(UUID.randomUUID());
            return l;
        });
    }

    @Test
    void enviaLosXmlFirmadosYDejaLosDocumentosEnviados() {
        FacturaElectronica a = firmado(TipoDocumento.FE, "<rDE>a</rDE>");
        FacturaElectronica b = firmado(TipoDocumento.FE, "<rDE>b</rDE>");
        bloqueables(a, b);
        when(eventosLotesSifenService.enviarLote(anyList(), eq(Ambiente.TEST)))
                .thenReturn(new RespuestaLote(true, "0300", "Lote recibido con éxito", "123456", false));

        LoteResponse lote = servicio.enviar(List.of(a.getId(), b.getId()));

        verify(eventosLotesSifenService).enviarLote(List.of("<rDE>a</rDE>", "<rDE>b</rDE>"), Ambiente.TEST);
        assertThat(lote.estado()).isEqualTo(LoteSifen.Estado.RECIBIDO);
        assertThat(lote.numeroLote()).isEqualTo("123456");
        assertThat(a.getEstadoDte()).isEqualTo(EstadoDte.ENVIADO_SIFEN);
        assertThat(b.getLote()).isNotNull();
    }

    @Test
    void siElSifenNoEncolaElLoteLosDocumentosSiguenFirmados() {
        FacturaElectronica a = firmado(TipoDocumento.NC, "<rDE>a</rDE>");
        bloqueables(a);
        when(eventosLotesSifenService.enviarLote(anyList(), any()))
                .thenReturn(new RespuestaLote(false, "0301", "Lote no encolado", null, false));

        LoteResponse lote = servicio.enviar(List.of(a.getId()));

        assertThat(lote.estado()).isEqualTo(LoteSifen.Estado.NO_RECIBIDO);
        assertThat(a.getEstadoDte()).isEqualTo(EstadoDte.FIRMADO);
        assertThat(a.getLote()).isNull();
    }

    @Test
    void sinRespuestaDelSifenLosDocumentosQuedanEnProceso() {
        FacturaElectronica a = firmado(TipoDocumento.FE, "<rDE>a</rDE>");
        bloqueables(a);
        when(eventosLotesSifenService.enviarLote(anyList(), any()))
                .thenReturn(RespuestaLote.sinConfirmar("ERROR_TIMEOUT", "El SIFEN no respondió"));

        servicio.enviar(List.of(a.getId()));

        assertThat(a.getEstadoDte()).isEqualTo(EstadoDte.EN_PROCESO);
    }

    @Test
    void unLoteNoMezclaTiposDeDocumento() {
        FacturaElectronica fe = firmado(TipoDocumento.FE, "<rDE/>");
        FacturaElectronica nc = firmado(TipoDocumento.NC, "<rDE/>");
        bloqueables(fe, nc);

        assertThatThrownBy(() -> servicio.enviar(List.of(fe.getId(), nc.getId())))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("mismo tipo");
        verify(eventosLotesSifenService, never()).enviarLote(anyList(), any());
    }

    @Test
    void soloSeEnvianDocumentosFirmadosDentroDeLas72Horas() {
        FacturaElectronica borrador = firmado(TipoDocumento.FE, "<rDE/>");
        borrador.setEstadoDte(EstadoDte.BORRADOR);
        FacturaElectronica viejo = firmado(TipoDocumento.FE, "<rDE/>");
        viejo.setFechaEmision(Instant.now().minus(Duration.ofHours(73)));
        bloqueables(borrador, viejo);

        assertThatThrownBy(() -> servicio.enviar(List.of(borrador.getId()))).hasMessageContaining("no está firmado");
        assertThatThrownBy(() -> servicio.enviar(List.of(viejo.getId()))).hasMessageContaining("72 h");
    }

    @Test
    void alConcluirElLoteCadaDocumentoRecibeSuResultado() {
        LoteSifen lote = lote();
        FacturaElectronica aprobado = firmado(TipoDocumento.FE, "<rDE/>");
        FacturaElectronica rechazado = firmado(TipoDocumento.FE, "<rDE/>");
        aprobado.setEstadoDte(EstadoDte.ENVIADO_SIFEN);
        rechazado.setEstadoDte(EstadoDte.ENVIADO_SIFEN);
        bloqueables(aprobado, rechazado);
        when(loteSifenRepository.findById(lote.getId())).thenReturn(Optional.of(lote));
        when(facturaRepository.findByLoteIdOrderByNumeroDocumentoAsc(lote.getId())).thenReturn(List.of(aprobado, rechazado));
        when(eventosLotesSifenService.consultarLote("123456", Ambiente.TEST)).thenReturn(new ResultadoLote("0362", "Concluido", List.of(
                new ResultadoDe(aprobado.getCdc(), true, "Aprobado", "0260", "Aprobado — satisfactoria"),
                new ResultadoDe(rechazado.getCdc(), false, "Rechazado", "1101", "Rechazado — timbrado inválido")), false));

        LoteResponse respuesta = servicio.consultar(lote.getId());

        ArgumentCaptor<RespuestaSifenResult> r = ArgumentCaptor.forClass(RespuestaSifenResult.class);
        verify(facturaService).registrarResultadoDeLote(eq(aprobado), r.capture(), eq(Ambiente.TEST));
        assertThat(r.getValue().aprobado()).isTrue();
        assertThat(r.getValue().cdc()).isEqualTo(aprobado.getCdc());
        verify(facturaService).registrarResultadoDeLote(eq(rechazado), r.capture(), eq(Ambiente.TEST));
        assertThat(r.getValue().aprobado()).isFalse();
        assertThat(r.getValue().codigo()).isEqualTo("1101");
        assertThat(respuesta.estado()).isEqualTo(LoteSifen.Estado.CONCLUIDO);
    }

    @Test
    void mientrasElLoteSeProcesaNoCambiaNada() {
        LoteSifen lote = lote();
        FacturaElectronica f = firmado(TipoDocumento.FE, "<rDE/>");
        f.setEstadoDte(EstadoDte.ENVIADO_SIFEN);
        when(loteSifenRepository.findById(lote.getId())).thenReturn(Optional.of(lote));
        when(facturaRepository.findByLoteIdOrderByNumeroDocumentoAsc(lote.getId())).thenReturn(List.of(f));
        when(eventosLotesSifenService.consultarLote(any(), any()))
                .thenReturn(new ResultadoLote("0361", "Lote en procesamiento", List.of(), false));

        LoteResponse respuesta = servicio.consultar(lote.getId());

        assertThat(respuesta.estado()).isEqualTo(LoteSifen.Estado.RECIBIDO);
        assertThat(f.getEstadoDte()).isEqualTo(EstadoDte.ENVIADO_SIFEN);
        verify(facturaService, never()).registrarResultadoDeLote(any(), any(), any());
    }

    private void bloqueables(FacturaElectronica... documentos) {
        for (FacturaElectronica f : documentos) {
            lenient().when(facturaRepository.findByIdParaActualizar(f.getId())).thenReturn(Optional.of(f));
        }
    }

    private static LoteSifen lote() {
        LoteSifen lote = new LoteSifen();
        lote.setId(UUID.randomUUID());
        lote.setNumeroLote("123456");
        lote.setEstado(LoteSifen.Estado.RECIBIDO);
        lote.setAmbiente(Ambiente.TEST);
        lote.setTipoDoc(TipoDocumento.FE);
        return lote;
    }

    private static FacturaElectronica firmado(TipoDocumento tipo, String xml) {
        FacturaElectronica f = new FacturaElectronica();
        f.setId(UUID.randomUUID());
        f.setTipoDoc(tipo);
        f.setEstadoDte(EstadoDte.FIRMADO);
        f.setFechaEmision(Instant.now());
        f.setCdc(UUID.randomUUID().toString());
        RespuestaSifen r = new RespuestaSifen();
        r.setXmlFirmado(xml);
        f.setRespuestaSifen(r);
        return f;
    }
}
