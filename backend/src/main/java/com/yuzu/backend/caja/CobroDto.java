package com.yuzu.backend.caja;

import java.math.BigDecimal;

/** Constancia del cobro, para mostrar el número de ticket. */
public record CobroDto(Long pedidoId, int ticket, BigDecimal total, String cobradoEn) {
}
