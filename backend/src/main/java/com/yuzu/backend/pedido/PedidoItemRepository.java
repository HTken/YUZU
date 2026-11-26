package com.yuzu.backend.pedido;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

/** Todos los métodos reciben el restaurante: nunca se busca un producto pedido "a secas". */
public interface PedidoItemRepository extends JpaRepository<PedidoItem, Long> {

    List<PedidoItem> findByRestauranteIdAndPedidoIdOrderByIdAsc(Long restauranteId, Long pedidoId);

    List<PedidoItem> findByRestauranteIdAndPedidoIdIn(Long restauranteId, Collection<Long> pedidoIds);

    Optional<PedidoItem> findByIdAndPedidoIdAndRestauranteId(Long id, Long pedidoId, Long restauranteId);

    /** Los platos de cocina en un estado, de los más antiguos a los más nuevos. No incluye cuentas anuladas. */
    @Query("""
            select i from PedidoItem i
            where i.restauranteId = :restauranteId
              and i.estado = :estado
              and i.pasaPorCocina = true
              and exists (select 1 from Pedido p
                          where p.id = i.pedidoId and p.restauranteId = :restauranteId and p.estado <> 'ANULADO')
            order by i.enviadoEn asc, i.id asc
            """)
    List<PedidoItem> deCocina(@Param("restauranteId") Long restauranteId, @Param("estado") String estado);

    /** Los últimos platos que cocina marcó como listos (desde cierta hora) y que todavía no se sirvieron. */
    @Query("""
            select i from PedidoItem i
            where i.restauranteId = :restauranteId
              and i.estado = 'LISTO'
              and i.pasaPorCocina = true
              and i.listoEn >= :desde
              and exists (select 1 from Pedido p
                          where p.id = i.pedidoId and p.restauranteId = :restauranteId and p.estado <> 'ANULADO')
            order by i.listoEn desc, i.id desc
            """)
    List<PedidoItem> listosDeCocina(@Param("restauranteId") Long restauranteId,
                                    @Param("desde") LocalDateTime desde,
                                    Pageable pagina);

    /** Los platos anulados que cocina todavía no confirmó haber visto. */
    @Query("""
            select i from PedidoItem i
            where i.restauranteId = :restauranteId
              and i.estado = 'ANULADO'
              and i.avisoCocina = true
            order by i.anuladoEn asc, i.id asc
            """)
    List<PedidoItem> anuladosSinVer(@Param("restauranteId") Long restauranteId);

    /**
     * Los platos indicados, bloqueados hasta que termine la operación, para que cocina y el administrador
     * (por ejemplo, al anular un plato) no se pisen. Debe ser lo PRIMERO que se lee en la operación.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from PedidoItem i where i.restauranteId = :restauranteId and i.id in :ids order by i.id")
    List<PedidoItem> bloquearPorIds(@Param("restauranteId") Long restauranteId, @Param("ids") Collection<Long> ids);
}
