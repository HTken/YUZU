package com.yuzu.backend.caja;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

public record AbrirCajaRequest(
        @NotNull(message = "Indica con cuánto efectivo abres la caja (puede ser 0)")
        @DecimalMin(value = "0.00", message = "El monto inicial no puede ser negativo")
        @Digits(integer = 8, fraction = 2, message = "El monto inicial no es válido (máximo 2 decimales)")
        BigDecimal montoInicial) {
}
