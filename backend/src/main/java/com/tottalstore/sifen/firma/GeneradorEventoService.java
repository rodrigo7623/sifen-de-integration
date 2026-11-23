package com.tottalstore.sifen.firma;

import java.time.LocalDateTime;

/**
 * Arma y firma el grupo {@code <gGroupGesEve>} de un evento del emisor (Manual Técnico v150, 11.5 y
 * 11.5.1), que va dentro de {@code <dEvReg>} en {@code siRecepEvento}. La firma XMLDSig cubre el
 * {@code <rEve>}, cuyo {@code Id} es {@code idEvento}.
 *
 * <p>Implementación real: {@code GeneradorEventoSifen} ({@code sifen.envio.modo=real}). Por defecto,
 * {@link GeneradorEventoStub}, sin firma.
 */
public interface GeneradorEventoService {

    /** GEC001 rGeVeCan: cancelación del DTE con ese CDC. */
    String cancelacion(long idEvento, LocalDateTime fechaFirma, String cdc, String motivo);

    /** GEI001 rGeVeInu: inutilización de un rango de números (hasta 1000) de un tipo de DE. */
    String inutilizacion(long idEvento, LocalDateTime fechaFirma, Inutilizacion datos);

    /** dEst/dPunExp a 3 dígitos, dNumIn/dNumFin a 7, iTiDE según C002. */
    record Inutilizacion(String timbrado, String establecimiento, String puntoExpedicion,
                         int desde, int hasta, int tipoDocumento, String motivo) {
    }
}
