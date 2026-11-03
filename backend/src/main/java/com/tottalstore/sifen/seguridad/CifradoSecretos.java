package com.tottalstore.sifen.seguridad;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Cifrado de secretos de configuración (RNF-03): contraseña del certificado, CSC del QR,
 * credenciales de la base de datos. AES-256-GCM con una clave maestra que vive fuera del repositorio
 * y fuera del {@code .env} (variable de entorno {@code SIFEN_MASTER_KEY}).
 *
 * <p>Formato de un valor cifrado: {@code ENC(base64(iv[12] || ciphertext+tag))}. GCM autentica el
 * contenido: un valor alterado o una clave maestra equivocada fallan al descifrar, nunca devuelven
 * basura.
 *
 * <p>Uso por línea de comandos (solo depende del JDK, se corre desde {@code target/classes}):
 * <pre>
 *   java -cp target/classes com.tottalstore.sifen.seguridad.CifradoSecretos generar-clave
 *   java -cp target/classes com.tottalstore.sifen.seguridad.CifradoSecretos cifrar   (lee SIFEN_MASTER_KEY y pide el valor)
 * </pre>
 */
public final class CifradoSecretos {

    public static final String PREFIJO = "ENC(";
    public static final String SUFIJO = ")";
    private static final String ALGORITMO = "AES/GCM/NoPadding";
    private static final int LARGO_IV = 12;
    private static final int LARGO_TAG_BITS = 128;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final SecretKey clave;

    public CifradoSecretos(String claveMaestraBase64) {
        if (claveMaestraBase64 == null || claveMaestraBase64.isBlank()) {
            throw new IllegalArgumentException("La clave maestra (SIFEN_MASTER_KEY) es obligatoria");
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(claveMaestraBase64.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("La clave maestra (SIFEN_MASTER_KEY) no es base64 válido", e);
        }
        if (bytes.length != 32) {
            throw new IllegalArgumentException(
                    "La clave maestra (SIFEN_MASTER_KEY) debe ser de 256 bits (32 bytes en base64), tiene " + bytes.length);
        }
        this.clave = new SecretKeySpec(bytes, "AES");
    }

    public static boolean estaCifrado(String valor) {
        return valor != null && valor.startsWith(PREFIJO) && valor.endsWith(SUFIJO);
    }

    public static String generarClaveMaestra() {
        try {
            KeyGenerator generador = KeyGenerator.getInstance("AES");
            generador.init(256, RANDOM);
            return Base64.getEncoder().encodeToString(generador.generateKey().getEncoded());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo generar la clave maestra", e);
        }
    }

    public String cifrar(String textoPlano) {
        try {
            byte[] iv = new byte[LARGO_IV];
            RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(ALGORITMO);
            cipher.init(Cipher.ENCRYPT_MODE, clave, new GCMParameterSpec(LARGO_TAG_BITS, iv));
            byte[] cifrado = cipher.doFinal(textoPlano.getBytes(StandardCharsets.UTF_8));
            byte[] salida = ByteBuffer.allocate(iv.length + cifrado.length).put(iv).put(cifrado).array();
            return PREFIJO + Base64.getEncoder().encodeToString(salida) + SUFIJO;
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo cifrar el secreto", e);
        }
    }

    /** Descifra un valor {@code ENC(...)}. Un valor sin ese formato se devuelve tal cual. */
    public String descifrar(String valor) {
        if (!estaCifrado(valor)) {
            return valor;
        }
        try {
            byte[] datos = Base64.getDecoder().decode(valor.substring(PREFIJO.length(), valor.length() - SUFIJO.length()));
            if (datos.length <= LARGO_IV) {
                throw new IllegalStateException("El secreto cifrado está truncado");
            }
            Cipher cipher = Cipher.getInstance(ALGORITMO);
            cipher.init(Cipher.DECRYPT_MODE, clave, new GCMParameterSpec(LARGO_TAG_BITS, datos, 0, LARGO_IV));
            return new String(cipher.doFinal(datos, LARGO_IV, datos.length - LARGO_IV), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            // No se incluye el valor en el mensaje: aunque esté cifrado, no tiene por qué ir a los logs.
            throw new IllegalStateException(
                    "No se pudo descifrar un secreto: la clave maestra no corresponde o el valor fue alterado", e);
        }
    }

    public static void main(String[] args) {
        if (args.length == 1 && args[0].equals("generar-clave")) {
            System.out.println(generarClaveMaestra());
            return;
        }
        if (args.length == 1 && args[0].equals("cifrar")) {
            String claveMaestra = System.getenv("SIFEN_MASTER_KEY");
            if (claveMaestra == null || claveMaestra.isBlank()) {
                System.err.println("Definí primero la variable de entorno SIFEN_MASTER_KEY");
                System.exit(1);
            }
            // Se lee de la consola sin eco, para que el secreto no quede en el historial de comandos.
            java.io.Console consola = System.console();
            char[] valor = consola != null
                    ? consola.readPassword("Valor a cifrar: ")
                    : new java.util.Scanner(System.in, StandardCharsets.UTF_8).nextLine().toCharArray();
            System.out.println(new CifradoSecretos(claveMaestra).cifrar(new String(valor)));
            return;
        }
        System.err.println("Uso: CifradoSecretos generar-clave | cifrar");
        System.exit(2);
    }
}
