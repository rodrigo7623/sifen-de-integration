package com.tottalstore.sifen.reportes.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Libro IVA Ventas de un mes (M4): un renglón por comprobante aprobado por el SIFEN, con los montos
 * separados por tasa. Los montos "gravado" incluyen el IVA (convención del registro de ventas de la
 * SET); la columna de IVA es la porción de impuesto contenida en ellos.
 */
public record LibroIvaResponse(
        String periodo,
        String emisorRuc,
        String emisorRazonSocial,
        List<Renglon> renglones,
        Totales totales) {

    public record Renglon(
            UUID facturaId,
            LocalDate fecha,
            String tipoComprobante,
            String numero,
            String timbrado,
            String cdc,
            String clienteRuc,
            String clienteRazonSocial,
            String condicion,
            BigDecimal gravado10,
            BigDecimal iva10,
            BigDecimal gravado5,
            BigDecimal iva5,
            BigDecimal exento,
            BigDecimal total) {
    }

    public record Totales(
            int cantidadComprobantes,
            BigDecimal gravado10,
            BigDecimal iva10,
            BigDecimal baseImponible10,
            BigDecimal gravado5,
            BigDecimal iva5,
            BigDecimal baseImponible5,
            BigDecimal exento,
            BigDecimal totalIva,
            BigDecimal total) {
    }
}
