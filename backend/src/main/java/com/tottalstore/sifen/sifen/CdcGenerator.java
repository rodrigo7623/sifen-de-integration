package com.tottalstore.sifen.sifen;

import com.tottalstore.sifen.shared.Modulo11;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

/**
 * Genera el CDC (Código de Control del DTE, 44 dígitos) según la "Conformación del CDC" del Manual
 * Técnico del SIFEN v150, sección 10.1 (pág. 57): 10 campos de ancho fijo concatenados (43 dígitos)
 * más 1 dígito verificador módulo 11 ({@link Modulo11}).
 *
 * <p><b>Fase 2 de la integración real</b> (ver plan): esta clase está verificada byte a byte contra
 * el ejemplo numérico que trae el propio manual ({@link CdcGeneratorTest}), pero todavía no está
 * conectada al flujo real de emisión — eso ocurre en la Fase 3, cuando exista el XML completo del
 * DE que necesita este CDC como atributo {@code Id} de la raíz.
 */
@Component
public class CdcGenerator {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("yyyyMMdd");

    public String generar(CdcDatos datos) {
        String base = new StringBuilder()
                .append(ancho(datos.tipoDocumento(), 2))
                .append(ancho(datos.rucEmisor(), 8))
                .append(ancho(String.valueOf(datos.dvEmisor()), 1))
                .append(ancho(datos.establecimiento(), 3))
                .append(ancho(datos.puntoExpedicion(), 3))
                .append(ancho(datos.numeroDocumento(), 7))
                .append(ancho(String.valueOf(datos.tipoContribuyente()), 1))
                .append(FECHA.format(datos.fechaEmision()))
                .append(ancho(String.valueOf(datos.tipoEmision()), 1))
                .append(ancho(datos.codigoSeguridad(), 9))
                .toString();

        if (base.length() != 43) {
            throw new IllegalStateException(
                    "La base del CDC debería tener 43 dígitos, tiene " + base.length() + ": " + base);
        }

        int digitoVerificador = Modulo11.calcularDigitoVerificador(base);
        return base + digitoVerificador;
    }

    /** Completa con ceros a la izquierda hasta el ancho fijo del campo (todos los campos numéricos
     * del CDC se rellenan así según el manual) y valida que no lo exceda. */
    private String ancho(String valor, int longitud) {
        if (valor == null) {
            throw new IllegalArgumentException("Un campo del CDC no puede ser null");
        }
        if (valor.length() > longitud) {
            throw new IllegalArgumentException(
                    "El campo '" + valor + "' excede el ancho fijo de " + longitud + " dígitos del CDC");
        }
        return "0".repeat(longitud - valor.length()) + valor;
    }

    /**
     * Los 10 campos que componen el CDC, en el orden exacto de la tabla "Conformación del CDC"
     * (Manual Técnico v150, pág. 57). Cada campo se completa con ceros a la izquierda hasta su
     * ancho fijo -- pasar el valor "crudo" (p.ej. "1" para establecimiento, no "001").
     */
    public record CdcDatos(
            String tipoDocumento,
            String rucEmisor,
            int dvEmisor,
            String establecimiento,
            String puntoExpedicion,
            String numeroDocumento,
            int tipoContribuyente,
            LocalDate fechaEmision,
            int tipoEmision,
            String codigoSeguridad) {}
}
