package com.tottalstore.sifen.facturacion.dto;

import com.tottalstore.sifen.facturacion.CondicionPago;
import com.tottalstore.sifen.facturacion.EstadoDte;
import com.tottalstore.sifen.facturacion.FacturaElectronica;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record FacturaResponse(
        UUID id,
        EstadoDte estadoDte,
        String clienteRuc,
        String clienteRazonSocial,
        CondicionPago condicionPago,
        Integer plazoDias,
        Integer cantidadCuotas,
        BigDecimal totalIva5,
        BigDecimal totalIva10,
        BigDecimal totalGeneral,
        Instant fechaEmision,
        List<ItemFacturaResponse> items) {

    public static FacturaResponse from(FacturaElectronica f) {
        return new FacturaResponse(
                f.getId(),
                f.getEstadoDte(),
                f.getCliente() != null ? f.getCliente().getRuc() : null,
                f.getCliente() != null ? f.getCliente().getRazonSocial() : null,
                f.getCondicionPago(),
                f.getPlazoDias(),
                f.getCantidadCuotas(),
                f.getTotalIva5(),
                f.getTotalIva10(),
                f.getTotalGeneral(),
                f.getFechaEmision(),
                f.getItems().stream().map(ItemFacturaResponse::from).toList());
    }
}
