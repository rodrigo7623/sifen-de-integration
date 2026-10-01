package com.tottalstore.sifen.dashboard.dto;

import com.tottalstore.sifen.facturacion.EstadoDte;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ResumenDashboardResponse(
        Map<EstadoDte, Long> facturasPorEstado,
        long cantidadFacturasMes,
        BigDecimal totalFacturadoMes,
        List<RechazoResumen> ultimosRechazados) {

    public record RechazoResumen(
            UUID facturaId, String clienteRazonSocial, String motivo, Instant fechaEmision) {
    }
}
