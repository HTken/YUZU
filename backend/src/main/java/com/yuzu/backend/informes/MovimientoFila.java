package com.yuzu.backend.informes;

import java.math.BigDecimal;

/**
 * Los movimientos de un insumo de un mismo tipo en el periodo, sumados.
 * La cantidad lleva signo (las ventas y mermas son negativas). Faltante y costo pueden venir vacíos.
 */
public record MovimientoFila(Long insumoId, String tipo, BigDecimal cantidad, BigDecimal faltante, BigDecimal costo) {

    public MovimientoFila {
        cantidad = cantidad == null ? BigDecimal.ZERO : cantidad;
        faltante = faltante == null ? BigDecimal.ZERO : faltante;
        costo = costo == null ? BigDecimal.ZERO : costo;
    }
}
