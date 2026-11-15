package com.yuzu.backend.plan;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "planes")
@Getter
@Setter
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String codigo;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(name = "precio_mensual", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioMensual;

    @Column(name = "max_usuarios", nullable = false)
    private int maxUsuarios;

    @Column(nullable = false)
    private boolean activo = true;
}
