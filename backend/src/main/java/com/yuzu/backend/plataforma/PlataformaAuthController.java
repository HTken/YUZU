package com.yuzu.backend.plataforma;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/plataforma/auth")
public class PlataformaAuthController {

    private final PlataformaAuthService servicio;

    public PlataformaAuthController(PlataformaAuthService servicio) {
        this.servicio = servicio;
    }

    @PostMapping("/login")
    public PlataformaLoginResponse login(@Valid @RequestBody PlataformaLoginRequest request) {
        return servicio.login(request);
    }
}
