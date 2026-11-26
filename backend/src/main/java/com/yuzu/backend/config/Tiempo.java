package com.yuzu.backend.config;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Las fechas se guardan en hora UTC y se envían al navegador con la "Z" al final
 * (por ejemplo 2026-11-18T07:05:12Z); el navegador las muestra en la hora de Lima.
 */
public final class Tiempo {

    private Tiempo() {
    }

    public static LocalDateTime ahora() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    public static String iso(LocalDateTime fecha) {
        return fecha == null ? null : fecha.toInstant(ZoneOffset.UTC).toString();
    }
}
