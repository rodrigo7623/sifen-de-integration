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
}
