package com.yuzu.backend.pedido;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

/** Todos los métodos reciben el restaurante: nunca se busca un pedido "a secas". */
public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    Optional<Pedido> findByIdAndRestauranteId(Long id, Long restauranteId);

    Optional<Pedido> findByRestauranteIdAndMesaIdAndEstado(Long restauranteId, Long mesaId, String estado);

    List<Pedido> findByRestauranteIdAndEstado(Long restauranteId, String estado);

    boolean existsByRestauranteIdAndMesaIdAndEstado(Long restauranteId, Long mesaId, String estado);

    long countByRestauranteIdAndEstado(Long restauranteId, String estado);

    List<Pedido> findByRestauranteIdAndIdIn(Long restauranteId, Collection<Long> ids);

    /**
     * Las cuentas de un turno de caja que están en un estado (PAGADO: las que suman; ANULADO: las cobradas y luego
     * anuladas), en el orden en que se cobraron. Una cuenta anulada sin cobrarse no tiene turno.
     */
    List<Pedido> findByRestauranteIdAndTurnoIdAndEstadoOrderByNumeroTicketAsc(
            Long restauranteId, Long turnoId, String estado);

    /** Último número de ticket usado por el restaurante (0 si todavía no hay ninguno). */
    @Query("select coalesce(max(p.numeroTicket), 0) from Pedido p where p.restauranteId = :restauranteId")
    int ultimoTicket(@Param("restauranteId") Long restauranteId);

    /**
     * La cuenta, bloqueada hasta que termine la operación. Se usa para cobrar y para modificar la cuenta,
     * de modo que nadie agregue un producto mientras se cobra, ni se cobre dos veces.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pedido p where p.id = :id and p.restauranteId = :restauranteId")
    Optional<Pedido> bloquearPorId(@Param("id") Long id, @Param("restauranteId") Long restauranteId);
}
