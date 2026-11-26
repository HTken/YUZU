package com.yuzu.backend.cocina;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ItemsRequest(
        @NotEmpty(message = "Indica qué platos quieres marcar")
        @Size(max = 100, message = "Son demasiados platos a la vez")
        List<@NotNull(message = "Plato no válido") Long> itemIds) {
}
