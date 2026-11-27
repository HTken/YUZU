package com.yuzu.backend.plataforma;

/** Lo que recibe el navegador al hacer login: o falta el código de 2FA, o ya quedó la sesión iniciada. */
public record PlataformaLoginRespuestaDto(boolean requiere2fa, PlataformaSesionDto sesion) {

    public static PlataformaLoginRespuestaDto requiere2FA() {
        return new PlataformaLoginRespuestaDto(true, null);
    }

    public static PlataformaLoginRespuestaDto de(PlataformaSesionDto sesion) {
        return new PlataformaLoginRespuestaDto(false, sesion);
    }
}
