package com.tottalstore.sifen.clientes;

import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Implementación stub de {@link ValidadorRuc}: valida formato y dígito verificador localmente,
 * sin consultar el padrón real de la SET (eso queda pendiente hasta contar con acceso al servicio
 * oficial de consulta de RUC).
 */
@Component
public class ValidadorRucStub implements ValidadorRuc {

    private static final Pattern FORMATO = Pattern.compile("^\\d{4,8}-\\d$");

    @Override
    public ResultadoValidacionRuc validar(String ruc) {
        if (ruc == null || ruc.isBlank()) {
            return ResultadoValidacionRuc.invalido("El RUC es obligatorio");
        }
        String rucLimpio = ruc.trim();
        if (!FORMATO.matcher(rucLimpio).matches()) {
            return ResultadoValidacionRuc.invalido(
                    "Formato inválido. Use número base y dígito verificador, ej: 80012345-6");
        }

        String[] partes = rucLimpio.split("-");
        String base = partes[0];
        int dvInformado = Integer.parseInt(partes[1]);
        int dvCalculado = calcularDigitoVerificador(base);

        if (dvCalculado != dvInformado) {
            return ResultadoValidacionRuc.invalido("El dígito verificador no corresponde al número base");
        }
        return ResultadoValidacionRuc.aceptado();
    }

    /**
     * Algoritmo de dígito verificador de RUC usado habitualmente en Paraguay (módulo 11, pesos
     * cíclicos 2..11 de derecha a izquierda). Solo confirma consistencia matemática del número, no
     * que el contribuyente esté inscripto ante la SET; verificar contra la especificación oficial
     * al integrar el servicio real de consulta de RUC.
     */
    private int calcularDigitoVerificador(String base) {
        int total = 0;
        int peso = 2;
        for (int i = base.length() - 1; i >= 0; i--) {
            int digito = Character.getNumericValue(base.charAt(i));
            total += digito * peso;
            peso = (peso == 11) ? 2 : peso + 1;
        }
        int resto = total % 11;
        return resto > 1 ? 11 - resto : 0;
    }
}
