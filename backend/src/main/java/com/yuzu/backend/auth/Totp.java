package com.yuzu.backend.auth;

import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Locale;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Códigos de verificación en dos pasos (TOTP, RFC 6238), compatibles con cualquier app autenticadora
 * (Google Authenticator, Authy, etc). No depende de Spring ni de nada externo, para poder probarla sola
 * con los valores oficiales de ejemplo del estándar (RFC 4226, anexo D).
 */
public final class Totp {

    private static final int DIGITOS = 6;
    private static final int PASO_SEGUNDOS = 30;
    private static final String ALGORITMO = "HmacSHA1";
    private static final String ALFABETO_BASE32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    private Totp() {
    }

    /** Un secreto nuevo al azar (160 bits), en Base32 para mostrarlo y guardarlo como texto. */
    public static String generarSecreto() {
        byte[] bytes = new byte[20];
        new SecureRandom().nextBytes(bytes);
        return base32Encode(bytes);
    }

    /**
     * Si el código de 6 dígitos que escribió el usuario es válido para este secreto en este momento.
     * Acepta también el paso anterior y el siguiente (hasta 30 segundos de diferencia de reloj).
     */
    public static boolean validar(String secretoBase32, String codigo, long epochSegundos) {
        if (codigo == null || !codigo.matches("\\d{" + DIGITOS + "}")) {
            return false;
        }
        byte[] clave = base32Decode(secretoBase32);
        long contador = epochSegundos / PASO_SEGUNDOS;
        for (long delta = -1; delta <= 1; delta++) {
            if (hotp(clave, contador + delta).equals(codigo)) {
                return true;
            }
        }
        return false;
    }

    /** La dirección "otpauth://" que entiende cualquier app autenticadora al escanearla o escribirla a mano. */
    public static String otpauthUrl(String cuenta, String secretoBase32) {
        String etiqueta = URLEncoder.encode("Yuzu:" + cuenta, StandardCharsets.UTF_8).replace("+", "%20");
        String emisor = URLEncoder.encode("Yuzu", StandardCharsets.UTF_8);
        return "otpauth://totp/" + etiqueta + "?secret=" + secretoBase32 + "&issuer=" + emisor
                + "&digits=" + DIGITOS + "&period=" + PASO_SEGUNDOS;
    }

    /** HOTP (RFC 4226): el código para un contador exacto. Se expone para poder probarlo con los valores
     *  oficiales del estándar, que usan la clave en bruto (sin pasar por Base32). */
    static String hotp(byte[] clave, long contador) {
        byte[] datosContador = ByteBuffer.allocate(8).putLong(contador).array();
        byte[] hash;
        try {
            Mac mac = Mac.getInstance(ALGORITMO);
            mac.init(new SecretKeySpec(clave, ALGORITMO));
            hash = mac.doFinal(datosContador);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo calcular el código TOTP", e);
        }

        int desplazamiento = hash[hash.length - 1] & 0x0F;
        int binario = ((hash[desplazamiento] & 0x7f) << 24)
                | ((hash[desplazamiento + 1] & 0xff) << 16)
                | ((hash[desplazamiento + 2] & 0xff) << 8)
                | (hash[desplazamiento + 3] & 0xff);

        int codigo = binario % (int) Math.pow(10, DIGITOS);
        return String.format(Locale.ROOT, "%0" + DIGITOS + "d", codigo);
    }

    static String base32Encode(byte[] datos) {
        StringBuilder resultado = new StringBuilder();
        int buffer = 0;
        int bitsEnBuffer = 0;
        for (byte b : datos) {
            buffer = (buffer << 8) | (b & 0xFF);
            bitsEnBuffer += 8;
            while (bitsEnBuffer >= 5) {
                int indice = (buffer >> (bitsEnBuffer - 5)) & 0x1F;
                resultado.append(ALFABETO_BASE32.charAt(indice));
                bitsEnBuffer -= 5;
            }
        }
        if (bitsEnBuffer > 0) {
            int indice = (buffer << (5 - bitsEnBuffer)) & 0x1F;
            resultado.append(ALFABETO_BASE32.charAt(indice));
        }
        return resultado.toString();
    }

    static byte[] base32Decode(String texto) {
        String limpio = texto.toUpperCase(Locale.ROOT).replace("=", "").replace(" ", "");
        java.io.ByteArrayOutputStream salida = new java.io.ByteArrayOutputStream();
        int buffer = 0;
        int bitsEnBuffer = 0;
        for (char c : limpio.toCharArray()) {
            int indice = ALFABETO_BASE32.indexOf(c);
            if (indice < 0) {
                continue;
            }
            buffer = (buffer << 5) | indice;
            bitsEnBuffer += 5;
            if (bitsEnBuffer >= 8) {
                salida.write((buffer >> (bitsEnBuffer - 8)) & 0xFF);
                bitsEnBuffer -= 8;
            }
        }
        return salida.toByteArray();
    }
}
