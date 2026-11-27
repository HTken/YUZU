package com.yuzu.backend.suscripcion;

/**
 * Cuántos restaurantes cambiaron de estado en una revisión de vencimientos, y a cuántos se les avisó
 * por correo que su plan vence pronto (sin haber cambiado de estado todavía).
 */
public record ResultadoVencimientosDto(int aGracia, int suspendidos, int avisados) {
}
