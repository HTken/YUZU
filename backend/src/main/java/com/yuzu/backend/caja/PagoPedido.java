package com.yuzu.backend.caja;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Una parte del pago de una cuenta: cuánto se pagó y con qué método. */
@Entity
@Table(name = "pedido_pagos")
@Getter
@Setter
public class PagoPedido {

    public static final String EFECTIVO = "EFECTIVO";
    public static final String YAPE = "YAPE";
    public static final String PLIN = "PLIN";
    public static final String TARJETA = "TARJETA";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "restaurante_id", nullable = false)
    private Long restauranteId;

    @Column(name = "pedido_id", nullable = false)
    private Long pedidoId;

    @Column(nullable = false, length = 20)
    private String metodo;

    /** Lo que se aplicó a la cuenta (el vuelto no se guarda: no es parte de la venta). */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;
}
