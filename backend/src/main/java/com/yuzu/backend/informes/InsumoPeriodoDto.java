package com.yuzu.backend.informes;

import java.math.BigDecimal;

/**
 * Lo que pasó con un insumo en el periodo, en su unidad de medida.
 * consumo: descontado por las ventas. merma: lo que se malogró. entradas: lo que se repuso.
 * compras: lo que costaron esas entradas (solo las que tienen costo). faltante: lo que se vendió sin que
 * el stock registrado alcanzara.
 */
public record InsumoPeriodoDto(
        Long insumoId,
        String nombre,
        String unidad,
        BigDecimal consumo,
        BigDecimal merma,
        BigDecimal entradas,
        BigDecimal compras,
        BigDecimal faltante) {
}
