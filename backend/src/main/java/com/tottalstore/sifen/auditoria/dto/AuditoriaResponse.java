package com.tottalstore.sifen.auditoria.dto;

import com.tottalstore.sifen.auditoria.LogAuditoria;
import java.time.Instant;
import java.util.UUID;

public record AuditoriaResponse(
        UUID id,
        String operacion,
        Instant fechaHora,
        String usuarioNombre,
        UUID facturaId) {

    public static AuditoriaResponse from(LogAuditoria log) {
        return new AuditoriaResponse(
                log.getId(),
                log.getOperacion(),
                log.getFechaHora(),
                log.getUsuario() != null ? log.getUsuario().getNombre() : null,
                log.getFactura() != null ? log.getFactura().getId() : null);
    }
}
