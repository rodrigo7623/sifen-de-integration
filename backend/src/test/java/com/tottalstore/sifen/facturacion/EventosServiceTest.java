package com.tottalstore.sifen.facturacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tottalstore.sifen.auditoria.AuditoriaService;
import com.tottalstore.sifen.auth.CurrentUser;
import com.tottalstore.sifen.common.BusinessException;
import com.tottalstore.sifen.configuracion.ConfiguracionEmisor;
import com.tottalstore.sifen.configuracion.ConfiguracionEmisorRepository;
import com.tottalstore.sifen.establecimientos.Establecimiento;
import com.tottalstore.sifen.establecimientos.PuntoExpedicion;
import com.tottalstore.sifen.firma.GeneradorEventoService;
import com.tottalstore.sifen.sifen.Ambiente;
import com.tottalstore.sifen.sifen.EventoSifen;
import com.tottalstore.sifen.sifen.EventoSifenRepository;
import com.tottalstore.sifen.sifen.EventosLotesSifenService;
import com.tottalstore.sifen.sifen.EventosLotesSifenService.RespuestaEvento;
import java.math.BigDecimal;
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
class EventosServiceTest {

    @Mock private FacturaRepository facturaRepository;
    @Mock private FacturaService facturaService;
    @Mock private EventoSifenRepository eventoSifenRepository;
    @Mock private GeneradorEventoService generadorEventoService;
    @Mock private EventosLotesSifenService eventosLotesSifenService;
    @Mock private ConfiguracionEmisorRepository configuracionEmisorRepository;
    @Mock private AuditoriaService auditoriaService;
    @Mock private CurrentUser currentUser;

    private EventosService servicio;

