package com.yuzu.backend.mesa;

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

import jakarta.validation.Valid;

/** Leer: cualquier usuario del restaurante. Crear y editar: solo ADMIN (ver SecurityConfig). */
@RestController
@RequestMapping("/api/salones")
public class SalonController {

    private final MesasService servicio;

    public SalonController(MesasService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<SalonDto> listar() {
        return servicio.listarSalones();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SalonDto crear(@Valid @RequestBody SalonRequest request) {
        return servicio.crearSalon(request);
    }

    @PutMapping("/{id}")
    public SalonDto actualizar(@PathVariable Long id, @Valid @RequestBody SalonRequest request) {
        return servicio.actualizarSalon(id, request);
    }
}
