package com.yuzu.backend.caja;

import java.math.BigDecimal;

import com.yuzu.backend.config.Tiempo;

public record MovimientoDto(
        Long id,
        String tipo,
        String concepto,
        BigDecimal monto,
        String registradoPor,
        String creadoEn) {

    public static MovimientoDto desde(Movimiento m) {
        return new MovimientoDto(m.getId(), m.getTipo(), m.getConcepto(), m.getMonto(), m.getRegistradoPor(),
                Tiempo.iso(m.getCreadoEn()));
    }
}
