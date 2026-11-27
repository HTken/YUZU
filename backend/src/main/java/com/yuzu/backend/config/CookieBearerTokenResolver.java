package com.yuzu.backend.config;

import java.util.Set;

import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

/**
 * El navegador manda el token en una cookie httpOnly (así nunca queda accesible desde JavaScript). Si no
 * hay cookie, se acepta igual el encabezado Authorization, que es lo que usan las pruebas automatizadas y
 * cualquier llamada directa a la API (Postman, etc).
 * <p>
 * En login/logout se ignora cualquier cookie existente: a diferencia del encabezado (que controlaba el
 * frontend y nunca mandaba en el login), el navegador adjunta la cookie sola aunque esté vencida; si se
 * intentara validar ahí, fallaría la autenticación antes de llegar al controlador y bloquearía el propio
 * login que se supone la debe reemplazar.
 */
public class CookieBearerTokenResolver implements BearerTokenResolver {

    private static final Set<String> SIN_COOKIE = Set.of(
            "/api/auth/login", "/api/auth/logout",
            "/api/plataforma/auth/login", "/api/plataforma/auth/logout");

    private final DefaultBearerTokenResolver resolverDeEncabezado = new DefaultBearerTokenResolver();

    @Override
    public String resolve(HttpServletRequest request) {
        if (SIN_COOKIE.contains(request.getRequestURI())) {
            return null;
        }

        String nombreCookie = request.getRequestURI().contains("/api/plataforma")
                ? CookieAuth.COOKIE_PLATAFORMA
                : CookieAuth.COOKIE_RESTAURANTE;

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (nombreCookie.equals(cookie.getName()) && !cookie.getValue().isBlank()) {
                    return cookie.getValue();
                }
            }
        }
        return resolverDeEncabezado.resolve(request);
    }
}
