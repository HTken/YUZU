package com.yuzu.backend.cocina;

/**
 * Un plato que se anuló mientras cocina lo tenía en pantalla. Se muestra hasta que cocina toca "Entendido".
 * yaEstabaListo = true si cocina ya lo había marcado como listo.
 */
public record AnuladoCocinaDto(
        Long id,
        Long pedidoId,
        String mesa,
        String nombre,
        int cantidad,
        String nota,
        boolean yaEstabaListo,
        String anuladoEn) {
}
