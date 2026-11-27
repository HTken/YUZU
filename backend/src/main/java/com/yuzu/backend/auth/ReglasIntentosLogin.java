package com.yuzu.backend.auth;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Cuándo bloquear una cuenta por demasiados intentos fallidos (usuario o clave incorrectos, o código de
 * 2FA incorrecto) y cuánto falta para que se desbloquee sola. Sin base de datos ni reloj propios, para
 * poder probarla con fechas fijas.
 */
public final class ReglasIntentosLogin {

    private ReglasIntentosLogin() {
    }

    public static int siguienteIntento(int intentosActuales) {
        return intentosActuales + 1;
    }

    public static boolean debeBloquear(int intentos, int maxIntentos) {
        return intentos >= maxIntentos;
    }

    public static LocalDateTime calcularBloqueoHasta(LocalDateTime ahora, int minutosBloqueo) {
        return ahora.plusMinutes(minutosBloqueo);
    }

    public static boolean estaBloqueado(LocalDateTime bloqueadoHasta, LocalDateTime ahora) {
        return bloqueadoHasta != null && ahora.isBefore(bloqueadoHasta);
    }

    /** Redondeado hacia arriba, mínimo 1 minuto, para no decir "0 minutos" si falta menos de uno. */
    public static long minutosRestantes(LocalDateTime bloqueadoHasta, LocalDateTime ahora) {
        long segundos = Duration.between(ahora, bloqueadoHasta).getSeconds();
        return Math.max(1, (segundos + 59) / 60);
    }
}
