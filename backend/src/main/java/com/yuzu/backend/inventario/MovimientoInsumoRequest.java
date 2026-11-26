package com.yuzu.backend.inventario;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MovimientoInsumoRequest(
        @NotBlank(message = "Elige el tipo de movimiento")
        String tipo,

        /** En entrada, salida y merma es cuánto entra o sale. En un ajuste es lo que se CONTÓ (cuánto hay en total). */
        @NotNull(message = "La cantidad es obligatoria")
        @DecimalMin(value = "0", message = "La cantidad no puede ser negativa")
        @Digits(integer = 9, fraction = 3, message = "La cantidad no es válida (máximo 3 decimales)")
        BigDecimal cantidad,

        /** Lo que costó la compra completa. Solo vale en entradas y es opcional. */
        @DecimalMin(value = "0", message = "El costo no puede ser negativo")
        @Digits(integer = 8, fraction = 2, message = "El costo no es válido (máximo 2 decimales)")
        BigDecimal costoTotal,

        @Size(max = 150, message = "La nota no puede pasar de 150 caracteres")
        String nota) {
}
