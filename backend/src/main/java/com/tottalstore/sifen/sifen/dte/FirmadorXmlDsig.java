package com.tottalstore.sifen.sifen.dte;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.List;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import org.w3c.dom.Element;

/**
 * Firma XMLDSig "enveloped" del DE, según el Manual Técnico del SIFEN v150, secciones 7.2.2.1 y 7.7
 * (Schema XML 1): referencia al {@code Id} del {@code <DE>} (el CDC), transformaciones enveloped +
 * exclusive C14N, digest SHA-256, firma RSA-SHA256 y el certificado X.509 en {@code KeyInfo}. El
 * elemento {@code <Signature>} queda como hermano siguiente del {@code <DE>} dentro de
 * {@code <rDE>}, con su propio namespace por defecto.
 */
public class FirmadorXmlDsig {

    static {
        // Sin esto, el JDK parte los valores base64 (SignatureValue, X509Certificate) cada 76
        // caracteres e inserta "&#13;", lo que altera el XML firmado que recibe el SIFEN.
        System.setProperty("com.sun.org.apache.xml.internal.security.ignoreLineBreaks", "true");
    }

    private final PrivateKey clavePrivada;
    private final X509Certificate certificado;

    public FirmadorXmlDsig(PrivateKey clavePrivada, X509Certificate certificado) {
        this.clavePrivada = clavePrivada;
        this.certificado = certificado;
    }

    /**
     * Firma el {@code <DE>} (que ya debe tener su atributo {@code Id} registrado como ID) y agrega la
     * firma al final de {@code rDE}. Devuelve el DigestValue en base64, que necesita el QR.
     */
    public String firmar(Element rDE, String idDe) {
        try {
            XMLSignatureFactory fabrica = XMLSignatureFactory.getInstance("DOM");
            Reference referencia = fabrica.newReference(
                    "#" + idDe,
                    fabrica.newDigestMethod(DigestMethod.SHA256, null),
                    List.of(
                            fabrica.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null),
                            fabrica.newTransform(CanonicalizationMethod.EXCLUSIVE, (TransformParameterSpec) null)),
                    null,
                    null);
            SignedInfo signedInfo = fabrica.newSignedInfo(
                    fabrica.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE, (C14NMethodParameterSpec) null),
                    fabrica.newSignatureMethod(SignatureMethod.RSA_SHA256, null),
                    List.of(referencia));

            KeyInfoFactory keyInfoFactory = fabrica.getKeyInfoFactory();
            KeyInfo keyInfo = keyInfoFactory.newKeyInfo(List.of(keyInfoFactory.newX509Data(List.of(certificado))));

            XMLSignature firma = fabrica.newXMLSignature(signedInfo, keyInfo);
            firma.sign(new DOMSignContext(clavePrivada, rDE));

            return Base64.getEncoder().encodeToString(referencia.getDigestValue());
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo firmar digitalmente el DE " + idDe, e);
        }
    }
}
