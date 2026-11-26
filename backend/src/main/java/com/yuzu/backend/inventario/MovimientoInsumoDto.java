package com.yuzu.backend.inventario;

import java.math.BigDecimal;

import com.yuzu.backend.config.Tiempo;

public record MovimientoInsumoDto(
        Long id,
        String tipo,
        /** Con signo: positiva sumó al stock y negativa restó. */
        BigDecimal cantidad,
        BigDecimal stockResultante,
        BigDecimal costoTotal,
        /** Solo en las ventas: lo que faltó porque el stock registrado no alcanzaba. */
        BigDecimal faltante,
        String nota,
        String registradoPor,
        String creadoEn) {

    public static MovimientoInsumoDto desde(MovimientoInsumo m) {
        return new MovimientoInsumoDto(m.getId(), m.getTipo(), m.getCantidad(), m.getStockResultante(),
                m.getCostoTotal(), m.getFaltante(), m.getNota(), m.getRegistradoPor(), Tiempo.iso(m.getCreadoEn()));
    }
}
