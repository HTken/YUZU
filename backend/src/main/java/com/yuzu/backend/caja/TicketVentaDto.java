package com.yuzu.backend.caja;

import java.math.BigDecimal;
import java.util.List;

import com.yuzu.backend.negocio.NegocioDto;

/** Todo lo que lleva el ticket de una venta cobrada, listo para imprimir. */
public record TicketVentaDto(
        NegocioDto negocio,
        Integer ticket,
        String mesa,
        String mozo,
        String cobradoPor,
        String cobradoEn,
        List<LineaTicketDto> lineas,
        BigDecimal total,
        List<PagoDto> pagos) {
}
