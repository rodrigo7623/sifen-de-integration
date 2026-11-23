package com.tottalstore.sifen.facturacion.dto;

import com.tottalstore.sifen.facturacion.EstadoDte;
import com.tottalstore.sifen.sifen.EventoSifen;
import java.time.Instant;
import java.util.UUID;

/** Evento del emisor y en qué estado dejó al documento. */
public record EventoResponse(
        Long id,
        EventoSifen.Tipo tipo,
        UUID facturaId,
        String numero,
        String motivo,
        Instant fecha,
        EventoSifen.Estado estado,
        String codigo,
        String mensaje,
        String protocolo,
        EstadoDte estadoDocumento) {

    public static EventoResponse from(EventoSifen e) {
        return new EventoResponse(e.getId(), e.getTipo(), e.getFactura().getId(), e.getFactura().numeroComprobante(),
                e.getMotivo(), e.getFecha(), e.getEstado(), e.getCodigo(), e.getMensaje(), e.getProtocolo(),
                e.getFactura().getEstadoDte());
    }
}
