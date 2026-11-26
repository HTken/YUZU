package com.yuzu.backend.cocina;

/** Un plato que cocina ya marcó como listo y que todavía no se sirvió. */
public record ListoCocinaDto(
        Long id,
        Long pedidoId,
        String mesa,
        String nombre,
        int cantidad,
        String nota,
        String listoEn) {
}
