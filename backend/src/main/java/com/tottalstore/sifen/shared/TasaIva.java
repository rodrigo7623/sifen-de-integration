package com.tottalstore.sifen.shared;

import java.math.BigDecimal;

/** Tasas de IVA vigentes en Paraguay aplicables a un ítem de factura (RF-12). */
public enum TasaIva {
    EXENTA(0),
    CINCO(5),
    DIEZ(10);

    private final int porcentaje;

    TasaIva(int porcentaje) {
        this.porcentaje = porcentaje;
    }

    public int porcentaje() {
        return porcentaje;
    }

    public BigDecimal fraccion() {
        return BigDecimal.valueOf(porcentaje).divide(BigDecimal.valueOf(100));
    }
}
