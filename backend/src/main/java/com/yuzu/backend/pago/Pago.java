package com.yuzu.backend.pago;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "pagos")
@Getter
@Setter
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "restaurante_id", nullable = false)
    private Long restauranteId;

    @Column(name = "plan_id", nullable = false)
    private Long planId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    @Column(nullable = false)
    private int meses;

    @Column(nullable = false, length = 30)
    private String metodo;

    @Column(length = 100)
    private String referencia;

    @Column(name = "registrado_por", nullable = false, length = 60)
    private String registradoPor;

    @Column(name = "vence_antes", nullable = false)
    private LocalDate venceAntes;

    @Column(name = "vence_despues", nullable = false)
    private LocalDate venceDespues;

    @Column(name = "creado_en", insertable = false, updatable = false)
    private LocalDateTime creadoEn;
}
