package com.yuzu.backend.pedido;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
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
 * Administrador, cajero y mozo pueden tomar pedidos. Anular una cuenta o un producto ya enviado
 * es solo del administrador (ver SecurityConfig).
 */
@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final PedidosService servicio;

    public PedidoController(PedidosService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/abiertos")
    public List<PedidoResumenDto> abiertos() {
        return servicio.abiertos();
    }

    @GetMapping("/{id}")
    public PedidoDto obtener(@PathVariable Long id) {
        return servicio.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PedidoDto abrir(@Valid @RequestBody AbrirPedidoRequest request) {
        return servicio.abrir(request.mesaId());
    }

    @PostMapping("/{id}/items")
    @ResponseStatus(HttpStatus.CREATED)
    public PedidoDto agregarItem(@PathVariable Long id, @Valid @RequestBody ItemPedidoRequest request) {
        return servicio.agregarItem(id, request);
    }

    @PutMapping("/{id}/items/{itemId}")
    public PedidoDto actualizarItem(@PathVariable Long id, @PathVariable Long itemId,
                                    @Valid @RequestBody ActualizarItemRequest request) {
        return servicio.actualizarItem(id, itemId, request);
    }

    @DeleteMapping("/{id}/items/{itemId}")
    public PedidoDto quitarItem(@PathVariable Long id, @PathVariable Long itemId) {
        return servicio.quitarItem(id, itemId);
    }

    @PostMapping("/{id}/enviar")
    public PedidoDto enviar(@PathVariable Long id) {
        return servicio.enviar(id);
    }

    @PostMapping("/{id}/anular")
    public PedidoDto anular(@PathVariable Long id) {
        return servicio.anular(id);
    }

    @PostMapping("/{id}/items/{itemId}/servir")
    public PedidoDto servir(@PathVariable Long id, @PathVariable Long itemId) {
        return servicio.servir(id, itemId);
    }

    @PostMapping("/{id}/servir")
    public PedidoDto servirListos(@PathVariable Long id) {
        return servicio.servirListos(id);
    }

    @PostMapping("/{id}/items/{itemId}/anular")
    public PedidoDto anularItem(@PathVariable Long id, @PathVariable Long itemId) {
        return servicio.anularItem(id, itemId);
    }
}
