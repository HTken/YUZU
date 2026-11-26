package com.yuzu.backend.caja;

import java.math.BigDecimal;

/** Una venta que se cobró y luego se anuló dentro del mismo turno. Ya no suma en la caja; queda como constancia. */
public record VentaAnuladaDto(
        Long pedidoId,
        Integer ticket,
        String mesa,
        BigDecimal total,
        String cobradoEn,
        String anuladoPor,
        String anuladoEn,
        String motivo) {
}
