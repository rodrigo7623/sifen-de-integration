package com.tottalstore.sifen.facturacion;

/**
 * Tipo de documento electrónico (C002 iTiDE / C003 dDesTiDE del Manual Técnico v150). El nombre de
 * cada constante es el valor de la columna {@code tipo_doc}. Cada tipo lleva su propia numeración
 * dentro del punto de expedición (C007 dNumDoc).
 */
public enum TipoDocumento {
    FE(1, "Factura electrónica"),
    NC(5, "Nota de crédito electrónica"),
    ND(6, "Nota de débito electrónica");

    private final int codigo;
    private final String descripcion;

    TipoDocumento(int codigo, String descripcion) {
        this.codigo = codigo;
        this.descripcion = descripcion;
    }

    /** C002 iTiDE. */
    public int codigo() {
        return codigo;
    }

    /** C003 dDesTiDE, también el título del KuDE. */
    public String descripcion() {
        return descripcion;
    }

    /** Primer campo del CDC (tipo de documento a 2 dígitos, sección 10.1). */
    public String codigoCdc() {
        return String.format("%02d", codigo);
    }

    public boolean esNota() {
        return this == NC || this == ND;
    }
}
