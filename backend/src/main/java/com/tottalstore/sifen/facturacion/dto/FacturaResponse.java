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
        UUID establecimientoId,
        String establecimientoCodigo,
        String establecimientoDenominacion,
        UUID puntoExpedicionId,
        String puntoExpedicionCodigo,
        String puntoExpedicionDescripcion,
        CondicionPago condicionPago,
        Integer plazoDias,
        Integer cantidadCuotas,
        BigDecimal totalIva5,
        BigDecimal totalIva10,
        BigDecimal totalGeneral,
        Instant fechaEmision,
        List<ItemFacturaResponse> items,
        String motivoRechazo,
        boolean emailEnviado) {

    public static FacturaResponse from(FacturaElectronica f) {
        boolean rechazada = f.getEstadoDte() == EstadoDte.RECHAZADO;
        return new FacturaResponse(
                f.getId(),
                f.getEstadoDte(),
                f.getCliente() != null ? f.getCliente().getRuc() : null,
                f.getCliente() != null ? f.getCliente().getRazonSocial() : null,
                f.getEstablecimiento() != null ? f.getEstablecimiento().getId() : null,
                f.getEstablecimiento() != null ? f.getEstablecimiento().getCodigo() : null,
                f.getEstablecimiento() != null ? f.getEstablecimiento().getDenominacion() : null,
                f.getPuntoExpedicion() != null ? f.getPuntoExpedicion().getId() : null,
                f.getPuntoExpedicion() != null ? f.getPuntoExpedicion().getCodigo() : null,
                f.getPuntoExpedicion() != null ? f.getPuntoExpedicion().getDescripcion() : null,
                f.getCondicionPago(),
                f.getPlazoDias(),
                f.getCantidadCuotas(),
                f.getTotalIva5(),
                f.getTotalIva10(),
                f.getTotalGeneral(),
                f.getFechaEmision(),
                f.getItems().stream().map(ItemFacturaResponse::from).toList(),
                rechazada && f.getRespuestaSifen() != null ? f.getRespuestaSifen().getDescripcion() : null,
                f.isEmailEnviado());
    }
}
