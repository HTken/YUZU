package com.yuzu.backend.informes;

import java.math.BigDecimal;

/** Lo cobrado con un método de pago (EFECTIVO, YAPE, PLIN o TARJETA) y en cuántas cuentas. */
public record MetodoPagoDto(String metodo, int cuentas, BigDecimal total) {
}
