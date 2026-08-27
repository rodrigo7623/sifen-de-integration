package com.tottalstore.sifen.firma;

/**
 * Punto de extensión para la firma digital del DTE (RF-02, CU-03).
 *
 * <p>La implementación real debe aplicar firma XAdES sobre certificado X.509 del contribuyente
 * (Apache Santuario + Bouncy Castle, según la planificación técnica). Mientras no se cuente con el
 * certificado de prueba, se usa {@link FirmaDigitalStub}.
 */
public interface FirmaDigitalService {

    DteFirmado firmar(String xmlDte);
}
