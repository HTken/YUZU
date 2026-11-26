package com.yuzu.backend.pago;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Para guardar la tarjeta y activar el cobro automático del plan. */
public record ActivarSuscripcionRequest(
        @NotBlank(message = "Falta el identificador de la tarjeta")
        @Pattern(regexp = "^tkn_(test|live)_[A-Za-z0-9]+$", message = "El identificador de la tarjeta no es válido")
        String tokenId,

        @NotBlank(message = "Falta el correo")
        @Email(message = "El correo no es válido")
        String email,

        @Valid
        Autenticacion3DSRequest authentication3DS) {
}
