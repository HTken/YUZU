package com.yuzu.backend.caja;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

/** Cómo se paga la cuenta. La suma de los pagos tiene que ser igual al total de la cuenta. */
public record CobrarRequest(
        @NotEmpty(message = "Indica cómo se paga la cuenta")
        @Size(max = 6, message = "Son demasiados pagos para una sola cuenta")
        @Valid
        List<PagoRequest> pagos) {
}
