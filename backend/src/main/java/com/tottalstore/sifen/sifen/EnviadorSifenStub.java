package com.tottalstore.sifen.sifen;

import java.security.SecureRandom;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Implementación stub (default): simula una aprobación del SIFEN en ambiente TEST, generando un CDC
 * ficticio de 44 dígitos (longitud real de un CDC de DTE), sin conectarse a nada. Activa mientras
 * {@code sifen.envio.modo} no sea {@code real} (ver {@link EnviadorSifenSoapService} para la
 * integración real).
 *
 * <p>Para poder probar manualmente el flujo de "gestión de rechazados" (Release 2 / M3) sin un
 * SIFEN real que rechace nada, este stub rechaza de forma determinística cuando el XML contiene la
 * palabra "RECHAZAR" (por ejemplo, en la descripción de un ítem) — ver
 * {@code GeneradorDteStub}. Del mismo modo, la marca "SIN_RESPUESTA" simula un timeout del SIFEN
 * (BUG-006): el DE queda sin confirmar y se resuelve consultándolo con {@link #consultarDe}.
 */
@Component
@ConditionalOnProperty(name = "sifen.envio.modo", havingValue = "stub", matchIfMissing = true)
public class EnviadorSifenStub implements EnviadorSifenService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String MARCA_RECHAZO = "RECHAZAR";
    private static final String MARCA_SIN_RESPUESTA = "SIN_RESPUESTA";

    @Override
    public RespuestaSifenResult enviar(UUID facturaId, String xmlFirmado, Ambiente ambiente) {
        if (xmlFirmado != null && xmlFirmado.toUpperCase().contains(MARCA_RECHAZO)) {
            String descripcion = "Rechazado (SIMULADO — el XML contenía la marca de prueba \""
                    + MARCA_RECHAZO + "\", ambiente " + ambiente + ")";
            return new RespuestaSifenResult(false, "0160", descripcion, null);
        }
        if (xmlFirmado != null && xmlFirmado.toUpperCase().contains(MARCA_SIN_RESPUESTA)) {
            return RespuestaSifenResult.sinConfirmar("ERROR_TIMEOUT",
                    "Sin respuesta del SIFEN (SIMULADO — el XML contenía la marca de prueba \""
                            + MARCA_SIN_RESPUESTA + "\"): hay que verificar el estado del documento");
        }
        String cdc = generarCdcSimulado();
        String descripcion = "Aprobado (SIMULADO — ambiente " + ambiente + ", sin conexión real al SIFEN)";
        return new RespuestaSifenResult(true, "0260", descripcion, cdc);
    }

    /** Simula que el SIFEN sí había recibido y aprobado el DE que quedó sin confirmar. */
    @Override
    public RespuestaSifenResult consultarDe(UUID facturaId, String cdc, Ambiente ambiente) {
        String encontrado = cdc != null ? cdc : generarCdcSimulado();
        return new RespuestaSifenResult(true, "0422",
                "CDC encontrado (SIMULADO — ambiente " + ambiente + ", sin conexión real al SIFEN)", encontrado);
    }

    private String generarCdcSimulado() {
        StringBuilder sb = new StringBuilder(44);
        for (int i = 0; i < 44; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }
}
