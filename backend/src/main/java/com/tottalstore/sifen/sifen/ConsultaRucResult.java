package com.tottalstore.sifen.sifen;

/**
 * Resultado de {@code siConsRUC} (Manual Técnico v150, sección 9.6). {@code codigo} es 0500 (RUC
 * inexistente), 0501 (sin permiso para el WS) o 0502 (encontrado); los datos del contribuyente solo
 * vienen con 0502. Si la llamada no llegó a tener respuesta del SIFEN, {@code codigo} es
 * ERROR_HTTP o ERROR_CONEXION y {@code respondioSifen} es false.
 */
public record ConsultaRucResult(
        boolean respondioSifen,
        String codigo,
        String mensaje,
        String ruc,
        String razonSocial,
        String codigoEstado,
        String descripcionEstado,
        Boolean facturadorElectronico) {

    static ConsultaRucResult sinRespuesta(String codigo, String mensaje) {
        return new ConsultaRucResult(false, codigo, mensaje, null, null, null, null, null);
    }
}
