package com.tottalstore.sifen.sifen;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Carga el certificado digital (.pfx / PKCS12) del contribuyente y expone el material criptográfico
 * que necesitan tanto la conexión TLS mutua contra el SIFEN (esta fase) como, más adelante, la firma
 * XMLDSig real del DTE (Fase 4) — un solo lugar donde se lee el .pfx, no dos.
 *
 * <p>Solo se activa con {@code sifen.envio.modo=real}: en modo stub (el default) no hace falta que el
 * archivo del certificado exista siquiera.
 */
@Configuration
@ConditionalOnProperty(name = "sifen.envio.modo", havingValue = "real")
public class SifenKeystoreConfig {

    private static final Logger log = LoggerFactory.getLogger(SifenKeystoreConfig.class);

    private final String keystorePath;
    private final char[] keystorePassword;
    private final String keyAlias;

    public SifenKeystoreConfig(
            @Value("${sifen.firma.keystore-path}") String keystorePath,
            @Value("${sifen.firma.keystore-password}") String keystorePassword,
            @Value("${sifen.firma.key-alias:}") String keyAlias) {
        if (keystorePath == null || keystorePath.isBlank()) {
            throw new IllegalStateException(
                    "sifen.envio.modo=real requiere SIFEN_FIRMA_KEYSTORE_PATH (ruta al .pfx del certificado)");
        }
        if (keystorePassword == null || keystorePassword.isBlank()) {
            throw new IllegalStateException(
                    "sifen.envio.modo=real requiere SIFEN_FIRMA_KEYSTORE_PASSWORD (contraseña del .pfx)");
        }
        this.keystorePath = keystorePath;
        this.keystorePassword = keystorePassword.toCharArray();
        this.keyAlias = (keyAlias == null || keyAlias.isBlank()) ? null : keyAlias;
    }

    private KeyStore cargarKeystore() {
        try (FileInputStream in = new FileInputStream(keystorePath)) {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(in, keystorePassword);

            Enumeration<String> aliases = keyStore.aliases();
            StringBuilder aliasesDisponibles = new StringBuilder();
            while (aliases.hasMoreElements()) {
                aliasesDisponibles.append(aliases.nextElement()).append(" ");
            }
            log.info("Certificado SIFEN cargado desde {} — alias disponibles: {}", keystorePath, aliasesDisponibles);
            if (keyAlias != null && !keyStore.containsAlias(keyAlias)) {
                throw new IllegalStateException("El alias '" + keyAlias + "' (SIFEN_FIRMA_KEY_ALIAS) no existe en "
                        + keystorePath + ". Alias disponibles: " + aliasesDisponibles);
            }
            return keyStore;
        } catch (IOException | GeneralSecurityException e) {
            throw new IllegalStateException(
                    "No se pudo cargar el certificado SIFEN desde " + keystorePath
                            + " — verificar ruta, contraseña y que sea un .pfx/.p12 válido",
                    e);
        }
    }

    private String aliasEfectivo(KeyStore keyStore) {
        try {
            return keyAlias != null ? keyAlias : keyStore.aliases().nextElement();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo determinar el alias del certificado SIFEN", e);
        }
    }

    @Bean
    public PrivateKey sifenPrivateKey() {
        KeyStore keyStore = cargarKeystore();
        try {
            PrivateKey privateKey =
                    (PrivateKey) keyStore.getKey(aliasEfectivo(keyStore), keystorePassword);
            if (privateKey == null) {
                throw new IllegalStateException(
                        "El alias del certificado SIFEN no tiene una clave privada asociada");
            }
            return privateKey;
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo leer la clave privada del certificado SIFEN", e);
        }
    }

    @Bean
    public X509Certificate sifenCertificado() {
        KeyStore keyStore = cargarKeystore();
        try {
            X509Certificate certificado = (X509Certificate) keyStore.getCertificate(aliasEfectivo(keyStore));
            log.info("Certificado SIFEN: Subject={}", certificado.getSubjectX500Principal());
            return certificado;
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo leer el certificado X.509 del .pfx", e);
        }
    }

    /** SSLContext con el certificado del contribuyente como material de autenticación de cliente
     * (TLS mutuo exigido por el SIFEN) y la cadena de confianza por defecto del JDK para validar el
     * servidor. Si el certificado del servidor del SIFEN no encadena a una CA pública reconocida por
     * el JDK, esto va a fallar con SSLHandshakeException — señal clara para investigar, no un stub
     * silencioso. */
    @Bean
    public SSLContext sifenSslContext() {
        KeyStore keyStore = cargarKeystore();
        try {
            KeyManagerFactory keyManagerFactory =
                    KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, keystorePassword);

            TrustManagerFactory trustManagerFactory =
                    TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init((KeyStore) null);

            SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
            sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);
            return sslContext;
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo armar el SSLContext con el certificado SIFEN", e);
        }
    }
}
