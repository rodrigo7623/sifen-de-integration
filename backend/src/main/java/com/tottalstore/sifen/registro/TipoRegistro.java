package com.tottalstore.sifen.registro;

/** Tabla 1 de la Especificación Técnica del Registro de Comprobantes (RG 90/2021). */
public enum TipoRegistro {
    VENTA(1),
    COMPRA(2);

    private final int codigo;

    TipoRegistro(int codigo) {
        this.codigo = codigo;
    }

    public int codigo() {
        return codigo;
    }
}
