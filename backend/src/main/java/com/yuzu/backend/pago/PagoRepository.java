package com.yuzu.backend.pago;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PagoRepository extends JpaRepository<Pago, Long> {

    boolean existsByMetodoAndReferencia(String metodo, String referencia);

    List<Pago> findByRestauranteIdOrderByCreadoEnDesc(Long restauranteId);
}
