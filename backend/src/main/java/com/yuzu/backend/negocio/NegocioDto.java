package com.yuzu.backend.negocio;

/**
 * Los datos del negocio para mostrar y para el ticket. "nombre" es el nombre del restaurante en el sistema
 * (lo cambia la plataforma, no el administrador); lo demás lo edita el administrador. Lo que no se llenó va null.
 */
public record NegocioDto(
        String nombre,
        String razonSocial,
        String ruc,
        String direccion,
        String telefono,
        String mensajeTicket,
        int anchoTicketMm) {
}
