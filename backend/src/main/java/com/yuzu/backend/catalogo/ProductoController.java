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
@RequestMapping("/api/productos")
public class ProductoController {

    private final CatalogoService servicio;

    public ProductoController(CatalogoService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<ProductoDto> listar() {
        return servicio.listarProductos();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductoDto crear(@Valid @RequestBody ProductoRequest request) {
        return servicio.crearProducto(request);
    }

    @PutMapping("/{id}")
    public ProductoDto actualizar(@PathVariable Long id, @Valid @RequestBody ProductoRequest request) {
        return servicio.actualizarProducto(id, request);
    }
}
