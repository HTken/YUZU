package com.yuzu.backend.restaurante;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RestauranteRepository extends JpaRepository<Restaurante, Long> {

    Optional<Restaurante> findBySubdominio(String subdominio);
}