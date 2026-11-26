package com.yuzu.backend.caja;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MovimientoRequest(
        @NotBlank(message = "Indica si es un ingreso o un egreso")
        String tipo,

        @NotBlank(message = "Escribe el motivo del movimiento")
        @Size(max = 120, message = "El motivo no puede pasar de 120 caracteres")
        String concepto,

        @NotNull(message = "El monto es obligatorio")
        @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
        @Digits(integer = 8, fraction = 2, message = "El monto no es válido (máximo 2 decimales)")
        BigDecimal monto) {
}
