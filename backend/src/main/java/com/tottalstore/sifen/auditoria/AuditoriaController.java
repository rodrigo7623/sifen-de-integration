package com.tottalstore.sifen.auditoria;

import com.tottalstore.sifen.auditoria.dto.AuditoriaResponse;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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

    @GetMapping
    public Page<AuditoriaResponse> listar(
            @RequestParam(required = false) UUID facturaId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("fechaHora").descending());
        Page<LogAuditoria> resultado = facturaId != null
                ? logAuditoriaRepository.findByFacturaIdOrderByFechaHoraDesc(facturaId, pageable)
                : logAuditoriaRepository.findAllByOrderByFechaHoraDesc(pageable);
        return resultado.map(AuditoriaResponse::from);
    }
}
