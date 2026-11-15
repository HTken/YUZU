package com.yuzu.backend.config;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

/** Devuelve los errores como {"mensaje": "..."} para que Angular pueda mostrarlos. */
@RestControllerAdvice
public class ManejadorErrores {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> estado(ResponseStatusException e) {
        String mensaje = e.getReason() != null ? e.getReason() : "No se pudo completar la operación";
        return ResponseEntity.status(e.getStatusCode()).body(Map.of("mensaje", mensaje));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> validacion(MethodArgumentNotValidException e) {
        String mensaje = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getDefaultMessage())
                .findFirst()
                .orElse("Los datos enviados no son válidos");
        return ResponseEntity.badRequest().body(Map.of("mensaje", mensaje));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> cuerpoIlegible(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest().body(Map.of("mensaje", "Los datos enviados no son válidos"));
    }
}
