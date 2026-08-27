package com.tottalstore.sifen.sifen;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

/**
 * Implementación stub: simula una aprobación del SIFEN en ambiente TEST, generando un CDC
 * ficticio de 44 dígitos (longitud real de un CDC de DTE). TODO: reemplazar por el cliente
 * SOAP/REST real del webservice de la SET al contar con acceso al ambiente de homologación.
 */
@Component
public class EnviadorSifenStub implements EnviadorSifenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public RespuestaSifenResult enviar(String xmlFirmado, Ambiente ambiente) {
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
