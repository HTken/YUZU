package com.yuzu.backend.pago;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Lo único que manda el navegador: el token que creó Culqi y cuántos meses quiere pagar.
 * El monto NO viene del navegador: el servidor lo calcula con el plan del restaurante.
 */
public record CobrarRequest(
        @NotBlank(message = "Falta el identificador del pago")
        @Pattern(regexp = "^(tkn|ype)_(test|live)_[A-Za-z0-9]+$", message = "El identificador del pago no es válido")
        String tokenId,

        @NotNull(message = "Indica cuántos meses pagar")
        @Min(value = 1, message = "Paga al menos 1 mes")
        @Max(value = 12, message = "Puedes pagar hasta 12 meses")
        Integer meses,

        @NotBlank(message = "Falta el correo del pago")
        @Email(message = "El correo no es válido")
        String email) {
}
