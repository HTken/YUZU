package com.yuzu.backend.auth;

public record LoginResponse(
        String token,
        long expiraEnSegundos,
        String usuario,
        String nombre,
        String rol,
        String restaurante) {
}