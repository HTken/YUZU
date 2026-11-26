package com.yuzu.backend.inventario;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record InsumoRequest(
        @NotBlank(message = "El nombre del insumo es obligatorio")
        @Size(max = 80, message = "El nombre no puede pasar de 80 caracteres")
        String nombre,

        @NotBlank(message = "Elige la unidad de medida")
        String unidad,

        @NotNull(message = "El stock mínimo es obligatorio (escribe 0 si no quieres aviso)")
        @DecimalMin(value = "0", message = "El stock mínimo no puede ser negativo")
        @Digits(integer = 9, fraction = 3, message = "El stock mínimo no es válido (máximo 3 decimales)")
        BigDecimal stockMinimo,

        @NotNull(message = "Indica si el insumo está activo")
        Boolean activo,

        /** Solo se usa al crear: lo que hay hoy en el local. Al editar se ignora (el stock cambia con movimientos). */
        @DecimalMin(value = "0", message = "El stock inicial no puede ser negativo")
        @Digits(integer = 9, fraction = 3, message = "El stock inicial no es válido (máximo 3 decimales)")
        BigDecimal stockInicial) {
}
