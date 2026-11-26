package com.yuzu.backend.caja;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MovimientoRepository extends JpaRepository<Movimiento, Long> {

    List<Movimiento> findByRestauranteIdAndTurnoIdOrderByIdDesc(Long restauranteId, Long turnoId);
}
