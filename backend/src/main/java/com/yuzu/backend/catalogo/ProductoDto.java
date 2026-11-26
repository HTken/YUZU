package com.yuzu.backend.catalogo;

import java.math.BigDecimal;

public record ProductoDto(
        Long id,
        Long categoriaId,
        String categoria,
        String nombre,
        String descripcion,
        BigDecimal precio,
        boolean activo) {

    public static ProductoDto desde(Producto p, String categoria) {
        return new ProductoDto(p.getId(), p.getCategoriaId(), categoria, p.getNombre(), p.getDescripcion(),
                p.getPrecio(), p.isActivo());
    }
}
