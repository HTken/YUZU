package com.yuzu.backend.plataforma;

public record PlataformaLoginResponse(
        String token,
        long expiraEnSegundos,
        String usuario,
        String nombre) {
}
