package com.yuzu.backend.catalogo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CategoriaRequest(
        @NotBlank(message = "El nombre de la categoría es obligatorio")
        @Size(max = 80, message = "El nombre de la categoría no puede pasar de 80 caracteres")
        String nombre,

        @NotNull(message = "El orden es obligatorio")
        @Min(value = 0, message = "El orden no puede ser negativo")
        @Max(value = 9999, message = "El orden no puede pasar de 9999")
        Integer orden,

        @NotNull(message = "Indica si la categoría está activa")
        Boolean activo,

        /** Si no se envía, se conserva lo que ya tenía (o "sí" en una categoría nueva). */
        Boolean pasaPorCocina) {
}
