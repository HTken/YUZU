package com.yuzu.backend.pago;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/** Solo el administrador del restaurante llega aquí (ver SecurityConfig), salvo el webhook. */
@RestController
@RequestMapping("/api/pagos/culqi")
public class PagoCulqiController {

    private final PagoCulqiService servicio;
    private final CulqiSuscripcionService suscripcion;

    public PagoCulqiController(PagoCulqiService servicio, CulqiSuscripcionService suscripcion) {
        this.servicio = servicio;
        this.suscripcion = suscripcion;
    }

    @GetMapping("/config")
    public CulqiConfigDto configuracion() {
        return servicio.configuracion();
    }

    @PostMapping("/cobrar")
    public ResultadoCobroDto cobrar(@Valid @RequestBody CobrarRequest request) {
        return servicio.cobrar(request);
    }

    @GetMapping("/suscripcion")
    public SuscripcionCulqiDto estadoSuscripcion() {
        return suscripcion.estado();
    }

    @PostMapping("/suscripcion")
    public SuscripcionCulqiDto activarSuscripcion(@Valid @RequestBody ActivarSuscripcionRequest request) {
        return suscripcion.activar(request);
    }

    @DeleteMapping("/suscripcion")
    public SuscripcionCulqiDto cancelarSuscripcion() {
        return suscripcion.cancelar();
    }

    /**
     * El cargo necesita 3D Secure. 428 (Precondition Required) para que el frontend lo distinga de un
     * pago rechazado y sepa que tiene que iniciar la autenticación con Culqi3DS y reintentar.
     */
    @ExceptionHandler(Requiere3DSException.class)
    public ResponseEntity<Map<String, Object>> requiere3DS() {
        return ResponseEntity.status(HttpStatus.PRECONDITION_REQUIRED)
                .body(Map.of("mensaje", "El banco pide una verificación adicional (3D Secure).", "requiere3DS", true));
    }
}
