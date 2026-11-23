package com.tottalstore.sifen.facturacion;

import com.tottalstore.sifen.auditoria.AuditoriaService;
import com.tottalstore.sifen.auth.CurrentUser;
import com.tottalstore.sifen.common.BusinessException;
import com.tottalstore.sifen.common.NotFoundException;
import com.tottalstore.sifen.configuracion.ConfiguracionEmisor;
import com.tottalstore.sifen.configuracion.ConfiguracionEmisorRepository;
import com.tottalstore.sifen.firma.GeneradorEventoService;
import com.tottalstore.sifen.sifen.Ambiente;
import com.tottalstore.sifen.sifen.EventoSifen;
import com.tottalstore.sifen.sifen.EventoSifenRepository;
import com.tottalstore.sifen.sifen.EventosLotesSifenService;
import com.tottalstore.sifen.sifen.EventosLotesSifenService.RespuestaEvento;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Eventos del emisor (Manual Técnico v150, 11.5.1; Guía de Pruebas e-Kuatia 4.3):
 *
 * <ul>
 *   <li><b>Cancelación</b> de un DTE aprobado: deja sin efecto el documento en el SIFEN. Plazo: 48 h
 *       para una factura y 168 h para las notas (validaciones 4009/4010).</li>
 *   <li><b>Inutilización</b> del número de un DE rechazado que no se va a volver a enviar: el SIFEN
 *       nunca lo tuvo, y sin el evento ese número quedaría como un salto en la numeración.</li>
 * </ul>
 */
@Service
public class EventosService {

    static final Duration PLAZO_CANCELACION_FE = Duration.ofHours(48);
    static final Duration PLAZO_CANCELACION_NOTAS = Duration.ofHours(168);
    private static final ZoneId ZONA_PARAGUAY = ZoneId.of("America/Asuncion");

    private final FacturaRepository facturaRepository;
    private final FacturaService facturaService;
    private final EventoSifenRepository eventoSifenRepository;
    private final GeneradorEventoService generadorEventoService;
    private final EventosLotesSifenService eventosLotesSifenService;
    private final ConfiguracionEmisorRepository configuracionEmisorRepository;
    private final AuditoriaService auditoriaService;
    private final CurrentUser currentUser;

    public EventosService(
            FacturaRepository facturaRepository,
            FacturaService facturaService,
            EventoSifenRepository eventoSifenRepository,
            GeneradorEventoService generadorEventoService,
            EventosLotesSifenService eventosLotesSifenService,
            ConfiguracionEmisorRepository configuracionEmisorRepository,
            AuditoriaService auditoriaService,
            CurrentUser currentUser) {
        this.facturaRepository = facturaRepository;
        this.facturaService = facturaService;
        this.eventoSifenRepository = eventoSifenRepository;
        this.generadorEventoService = generadorEventoService;
        this.eventosLotesSifenService = eventosLotesSifenService;
        this.configuracionEmisorRepository = configuracionEmisorRepository;
        this.auditoriaService = auditoriaService;
        this.currentUser = currentUser;
    }

    @Transactional
    public EventoSifen cancelar(UUID facturaId, String motivo) {
        FacturaElectronica factura = bloquear(facturaId);
        if (factura.getEstadoDte() != EstadoDte.APROBADO || factura.cdcVigente() == null) {
            throw new BusinessException("Solo se puede cancelar un documento aprobado por el SIFEN (estado actual: "
                    + factura.getEstadoDte() + ")");
        }
        Duration plazo = factura.esNota() ? PLAZO_CANCELACION_NOTAS : PLAZO_CANCELACION_FE;
        if (Instant.now().isAfter(factura.getFechaEmision().plus(plazo))) {
            throw new BusinessException("Pasó el plazo de cancelación de " + plazo.toHours() + " h desde la emisión"
                    + " (Manual Técnico 11.6.1). Para dejarlo sin efecto, emití una nota de crédito.");
        }
        if (factura.getTipoDoc() == TipoDocumento.FE) {
            List<String> vigentes = facturaRepository.findByFacturaAsociadaIdOrderByFechaEmisionAsc(factura.getId()).stream()
                    .filter(n -> !FacturaService.SIN_EFECTO.contains(n.getEstadoDte()))
                    .map(n -> n.getTipoDoc() + " " + (n.numeroComprobante() != null ? n.numeroComprobante() : "(borrador)"))
                    .toList();
            if (!vigentes.isEmpty()) {
                throw new BusinessException("La factura tiene notas vigentes (" + String.join(", ", vigentes)
                        + "): cancelalas o descartalas antes de cancelar la factura");
            }
        }
        String cdc = factura.cdcVigente();
        EventoSifen evento = enviar(factura, EventoSifen.Tipo.CANCELACION, motivo,
                id -> generadorEventoService.cancelacion(id, ahora(), cdc, motivo));

        if (evento.getEstado() == EventoSifen.Estado.APROBADO) {
            factura.setEstadoDte(EstadoDte.CANCELADO);
            facturaRepository.save(factura);
            auditoriaService.registrar("CANCELADO_SIFEN", currentUser.obtener(), factura);
            // Una NC cancelada deja de acreditar: si era la que anulaba la factura, la factura vuelve a estar vigente.
            FacturaElectronica asociada = factura.getFacturaAsociada();
            if (factura.getTipoDoc() == TipoDocumento.NC && asociada != null && asociada.getEstadoDte() == EstadoDte.ANULADO
                    && facturaService.saldoAcreditable(asociada, null).signum() > 0) {
                asociada.setEstadoDte(EstadoDte.APROBADO);
                facturaRepository.save(asociada);
                auditoriaService.registrar("ANULACION_REVERTIDA", currentUser.obtener(), asociada);
            }
        }
        return evento;
    }

