package com.yuzu.backend.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.yuzu.backend.suscripcion.SuscripcionService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Si la suscripción del restaurante está suspendida, bloquea el resto de la API. */
@Component
public class SuscripcionInterceptor implements HandlerInterceptor {

    private final SuscripcionService suscripcion;

    public SuscripcionInterceptor(SuscripcionService suscripcion) {
        this.suscripcion = suscripcion;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion != null && autenticacion.getPrincipal() instanceof Jwt) {
            suscripcion.exigirActiva(SesionActual.restauranteId());
        }
        return true;
    }
}
