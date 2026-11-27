package com.yuzu.backend.auth;

import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yuzu.backend.config.CookieAuth;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final AuthService authService;
    private final CookieAuth cookieAuth;

    public AuthController(AuthService authService, CookieAuth cookieAuth) {
        this.authService = authService;
        this.cookieAuth = cookieAuth;
    }

    @PostMapping("/auth/login")
    public LoginRespuestaDto login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        LoginResultado resultado = authService.login(request);
        if (resultado.requiere2fa()) {
            return LoginRespuestaDto.requiere2FA();
        }

        LoginResponse r = resultado.sesion();
        response.addHeader(HttpHeaders.SET_COOKIE,
                cookieAuth.sesion(CookieAuth.COOKIE_RESTAURANTE, r.token(), r.expiraEnSegundos()).toString());
        return LoginRespuestaDto.de(new SesionDto(r.expiraEnSegundos(), r.usuario(), r.nombre(), r.rol(), r.restaurante()));
    }

    @PostMapping("/auth/logout")
    public void logout(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookieAuth.cerrar(CookieAuth.COOKIE_RESTAURANTE).toString());
    }

    @GetMapping("/me")
    public Map<String, String> me(@AuthenticationPrincipal Jwt jwt) {
        return Map.of(
                "usuario", jwt.getClaimAsString("usuario"),
                "rol", jwt.getClaimAsString("rol"),
                "restauranteId", jwt.getClaimAsString("restauranteId"));
    }
}
