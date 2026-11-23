package com.tottalstore.sifen.sifen;

import java.util.List;

/**
 * WS del SIFEN para eventos y envío asíncrono (Manual Técnico v150): {@code siRecepEvento} (9.5),
 * {@code siRecepLoteDE} (9.2) y {@code siResultLoteDE} (9.3).
 *
 * <p>Implementación real: {@link EventosLotesSifenSoap} ({@code sifen.envio.modo=real}). Por defecto,
 * {@link EventosLotesSifenStub}.
 */
public interface EventosLotesSifenService {

    /** Envía el {@code <gGroupGesEve>} ya firmado dentro de {@code <dEvReg>}. */
    RespuestaEvento enviarEvento(long idEvento, String gGroupGesEveFirmado, Ambiente ambiente);

    /** Comprime los {@code <rDE>} firmados en un {@code <rLoteDE>} (zip + base64) y los envía. */
    RespuestaLote enviarLote(List<String> rDeFirmados, Ambiente ambiente);

    /** Resultado del procesamiento de cada DE del lote. */
    ResultadoLote consultarLote(String numeroLote, Ambiente ambiente);

    /** gResProcEVe: dEstRes, dProtAut, gResProc (dCodRes/dMsgRes). 0600 = evento registrado. */
    record RespuestaEvento(boolean aprobado, String codigo, String mensaje, String protocolo, boolean sinConfirmar) {

        public static RespuestaEvento sinConfirmar(String codigo, String mensaje) {
            return new RespuestaEvento(false, codigo, mensaje, null, true);
        }
    }

    /** rResEnviLoteDe: 0300 = lote recibido, con dProtConsLote; cualquier otro código, no encolado. */
    record RespuestaLote(boolean recibido, String codigo, String mensaje, String numeroLote, boolean sinConfirmar) {

        public static RespuestaLote sinConfirmar(String codigo, String mensaje) {
            return new RespuestaLote(false, codigo, mensaje, null, true);
        }
    }

    /**
     * rResEnviConsLoteDe: 0362 = procesamiento concluido (con un resultado por DE); 0361 = todavía en
     * procesamiento; 0360 = lote inexistente. {@code sinConfirmar}: la consulta misma no tuvo respuesta.
     */
    record ResultadoLote(String codigo, String mensaje, List<ResultadoDe> documentos, boolean sinConfirmar) {

        public static final String CONCLUIDO = "0362";
        public static final String EN_PROCESAMIENTO = "0361";

        public boolean concluido() {
            return CONCLUIDO.equals(codigo);
        }
    }

    /** gResProcLote: id (CDC), dEstRes, dProtAut y el primer gResProc. */
    record ResultadoDe(String cdc, boolean aprobado, String estado, String codigo, String mensaje) {
    }
}
