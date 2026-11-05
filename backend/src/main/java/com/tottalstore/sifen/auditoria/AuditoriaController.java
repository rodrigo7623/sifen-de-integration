package com.tottalstore.sifen.auditoria;

import com.tottalstore.sifen.auditoria.dto.AuditoriaResponse;
import com.tottalstore.sifen.facturacion.TipoDocumento;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Visor de auditoría (RF-08). Restringido a ADMIN en {@code SecurityConfig}. */
@RestController
@RequestMapping("/api/auditoria")
public class AuditoriaController {

    private final LogAuditoriaRepository logAuditoriaRepository;

    public AuditoriaController(LogAuditoriaRepository logAuditoriaRepository) {
        this.logAuditoriaRepository = logAuditoriaRepository;
    }

    /**
     * Todos los filtros son opcionales y se combinan (AND). {@code q} busca en el número del
     * comprobante (001-001-0000005 o solo 5) y en el RUC o la razón social del cliente.
     */
    @GetMapping
    public Page<AuditoriaResponse> listar(
            @RequestParam(required = false) UUID facturaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String operacion,
            @RequestParam(required = false) UUID usuarioId,
            @RequestParam(required = false) TipoDocumento tipoDocumento,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (desde != null && hasta != null && hasta.isBefore(desde)) {
            throw new IllegalArgumentException("La fecha 'hasta' no puede ser anterior a 'desde'");
        }
        PageRequest pageable = PageRequest.of(page, Math.min(Math.max(size, 1), 200), Sort.by("fechaHora").descending());
        var filtro = new FiltroAuditoria(facturaId, desde, hasta, operacion, usuarioId, tipoDocumento, q);
        return logAuditoriaRepository.findAll(filtro.especificacion(), pageable).map(AuditoriaResponse::from);
    }

    @GetMapping("/operaciones")
    public List<String> operaciones() {
        return logAuditoriaRepository.operacionesRegistradas();
    }
}
