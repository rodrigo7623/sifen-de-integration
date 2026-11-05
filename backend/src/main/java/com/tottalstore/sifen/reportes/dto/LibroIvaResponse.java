package com.tottalstore.sifen.reportes.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Libro IVA de un mes (M4): Ventas (débito fiscal) o Compras (crédito fiscal), un renglón por
 * comprobante, con los montos separados por tasa. Los montos "gravado" incluyen el IVA (convención del
 * Registro de Comprobantes de la SET); la columna de IVA es la porción de impuesto contenida en ellos.
 * Las notas de crédito van con montos negativos.
 */
public record LibroIvaResponse(
        TipoLibro libro,
        String periodo,
        String emisorRuc,
        String emisorRazonSocial,
        List<Renglon> renglones,
        Totales totales) {

    public enum TipoLibro {
        VENTAS("Libro IVA Ventas", "RUC/CI cliente", "Cliente", "IVA débito fiscal"),
        COMPRAS("Libro IVA Compras", "RUC proveedor", "Proveedor", "IVA crédito fiscal");

        private final String titulo;
        private final String columnaRuc;
        private final String columnaRazonSocial;
        private final String etiquetaIva;

        TipoLibro(String titulo, String columnaRuc, String columnaRazonSocial, String etiquetaIva) {
            this.titulo = titulo;
            this.columnaRuc = columnaRuc;
            this.columnaRazonSocial = columnaRazonSocial;
            this.etiquetaIva = etiquetaIva;
        }

        public String titulo() {
            return titulo;
        }

        public String columnaRuc() {
            return columnaRuc;
        }

        public String columnaRazonSocial() {
            return columnaRazonSocial;
        }

        public String etiquetaIva() {
            return etiquetaIva;
        }

        public String archivo() {
            return this == VENTAS ? "libro-iva-ventas" : "libro-iva-compras";
        }
    }

    /** De dónde sale el renglón: un DE propio aprobado por el SIFEN, o un comprobante cargado a mano. */
    public enum Origen {
        SIFEN,
        REGISTRADO,
        DE_RECIBIDO
    }

    public record Renglon(
            UUID facturaId,
            UUID comprobanteRegistradoId,
            Origen origen,
            LocalDate fecha,
            String tipoComprobante,
            String sigla,
            String numero,
            String timbrado,
            String cdc,
            String ruc,
            String razonSocial,
            String condicion,
            String documentoAsociado,
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
