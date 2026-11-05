package com.tottalstore.sifen.sifen;

/**
 * Resultado de un envío o de una consulta de DE al SIFEN.
 *
 * <p>{@code sinConfirmar}: no se sabe si el SIFEN recibió el DE (timeout esperando la respuesta, la
 * conexión se cortó a mitad del envío o la respuesta no se pudo leer). No es un rechazo: el DE pudo
 * haber quedado aprobado, así que hay que consultarlo por su CDC antes de reenviarlo (BUG-006).
 */
public record RespuestaSifenResult(boolean aprobado, String codigo, String descripcion, String cdc, boolean sinConfirmar) {

    public RespuestaSifenResult(boolean aprobado, String codigo, String descripcion, String cdc) {
        this(aprobado, codigo, descripcion, cdc, false);
    }

    public static RespuestaSifenResult sinConfirmar(String codigo, String descripcion) {
        return new RespuestaSifenResult(false, codigo, descripcion, null, true);
    }
}
