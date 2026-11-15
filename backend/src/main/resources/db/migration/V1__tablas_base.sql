CREATE TABLE restaurantes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    subdominio VARCHAR(63) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    creado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE KEY uk_restaurantes_subdominio (subdominio)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE usuarios (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    restaurante_id BIGINT NOT NULL,
    usuario VARCHAR(60) NOT NULL,
    nombre VARCHAR(150) NOT NULL,
    email VARCHAR(150),
    clave_hash VARCHAR(100) NOT NULL,
    rol VARCHAR(20) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE KEY uk_usuarios_restaurante_usuario (restaurante_id, usuario),
    CONSTRAINT fk_usuarios_restaurante FOREIGN KEY (restaurante_id) REFERENCES restaurantes (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
