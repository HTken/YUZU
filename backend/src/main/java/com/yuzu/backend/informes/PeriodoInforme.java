package com.yuzu.backend.informes;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

/**
 * El periodo de un informe: del día "desde" al día "hasta", ambos incluidos, contados en días de Lima
 * (de las 00:00 a las 23:59 hora de Lima). Las ventas se guardan en hora UTC, así que aquí cada día de Lima
 * se convierte a su rango en UTC: desde el inicio de "desde" hasta el inicio del día siguiente a "hasta" (sin incluirlo).
 *
 * Si algo no es válido lanza IllegalArgumentException con un mensaje listo para mostrar a la persona.
 */
public record PeriodoInforme(LocalDate desde, LocalDate hasta) {

    public static final ZoneId LIMA = ZoneId.of("America/Lima");

    /** Un año completo (bisiesto incluido). Más que eso vuelve lento el informe y no se puede leer. */
    public static final int MAXIMO_DIAS = 366;

    private static final int ANIO_MINIMO = 2020;
    private static final int ANIO_MAXIMO = 2100;

    public PeriodoInforme {
        if (desde == null || hasta == null) {
            throw new IllegalArgumentException("Elige la fecha de inicio y la de fin.");
        }
        if (desde.isAfter(hasta)) {
            throw new IllegalArgumentException("La fecha de inicio no puede ser posterior a la de fin.");
        }
        if (desde.getYear() < ANIO_MINIMO || hasta.getYear() > ANIO_MAXIMO) {
            throw new IllegalArgumentException("Las fechas están fuera de lo permitido.");
        }
        if (ChronoUnit.DAYS.between(desde, hasta) + 1 > MAXIMO_DIAS) {
            throw new IllegalArgumentException("El periodo puede abarcar como máximo " + MAXIMO_DIAS + " días (un año).");
        }
    }

    /** Desde el texto que llega en la dirección, con formato AAAA-MM-DD (por ejemplo 2026-11-20). */
    public static PeriodoInforme de(String desde, String hasta) {
        return new PeriodoInforme(fecha(desde), fecha(hasta));
    }

    private static LocalDate fecha(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("Elige la fecha de inicio y la de fin.");
        }
        try {
            return LocalDate.parse(texto.trim());
        } catch (DateTimeException e) {
            throw new IllegalArgumentException("Las fechas deben escribirse como AAAA-MM-DD, por ejemplo 2026-11-20.");
        }
    }

    /** Cuántos días abarca, contando el primero y el último. */
    public int dias() {
        return (int) ChronoUnit.DAYS.between(desde, hasta) + 1;
    }

    /** El primer instante del periodo, en hora UTC (se incluye). */
    public LocalDateTime inicioUtc() {
        return aUtc(desde);
    }

    /** El primer instante después del periodo, en hora UTC (no se incluye). */
    public LocalDateTime finUtc() {
        return aUtc(hasta.plusDays(1));
    }

    private static LocalDateTime aUtc(LocalDate diaLima) {
        return diaLima.atStartOfDay(LIMA).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    /** A qué día de Lima corresponde un momento guardado en hora UTC. */
    public static LocalDate diaLima(LocalDateTime utc) {
        return utc.atOffset(ZoneOffset.UTC).atZoneSameInstant(LIMA).toLocalDate();
    }
}