    @Transactional
    public EventoSifen inutilizar(UUID facturaId, String motivo) {
        FacturaElectronica factura = bloquear(facturaId);
        if (factura.getEstadoDte() != EstadoDte.RECHAZADO || factura.getNumeroDocumento() == null) {
            throw new BusinessException("Solo se inutiliza el número de un documento rechazado por el SIFEN (estado actual: "
                    + factura.getEstadoDte() + ")");
        }
        ConfiguracionEmisor emisor = configuracionEmisorRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new BusinessException("Falta cargar la configuración del emisor"));
        if (emisor.getTimbradoNumero() == null || emisor.getTimbradoNumero().isBlank()) {
            throw new BusinessException("Falta el número de timbrado en la configuración del emisor");
        }
        GeneradorEventoService.Inutilizacion datos = new GeneradorEventoService.Inutilizacion(
                emisor.getTimbradoNumero(),
                factura.getEstablecimiento().getCodigo(),
                factura.getPuntoExpedicion().getCodigo(),
                factura.getNumeroDocumento(),
                factura.getNumeroDocumento(),
                factura.getTipoDoc().codigo(),
                motivo);
        EventoSifen evento = enviar(factura, EventoSifen.Tipo.INUTILIZACION, motivo,
                id -> generadorEventoService.inutilizacion(id, ahora(), datos));

        if (evento.getEstado() == EventoSifen.Estado.APROBADO) {
            factura.setEstadoDte(EstadoDte.INUTILIZADO);
            facturaRepository.save(factura);
            auditoriaService.registrar("INUTILIZADO_SIFEN", currentUser.obtener(), factura);
        }
        return evento;
    }

    public List<EventoSifen> eventosDe(UUID facturaId) {
        facturaService.obtener(facturaId);
        return eventoSifenRepository.findByFacturaIdOrderByFechaDesc(facturaId);
    }

    /** El id del evento (atributo Id del rEve) sale de la base, así que el evento se guarda antes de firmarlo. */
    private EventoSifen enviar(FacturaElectronica factura, EventoSifen.Tipo tipo, String motivo, Function<Long, String> firmar) {
        Ambiente ambiente = facturaService.ambienteActual();
        EventoSifen evento = new EventoSifen();
        evento.setTipo(tipo);
        evento.setFactura(factura);
        evento.setMotivo(motivo.strip());
        evento.setAmbiente(ambiente);
        evento.setUsuario(currentUser.obtener());
        evento = eventoSifenRepository.saveAndFlush(evento);

        String xml = firmar.apply(evento.getId());
        evento.setXmlFirmado(xml);
        RespuestaEvento respuesta = eventosLotesSifenService.enviarEvento(evento.getId(), xml, ambiente);
        evento.setEstado(respuesta.aprobado() ? EventoSifen.Estado.APROBADO
                : respuesta.sinConfirmar() ? EventoSifen.Estado.SIN_CONFIRMAR : EventoSifen.Estado.RECHAZADO);
        evento.setCodigo(respuesta.codigo());
        evento.setMensaje(respuesta.mensaje());
        evento.setProtocolo(respuesta.protocolo());
        return eventoSifenRepository.save(evento);
    }

    private FacturaElectronica bloquear(UUID id) {
        return facturaRepository.findByIdParaActualizar(id)
                .orElseThrow(() -> new NotFoundException("Documento no encontrado: " + id));
    }

    private static LocalDateTime ahora() {
        return LocalDateTime.now(ZONA_PARAGUAY).withNano(0);
    }
}
