package com.yuzu.backend.plataforma;

public record PlataformaLoginResponse(
        String token,
        String tipo,
        long expiraEnSegundos,
        String usuario,
        String nombre) {
}
