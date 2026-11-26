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
@RequestMapping("/api/mesas")
public class MesaController {

    private final MesasService servicio;

    public MesaController(MesasService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<MesaDto> listar() {
        return servicio.listarMesas();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MesaDto crear(@Valid @RequestBody MesaRequest request) {
        return servicio.crearMesa(request);
    }

    @PostMapping("/lote")
    @ResponseStatus(HttpStatus.CREATED)
    public List<MesaDto> crearLote(@Valid @RequestBody MesaLoteRequest request) {
        return servicio.crearLote(request);
    }

    @PutMapping("/{id}")
    public MesaDto actualizar(@PathVariable Long id, @Valid @RequestBody MesaRequest request) {
        return servicio.actualizarMesa(id, request);
    }
}
