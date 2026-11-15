package com.yuzu.backend.restaurante;

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
@Table(name = "restaurantes")
@Getter
@Setter
public class Restaurante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(nullable = false, length = 63)
    private String subdominio;

    /** ACTIVO, EN_GRACIA o SUSPENDIDO. */
    @Column(nullable = false, length = 20)
    private String estado = "ACTIVO";

    @Column(name = "plan_id", nullable = false)
    private Long planId;

    @Column(name = "vence_en", nullable = false)
    private LocalDate venceEn;

    @Column(name = "creado_en", insertable = false, updatable = false)
    private LocalDateTime creadoEn;
}
