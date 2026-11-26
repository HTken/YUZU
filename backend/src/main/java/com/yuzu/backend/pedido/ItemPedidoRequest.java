package com.yuzu.backend.pedido;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ItemPedidoRequest(
        @NotNull(message = "Elige un producto")
        Long productoId,

        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 1, message = "La cantidad debe ser de al menos 1")
        @Max(value = 99, message = "La cantidad no puede pasar de 99")
        Integer cantidad,

        @Size(max = 150, message = "La nota no puede pasar de 150 caracteres")
        String nota) {
}
