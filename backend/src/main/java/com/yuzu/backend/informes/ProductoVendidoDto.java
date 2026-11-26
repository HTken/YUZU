package com.yuzu.backend.informes;

import java.math.BigDecimal;

/** Cuánto se vendió de un producto: unidades e importe (a los precios a los que se vendió). */
public record ProductoVendidoDto(Long productoId, String nombre, String categoria, long cantidad, BigDecimal importe) {
}
