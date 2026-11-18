package com.yuzu.backend.plataforma;

public record RestauranteResumenDto(
        Long id,
        String nombre,
        String subdominio,
        String estado,
        Long planId,
        String plan,
        String venceEn,
        int diasRestantes,
        int usuariosActivos,
        int maxUsuarios) {
}
