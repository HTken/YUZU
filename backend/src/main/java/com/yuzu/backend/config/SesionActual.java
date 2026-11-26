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
        if (id == null) {
            // Es un token de la consola de plataforma: no pertenece a ningún restaurante.
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Esta sesión no pertenece a un restaurante");
        }
        return id.longValue();
    }

    public static Long usuarioId() {
        return Long.valueOf(jwt().getSubject());
    }

    /** Si quien hace la petición tiene ese rol (por ejemplo "ADMIN"), según las autoridades del token verificado. */
    public static boolean tieneRol(String rol) {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        return autenticacion != null && autenticacion.getAuthorities().stream()
                .anyMatch(autoridad -> ("ROLE_" + rol).equals(autoridad.getAuthority()));
    }

    /** Nombre de usuario de quien hace la petición, para dejar constancia de quién registró algo. */
    public static String usuario() {
        String usuario = jwt().getClaimAsString("usuario");
        return usuario != null ? usuario : "desconocido";
    }
}
