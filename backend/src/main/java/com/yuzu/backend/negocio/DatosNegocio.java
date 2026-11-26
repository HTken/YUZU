package com.yuzu.backend.negocio;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Datos del negocio que salen en el ticket. La clave es el propio restaurante: no hay forma de leer ni
 * guardar los datos de otro, porque el id siempre sale del token.
 */
@Entity
@Table(name = "negocio_datos")
@Getter
@Setter
public class DatosNegocio {

    @Id
    @Column(name = "restaurante_id")
    private Long restauranteId;

    @Column(name = "razon_social", length = 150)
    private String razonSocial;

    @Column(length = 11)
    private String ruc;

    @Column(length = 200)
    private String direccion;

    @Column(length = 30)
    private String telefono;

    @Column(name = "mensaje_ticket", length = 120)
    private String mensajeTicket;

    @Column(name = "ancho_ticket_mm", nullable = false)
    private int anchoTicketMm = 80;

    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn;
}
