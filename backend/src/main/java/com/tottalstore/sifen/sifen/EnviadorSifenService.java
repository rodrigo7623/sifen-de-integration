package com.tottalstore.sifen.sifen;

import java.util.UUID;

/**
 * Punto de extensión para el envío del DTE firmado al SIFEN (RF-03, CU-04).
 *
 * <p>Implementación real: {@link EnviadorSifenSoapService} (cliente SOAP con TLS mutuo, activo con
 * {@code sifen.envio.modo=real}). Implementación por defecto: {@link EnviadorSifenStub}.
 */
public interface EnviadorSifenService {

    RespuestaSifenResult enviar(UUID facturaId, String xmlFirmado, Ambiente ambiente);

    /**
     * Consulta un DE por su CDC ({@code siConsDE}). Aprobado si el SIFEN lo tiene; no aprobado y no
     * {@code sinConfirmar} si el SIFEN responde que no existe (nunca lo recibió o lo rechazó);
     * {@code sinConfirmar} si la consulta misma no tuvo respuesta.
     */
    RespuestaSifenResult consultarDe(UUID facturaId, String cdc, Ambiente ambiente);
}
