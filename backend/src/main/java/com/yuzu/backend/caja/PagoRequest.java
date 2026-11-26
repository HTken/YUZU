package com.yuzu.backend.caja;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PagoRequest(
        @NotBlank(message = "Elige el método de pago")
        String metodo,

        @NotNull(message = "El monto de cada pago es obligatorio")
        @DecimalMin(value = "0.01", message = "Cada pago debe ser mayor a 0")
        @Digits(integer = 8, fraction = 2, message = "El monto no es válido (máximo 2 decimales)")
        BigDecimal monto) {
}
