package com.tottalstore.sifen.sifen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/**
 * El caso "ManualTecnico" replica EXACTAMENTE el ejemplo numérico de la sección 10.1 (pág. 57) del
 * Manual Técnico del SIFEN v150 -- incluido el dígito verificador esperado (8) y la representación
 * agrupada en bloques de 4 que trae el propio manual. Si esto pasa, el generador arma el CDC en el
 * mismo orden y ancho de campo que espera la SET.
 */
class CdcGeneratorTest {

    private final CdcGenerator generator = new CdcGenerator();

    @Test
    void generaElCdcDelEjemploOficialDelManualTecnico() {
        var datos = new CdcGenerator.CdcDatos(
                "01", // Factura electrónica
                "44444401", // RUC emisor (ya de 8 dígitos en el ejemplo)
                7, // DV del emisor
                "001", // Establecimiento
                "001", // Punto de expedición
                "0014528", // Número de documento
                2, // Tipo de contribuyente
                LocalDate.of(2017, 1, 25), // Fecha de emisión
                1, // Tipo de emisión
                "587326098" // Código de seguridad
                );

        String cdc = generator.generar(datos);

        // Representación agrupada en bloques de 4 que trae el manual: "0144 4444 0170 0100 1001
        // 4528 2201 7012 5158 7326 0988" -- sin espacios es el CDC completo esperado.
        assertThat(cdc).isEqualTo("01444444017001001001452822017012515873260988");
        assertThat(cdc).hasSize(44);
        assertThat(cdc).endsWith("8"); // dígito verificador esperado según la tabla del manual
    }

    @Test
    void completaLosCamposConCerosALaIzquierdaHastaSuAnchoFijo() {
        var datos = new CdcGenerator.CdcDatos(
                "1", // -> "01"
                "12345", // -> "00012345"
                0,
                "1", // -> "001"
                "1", // -> "001"
                "1", // -> "0000001"
                2,
                LocalDate.of(2026, 9, 22),
                1,
                "1" // -> "000000001"
                );

        String cdc = generator.generar(datos);

        assertThat(cdc).hasSize(44);
        assertThat(cdc).startsWith("0100012345"); // tipoDocumento(2) + rucEmisor(8)
    }

    @Test
    void rechazaUnCampoMasLargoQueSuAnchoFijo() {
        var datos = new CdcGenerator.CdcDatos(
                "01",
                "123456789", // 9 dígitos, el ancho fijo de rucEmisor es 8
                7,
                "001",
                "001",
                "0014528",
                2,
                LocalDate.of(2017, 1, 25),
                1,
                "587326098");

        assertThatThrownBy(() -> generator.generar(datos)).isInstanceOf(IllegalArgumentException.class);
    }
}
