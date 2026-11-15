package com.yuzu.backend.usuario;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UsuarioActualizarRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150, message = "El nombre no puede pasar de 150 caracteres")
        String nombre,

        @Email(message = "El correo no es válido")
        @Size(max = 150, message = "El correo no puede pasar de 150 caracteres")
        String email,

        @NotBlank(message = "El rol es obligatorio")
        String rol,

        @NotNull(message = "Indica si el usuario está activo")
        Boolean activo,

        /** Opcional: si viene vacío, la contraseña actual no cambia. */
        @Size(min = 8, max = 64, message = "La contraseña debe tener entre 8 y 64 caracteres")
        String clave) {
}
