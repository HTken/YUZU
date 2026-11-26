package com.yuzu.backend.pago;

/** El estado del cobro automático del plan: si está activo y con qué tarjeta. */
public record SuscripcionCulqiDto(boolean activa, String marcaTarjeta, String ultimosCuatro) {
}
