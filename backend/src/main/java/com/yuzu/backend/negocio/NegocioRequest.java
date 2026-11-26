package com.yuzu.backend.negocio;

import jakarta.validation.constraints.Size;

/**
 * Lo que el administrador puede cambiar. Todo es opcional salvo el ancho de papel. Los textos vacíos se guardan
 * como "sin dato". Las demás reglas (RUC con dígito verificador, teléfono, ancho 58 u 80) están en ReglasNegocio.
 */
public record NegocioRequest(
        @Size(max = 150, message = "La razón social no puede pasar de 150 caracteres")
        String razonSocial,

        @Size(max = 20, message = "El RUC no es válido")
        String ruc,

        @Size(max = 200, message = "La dirección no puede pasar de 200 caracteres")
        String direccion,

        @Size(max = 40, message = "El teléfono no puede pasar de 30 caracteres")
        String telefono,

        @Size(max = 160, message = "El mensaje no puede pasar de 120 caracteres")
        String mensajeTicket,

        Integer anchoTicketMm) {
}
