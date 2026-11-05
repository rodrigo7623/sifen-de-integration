package com.tottalstore.sifen.registro;

/** Tabla 3 de la Especificación Técnica del Registro de Comprobantes (RG 90/2021). */
public enum TipoIdentificacion {
    RUC(11, "RUC"),
    CEDULA(12, "Cédula de identidad"),
    PASAPORTE(13, "Pasaporte"),
    CEDULA_EXTRANJERO(14, "Cédula de extranjero"),
    SIN_NOMBRE(15, "Sin nombre"),
    DIPLOMATICO(16, "Diplomático"),
    IDENTIFICACION_TRIBUTARIA(17, "Identificación tributaria");

    private final int codigo;
    private final String descripcion;

    TipoIdentificacion(int codigo, String descripcion) {
        this.codigo = codigo;
        this.descripcion = descripcion;
    }

    public int codigo() {
        return codigo;
    }

    public String descripcion() {
        return descripcion;
    }

    /** RUC, cédula y "sin nombre": la SET toma el nombre de sus registros y no exige cargarlo. */
    public boolean nombreOpcional() {
        return this == RUC || this == CEDULA || this == SIN_NOMBRE;
    }
}
