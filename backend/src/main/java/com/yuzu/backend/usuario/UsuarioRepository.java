package com.yuzu.backend.usuario;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByRestauranteIdAndUsuario(Long restauranteId, String usuario);

    List<Usuario> findByRestauranteIdOrderByNombreAsc(Long restauranteId);

    Optional<Usuario> findByIdAndRestauranteId(Long id, Long restauranteId);

    boolean existsByRestauranteIdAndUsuario(Long restauranteId, String usuario);

    long countByRestauranteIdAndActivoTrue(Long restauranteId);
}
