package com.yuzu.backend.auth;

/** Lo que ve el navegador después de iniciar sesión: el token en sí va solo en la cookie httpOnly. */
public record SesionDto(
        long expiraEnSegundos,
        String usuario,
        String nombre,
        String rol,
        String restaurante) {
}
