package com.yuzu.backend.cocina;

import java.util.List;

/**
 * Los platos que un mozo envió juntos desde una mesa. Cada vez que se envía la cuenta se crea un ticket nuevo.
 * Las fechas van en formato 2026-11-18T07:05:12Z (hora UTC).
 */
public record TicketCocinaDto(
        String clave,
        Long pedidoId,
        String mesa,
        String salon,
        String mozo,
        String enviadoEn,
        List<ItemCocinaDto> items) {
}
