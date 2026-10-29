package com.tottalstore.sifen.sifen.dte;

import com.tottalstore.sifen.sifen.Ambiente;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Contenido del QR del DE ({@code dCarQR}, J002), según el Manual Técnico del SIFEN v150, sección
 * 13.8: parámetros del DE (fecha de emisión y DigestValue en hexadecimal) + hash SHA-256 de esos
 * parámetros concatenados con el CSC del contribuyente. El CSC nunca se incluye en la URL.
 */
public final class CodigoQrSifen {

    static final String URL_TEST = "https://ekuatia.set.gov.py/consultas-test/qr?";
    static final String URL_PRODUCCION = "https://ekuatia.set.gov.py/consultas/qr?";

    private CodigoQrSifen() {}

    public static String generar(
            Ambiente ambiente,
            String cdc,
            String fechaEmision,
            String identificacionReceptor,
            String totalGeneral,
            String totalIva,
            int cantidadItems,
            String digestValueBase64,
            String idCsc,
            String csc) {
        String parametros = "nVersion=150"
                + "&Id=" + cdc
                + "&dFeEmiDE=" + hex(fechaEmision)
                + "&dRucRec=" + identificacionReceptor
                + "&dTotGralOpe=" + totalGeneral
                + "&dTotIVA=" + totalIva
                + "&cItems=" + cantidadItems
                + "&DigestValue=" + hex(digestValueBase64)
                + "&IdCSC=" + idCsc;
        String hash = sha256Hex(parametros + csc);
        return (ambiente == Ambiente.PRODUCCION ? URL_PRODUCCION : URL_TEST) + parametros + "&cHashQR=" + hash;
    }

    static String hex(String texto) {
        return HexFormat.of().formatHex(texto.getBytes(StandardCharsets.UTF_8));
    }

    static String sha256Hex(String texto) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
