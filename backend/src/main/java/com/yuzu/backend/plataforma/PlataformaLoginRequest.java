package com.yuzu.backend.plataforma;

import jakarta.validation.constraints.NotBlank;

public record PlataformaLoginRequest(
        @NotBlank(message = "El usuario es obligatorio") String usuario,
        @NotBlank(message = "La contraseña es obligatoria") String clave,
        /** Código de 2FA, solo si el administrador lo tiene activado. Vacío en el primer paso del login. */
        String codigoTotp) {
}
