package com.tottalstore.sifen.firma;

import java.time.Instant;
import org.springframework.stereotype.Component;

/**
 * Implementación stub: envuelve el XML del DTE con un elemento de firma simulado, sin usar
 * certificado real. TODO: reemplazar por firma XAdES real (Apache Santuario + Bouncy Castle) al
 * contar con el certificado X.509 de Tottal Store.
 */
@Component
public class FirmaDigitalStub implements FirmaDigitalService {

    @Override
    public DteFirmado firmar(String xmlDte) {
        String xmlFirmado = xmlDte + "<Signature simulada=\"true\">STUB-NO-VALIDO-PARA-SIFEN-REAL</Signature>";
        return new DteFirmado(xmlFirmado, Instant.now());
    }
}
