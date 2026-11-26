package com.yuzu.backend.informes;

import java.math.BigDecimal;

/** Lo cobrado con un método de pago en el periodo, y en cuántas cuentas se usó. */
public record MetodoFila(String metodo, BigDecimal total, Long cuentas) {

    public MetodoFila {
        total = total == null ? BigDecimal.ZERO : total;
        cuentas = cuentas == null ? 0L : cuentas;
    }
}
