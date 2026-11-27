package com.yuzu.backend.auth;

/** Lo que recibe el navegador al hacer login: o falta el código de 2FA, o ya quedó la sesión iniciada. */
public record LoginRespuestaDto(boolean requiere2fa, SesionDto sesion) {

    public static LoginRespuestaDto requiere2FA() {
        return new LoginRespuestaDto(true, null);
    }

    public static LoginRespuestaDto de(SesionDto sesion) {
        return new LoginRespuestaDto(false, sesion);
    }
}
