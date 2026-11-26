package com.yuzu.backend.inventario;

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

/**
 * Ver el inventario y registrar movimientos: administrador, cajero y cocina.
 * Crear o editar insumos: solo el administrador. Ver SecurityConfig.
 */
@RestController
@RequestMapping("/api/inventario")
public class InventarioController {

    private final InventarioService inventario;

    public InventarioController(InventarioService inventario) {
        this.inventario = inventario;
    }

    @GetMapping("/insumos")
    public List<InsumoDto> listar() {
        return inventario.listar();
    }

    @PostMapping("/insumos")
    @ResponseStatus(HttpStatus.CREATED)
    public InsumoDto crear(@Valid @RequestBody InsumoRequest request) {
        return inventario.crear(request);
    }

    @PutMapping("/insumos/{id}")
    public InsumoDto actualizar(@PathVariable Long id, @Valid @RequestBody InsumoRequest request) {
        return inventario.actualizar(id, request);
    }

    @GetMapping("/insumos/{id}/movimientos")
    public List<MovimientoInsumoDto> historial(@PathVariable Long id) {
        return inventario.historial(id);
    }

    @PostMapping("/insumos/{id}/movimientos")
    @ResponseStatus(HttpStatus.CREATED)
    public InsumoDto movimiento(@PathVariable Long id, @Valid @RequestBody MovimientoInsumoRequest request) {
        return inventario.registrarMovimiento(id, request);
    }
}
