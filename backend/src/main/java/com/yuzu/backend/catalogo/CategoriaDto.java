package com.yuzu.backend.catalogo;

public record CategoriaDto(
        Long id,
        String nombre,
        int orden,
        boolean activo,
        boolean pasaPorCocina,
        int productos,
        int productosActivos) {

    public static CategoriaDto desde(Categoria c, int productos, int productosActivos) {
        return new CategoriaDto(c.getId(), c.getNombre(), c.getOrden(), c.isActivo(), c.isPasaPorCocina(), productos,
                productosActivos);
    }
}
