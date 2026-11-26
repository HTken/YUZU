package com.yuzu.backend.caja;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

/** Todos los métodos reciben el restaurante: nunca se busca un turno "a secas". */
public interface TurnoRepository extends JpaRepository<Turno, Long> {

    Optional<Turno> findByRestauranteIdAndEstado(Long restauranteId, String estado);

    Optional<Turno> findByIdAndRestauranteId(Long id, Long restauranteId);

    List<Turno> findTop15ByRestauranteIdAndEstadoOrderByIdDesc(Long restauranteId, String estado);

    /**
     * La caja abierta, bloqueada hasta que termine la operación. Así dos cobros, o un cobro y el cierre
     * de caja, no se pisan: uno espera al otro. Debe ser lo PRIMERO que se lee en la operación.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Turno t where t.restauranteId = :restauranteId and t.estado = 'ABIERTA'")
    Optional<Turno> bloquearAbierto(@Param("restauranteId") Long restauranteId);
}
