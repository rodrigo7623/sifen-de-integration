package com.tottalstore.sifen.sifen;

/**
 * Punto de extensión para el envío del DTE firmado al SIFEN (RF-03, CU-04).
 *
 * <p>La implementación real debe comunicarse por SOAP/REST con el webservice de la SET (ambiente
 * de homologación en Release 1 y 2, producción en Release 3). Mientras no se cuente con acceso a
 * ese ambiente, se usa {@link EnviadorSifenStub}.
 */
public interface EnviadorSifenService {

    RespuestaSifenResult enviar(String xmlFirmado, Ambiente ambiente);
}
