package com.yuzu.backend.caja;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Anular una venta ya cobrada. El motivo es obligatorio y queda registrado junto con quién la anuló.
 * devolverInsumos: si es true, los insumos que descontó la venta vuelven al stock (por ejemplo, un cobro duplicado
 * o equivocado). Si es false, se asume que el plato ya se preparó y el stock queda como está.
 */
public record AnularVentaRequest(
        @NotBlank(message = "Escribe el motivo de la anulación")
        @Size(max = 200, message = "El motivo no puede pasar de 200 caracteres")
        String motivo,
        boolean devolverInsumos) {
}
