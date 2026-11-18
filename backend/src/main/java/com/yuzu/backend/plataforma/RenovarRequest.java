package com.yuzu.backend.plataforma;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RenovarRequest(
        @NotNull(message = "Indica cuántos meses renovar")
        @Min(value = 1, message = "Renueva al menos 1 mes")
        @Max(value = 12, message = "Puedes renovar hasta 12 meses")
        Integer meses,

        @NotBlank(message = "Elige el método de pago")
        @Pattern(regexp = "EFECTIVO|TRANSFERENCIA|YAPE|PLIN|OTRO", message = "El método de pago no es válido")
        String metodo,

        /** Número de operación o comprobante. Opcional, pero evita registrar dos veces el mismo pago. */
        @Size(max = 100, message = "La referencia no puede pasar de 100 caracteres")
        String referencia) {
}
