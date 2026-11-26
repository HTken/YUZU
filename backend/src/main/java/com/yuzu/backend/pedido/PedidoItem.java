package com.yuzu.backend.pedido;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.DynamicUpdate;
import lombok.Getter;
import lombok.Setter;

/**
 * DynamicUpdate: al guardar solo se actualiza lo que cambió. Así, cuando cocina marca un plato como listo
 * y el mozo lo marca como servido casi a la vez, uno no borra por accidente lo que escribió el otro.
 */
@Entity
@Table(name = "pedido_items")
@DynamicUpdate
@Getter
@Setter
public class PedidoItem {

    public static final String PENDIENTE = "PENDIENTE";
    public static final String ENVIADO = "ENVIADO";
    public static final String LISTO = "LISTO";
    public static final String SERVIDO = "SERVIDO";
    public static final String ANULADO = "ANULADO";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "restaurante_id", nullable = false)
    private Long restauranteId;

    @Column(name = "pedido_id", nullable = false)
    private Long pedidoId;

    @Column(name = "producto_id", nullable = false)
    private Long productoId;

    /** Copia del nombre del producto al momento de pedir. */
    @Column(nullable = false, length = 120)
    private String nombre;

    /** Copia del precio al momento de pedir. */
    @Column(name = "precio_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioUnitario;

    @Column(nullable = false)
    private int cantidad;

    @Column(length = 150)
    private String nota;

    /** PENDIENTE (aún en la mesa), ENVIADO (en cocina), LISTO (listo para servir), SERVIDO o ANULADO. */
    @Column(nullable = false, length = 20)
    private String estado = "PENDIENTE";

    @Column(name = "enviado_en")
    private LocalDateTime enviadoEn;

    /** false si la categoría del producto no pasa por cocina (por ejemplo, las bebidas). */
    @Column(name = "pasa_por_cocina", nullable = false)
    private boolean pasaPorCocina = true;

    @Column(name = "listo_en")
    private LocalDateTime listoEn;

    @Column(name = "servido_en")
    private LocalDateTime servidoEn;

    @Column(name = "anulado_en")
    private LocalDateTime anuladoEn;

    /** true si se anuló un plato que cocina tenía en pantalla y todavía no vio el aviso. */
    @Column(name = "aviso_cocina", nullable = false)
    private boolean avisoCocina = false;
}
