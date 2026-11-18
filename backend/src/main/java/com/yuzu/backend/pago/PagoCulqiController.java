package com.yuzu.backend.pago;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/** Solo el administrador del restaurante llega aquí (ver SecurityConfig). */
@RestController
@RequestMapping("/api/pagos/culqi")
public class PagoCulqiController {

    private final PagoCulqiService servicio;

    public PagoCulqiController(PagoCulqiService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/config")
    public CulqiConfigDto configuracion() {
        return servicio.configuracion();
    }

    @PostMapping("/cobrar")
    public ResultadoCobroDto cobrar(@Valid @RequestBody CobrarRequest request) {
        return servicio.cobrar(request);
    }
}
