package com.yuzu.backend.caja;

import java.math.BigDecimal;

public record PagoDto(String metodo, BigDecimal monto) {
}
