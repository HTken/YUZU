package com.yuzu.backend.mesa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/** Todos los métodos reciben el restaurante: nunca se busca un salón "a secas". */
public interface SalonRepository extends JpaRepository<Salon, Long> {

    List<Salon> findByRestauranteIdOrderByOrdenAscNombreAsc(Long restauranteId);

    Optional<Salon> findByIdAndRestauranteId(Long id, Long restauranteId);

    boolean existsByRestauranteIdAndNombreIgnoreCase(Long restauranteId, String nombre);

    boolean existsByRestauranteIdAndNombreIgnoreCaseAndIdNot(Long restauranteId, String nombre, Long id);
}
