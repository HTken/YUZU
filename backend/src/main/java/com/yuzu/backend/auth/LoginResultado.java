package com.yuzu.backend.auth;

/**
 * Lo que devuelve {@code AuthService.login}: o falta el código de 2FA ({@code sesion} viene vacío), o el
 * login ya quedó completo y {@code sesion} trae el token para la cookie.
 */
public record LoginResultado(boolean requiere2fa, LoginResponse sesion) {

    public static LoginResultado requiere2FA() {
        return new LoginResultado(true, null);
    }

    public static LoginResultado completo(LoginResponse sesion) {
        return new LoginResultado(false, sesion);
    }
}
