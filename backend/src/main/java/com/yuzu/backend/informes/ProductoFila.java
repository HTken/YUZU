package com.yuzu.backend.informes;

import java.math.BigDecimal;

/** Lo vendido de un producto en el periodo (sin contar lo anulado). */
public record ProductoFila(Long productoId, String nombre, String categoria, Long cantidad, BigDecimal importe) {

    public ProductoFila {
        categoria = categoria == null ? "Sin categoría" : categoria;
        cantidad = cantidad == null ? 0L : cantidad;
        importe = importe == null ? BigDecimal.ZERO : importe;
    }
}
