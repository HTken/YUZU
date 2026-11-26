package com.yuzu.backend.inventario;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Algo que el restaurante compra y gasta: arroz, pollo, una gaseosa… Su stock solo cambia con movimientos. */
@Entity
@Table(name = "insumos")
@Getter
@Setter
public class Insumo {

    public static final String KG = "KG";
    public static final String G = "G";
    public static final String L = "L";
    public static final String ML = "ML";
    public static final String UNIDAD = "UNIDAD";

    /** Unidades aceptadas, en el orden en que se muestran. */
    public static final List<String> UNIDADES = List.of(KG, G, L, ML, UNIDAD);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "restaurante_id", nullable = false)
    private Long restauranteId;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(nullable = false, length = 10)
    private String unidad;

    @Column(name = "stock_actual", nullable = false, precision = 12, scale = 3)
    private BigDecimal stockActual = BigDecimal.ZERO;

    @Column(name = "stock_minimo", nullable = false, precision = 12, scale = 3)
    private BigDecimal stockMinimo = BigDecimal.ZERO;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "creado_en", insertable = false, updatable = false)
    private LocalDateTime creadoEn;
}
