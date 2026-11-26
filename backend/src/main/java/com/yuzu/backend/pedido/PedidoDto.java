package com.yuzu.backend.pedido;

import java.math.BigDecimal;
import java.util.List;

/** Una cuenta completa. Las fechas van en formato 2026-11-18T07:05:12Z (hora UTC). */
public record PedidoDto(
        Long id,
        Long mesaId,
        String mesa,
        String salon,
        String estado,
        String mozo,
        String abiertoEn,
        List<ItemDto> items,
        BigDecimal total,
        int pendientes) {
}
