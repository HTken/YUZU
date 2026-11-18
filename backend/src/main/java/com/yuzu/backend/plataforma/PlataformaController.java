package com.yuzu.backend.plataforma;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.yuzu.backend.suscripcion.ResultadoVencimientosDto;

import jakarta.validation.Valid;

/** Solo el equipo de Yuzu llega aquí (ver SecurityConfig). */
@RestController
@RequestMapping("/api/plataforma")
public class PlataformaController {

    private final PlataformaService servicio;

    public PlataformaController(PlataformaService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/planes")
    public List<PlanDto> planes() {
        return servicio.listarPlanes();
    }

    @GetMapping("/restaurantes")
    public List<RestauranteResumenDto> restaurantes() {
        return servicio.listarRestaurantes();
    }

    @PostMapping("/restaurantes")
    @ResponseStatus(HttpStatus.CREATED)
    public RestauranteResumenDto crear(@Valid @RequestBody RestauranteCrearRequest request) {
        return servicio.crear(request);
    }

    @PutMapping("/restaurantes/{id}/plan")
    public RestauranteResumenDto cambiarPlan(@PathVariable Long id, @Valid @RequestBody CambiarPlanRequest request) {
        return servicio.cambiarPlan(id, request.planId());
    }

    @PostMapping("/restaurantes/{id}/suspender")
    public RestauranteResumenDto suspender(@PathVariable Long id) {
        return servicio.suspender(id);
    }

    @PostMapping("/restaurantes/{id}/reactivar")
    public RestauranteResumenDto reactivar(@PathVariable Long id) {
        return servicio.reactivar(id);
    }

    @PostMapping("/restaurantes/{id}/renovar")
    public RestauranteResumenDto renovar(@PathVariable Long id, @Valid @RequestBody RenovarRequest request) {
        return servicio.renovar(id, request.meses(), request.metodo(), request.referencia());
    }

    @PostMapping("/vencimientos/procesar")
    public ResultadoVencimientosDto procesarVencimientos() {
        return servicio.procesarVencimientos();
    }
}
