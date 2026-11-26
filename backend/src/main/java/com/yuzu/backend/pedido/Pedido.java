package com.yuzu.backend.pedido;

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

/** Cuenta de una mesa. Las fechas se guardan en hora UTC y se envían al navegador con la "Z" al final. */
@Entity
@Table(name = "pedidos")
@Getter
@Setter
public class Pedido {

    public static final String ABIERTO = "ABIERTO";
    public static final String ANULADO = "ANULADO";
    public static final String PAGADO = "PAGADO";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "restaurante_id", nullable = false)
    private Long restauranteId;

    @Column(name = "mesa_id", nullable = false)
    private Long mesaId;

    /** ABIERTO, PAGADO o ANULADO. Solo la cuenta ABIERTA ocupa la mesa. */
    @Column(nullable = false, length = 20)
    private String estado = "ABIERTO";

    @Column(nullable = false, length = 150)
    private String mozo;

    @Column(name = "abierto_en", nullable = false)
    private LocalDateTime abiertoEn;

    @Column(name = "cerrado_en")
    private LocalDateTime cerradoEn;

    // Datos del cobro (solo las cuentas PAGADO los tienen).

    /** Turno de caja en el que se cobró. */
    @Column(name = "turno_id")
    private Long turnoId;

    /** Número correlativo del restaurante: 1, 2, 3... */
    @Column(name = "numero_ticket")
    private Integer numeroTicket;

    @Column(name = "total_cobrado", precision = 10, scale = 2)
    private BigDecimal totalCobrado;

    @Column(name = "cobrado_por", length = 150)
    private String cobradoPor;

    // Datos de la anulación de una venta ya cobrada (solo si pasó de PAGADO a ANULADO). La cuenta conserva su
    // ticket, su turno y su total; con ticket y estado ANULADO se sabe que se cobró y luego se anuló.

    @Column(name = "anulado_cobro_por", length = 150)
    private String anuladoCobroPor;

    @Column(name = "anulado_cobro_en")
    private LocalDateTime anuladoCobroEn;

    @Column(name = "motivo_anulacion", length = 200)
    private String motivoAnulacion;
}
