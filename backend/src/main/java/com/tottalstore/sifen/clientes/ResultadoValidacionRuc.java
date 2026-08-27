package com.tottalstore.sifen.clientes;

public record ResultadoValidacionRuc(boolean valido, String mensaje) {

    public static ResultadoValidacionRuc aceptado() {
        return new ResultadoValidacionRuc(true, "RUC con formato válido");
    }

    public static ResultadoValidacionRuc invalido(String mensaje) {
        return new ResultadoValidacionRuc(false, mensaje);
    }
}
