package com.yuzu.backend.plataforma;

/** Lo que ve el navegador después de iniciar sesión: el token en sí va solo en la cookie httpOnly. */
public record PlataformaSesionDto(
        long expiraEnSegundos,
        String usuario,
        String nombre) {
}
