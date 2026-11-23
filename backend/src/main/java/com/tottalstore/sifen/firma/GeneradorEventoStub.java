package com.tottalstore.sifen.firma;

import java.time.LocalDateTime;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Implementación stub (default): el evento sin firma, solo para ejercitar el flujo en modo simulado. */
@Component
@ConditionalOnProperty(name = "sifen.envio.modo", havingValue = "stub", matchIfMissing = true)
public class GeneradorEventoStub implements GeneradorEventoService {

    @Override
    public String cancelacion(long idEvento, LocalDateTime fechaFirma, String cdc, String motivo) {
        return "<gGroupGesEve><rGesEve><rEve Id=\"" + idEvento + "\"><dFecFirma>" + fechaFirma
                + "</dFecFirma><gGroupTiEvt><rGeVeCan><Id>" + cdc + "</Id><mOtEve>" + escapar(motivo)
                + "</mOtEve></rGeVeCan></gGroupTiEvt></rEve></rGesEve></gGroupGesEve>";
    }

    @Override
    public String inutilizacion(long idEvento, LocalDateTime fechaFirma, Inutilizacion d) {
        return "<gGroupGesEve><rGesEve><rEve Id=\"" + idEvento + "\"><dFecFirma>" + fechaFirma
                + "</dFecFirma><gGroupTiEvt><rGeVeInu><dNumTim>" + d.timbrado() + "</dNumTim><dEst>"
                + d.establecimiento() + "</dEst><dPunExp>" + d.puntoExpedicion() + "</dPunExp><dNumIn>"
                + d.desde() + "</dNumIn><dNumFin>" + d.hasta() + "</dNumFin><iTiDE>" + d.tipoDocumento()
                + "</iTiDE><mOtEve>" + escapar(d.motivo()) + "</mOtEve></rGeVeInu></gGroupTiEvt></rEve>"
                + "</rGesEve></gGroupGesEve>";
    }

    private static String escapar(String texto) {
        return texto == null ? "" : texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
