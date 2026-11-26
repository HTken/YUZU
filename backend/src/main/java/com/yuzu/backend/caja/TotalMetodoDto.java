package com.yuzu.backend.caja;

import java.math.BigDecimal;

/** Lo cobrado con un método de pago en el turno. */
public record TotalMetodoDto(String metodo, BigDecimal monto) {
}
