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

/** Entrada (INGRESO) o salida (EGRESO) de efectivo de la caja que no es una venta. */
@Entity
@Table(name = "caja_movimientos")
@Getter
@Setter
public class Movimiento {

    public static final String INGRESO = "INGRESO";
    public static final String EGRESO = "EGRESO";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "restaurante_id", nullable = false)
    private Long restauranteId;

    @Column(name = "turno_id", nullable = false)
    private Long turnoId;

    @Column(nullable = false, length = 10)
    private String tipo;

    @Column(nullable = false, length = 120)
    private String concepto;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    @Column(name = "registrado_por", nullable = false, length = 150)
    private String registradoPor;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;
}
