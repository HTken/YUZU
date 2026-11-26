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

    @Column(name = "max_productos", nullable = false)
    private int maxProductos;

    @Column(name = "max_mesas", nullable = false)
    private int maxMesas;

    @Column(name = "max_insumos", nullable = false)
    private int maxInsumos;

    @Column(nullable = false)
    private boolean activo = true;

    /** El plan recurrente en Culqi que cobra este plan cada mes. Se crea solo, la primera vez que hace falta. */
    @Column(name = "culqi_plan_id", length = 40)
    private String culqiPlanId;
}
