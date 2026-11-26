package com.yuzu.backend.caja;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/** Solo el administrador y el cajero usan la caja; anular una venta cobrada es solo del administrador (ver SecurityConfig). */
@RestController
@RequestMapping("/api/caja")
public class CajaController {

    private final CajaService caja;
    private final CobroService cobros;
    private final AnulacionVentaService anulaciones;

    public CajaController(CajaService caja, CobroService cobros, AnulacionVentaService anulaciones) {
        this.caja = caja;
        this.cobros = cobros;
        this.anulaciones = anulaciones;
    }

    @GetMapping("/actual")
    public EstadoCajaDto actual() {
        return caja.estado();
    }

    @GetMapping("/turnos")
    public List<TurnoDto> turnos() {
        return caja.historial();
    }

    @PostMapping("/abrir")
    @ResponseStatus(HttpStatus.CREATED)
    public ResumenTurnoDto abrir(@Valid @RequestBody AbrirCajaRequest request) {
        return caja.abrir(request);
    }

    @PostMapping("/movimientos")
    @ResponseStatus(HttpStatus.CREATED)
    public ResumenTurnoDto movimiento(@Valid @RequestBody MovimientoRequest request) {
        return caja.registrarMovimiento(request);
    }

    @PostMapping("/cerrar")
    public TurnoDto cerrar(@Valid @RequestBody CerrarCajaRequest request) {
        return caja.cerrar(request);
    }

    @PostMapping("/pedidos/{pedidoId}/cobrar")
    public CobroDto cobrar(@PathVariable Long pedidoId, @Valid @RequestBody CobrarRequest request) {
        return cobros.cobrar(pedidoId, request);
    }

    /** Anular una venta ya cobrada: solo el administrador (ver SecurityConfig). */
    @PostMapping("/ventas/{pedidoId}/anular")
    public ResumenTurnoDto anularVenta(@PathVariable Long pedidoId, @Valid @RequestBody AnularVentaRequest request) {
        return anulaciones.anular(pedidoId, request);
    }
}
