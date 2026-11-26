package com.yuzu.backend.negocio;

import org.springframework.data.jpa.repository.JpaRepository;

/** La clave primaria es el restaurante: findById(restauranteId) ya es "los datos de ese restaurante y de nadie más". */
public interface DatosNegocioRepository extends JpaRepository<DatosNegocio, Long> {
}
