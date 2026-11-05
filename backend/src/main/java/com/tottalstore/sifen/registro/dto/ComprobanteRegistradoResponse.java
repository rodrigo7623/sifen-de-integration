package com.tottalstore.sifen.registro.dto;

import com.tottalstore.sifen.facturacion.CondicionPago;
import com.tottalstore.sifen.registro.ComprobanteRegistrado;
import com.tottalstore.sifen.registro.TipoComprobanteRg90;
import com.tottalstore.sifen.registro.TipoIdentificacion;
import com.tottalstore.sifen.registro.TipoRegistro;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ComprobanteRegistradoResponse(
        UUID id,
        TipoRegistro tipoRegistro,
        TipoComprobanteRg90 tipoComprobante,
        String tipoComprobanteDescripcion,
        LocalDate fechaEmision,
        TipoIdentificacion tipoIdentificacion,
        String identificacion,
        String razonSocial,
        String timbrado,
        String numero,
        String cdc,
        boolean electronico,
        CondicionPago condicion,
        boolean monedaExtranjera,
        BigDecimal gravado10,
        BigDecimal gravado5,
        BigDecimal exento,
        BigDecimal total,
        BigDecimal iva10,
        BigDecimal iva5,
        boolean imputaIva,
        boolean imputaIre,
        boolean imputaIrp,
        boolean noImputa,
        String asociadoNumero,
        String asociadoTimbrado,
        String observacion,
        String creadoPor,
        String modificadoPor,
        Instant updatedAt) {

    public static ComprobanteRegistradoResponse from(ComprobanteRegistrado c) {
        return new ComprobanteRegistradoResponse(
                c.getId(),
                c.getTipoRegistro(),
                c.getTipoComprobante(),
                c.getTipoComprobante().descripcion(),
                c.getFechaEmision(),
                c.getTipoIdentificacion(),
                c.getIdentificacion(),
                c.getRazonSocial(),
                c.getTimbrado(),
                c.getNumero(),
                c.getCdc(),
                c.esElectronico(),
                c.getCondicion(),
                c.isMonedaExtranjera(),
                c.getGravado10(),
                c.getGravado5(),
                c.getExento(),
                c.getTotal(),
                c.getIva10(),
                c.getIva5(),
                c.isImputaIva(),
                c.isImputaIre(),
                c.isImputaIrp(),
                c.isNoImputa(),
                c.getAsociadoNumero(),
                c.getAsociadoTimbrado(),
                c.getObservacion(),
                c.getCreadoPor() != null ? c.getCreadoPor().getEmail() : null,
                c.getModificadoPor() != null ? c.getModificadoPor().getEmail() : null,
                c.getUpdatedAt());
    }
}
