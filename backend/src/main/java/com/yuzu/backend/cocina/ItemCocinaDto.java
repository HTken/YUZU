package com.yuzu.backend.cocina;

/** Un plato por preparar dentro de un ticket de cocina. */
public record ItemCocinaDto(
        Long id,
        String nombre,
        int cantidad,
        String nota) {
}
