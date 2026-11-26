package com.yuzu.backend.mesa;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SalonRequest(
        @NotBlank(message = "El nombre del salón es obligatorio")
        @Size(max = 60, message = "El nombre del salón no puede pasar de 60 caracteres")
        String nombre,

        @NotNull(message = "El orden es obligatorio")
        @Min(value = 0, message = "El orden no puede ser negativo")
        @Max(value = 9999, message = "El orden no puede pasar de 9999")
        Integer orden,

        @NotNull(message = "Indica si el salón está activo")
        Boolean activo) {
}
