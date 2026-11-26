package com.yuzu.backend.inventario;

import java.math.BigDecimal;
import java.util.List;

public record RecetaDto(Long productoId, List<Linea> lineas) {

    public record Linea(Long insumoId, String insumo, String unidad, BigDecimal cantidad) {
    }
}
