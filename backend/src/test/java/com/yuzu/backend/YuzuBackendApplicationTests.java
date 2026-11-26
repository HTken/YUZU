package com.yuzu.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Prueba de humo: que el contexto de Spring arranca completo (todos los beans, la configuración de
 * seguridad, las migraciones de Flyway). Usa un MySQL real de prueba (Testcontainers) y una clave de
 * JWT de prueba, para no depender del MySQL ni de application-local.properties de cada máquina.
 */
@SpringBootTest
@Testcontainers
@TestPropertySource(properties = "yuzu.jwt.secret=clave-de-pruebas-automatizadas-no-es-la-real-0123456789")
class YuzuBackendApplicationTests {

	@Container
	@ServiceConnection
	static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

	@Test
	void contextLoads() {
	}

}
