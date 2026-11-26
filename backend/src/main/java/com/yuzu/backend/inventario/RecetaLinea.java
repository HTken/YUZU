package com.yuzu.backend.inventario;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Una línea de la receta de un producto: cuánto de un insumo gasta UNA unidad de ese producto. */
@Entity
@Table(name = "producto_insumos")
@Getter
@Setter
public class RecetaLinea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "restaurante_id", nullable = false)
    private Long restauranteId;

    @Column(name = "producto_id", nullable = false)
    private Long productoId;

    @Column(name = "insumo_id", nullable = false)
    private Long insumoId;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal cantidad;
}
