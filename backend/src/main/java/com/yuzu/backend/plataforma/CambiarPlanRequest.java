package com.yuzu.backend.plataforma;

import jakarta.validation.constraints.NotNull;

public record CambiarPlanRequest(
        @NotNull(message = "Elige un plan") Long planId) {
}
