package com.tottalstore.sifen.reportes.dto;

import java.math.BigDecimal;

/**
 * Resumen mensual del IVA: débito fiscal del Libro Ventas menos crédito fiscal del Libro Compras. Es
 * referencial para el Formulario 120: no incluye el saldo a favor de meses anteriores, retenciones ni
 * percepciones, que se cargan en la declaración.
 */
public record LiquidacionIvaResponse(
        String periodo,
        int comprobantesVenta,
        BigDecimal totalVentas,
        BigDecimal debitoFiscal10,
        BigDecimal debitoFiscal5,
        BigDecimal debitoFiscal,
        int comprobantesCompra,
        BigDecimal totalCompras,
        BigDecimal creditoFiscal10,
        BigDecimal creditoFiscal5,
        BigDecimal creditoFiscal,
        /** Débito menos crédito: positivo es IVA a pagar; negativo, saldo a favor del contribuyente. */
        BigDecimal saldo,
        int comprobantesParaRg90,
        int comprobantesElectronicosExcluidos) {
}
