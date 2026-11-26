package com.yuzu.backend.informes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Una cuenta cobrada: cuándo (hora UTC) y cuánto. Es lo único que hace falta para sumar por día. */
public record VentaFila(LocalDateTime cobradoEn, BigDecimal total) {

    public VentaFila {
        total = total == null ? BigDecimal.ZERO : total;
    }
}
