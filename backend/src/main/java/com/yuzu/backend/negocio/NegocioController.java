package com.yuzu.backend.negocio;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/** Datos del negocio (ticket). Solo el administrador (ver SecurityConfig). */
@RestController
@RequestMapping("/api/negocio")
public class NegocioController {

    private final NegocioService negocio;

    public NegocioController(NegocioService negocio) {
        this.negocio = negocio;
    }

    @GetMapping
    public NegocioDto obtener() {
        return negocio.obtener();
    }

    @PutMapping
    public NegocioDto guardar(@Valid @RequestBody NegocioRequest request) {
        return negocio.guardar(request);
    }
}
