package com.yuzu.backend.usuario;

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

/** Solo los administradores llegan aquí (ver SecurityConfig). */
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService servicio;

    public UsuarioController(UsuarioService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<UsuarioDto> listar() {
        return servicio.listar();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioDto crear(@Valid @RequestBody UsuarioCrearRequest request) {
        return servicio.crear(request);
    }

    @PutMapping("/{id}")
    public UsuarioDto actualizar(@PathVariable Long id, @Valid @RequestBody UsuarioActualizarRequest request) {
        return servicio.actualizar(id, request);
    }
}
