package com.yuzu.backend.plataforma;

import java.math.BigDecimal;

import com.yuzu.backend.plan.Plan;

public record PlanDto(
        Long id,
        String codigo,
        String nombre,
        BigDecimal precioMensual,
        int maxUsuarios,
        boolean activo) {

    public static PlanDto desde(Plan p) {
        return new PlanDto(p.getId(), p.getCodigo(), p.getNombre(), p.getPrecioMensual(), p.getMaxUsuarios(),
                p.isActivo());
    }
}
