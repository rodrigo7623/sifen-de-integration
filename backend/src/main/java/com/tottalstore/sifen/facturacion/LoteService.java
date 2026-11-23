package com.tottalstore.sifen.facturacion;

import com.tottalstore.sifen.auditoria.AuditoriaService;
import com.tottalstore.sifen.auth.CurrentUser;
import com.tottalstore.sifen.common.BusinessException;
import com.tottalstore.sifen.common.NotFoundException;
import com.tottalstore.sifen.facturacion.dto.LoteResponse;
import com.tottalstore.sifen.sifen.Ambiente;
import com.tottalstore.sifen.sifen.EventosLotesSifenService;
import com.tottalstore.sifen.sifen.EventosLotesSifenService.RespuestaLote;
import com.tottalstore.sifen.sifen.EventosLotesSifenService.ResultadoDe;
import com.tottalstore.sifen.sifen.EventosLotesSifenService.ResultadoLote;
import com.tottalstore.sifen.sifen.LoteSifen;
import com.tottalstore.sifen.sifen.LoteSifenRepository;
import com.tottalstore.sifen.sifen.RespuestaSifenResult;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Envío asíncrono por lote (Manual Técnico v150, 9.2 y 9.3; Guía de Pruebas e-Kuatia 4.2):
 *
 * <ol>
 *   <li>Cada documento se firma por separado ({@link FacturaService#firmarParaLote}) y queda FIRMADO.</li>
 *   <li>{@link #enviar}: hasta 50 DE firmados del mismo tipo van en un solo lote ({@code siRecepLoteDE}).
 *       Si el SIFEN lo encola (0300) devuelve un número de lote y los DE quedan ENVIADO_SIFEN.</li>
 *   <li>{@link #consultar}: con ese número, {@code siResultLoteDE} devuelve el resultado de cada DE
 *       (0362) y cada uno queda APROBADO o RECHAZADO como en el envío sincrónico; 0361 = todavía en
 *       procesamiento, hay que volver a consultar.</li>
 * </ol>
 */
@Service
public class LoteService {

    static final int MAXIMO_DE_POR_LOTE = 50;

    private final FacturaRepository facturaRepository;
    private final FacturaService facturaService;
    private final LoteSifenRepository loteSifenRepository;
    private final EventosLotesSifenService eventosLotesSifenService;
    private final AuditoriaService auditoriaService;
    private final CurrentUser currentUser;

    public LoteService(
            FacturaRepository facturaRepository,
            FacturaService facturaService,
            LoteSifenRepository loteSifenRepository,
            EventosLotesSifenService eventosLotesSifenService,
            AuditoriaService auditoriaService,
            CurrentUser currentUser) {
        this.facturaRepository = facturaRepository;
        this.facturaService = facturaService;
        this.loteSifenRepository = loteSifenRepository;
        this.eventosLotesSifenService = eventosLotesSifenService;
        this.auditoriaService = auditoriaService;
        this.currentUser = currentUser;
    }

    @Transactional
    public LoteResponse enviar(List<UUID> facturaIds) {
        List<UUID> ids = new ArrayList<>(new LinkedHashSet<>(facturaIds == null ? List.of() : facturaIds));
        if (ids.isEmpty() || ids.size() > MAXIMO_DE_POR_LOTE) {
            throw new BusinessException("Un lote lleva entre 1 y " + MAXIMO_DE_POR_LOTE + " documentos (Manual Técnico 9.2)");
        }
        List<FacturaElectronica> documentos = new ArrayList<>();
        for (UUID id : ids) {
            FacturaElectronica f = facturaRepository.findByIdParaActualizar(id)
                    .orElseThrow(() -> new NotFoundException("Documento no encontrado: " + id));
            if (f.getEstadoDte() != EstadoDte.FIRMADO || f.getRespuestaSifen() == null || f.getRespuestaSifen().getXmlFirmado() == null) {
                throw new BusinessException("El documento " + etiqueta(f) + " no está firmado para lote (estado " + f.getEstadoDte() + ")");
            }
            if (Instant.now().isAfter(f.getFechaEmision().plus(FacturaService.PLAZO_TRANSMISION))) {
                throw new BusinessException("El documento " + etiqueta(f) + " se firmó hace más de 72 h y ya no se puede"
                        + " transmitir (Manual Técnico 6.2)");
            }
            documentos.add(f);
        }
        TipoDocumento tipo = documentos.get(0).getTipoDoc();
        if (documentos.stream().anyMatch(f -> f.getTipoDoc() != tipo)) {
            throw new BusinessException("Un lote solo puede contener documentos de un mismo tipo (Manual Técnico 9.2)");
        }

        Ambiente ambiente = facturaService.ambienteActual();
        RespuestaLote respuesta = eventosLotesSifenService.enviarLote(
                documentos.stream().map(f -> f.getRespuestaSifen().getXmlFirmado()).toList(), ambiente);

        LoteSifen lote = new LoteSifen();
        lote.setTipoDoc(tipo);
        lote.setCantidad(documentos.size());
        lote.setAmbiente(ambiente);
        lote.setUsuario(currentUser.obtener());
        lote.setCodigo(respuesta.codigo());
        lote.setMensaje(respuesta.mensaje());
        lote.setNumeroLote(respuesta.numeroLote());
        lote.setEstado(respuesta.recibido() ? LoteSifen.Estado.RECIBIDO
                : respuesta.sinConfirmar() ? LoteSifen.Estado.SIN_CONFIRMAR : LoteSifen.Estado.NO_RECIBIDO);
        lote = loteSifenRepository.save(lote);

        // NO_RECIBIDO: el SIFEN no encoló nada, los DE siguen FIRMADO y se pueden mandar en otro lote.
        if (lote.getEstado() != LoteSifen.Estado.NO_RECIBIDO) {
            for (FacturaElectronica f : documentos) {
                f.setLote(lote);
                if (lote.getEstado() == LoteSifen.Estado.RECIBIDO) {
                    f.setEstadoDte(EstadoDte.ENVIADO_SIFEN);
                    f.getRespuestaSifen().setDescripcion("Enviado en el lote " + lote.getNumeroLote() + ": esperando el resultado");
                } else {
                    // Sin respuesta: puede haber llegado. Se resuelve como cualquier envío sin confirmar (BUG-006).
                    f.setEstadoDte(EstadoDte.EN_PROCESO);
                    f.getRespuestaSifen().setDescripcion(respuesta.mensaje());
                }
                facturaRepository.save(f);
                auditoriaService.registrar("ENVIADO_EN_LOTE", currentUser.obtener(), f);
            }
        }
        return respuesta(lote, documentos);
    }

    @Transactional
    public LoteResponse consultar(UUID loteId) {
        LoteSifen lote = loteSifenRepository.findById(loteId)
                .orElseThrow(() -> new NotFoundException("Lote no encontrado: " + loteId));
        List<FacturaElectronica> documentos = facturaRepository.findByLoteIdOrderByNumeroDocumentoAsc(loteId);
        if (lote.getEstado() != LoteSifen.Estado.RECIBIDO) {
            return respuesta(lote, documentos);
        }
        ResultadoLote resultado = eventosLotesSifenService.consultarLote(lote.getNumeroLote(), lote.getAmbiente());
        lote.setFechaConsulta(Instant.now());
        lote.setCodigo(resultado.codigo());
        lote.setMensaje(resultado.mensaje());
        if (resultado.sinConfirmar() || ResultadoLote.EN_PROCESAMIENTO.equals(resultado.codigo())) {
            loteSifenRepository.save(lote);
            return respuesta(lote, documentos);
        }

        // 0362 con el resultado de cada DE, o un código final sin resultados (p. ej. 0360 lote inexistente).
        Map<String, ResultadoDe> porCdc = resultado.documentos().stream()
                .filter(r -> r.cdc() != null)
                .collect(Collectors.toMap(ResultadoDe::cdc, Function.identity(), (a, b) -> a));
        for (FacturaElectronica f : documentos) {
            if (f.getEstadoDte() != EstadoDte.ENVIADO_SIFEN) {
                continue;
            }
            FacturaElectronica bloqueada = facturaRepository.findByIdParaActualizar(f.getId()).orElse(f);
            ResultadoDe r = f.getCdc() != null ? porCdc.get(f.getCdc()) : porCdc.get(f.getId().toString());
            if (r != null) {
                String cdc = r.aprobado() ? (f.getCdc() != null ? f.getCdc() : r.cdc()) : null;
                facturaService.registrarResultadoDeLote(bloqueada,
                        new RespuestaSifenResult(r.aprobado(), r.codigo(), r.mensaje(), cdc), lote.getAmbiente());
            } else {
                // El lote terminó sin informar este DE: se verifica individualmente por su CDC.
                bloqueada.setEstadoDte(EstadoDte.EN_PROCESO);
                bloqueada.getRespuestaSifen().setDescripcion("El lote " + lote.getNumeroLote() + " terminó (" + resultado.codigo()
                        + " " + resultado.mensaje() + ") sin informar este documento: verificalo por su CDC");
                facturaRepository.save(bloqueada);
            }
        }
        lote.setEstado(LoteSifen.Estado.CONCLUIDO);
        loteSifenRepository.save(lote);
        return respuesta(lote, facturaRepository.findByLoteIdOrderByNumeroDocumentoAsc(loteId));
    }

    @Transactional(readOnly = true)
    public List<LoteResponse> listar() {
        return loteSifenRepository.findAllByOrderByFechaEnvioDesc().stream()
                .map(l -> respuesta(l, facturaRepository.findByLoteIdOrderByNumeroDocumentoAsc(l.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public LoteResponse obtener(UUID loteId) {
        LoteSifen lote = loteSifenRepository.findById(loteId)
                .orElseThrow(() -> new NotFoundException("Lote no encontrado: " + loteId));
        return respuesta(lote, facturaRepository.findByLoteIdOrderByNumeroDocumentoAsc(loteId));
    }

    private static LoteResponse respuesta(LoteSifen lote, List<FacturaElectronica> documentos) {
        return LoteResponse.from(lote, documentos);
    }

    private static String etiqueta(FacturaElectronica f) {
        return f.getTipoDoc() + " " + (f.numeroComprobante() != null ? f.numeroComprobante() : f.getId());
    }
}
