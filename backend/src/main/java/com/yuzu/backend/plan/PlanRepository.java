package com.yuzu.backend.plan;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanRepository extends JpaRepository<Plan, Long> {

    Optional<Plan> findByCodigo(String codigo);
}
