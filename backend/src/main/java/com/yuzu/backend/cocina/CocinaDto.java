package com.yuzu.backend.cocina;

import java.util.List;

/** Lo que muestra la pantalla de cocina: lo que hay que preparar, lo que ya salió y lo que se anuló. */
public record CocinaDto(
        List<TicketCocinaDto> pendientes,
        List<ListoCocinaDto> listos,
        List<AnuladoCocinaDto> anulados) {
}
