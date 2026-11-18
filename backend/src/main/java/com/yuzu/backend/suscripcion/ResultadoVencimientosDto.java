package com.yuzu.backend.suscripcion;

/** Cuántos restaurantes cambiaron de estado en una revisión de vencimientos. */
public record ResultadoVencimientosDto(int aGracia, int suspendidos) {
}
