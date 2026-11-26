package com.yuzu.backend.catalogo;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProductoRequest(
        @NotNull(message = "Elige una categoría")
        Long categoriaId,

        @NotBlank(message = "El nombre del producto es obligatorio")
        @Size(max = 120, message = "El nombre del producto no puede pasar de 120 caracteres")
        String nombre,

        @Size(max = 300, message = "La descripción no puede pasar de 300 caracteres")
        String descripcion,

        @NotNull(message = "El precio es obligatorio")
        @DecimalMin(value = "0.01", message = "El precio debe ser mayor que cero")
        @Digits(integer = 8, fraction = 2, message = "El precio puede tener hasta 2 decimales")
        BigDecimal precio,

        @NotNull(message = "Indica si el producto está activo")
        Boolean activo) {
}
