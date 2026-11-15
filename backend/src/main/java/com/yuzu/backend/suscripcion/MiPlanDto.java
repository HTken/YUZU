package com.yuzu.backend.suscripcion;

import java.math.BigDecimal;

/** Resumen del plan del restaurante que inició sesión. */
public record MiPlanDto(
        String plan,
        BigDecimal precioMensual,
        int maxUsuarios,
        int usuariosActivos,
        String venceEn,
        int diasRestantes,
        String estado) {
}
