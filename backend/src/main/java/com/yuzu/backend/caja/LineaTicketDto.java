package com.yuzu.backend.caja;

import java.math.BigDecimal;

/** Una línea del ticket: lo que se pidió, a qué precio y cuánto suma. Los platos anulados no salen. */
public record LineaTicketDto(String nombre, int cantidad, BigDecimal precioUnitario, BigDecimal importe, String nota) {
}
