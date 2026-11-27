package com.yuzu.backend.plataforma;

import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yuzu.backend.config.CookieAuth;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/plataforma/auth")
public class PlataformaAuthController {

    private final PlataformaAuthService servicio;
    private final CookieAuth cookieAuth;

    public PlataformaAuthController(PlataformaAuthService servicio, CookieAuth cookieAuth) {
        this.servicio = servicio;
        this.cookieAuth = cookieAuth;
    }

    @PostMapping("/login")
    public PlataformaLoginRespuestaDto login(@Valid @RequestBody PlataformaLoginRequest request,
                                              HttpServletResponse response) {
        PlataformaLoginResultado resultado = servicio.login(request);
        if (resultado.requiere2fa()) {
            return PlataformaLoginRespuestaDto.requiere2FA();
        }

        PlataformaLoginResponse r = resultado.sesion();
        response.addHeader(HttpHeaders.SET_COOKIE,
                cookieAuth.sesion(CookieAuth.COOKIE_PLATAFORMA, r.token(), r.expiraEnSegundos()).toString());
        return PlataformaLoginRespuestaDto.de(new PlataformaSesionDto(r.expiraEnSegundos(), r.usuario(), r.nombre()));
    }

    @PostMapping("/logout")
    public void logout(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookieAuth.cerrar(CookieAuth.COOKIE_PLATAFORMA).toString());
    }
}
