package com.yuzu.backend.inventario;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/** Ver y editar recetas es solo del administrador. Ver SecurityConfig. */
@RestController
@RequestMapping("/api/inventario/recetas")
public class RecetasController {

    private final RecetasService recetas;

    public RecetasController(RecetasService recetas) {
        this.recetas = recetas;
    }

    @GetMapping
    public List<ResumenReceta> resumen() {
        return recetas.resumen();
    }

    @GetMapping("/{productoId}")
    public RecetaDto obtener(@PathVariable Long productoId) {
        return recetas.obtener(productoId);
    }

    @PutMapping("/{productoId}")
    public RecetaDto guardar(@PathVariable Long productoId, @Valid @RequestBody RecetaRequest request) {
        return recetas.guardar(productoId, request);
    }
}
