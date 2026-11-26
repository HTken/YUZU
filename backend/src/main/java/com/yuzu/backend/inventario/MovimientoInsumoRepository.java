package com.yuzu.backend.inventario;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MovimientoInsumoRepository extends JpaRepository<MovimientoInsumo, Long> {

    List<MovimientoInsumo> findTop30ByRestauranteIdAndInsumoIdOrderByIdDesc(Long restauranteId, Long insumoId);

    boolean existsByRestauranteIdAndInsumoId(Long restauranteId, Long insumoId);

    /** Los movimientos de un tipo que dejó una cuenta (por ejemplo, lo que descontó al cobrarse). */
    List<MovimientoInsumo> findByRestauranteIdAndPedidoIdAndTipo(Long restauranteId, Long pedidoId, String tipo);

    /** Los insumos que ya tienen historial: a esos ya no se les puede cambiar la unidad de medida. */
    @Query("select distinct m.insumoId from MovimientoInsumo m where m.restauranteId = :restauranteId")
    List<Long> insumosConMovimientos(@Param("restauranteId") Long restauranteId);
}
