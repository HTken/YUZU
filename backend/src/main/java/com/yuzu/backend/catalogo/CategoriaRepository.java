package com.yuzu.backend.catalogo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/** Todos los métodos reciben el restaurante: nunca se busca una categoría "a secas". */
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    List<Categoria> findByRestauranteIdOrderByOrdenAscNombreAsc(Long restauranteId);

    Optional<Categoria> findByIdAndRestauranteId(Long id, Long restauranteId);

    boolean existsByRestauranteIdAndNombreIgnoreCase(Long restauranteId, String nombre);

    boolean existsByRestauranteIdAndNombreIgnoreCaseAndIdNot(Long restauranteId, String nombre, Long id);
}
