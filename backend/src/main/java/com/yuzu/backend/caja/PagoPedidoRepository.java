package com.yuzu.backend.caja;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PagoPedidoRepository extends JpaRepository<PagoPedido, Long> {

    List<PagoPedido> findByRestauranteIdAndPedidoIdIn(Long restauranteId, Collection<Long> pedidoIds);
}