    @BeforeEach
    void setUp() {
        servicio = new EventosService(facturaRepository, facturaService, eventoSifenRepository, generadorEventoService,
                eventosLotesSifenService, configuracionEmisorRepository, auditoriaService, currentUser);
        lenient().when(facturaService.ambienteActual()).thenReturn(Ambiente.TEST);
        lenient().when(eventoSifenRepository.saveAndFlush(any())).thenAnswer(i -> {
            EventoSifen e = i.getArgument(0);
            e.setId(7L);
            return e;
        });
        lenient().when(eventoSifenRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        lenient().when(generadorEventoService.cancelacion(anyLong(), any(), anyString(), anyString())).thenReturn("<gGroupGesEve/>");
        lenient().when(generadorEventoService.inutilizacion(anyLong(), any(), any())).thenReturn("<gGroupGesEve/>");
    }

    @Test
    void cancelarUnaFacturaAprobadaLaDejaCancelada() {
        FacturaElectronica fe = documento(TipoDocumento.FE, EstadoDte.APROBADO, Instant.now());
        when(facturaRepository.findByIdParaActualizar(fe.getId())).thenReturn(Optional.of(fe));
        when(facturaRepository.findByFacturaAsociadaIdOrderByFechaEmisionAsc(fe.getId())).thenReturn(List.of());
        when(eventosLotesSifenService.enviarEvento(eq(7L), anyString(), eq(Ambiente.TEST)))
                .thenReturn(new RespuestaEvento(true, "0600", "Aprobado — Evento registrado correctamente", "555", false));

        EventoSifen evento = servicio.cancelar(fe.getId(), "Error en el precio");

        assertThat(evento.getEstado()).isEqualTo(EventoSifen.Estado.APROBADO);
        assertThat(evento.getProtocolo()).isEqualTo("555");
        assertThat(fe.getEstadoDte()).isEqualTo(EstadoDte.CANCELADO);
        verify(generadorEventoService).cancelacion(eq(7L), any(), eq(fe.getCdc()), eq("Error en el precio"));
        verify(auditoriaService).registrar(eq("CANCELADO_SIFEN"), any(), eq(fe));
    }

    @Test
    void siElSifenRechazaLaCancelacionElDocumentoSigueAprobado() {
        FacturaElectronica fe = documento(TipoDocumento.FE, EstadoDte.APROBADO, Instant.now());
        when(facturaRepository.findByIdParaActualizar(fe.getId())).thenReturn(Optional.of(fe));
        when(facturaRepository.findByFacturaAsociadaIdOrderByFechaEmisionAsc(fe.getId())).thenReturn(List.of());
        when(eventosLotesSifenService.enviarEvento(anyLong(), anyString(), any()))
                .thenReturn(new RespuestaEvento(false, "4003", "Rechazado — CDC ya se encuentra con el mismo evento", null, false));

        EventoSifen evento = servicio.cancelar(fe.getId(), "Error en el precio");

        assertThat(evento.getEstado()).isEqualTo(EventoSifen.Estado.RECHAZADO);
        assertThat(evento.getCodigo()).isEqualTo("4003");
        assertThat(fe.getEstadoDte()).isEqualTo(EstadoDte.APROBADO);
    }

    @Test
    void noSeCancelaUnaFacturaConNotasVigentes() {
        FacturaElectronica fe = documento(TipoDocumento.FE, EstadoDte.APROBADO, Instant.now());
        FacturaElectronica nc = documento(TipoDocumento.NC, EstadoDte.APROBADO, Instant.now());
        FacturaElectronica rechazada = documento(TipoDocumento.ND, EstadoDte.RECHAZADO, Instant.now());
        when(facturaRepository.findByIdParaActualizar(fe.getId())).thenReturn(Optional.of(fe));
        when(facturaRepository.findByFacturaAsociadaIdOrderByFechaEmisionAsc(fe.getId())).thenReturn(List.of(nc, rechazada));

        assertThatThrownBy(() -> servicio.cancelar(fe.getId(), "Error en el precio"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("NC 001-001-0000003")
                .hasMessageNotContaining("ND");
        verify(eventosLotesSifenService, never()).enviarEvento(anyLong(), anyString(), any());
    }

    @Test
    void laFacturaSoloSeCancelaDentroDeLas48Horas() {
        FacturaElectronica fe = documento(TipoDocumento.FE, EstadoDte.APROBADO, Instant.now().minus(Duration.ofHours(49)));
        when(facturaRepository.findByIdParaActualizar(fe.getId())).thenReturn(Optional.of(fe));

        assertThatThrownBy(() -> servicio.cancelar(fe.getId(), "Error en el precio"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("48 h");
    }

    @Test
    void lasNotasSeCancelanHasta168Horas() {
        FacturaElectronica nd = documento(TipoDocumento.ND, EstadoDte.APROBADO, Instant.now().minus(Duration.ofHours(100)));
        when(facturaRepository.findByIdParaActualizar(nd.getId())).thenReturn(Optional.of(nd));
        when(eventosLotesSifenService.enviarEvento(anyLong(), anyString(), any()))
                .thenReturn(new RespuestaEvento(true, "0600", "Aprobado", "1", false));

        servicio.cancelar(nd.getId(), "Cargo mal calculado");

        assertThat(nd.getEstadoDte()).isEqualTo(EstadoDte.CANCELADO);
    }

    @Test
    void cancelarLaNotaDeCreditoQueAnulabaLaFacturaLaDevuelveAAprobada() {
        FacturaElectronica fe = documento(TipoDocumento.FE, EstadoDte.ANULADO, Instant.now());
        FacturaElectronica nc = documento(TipoDocumento.NC, EstadoDte.APROBADO, Instant.now());
        nc.setFacturaAsociada(fe);
        when(facturaRepository.findByIdParaActualizar(nc.getId())).thenReturn(Optional.of(nc));
        when(eventosLotesSifenService.enviarEvento(anyLong(), anyString(), any()))
                .thenReturn(new RespuestaEvento(true, "0600", "Aprobado", "1", false));
        when(facturaService.saldoAcreditable(fe, null)).thenReturn(new BigDecimal("100000"));

        servicio.cancelar(nc.getId(), "Devolución no realizada");

        assertThat(fe.getEstadoDte()).isEqualTo(EstadoDte.APROBADO);
        verify(auditoriaService).registrar(eq("ANULACION_REVERTIDA"), any(), eq(fe));
    }

    @Test
    void noSeCancelaUnDocumentoRechazado() {
        FacturaElectronica fe = documento(TipoDocumento.FE, EstadoDte.RECHAZADO, Instant.now());
        when(facturaRepository.findByIdParaActualizar(fe.getId())).thenReturn(Optional.of(fe));

        assertThatThrownBy(() -> servicio.cancelar(fe.getId(), "Error en el precio"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("aprobado");
    }

    @Test
    void inutilizarUnDocumentoRechazadoMandaSuNumeroYTimbrado() {
        FacturaElectronica nc = documento(TipoDocumento.NC, EstadoDte.RECHAZADO, Instant.now());
        when(facturaRepository.findByIdParaActualizar(nc.getId())).thenReturn(Optional.of(nc));
        ConfiguracionEmisor emisor = new ConfiguracionEmisor();
        emisor.setTimbradoNumero("04309652");
        when(configuracionEmisorRepository.findAll()).thenReturn(List.of(emisor));
        when(eventosLotesSifenService.enviarEvento(anyLong(), anyString(), any()))
                .thenReturn(new RespuestaEvento(true, "0600", "Aprobado", "9", false));

        servicio.inutilizar(nc.getId(), "Rechazado, no se reenvía");

        ArgumentCaptor<GeneradorEventoService.Inutilizacion> datos = ArgumentCaptor.forClass(GeneradorEventoService.Inutilizacion.class);
        verify(generadorEventoService).inutilizacion(eq(7L), any(), datos.capture());
        assertThat(datos.getValue()).isEqualTo(new GeneradorEventoService.Inutilizacion(
                "04309652", "001", "001", 3, 3, 5, "Rechazado, no se reenvía"));
        assertThat(nc.getEstadoDte()).isEqualTo(EstadoDte.INUTILIZADO);
    }

    @Test
    void noSeInutilizaUnDocumentoAprobado() {
        FacturaElectronica fe = documento(TipoDocumento.FE, EstadoDte.APROBADO, Instant.now());
        when(facturaRepository.findByIdParaActualizar(fe.getId())).thenReturn(Optional.of(fe));

        assertThatThrownBy(() -> servicio.inutilizar(fe.getId(), "Rechazado, no se reenvía"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("rechazado");
    }

    private static FacturaElectronica documento(TipoDocumento tipo, EstadoDte estado, Instant emision) {
        FacturaElectronica f = new FacturaElectronica();
        f.setId(UUID.randomUUID());
        f.setTipoDoc(tipo);
        f.setEstadoDte(estado);
        f.setFechaEmision(emision);
        f.setNumeroDocumento(3);
        f.setCdc("0" + tipo.codigo() + "800160967001001000000322026112319012345678");
        Establecimiento est = new Establecimiento();
        est.setCodigo("001");
        PuntoExpedicion punto = new PuntoExpedicion();
        punto.setCodigo("001");
        f.setEstablecimiento(est);
        f.setPuntoExpedicion(punto);
        return f;
    }
}
