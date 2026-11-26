package com.yuzu.backend.caja;

import java.math.BigDecimal;
import java.util.List;

/** Una cuenta cobrada dentro del turno. */
public record VentaDto(
        Long pedidoId,
        Integer ticket,
        String mesa,
        String mozo,
        String cobradoPor,
        String cobradoEn,
        BigDecimal total,
        List<PagoDto> pagos) {
}
