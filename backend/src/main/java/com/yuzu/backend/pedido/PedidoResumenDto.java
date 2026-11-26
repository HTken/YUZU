package com.yuzu.backend.pedido;

import java.math.BigDecimal;

/**
 * Lo mínimo de cada cuenta abierta, para pintar el mapa de mesas.
 * enCocina = platos que cocina todavía prepara; listos = platos listos para que el mozo los sirva.
 */
public record PedidoResumenDto(
        Long id,
        Long mesaId,
        String mozo,
        String abiertoEn,
        BigDecimal total,
        int productos,
        int pendientes,
        int enCocina,
        int listos) {
}
