package com.yuzu.backend.inventario;

import java.math.BigDecimal;

public record InsumoDto(
        Long id,
        String nombre,
        String unidad,
        BigDecimal stockActual,
        BigDecimal stockMinimo,
        boolean activo,
        /** OK, BAJO, AGOTADO o INACTIVO. */
        String estado,
        /** Si ya tiene movimientos, su unidad de medida no se puede cambiar. */
        boolean unidadFija) {

    public static InsumoDto desde(Insumo i, boolean unidadFija) {
        return new InsumoDto(i.getId(), i.getNombre(), i.getUnidad(), i.getStockActual(), i.getStockMinimo(),
                i.isActivo(), estado(i), unidadFija);
    }

    /** Agotado si no queda nada; bajo si llegó (o bajó de) su mínimo, cuando se configuró uno. */
    static String estado(Insumo i) {
        if (!i.isActivo()) {
            return "INACTIVO";
        }
        if (i.getStockActual().signum() <= 0) {
            return "AGOTADO";
        }
        if (i.getStockMinimo().signum() > 0 && i.getStockActual().compareTo(i.getStockMinimo()) <= 0) {
            return "BAJO";
        }
        return "OK";
    }
}
