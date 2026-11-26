package com.yuzu.backend.suscripcion;

import java.math.BigDecimal;

/** Resumen del plan del restaurante que inició sesión. */
public record MiPlanDto(
        String plan,
        BigDecimal precioMensual,
        int maxUsuarios,
        int usuariosActivos,
        int maxProductos,
        int productosActivos,
        int maxMesas,
        int mesasActivas,
        int maxInsumos,
        int insumosActivos,
        String venceEn,
        int diasRestantes,
        String estado) {
}
