-- Personas del equipo de Yuzu que pueden entrar a la consola de plataforma.
CREATE TABLE administradores_plataforma (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario VARCHAR(60) NOT NULL,
    nombre VARCHAR(150) NOT NULL,
    clave_hash VARCHAR(100) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE KEY uk_admin_plataforma_usuario (usuario)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
