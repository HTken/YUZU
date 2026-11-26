package com.yuzu.backend.negocio;

import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Las reglas de los datos del negocio, sin base de datos, para poder probarlas solas. */
final class ReglasNegocio {

    static final int RAZON_MAXIMO = 150;
    static final int DIRECCION_MAXIMO = 200;
    static final int TELEFONO_MAXIMO = 30;
    static final int MENSAJE_MAXIMO = 120;
    static final int ANCHO_POR_DEFECTO = 80;
    static final Set<Integer> ANCHOS = Set.of(58, 80);
    static final String MENSAJE_POR_DEFECTO = "¡Gracias por su visita!";

    private static final Pattern DIGITOS_RUC = Pattern.compile("\\d{11}");
    private static final Pattern TELEFONO = Pattern.compile("[0-9+()\\-./ ]{5,30}");
    private static final int[] PESOS_RUC = {5, 4, 3, 2, 7, 6, 5, 4, 3, 2};
    private static final Set<String> PREFIJOS_RUC = Set.of("10", "15", "16", "17", "20");

    private ReglasNegocio() {
    }

    /**
     * El texto de una sola línea, sin caracteres de control ni espacios de más. Vacío significa "sin dato" (null).
     * Pasar el máximo es un error 400 (no se corta en silencio, para no guardar una dirección a medias).
     */
    static String texto(String valor, int maximo, String campo) {
        if (valor == null) {
            return null;
        }
        String limpio = valor.replaceAll("[\\p{Cntrl}]", " ").replaceAll("\\s+", " ").trim();
        if (limpio.isEmpty()) {
            return null;
        }
        if (limpio.length() > maximo) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    campo + " no puede pasar de " + maximo + " caracteres");
        }
        return limpio;
    }

    /**
     * RUC peruano: 11 dígitos, empieza con 10, 15, 16, 17 o 20, y el último dígito es el verificador
     * (módulo 11 con los pesos de SUNAT). Vacío es válido: el RUC es opcional.
     */
    static String ruc(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String limpio = valor.replaceAll("[\\s-]", "");
        if (!DIGITOS_RUC.matcher(limpio).matches() || !PREFIJOS_RUC.contains(limpio.substring(0, 2))
                || digitoVerificador(limpio) != limpio.charAt(10) - '0') {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El RUC no es válido. Debe tener 11 números y empezar con 10, 15, 16, 17 o 20.");
        }
        return limpio;
    }

    static int digitoVerificador(String ruc) {
        int suma = 0;
        for (int i = 0; i < 10; i++) {
            suma += (ruc.charAt(i) - '0') * PESOS_RUC[i];
        }
        int resto = 11 - (suma % 11);
        return resto == 10 ? 0 : resto == 11 ? 1 : resto;
    }

    /** Teléfono: solo números y los signos que se usan al escribirlos (+ ( ) - . / y espacio). Vacío = sin dato. */
    static String telefono(String valor) {
        String limpio = texto(valor, TELEFONO_MAXIMO, "El teléfono");
        if (limpio != null && !TELEFONO.matcher(limpio).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El teléfono no es válido. Usa solo números, por ejemplo 987 654 321.");
        }
        return limpio;
    }

    /** El papel de la impresora: 58 u 80 mm. Sin dato, 80. */
    static int ancho(Integer valor) {
        if (valor == null) {
            return ANCHO_POR_DEFECTO;
        }
        if (!ANCHOS.contains(valor)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El ancho del papel debe ser 58 u 80 mm");
        }
        return valor;
    }
}
