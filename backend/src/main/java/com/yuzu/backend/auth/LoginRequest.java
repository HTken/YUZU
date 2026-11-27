package com.yuzu.backend.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank String subdominio,
        @NotBlank String usuario,
        @NotBlank String clave,
        /** Código de 2FA, solo si el usuario lo tiene activado. Vacío en el primer paso del login. */
        String codigoTotp) {
}
