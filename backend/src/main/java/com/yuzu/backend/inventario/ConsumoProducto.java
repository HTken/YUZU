package com.yuzu.backend.inventario;

/** Un producto vendido y cuántas unidades. Es lo único que el inventario necesita saber de una cuenta. */
public record ConsumoProducto(Long productoId, int cantidad) {
}
