package com.yuzu.backend.inventario;

/** Cuántos insumos tiene la receta de un producto. Los productos sin receta no aparecen. */
public record ResumenReceta(Long productoId, Long lineas) {
}
