package com.yuzu.backend.suscripcion;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mi-plan")
public class MiPlanController {

    private final SuscripcionService servicio;

    public MiPlanController(SuscripcionService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public MiPlanDto obtener() {
        return servicio.miPlan();
    }
}
