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

/** Un turno de caja: desde que se abre con un monto inicial hasta que se cierra y se cuenta el efectivo. */
@Entity
@Table(name = "caja_turnos")
@Getter
@Setter
public class Turno {

    public static final String ABIERTA = "ABIERTA";
    public static final String CERRADA = "CERRADA";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "restaurante_id", nullable = false)
    private Long restauranteId;

    @Column(nullable = false, length = 20)
    private String estado = ABIERTA;

    @Column(name = "abierta_por", nullable = false, length = 150)
    private String abiertaPor;

    @Column(name = "abierta_en", nullable = false)
    private LocalDateTime abiertaEn;

    @Column(name = "monto_inicial", nullable = false, precision = 10, scale = 2)
    private BigDecimal montoInicial = BigDecimal.ZERO;

    @Column(name = "cerrada_por", length = 150)
    private String cerradaPor;

    @Column(name = "cerrada_en")
    private LocalDateTime cerradaEn;

    @Column(name = "total_ventas", precision = 10, scale = 2)
    private BigDecimal totalVentas;

    @Column(name = "num_ventas")
    private Integer numVentas;

    @Column(name = "efectivo_esperado", precision = 10, scale = 2)
    private BigDecimal efectivoEsperado;

    @Column(name = "efectivo_contado", precision = 10, scale = 2)
    private BigDecimal efectivoContado;

    @Column(precision = 10, scale = 2)
    private BigDecimal diferencia;

    @Column(name = "nota_cierre", length = 200)
    private String notaCierre;
}
