package com.yuzu.backend.pago;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CulqiSuscripcionRepository extends JpaRepository<CulqiSuscripcion, Long> {

    Optional<CulqiSuscripcion> findByCulqiSuscripcionId(String culqiSuscripcionId);
}
