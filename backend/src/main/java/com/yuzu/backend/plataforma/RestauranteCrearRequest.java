package com.yuzu.backend.plataforma;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RestauranteCrearRequest(
        @NotBlank(message = "El nombre del restaurante es obligatorio")
        @Size(max = 150, message = "El nombre no puede pasar de 150 caracteres")
        String nombre,

        @NotBlank(message = "El subdominio es obligatorio")
        @Size(max = 63, message = "El subdominio no puede pasar de 63 caracteres")
        String subdominio,

        @NotNull(message = "Elige un plan")
        Long planId,

        @NotNull(message = "Indica los días hasta el primer vencimiento")
        @Min(value = 0, message = "Los días no pueden ser negativos")
        @Max(value = 365, message = "Los días no pueden pasar de 365")
        Integer diasDePrueba,

        @NotBlank(message = "El usuario administrador es obligatorio")
        @Size(max = 60, message = "El usuario no puede pasar de 60 caracteres")
        @Pattern(regexp = "^[A-Za-z0-9._-]+$",
                 message = "El usuario solo puede tener letras, números, punto, guion y guion bajo")
        String adminUsuario,

        @NotBlank(message = "El nombre del administrador es obligatorio")
        @Size(max = 150, message = "El nombre no puede pasar de 150 caracteres")
        String adminNombre,

        @NotBlank(message = "La contraseña del administrador es obligatoria")
        @Size(min = 8, max = 64, message = "La contraseña debe tener entre 8 y 64 caracteres")
        String adminClave) {
}
