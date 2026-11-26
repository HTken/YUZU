package com.yuzu.backend.caja;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CerrarCajaRequest(
        @NotNull(message = "Indica cuánto efectivo contaste en el cajón")
        @DecimalMin(value = "0.00", message = "El efectivo contado no puede ser negativo")
        @Digits(integer = 8, fraction = 2, message = "El efectivo contado no es válido (máximo 2 decimales)")
        BigDecimal efectivoContado,

        @Size(max = 200, message = "La nota no puede pasar de 200 caracteres")
        String nota) {
}
