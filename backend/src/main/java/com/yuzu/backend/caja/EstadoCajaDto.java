package com.yuzu.backend.caja;

/** Si hay caja abierta, trae su resumen; si no, resumen es null. */
public record EstadoCajaDto(boolean abierta, ResumenTurnoDto resumen) {
}
