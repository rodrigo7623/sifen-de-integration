package com.tottalstore.sifen.facturacion;

/**
 * Motivo de emisión de una nota de crédito/débito (E401 iMotEmi / E402 dDesMotEmi del Manual Técnico
 * v150). La descripción debe ser exactamente la del manual: si no coincide con el código, el SIFEN
 * rechaza con 1452.
 */
public enum MotivoNota {
    DEVOLUCION_Y_AJUSTE_PRECIOS(1, "Devolución y Ajuste de precios"),
    DEVOLUCION(2, "Devolución"),
    DESCUENTO(3, "Descuento"),
    BONIFICACION(4, "Bonificación"),
    CREDITO_INCOBRABLE(5, "Crédito incobrable"),
    RECUPERO_COSTO(6, "Recupero de costo"),
    RECUPERO_GASTO(7, "Recupero de gasto"),
    AJUSTE_PRECIO(8, "Ajuste de precio");

    private final int codigo;
    private final String descripcion;

    MotivoNota(int codigo, String descripcion) {
        this.codigo = codigo;
        this.descripcion = descripcion;
    }

    public int codigo() {
        return codigo;
    }

    public String descripcion() {
        return descripcion;
    }
}
