package com.yuzu.backend.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank String subdominio,
        @NotBlank String usuario,
        @NotBlank String clave) {
}