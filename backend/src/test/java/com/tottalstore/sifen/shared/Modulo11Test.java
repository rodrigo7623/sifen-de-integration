package com.tottalstore.sifen.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class Modulo11Test {

    @Test
    void calculaElDigitoVerificadorDeLaBase43DelEjemploDelManual() {
        // Base de 43 dígitos del ejemplo oficial del Manual Técnico v150, sección 10.1 (pág. 57).
        // El dígito verificador esperado, según la misma tabla, es 8.
        String base43 = "0144444401700100100145282201701251587326098";

        assertThat(Modulo11.calcularDigitoVerificador(base43)).isEqualTo(8);
    }

    @Test
    void rechazaUnaBaseVacia() {
        assertThatThrownBy(() -> Modulo11.calcularDigitoVerificador(""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Modulo11.calcularDigitoVerificador(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void convierteLetrasASuCodigoAsciiAntesDeAplicarLosPesos() {
        // No hay un ejemplo oficial con letras a mano, pero el algoritmo no debe explotar con una
        // cédula terminada en letra (caso mencionado en el manual para el DV de RUC) -- este test
        // solo verifica que no lance excepción y devuelva un dígito válido (0-9).
        int resultado = Modulo11.calcularDigitoVerificador("1234567A");

        assertThat(resultado).isBetween(0, 9);
    }
}
