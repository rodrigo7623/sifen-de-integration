package com.tottalstore.sifen.facturacion.dto;

import com.tottalstore.sifen.facturacion.CondicionPago;
import com.tottalstore.sifen.facturacion.EstadoDte;
import com.tottalstore.sifen.facturacion.FacturaElectronica;
import com.tottalstore.sifen.facturacion.MotivoNota;
import com.tottalstore.sifen.facturacion.TipoDocumento;
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
        boolean emailEnviado,
        /** Número impreso 001-001-0000001; null mientras no se confirmó nunca. */
        String numero,
        String cdc,
        TipoDocumento tipoDocumento,
        /** Solo en notas de crédito/débito. */
        MotivoNota motivoEmision,
        UUID facturaAsociadaId,
        String facturaAsociadaNumero,
        String facturaAsociadaCdc,
        BigDecimal facturaAsociadaTotal,
        /** Email del cliente al que se manda el documento aprobado (RF-09); null si no tiene. */
        String clienteEmail) {

    public static FacturaResponse from(FacturaElectronica f) {
        // EN_PROCESO también muestra el último mensaje del SIFEN (por qué hay que verificarlo).
        boolean rechazada = f.getEstadoDte() == EstadoDte.RECHAZADO || f.getEstadoDte() == EstadoDte.EN_PROCESO;
        FacturaElectronica asociada = f.getFacturaAsociada();
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
                f.isEmailEnviado(),
                f.numeroComprobante(),
                f.getCdc(),
                f.getTipoDoc(),
                f.getMotivoEmision(),
                asociada != null ? asociada.getId() : null,
                asociada != null ? asociada.numeroComprobante() : null,
                asociada != null ? asociada.cdcVigente() : null,
                asociada != null ? asociada.getTotalGeneral() : null,
                f.getCliente() != null ? f.getCliente().getEmail() : null);
    }
}
