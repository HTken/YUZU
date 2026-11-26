package com.yuzu.backend.mesa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/** Todos los métodos reciben el restaurante: nunca se busca una mesa "a secas". */
public interface MesaRepository extends JpaRepository<Mesa, Long> {

    List<Mesa> findByRestauranteId(Long restauranteId);

    Optional<Mesa> findByIdAndRestauranteId(Long id, Long restauranteId);

    boolean existsByRestauranteIdAndSalonIdAndNombreIgnoreCase(Long restauranteId, Long salonId, String nombre);

    boolean existsByRestauranteIdAndSalonIdAndNombreIgnoreCaseAndIdNot(
            Long restauranteId, Long salonId, String nombre, Long id);

    long countByRestauranteIdAndActivoTrue(Long restauranteId);

    long countByRestauranteIdAndSalonIdAndActivoTrue(Long restauranteId, Long salonId);
}
