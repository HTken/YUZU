package com.yuzu.backend.pago;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Lo único que manda el navegador: el token que creó Culqi y cuántos meses quiere pagar.
 * El monto NO viene del navegador: el servidor lo calcula con el plan del restaurante.
 *
 * deviceFingerPrintId y authentication3DS son del flujo de autenticación 3DS (Culqi3DS): el primero se
 * manda siempre que se tenga, el segundo solo en el reintento, después de que el banco autenticó al cliente.
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
        String email,

        String deviceFingerPrintId,

        @Valid
        Autenticacion3DSRequest authentication3DS) {
}
