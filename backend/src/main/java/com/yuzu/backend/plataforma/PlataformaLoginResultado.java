package com.yuzu.backend.plataforma;

/**
 * Lo que devuelve {@code PlataformaAuthService.login}: o falta el código de 2FA ({@code sesion} viene
 * vacío), o el login ya quedó completo y {@code sesion} trae el token para la cookie.
 */
public record PlataformaLoginResultado(boolean requiere2fa, PlataformaLoginResponse sesion) {

    public static PlataformaLoginResultado requiere2FA() {
        return new PlataformaLoginResultado(true, null);
    }

    public static PlataformaLoginResultado completo(PlataformaLoginResponse sesion) {
        return new PlataformaLoginResultado(false, sesion);
    }
}
