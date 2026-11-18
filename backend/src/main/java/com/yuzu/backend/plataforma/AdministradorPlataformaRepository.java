package com.yuzu.backend.plataforma;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AdministradorPlataformaRepository extends JpaRepository<AdministradorPlataforma, Long> {

    Optional<AdministradorPlataforma> findByUsuario(String usuario);
}
