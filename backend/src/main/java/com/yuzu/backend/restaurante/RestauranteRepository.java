package com.yuzu.backend.restaurante;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RestauranteRepository extends JpaRepository<Restaurante, Long> {

    Optional<Restaurante> findBySubdominio(String subdominio);

    List<Restaurante> findByEstadoAndVenceEnBefore(String estado, LocalDate fecha);

    /** Para el aviso de "tu plan vence pronto": los que vencen justo ese día. */
    List<Restaurante> findByEstadoAndVenceEn(String estado, LocalDate fecha);
}
