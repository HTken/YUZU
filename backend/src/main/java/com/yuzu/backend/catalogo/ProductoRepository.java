package com.yuzu.backend.catalogo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/** Todos los métodos reciben el restaurante: nunca se busca un producto "a secas". */
public interface ProductoRepository extends JpaRepository<Producto, Long> {

    List<Producto> findByRestauranteIdOrderByNombreAsc(Long restauranteId);

    Optional<Producto> findByIdAndRestauranteId(Long id, Long restauranteId);

    boolean existsByRestauranteIdAndNombreIgnoreCase(Long restauranteId, String nombre);

    boolean existsByRestauranteIdAndNombreIgnoreCaseAndIdNot(Long restauranteId, String nombre, Long id);

    long countByRestauranteIdAndActivoTrue(Long restauranteId);

    long countByRestauranteIdAndCategoriaIdAndActivoTrue(Long restauranteId, Long categoriaId);
}
