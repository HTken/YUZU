package com.yuzu.backend.pedido;

import java.math.BigDecimal;

public record ItemDto(
        Long id,
        Long productoId,
        String nombre,
        BigDecimal precioUnitario,
        int cantidad,
        String nota,
        String estado,
        BigDecimal subtotal) {

    public static ItemDto desde(PedidoItem i) {
        return new ItemDto(i.getId(), i.getProductoId(), i.getNombre(), i.getPrecioUnitario(), i.getCantidad(),
                i.getNota(), i.getEstado(), i.getPrecioUnitario().multiply(BigDecimal.valueOf(i.getCantidad())));
    }
}
