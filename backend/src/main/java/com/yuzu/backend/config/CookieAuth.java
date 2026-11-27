package com.yuzu.backend.config;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * Arma la cookie httpOnly donde viaja el token (nunca accesible desde JavaScript) y la que la borra
 * al cerrar sesión.
 * <p>
 * {@code SameSite=None} porque app-cliente usa subdominios por restaurante (demo.localhost, etc.) que
 * el navegador trata como un sitio distinto de donde corre el backend (localhost): con {@code Lax} la
 * cookie no viajaría entre ellos (confirmado probando con un navegador real). {@code Secure} es
 * obligatorio junto con {@code None}; funciona igual sobre http en localhost porque Chrome lo trata como
 * seguro, pero en producción el backend y los frontends sí deben servirse por HTTPS, o el navegador no
 * va a guardar ni mandar la cookie.
 */
@Component
public class CookieAuth {

    public static final String COOKIE_RESTAURANTE = "yuzu_token";
    public static final String COOKIE_PLATAFORMA = "yuzu_plataforma_token";

    public ResponseCookie sesion(String nombre, String token, long segundos) {
        return base(nombre, token).maxAge(segundos).build();
    }

    public ResponseCookie cerrar(String nombre) {
        return base(nombre, "").maxAge(0).build();
    }

    private ResponseCookie.ResponseCookieBuilder base(String nombre, String valor) {
        return ResponseCookie.from(nombre, valor)
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/");
    }
}
