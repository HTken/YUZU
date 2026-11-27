package com.yuzu.backend.usuario;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yuzu.backend.auth.TotpActivarRequest;
import com.yuzu.backend.auth.TotpEstadoDto;
import com.yuzu.backend.auth.TotpSetupDto;

import jakarta.validation.Valid;

/** Solo los administradores llegan aquí (ver SecurityConfig): cada quien activa su propio 2FA. */
@RestController
@RequestMapping("/api/usuarios/2fa")
public class DosFactoresController {

    private final DosFactoresService servicio;

    public DosFactoresController(DosFactoresService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public TotpEstadoDto estado() {
        return servicio.estado();
    }

    @PostMapping("/iniciar")
    public TotpSetupDto iniciar() {
        return servicio.iniciar();
    }

    @PostMapping("/activar")
    public TotpEstadoDto activar(@Valid @RequestBody TotpActivarRequest request) {
        return servicio.activar(request);
    }

    @PostMapping("/desactivar")
    public TotpEstadoDto desactivar() {
        return servicio.desactivar();
    }
}
