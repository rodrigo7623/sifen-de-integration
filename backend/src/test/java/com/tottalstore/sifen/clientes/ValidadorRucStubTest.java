package com.tottalstore.sifen.clientes;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ValidadorRucStubTest {

    private final ValidadorRucStub validador = new ValidadorRucStub();

    @Test
    void aceptaRucConDigitoVerificadorCorrecto() {
        // Base 80012345 -> dígito verificador calculado 0 (módulo 11, pesos cíclicos 2..11).
        ResultadoValidacionRuc resultado = validador.validar("80012345-0");

        assertThat(resultado.valido()).isTrue();
    }

    @Test
    void rechazaRucConDigitoVerificadorIncorrecto() {
        ResultadoValidacionRuc resultado = validador.validar("80012345-6");

        assertThat(resultado.valido()).isFalse();
        assertThat(resultado.mensaje()).containsIgnoringCase("dígito verificador");
    }

    @Test
    void rechazaFormatoSinGuion() {
        ResultadoValidacionRuc resultado = validador.validar("800123450");

        assertThat(resultado.valido()).isFalse();
        assertThat(resultado.mensaje()).containsIgnoringCase("formato");
    }

    @Test
    void rechazaRucVacio() {
        ResultadoValidacionRuc resultado = validador.validar("  ");

        assertThat(resultado.valido()).isFalse();
        assertThat(resultado.mensaje()).containsIgnoringCase("obligatorio");
    }
}
