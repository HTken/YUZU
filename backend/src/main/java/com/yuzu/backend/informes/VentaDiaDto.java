package com.yuzu.backend.informes;

import java.math.BigDecimal;

/** Lo vendido en un día de Lima. La fecha va como AAAA-MM-DD. */
public record VentaDiaDto(String fecha, int ventas, BigDecimal total) {
}
