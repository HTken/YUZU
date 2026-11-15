package com.yuzu.backend.config;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;

/**
 * Datos de quien hace la petición, tomados del token ya verificado.
 * Nunca se toman de lo que envía el navegador.
 */
public final class SesionActual {

    private SesionActual() {
    }

    private static Jwt jwt() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion != null && autenticacion.getPrincipal() instanceof Jwt jwt) {
            return jwt;
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesión no válida");
    }

    public static Long restauranteId() {
        Number id = jwt().getClaim("restauranteId");
        return id.longValue();
    }

    public static Long usuarioId() {
        return Long.valueOf(jwt().getSubject());
    }
}
