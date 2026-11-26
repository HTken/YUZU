package com.yuzu.backend.pedido;

import jakarta.validation.constraints.NotNull;

public record AbrirPedidoRequest(
        @NotNull(message = "Elige una mesa")
        Long mesaId) {
}
