package com.yuzu.backend.plataforma;

import jakarta.validation.constraints.NotBlank;

public record PlataformaLoginRequest(
        @NotBlank(message = "El usuario es obligatorio") String usuario,
        @NotBlank(message = "La contraseña es obligatoria") String clave) {
}
