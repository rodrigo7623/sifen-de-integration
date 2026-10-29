package com.tottalstore.sifen.reportes.dto;

import com.tottalstore.sifen.facturacion.EstadoDte;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Historial de facturación de un cliente (Release 3): resumen y cada factura con su paso por el SIFEN. */
public record HistorialClienteResponse(
        String ruc,
        String razonSocial,
        long cantidadFacturas,
        long aprobadas,
        long rechazadas,
        long pendientes,
        BigDecimal montoAprobado,
        long intentosEnvio,
        long intentosRechazados,
        Instant ultimaFactura,
        List<FacturaCliente> facturas) {

    public record FacturaCliente(
            UUID facturaId,
            Instant fechaEmision,
            String numero,
            EstadoDte estado,
            BigDecimal total,
            long intentos,
            String ultimoCodigo,
            String ultimoMensaje) {
    }
}
