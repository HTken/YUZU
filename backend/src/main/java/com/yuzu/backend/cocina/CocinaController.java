package com.yuzu.backend.cocina;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/** Solo el administrador y el personal de cocina usan esta pantalla (ver SecurityConfig). */
@RestController
@RequestMapping("/api/cocina")
public class CocinaController {

    private final CocinaService servicio;

    public CocinaController(CocinaService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public CocinaDto estado() {
        return servicio.estado();
    }

    @PostMapping("/listo")
    public CocinaDto listo(@Valid @RequestBody ItemsRequest request) {
        return servicio.marcarListos(request.itemIds());
    }

    @PostMapping("/anulados/visto")
    public CocinaDto anuladosVistos(@Valid @RequestBody ItemsRequest request) {
        return servicio.marcarVistos(request.itemIds());
    }

    @PostMapping("/regresar")
    public CocinaDto regresar(@Valid @RequestBody ItemsRequest request) {
        return servicio.regresar(request.itemIds());
    }
}
