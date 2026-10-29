package com.tottalstore.sifen.firma;

import com.tottalstore.sifen.facturacion.FacturaElectronica;
import com.tottalstore.sifen.sifen.Ambiente;

/**
 * Genera el XML del DE de una factura y lo firma digitalmente (RF-02, CU-03, RNF-05).
 *
 * <p>Implementación real: {@code GeneradorDteSifen} (XML conforme al Manual Técnico v150 + firma
 * XMLDSig con el certificado del contribuyente + QR), activa con {@code sifen.envio.modo=real}.
 * Implementación por defecto: {@link GeneradorDteStub}.
 */
public interface GeneradorDteService {

    /** La factura ya tiene asignado su número de documento y su fecha de emisión. */
    DteFirmado generar(FacturaElectronica factura, Ambiente ambiente);
}
