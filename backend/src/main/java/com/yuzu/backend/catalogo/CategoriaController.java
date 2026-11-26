package com.yuzu.backend.catalogo;

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
@RequestMapping("/api/categorias")
public class CategoriaController {

    private final CatalogoService servicio;

    public CategoriaController(CatalogoService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<CategoriaDto> listar() {
        return servicio.listarCategorias();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoriaDto crear(@Valid @RequestBody CategoriaRequest request) {
        return servicio.crearCategoria(request);
    }

    @PutMapping("/{id}")
    public CategoriaDto actualizar(@PathVariable Long id, @Valid @RequestBody CategoriaRequest request) {
        return servicio.actualizarCategoria(id, request);
    }
}
