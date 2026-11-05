package com.tottalstore.sifen.auditoria.dto;

import com.tottalstore.sifen.auditoria.LogAuditoria;
import com.tottalstore.sifen.auth.Usuario;
import com.tottalstore.sifen.facturacion.EstadoDte;
import com.tottalstore.sifen.facturacion.FacturaElectronica;
import com.tottalstore.sifen.facturacion.TipoDocumento;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Una entrada de la auditoría con los datos del usuario y del documento afectado, para que la
 * pantalla muestre el número del comprobante y el cliente en vez del UUID. {@code documentoEstado}
 * es el estado actual del documento, no el que tenía al momento de la operación.
 */
public record AuditoriaResponse(
        UUID id,
        String operacion,
        Instant fechaHora,
        String usuarioNombre,
        String usuarioEmail,
        String usuarioRol,
        UUID facturaId,
        TipoDocumento documentoTipo,
        String documentoNumero,
        EstadoDte documentoEstado,
        String clienteRuc,
        String clienteRazonSocial,
        BigDecimal documentoTotal) {

    public static AuditoriaResponse from(LogAuditoria log) {
        Usuario u = log.getUsuario();
        FacturaElectronica f = log.getFactura();
        return new AuditoriaResponse(
                log.getId(),
                log.getOperacion(),
                log.getFechaHora(),
                u != null ? u.getNombre() : null,
                u != null ? u.getEmail() : null,
                u != null && u.getRol() != null ? u.getRol().name() : null,
                f != null ? f.getId() : null,
                f != null ? f.getTipoDoc() : null,
                f != null ? f.numeroComprobante() : null,
                f != null ? f.getEstadoDte() : null,
                f != null && f.getCliente() != null ? f.getCliente().getRuc() : null,
                f != null && f.getCliente() != null ? f.getCliente().getRazonSocial() : null,
                f != null ? f.getTotalGeneral() : null);
    }
}
