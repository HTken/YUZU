-- Salones y mesas del local. Igual que la carta: cada fila lleva restaurante_id y la clave foránea
-- compuesta impide que una mesa quede en el salón de OTRO restaurante.

ALTER TABLE planes
    ADD COLUMN max_mesas INT NOT NULL DEFAULT 10;

UPDATE planes SET max_mesas = 10  WHERE codigo = 'ESENCIAL';
UPDATE planes SET max_mesas = 30  WHERE codigo = 'ESTANDAR';
UPDATE planes SET max_mesas = 100 WHERE codigo = 'PREMIUM';

CREATE TABLE salones (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    restaurante_id BIGINT NOT NULL,
    nombre VARCHAR(60) NOT NULL,
    orden INT NOT NULL DEFAULT 0,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE KEY uk_salones_restaurante_nombre (restaurante_id, nombre),
    UNIQUE KEY uk_salones_restaurante_id (restaurante_id, id),
    CONSTRAINT fk_salones_restaurante FOREIGN KEY (restaurante_id) REFERENCES restaurantes (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE mesas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    restaurante_id BIGINT NOT NULL,
    salon_id BIGINT NOT NULL,
    nombre VARCHAR(40) NOT NULL,
    capacidad INT NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE KEY uk_mesas_salon_nombre (restaurante_id, salon_id, nombre),
    CONSTRAINT fk_mesas_restaurante FOREIGN KEY (restaurante_id) REFERENCES restaurantes (id),
    CONSTRAINT fk_mesas_salon FOREIGN KEY (restaurante_id, salon_id)
        REFERENCES salones (restaurante_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
