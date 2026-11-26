package com.yuzu.backend.inventario;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

/** Todos los métodos reciben el restaurante: nunca se busca un insumo "a secas". */
public interface InsumoRepository extends JpaRepository<Insumo, Long> {

    List<Insumo> findByRestauranteIdOrderByNombreAsc(Long restauranteId);

    Optional<Insumo> findByIdAndRestauranteId(Long id, Long restauranteId);

    List<Insumo> findByRestauranteIdAndIdIn(Long restauranteId, Collection<Long> ids);

    boolean existsByRestauranteIdAndNombreIgnoreCase(Long restauranteId, String nombre);

    boolean existsByRestauranteIdAndNombreIgnoreCaseAndIdNot(Long restauranteId, String nombre, Long id);

    long countByRestauranteIdAndActivoTrue(Long restauranteId);

    /**
     * El insumo bloqueado hasta que termine la operación. Así dos movimientos al mismo tiempo
     * (por ejemplo, dos salidas) no leen el mismo stock y se pisan: uno espera al otro.
     * Debe ser lo PRIMERO que se lee en la operación.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Insumo i where i.id = :id and i.restauranteId = :restauranteId")
    Optional<Insumo> bloquearPorId(@Param("id") Long id, @Param("restauranteId") Long restauranteId);

    /** Varios insumos bloqueados a la vez, siempre en el mismo orden (por id) para que nunca se trabe una operación con otra. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Insumo i where i.restauranteId = :restauranteId and i.id in :ids order by i.id")
    List<Insumo> bloquearPorIds(@Param("restauranteId") Long restauranteId, @Param("ids") Collection<Long> ids);
}
