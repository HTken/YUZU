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

/**
 * Un cambio en el stock de un insumo. Se llama MovimientoInsumo porque la caja ya tiene una entidad
 * Movimiento y JPA no admite dos con el mismo nombre.
 */
@Entity
@Table(name = "inventario_movimientos")
@Getter
@Setter
public class MovimientoInsumo {

    public static final String ENTRADA = "ENTRADA";
    public static final String SALIDA = "SALIDA";
    public static final String MERMA = "MERMA";
    public static final String AJUSTE = "AJUSTE";

    /** Lo descuenta el sistema solo, al cobrar una cuenta: nadie lo puede registrar a mano. */
    public static final String VENTA = "VENTA";

    /**
     * Lo devuelve el sistema solo, cuando se anula una venta cobrada y se elige devolver los insumos al stock.
     * Suma lo que esa venta había descontado. Nadie lo puede registrar a mano.
     */
    public static final String DEVOLUCION = "DEVOLUCION";

    /** Los tipos que una persona puede registrar a mano. */
    public static final List<String> TIPOS = List.of(ENTRADA, SALIDA, MERMA, AJUSTE);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "restaurante_id", nullable = false)
    private Long restauranteId;

    @Column(name = "insumo_id", nullable = false)
    private Long insumoId;

    @Column(nullable = false, length = 10)
    private String tipo;

    /** Con signo: positiva suma al stock y negativa resta. */
    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal cantidad;

    @Column(name = "stock_resultante", nullable = false, precision = 12, scale = 3)
    private BigDecimal stockResultante;

    @Column(name = "costo_total", precision = 10, scale = 2)
    private BigDecimal costoTotal;

    @Column(length = 150)
    private String nota;

    /** Solo en las ventas y sus devoluciones: la cuenta de donde salió el descuento. */
    @Column(name = "pedido_id")
    private Long pedidoId;

    /** Solo en las ventas: lo que faltó porque el stock registrado no alcanzaba. */
    @Column(precision = 12, scale = 3)
    private BigDecimal faltante;

    @Column(name = "registrado_por", nullable = false, length = 150)
    private String registradoPor;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;
}
