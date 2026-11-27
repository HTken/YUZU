package com.yuzu.backend.auth;

/**
 * El secreto todavía no se guarda: solo queda activado cuando se confirma con un código válido
 * (ver {@link TotpActivarRequest}).
 */
public record TotpSetupDto(String secreto, String otpauthUrl) {
}
