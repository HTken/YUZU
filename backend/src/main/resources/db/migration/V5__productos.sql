-- Carta del restaurante: categorías y productos.
-- Cada fila lleva restaurante_id. Además, la clave foránea compuesta impide que un producto
-- apunte a una categoría de OTRO restaurante, aunque algún día se cometiera un error en el código.

ALTER TABLE planes
    ADD COLUMN max_productos INT NOT NULL DEFAULT 50;

UPDATE planes SET max_productos = 40  WHERE codigo = 'ESENCIAL';
UPDATE planes SET max_productos = 150 WHERE codigo = 'ESTANDAR';
UPDATE planes SET max_productos = 600 WHERE codigo = 'PREMIUM';

CREATE TABLE categorias (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    restaurante_id BIGINT NOT NULL,
    nombre VARCHAR(80) NOT NULL,
    orden INT NOT NULL DEFAULT 0,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE KEY uk_categorias_restaurante_nombre (restaurante_id, nombre),
    UNIQUE KEY uk_categorias_restaurante_id (restaurante_id, id),
    CONSTRAINT fk_categorias_restaurante FOREIGN KEY (restaurante_id) REFERENCES restaurantes (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE productos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    restaurante_id BIGINT NOT NULL,
    categoria_id BIGINT NOT NULL,
    nombre VARCHAR(120) NOT NULL,
    descripcion VARCHAR(300) NULL,
    precio DECIMAL(10,2) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE KEY uk_productos_restaurante_nombre (restaurante_id, nombre),
    KEY ix_productos_categoria (restaurante_id, categoria_id),
    CONSTRAINT fk_productos_restaurante FOREIGN KEY (restaurante_id) REFERENCES restaurantes (id),
    CONSTRAINT fk_productos_categoria FOREIGN KEY (restaurante_id, categoria_id)
        REFERENCES categorias (restaurante_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
