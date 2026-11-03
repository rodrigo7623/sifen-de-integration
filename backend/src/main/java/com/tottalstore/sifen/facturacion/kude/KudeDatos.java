package com.tottalstore.sifen.facturacion.kude;

import java.math.BigDecimal;
import java.util.List;

/**
 * Contenido del KuDE (representación gráfica del DE, sección 13 del Manual Técnico v150). Se arma
 * preferentemente desde el XML firmado ({@link KudeXmlParser}): el manual exige que no haya en el
 * KuDE información que no forme parte del DE firmado. Los campos entre paréntesis son los del manual.
 */
public record KudeDatos(
        String tipoDocumento,
        // Emisor (D1)
        String emisorRazonSocial,
        String emisorNombreFantasia,
        String emisorRuc,
        String emisorActividad,
        String emisorDireccion,
        String emisorCiudad,
        String emisorTelefono,
        String emisorEmail,
        // Timbrado (C)
        String timbrado,
        String timbradoInicio,
        String timbradoFin,
        String numero,
        // Datos generales (D)
        String fechaEmision,
        String condicion,
        String detalleCredito,
        String moneda,
        String tipoTransaccion,
        // Nota de crédito/débito (E402, H004): null en facturas
        String motivoEmision,
        String cdcDocumentoAsociado,
        // Receptor (D2)
        String receptorDocumento,
        String receptorNombre,
        String receptorDireccion,
        String receptorTelefono,
        String receptorEmail,
        List<Item> items,
        // Totales (F)
        BigDecimal subtotalExentas,
        BigDecimal subtotal5,
        BigDecimal subtotal10,
        BigDecimal totalOperacion,
        BigDecimal totalGuaranies,
        BigDecimal liquidacionIva5,
        BigDecimal liquidacionIva10,
        BigDecimal totalIva,
        // Consulta (A002, J002)
        String cdc,
        String urlQr,
        boolean ambienteTest,
        /** True si el contenido sale del XML firmado; false si es una vista previa armada con los
         * datos de la factura (borrador o modo stub). */
        boolean desdeXmlFirmado) {

    public record Item(
            String codigo,
            String descripcion,
            String unidad,
            BigDecimal cantidad,
            BigDecimal precioUnitario,
            BigDecimal descuento,
            BigDecimal exentas,
            BigDecimal gravado5,
            BigDecimal gravado10) {
    }

    /** CDC en once grupos de cuatro posiciones, como pide el manual para el KuDE. */
    public String cdcAgrupado() {
        return agrupar(cdc);
    }

    public String cdcAgrupadoAsociado() {
        return agrupar(cdcDocumentoAsociado);
    }

    private static String agrupar(String cdc) {
        if (cdc == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cdc.length(); i += 4) {
            if (i > 0) {
                sb.append(' ');
            }
            sb.append(cdc, i, Math.min(i + 4, cdc.length()));
        }
        return sb.toString();
    }
}
