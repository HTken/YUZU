package com.yuzu.backend.pago;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** La tarjeta guardada y la suscripción de Culqi que cobra el plan solo, cada mes. */
@Entity
@Table(name = "culqi_suscripciones")
@Getter
@Setter
public class CulqiSuscripcion {

    @Id
    @Column(name = "restaurante_id")
    private Long restauranteId;

    @Column(name = "culqi_customer_id", nullable = false, length = 40)
    private String culqiCustomerId;

    @Column(name = "culqi_card_id", nullable = false, length = 40)
    private String culqiCardId;

    @Column(name = "marca_tarjeta", length = 30)
    private String marcaTarjeta;

    @Column(name = "ultimos_cuatro", length = 4)
    private String ultimosCuatro;

    @Column(name = "culqi_plan_id", nullable = false, length = 40)
    private String culqiPlanId;

    @Column(name = "culqi_suscripcion_id", nullable = false, length = 40)
    private String culqiSuscripcionId;

    @Column(nullable = false)
    private boolean activa = true;

    @Column(name = "creado_en", insertable = false, updatable = false)
    private LocalDateTime creadoEn;

    @Column(name = "actualizado_en", insertable = false, updatable = false)
    private LocalDateTime actualizadoEn;
}
