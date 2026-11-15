package com.yuzu.backend.usuario;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UsuarioCrearRequest(
        @NotBlank(message = "El usuario es obligatorio")
        @Size(max = 60, message = "El usuario no puede pasar de 60 caracteres")
        @Pattern(regexp = "^[A-Za-z0-9._-]+$",
                 message = "El usuario solo puede tener letras, números, punto, guion y guion bajo")
        String usuario,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150, message = "El nombre no puede pasar de 150 caracteres")
        String nombre,

        @Email(message = "El correo no es válido")
        @Size(max = 150, message = "El correo no puede pasar de 150 caracteres")
        String email,

        @NotBlank(message = "El rol es obligatorio")
        String rol,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, max = 64, message = "La contraseña debe tener entre 8 y 64 caracteres")
        String clave) {
}
