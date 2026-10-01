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
 * {@code FacturaService#construirXmlSimplificado}.
 */
@Component
@ConditionalOnProperty(name = "sifen.envio.modo", havingValue = "stub", matchIfMissing = true)
public class EnviadorSifenStub implements EnviadorSifenService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String MARCA_RECHAZO = "RECHAZAR";

    @Override
    public RespuestaSifenResult enviar(UUID facturaId, String xmlFirmado, Ambiente ambiente) {
        if (xmlFirmado != null && xmlFirmado.toUpperCase().contains(MARCA_RECHAZO)) {
            String descripcion = "Rechazado (SIMULADO — el XML contenía la marca de prueba \""
                    + MARCA_RECHAZO + "\", ambiente " + ambiente + ")";
            return new RespuestaSifenResult(false, "0160", descripcion, null);
        }
        String cdc = generarCdcSimulado();
        String descripcion = "Aprobado (SIMULADO — ambiente " + ambiente + ", sin conexión real al SIFEN)";
        return new RespuestaSifenResult(true, "0260", descripcion, cdc);
    }

    private String generarCdcSimulado() {
        StringBuilder sb = new StringBuilder(44);
        for (int i = 0; i < 44; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }
}
