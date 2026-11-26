-- Pedidos de salón: una cuenta abierta por mesa, con los productos que se van pidiendo.
-- Igual que antes, cada fila lleva restaurante_id y las claves foráneas compuestas impiden
-- mezclar datos de dos restaurantes aunque algún día hubiera un error en el código.

-- Claves únicas (restaurante_id, id) para poder apuntar a mesas y productos con clave compuesta.
ALTER TABLE mesas ADD UNIQUE KEY uk_mesas_restaurante_id (restaurante_id, id);
ALTER TABLE productos ADD UNIQUE KEY uk_productos_restaurante_id (restaurante_id, id);

CREATE TABLE pedidos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    restaurante_id BIGINT NOT NULL,
    mesa_id BIGINT NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'ABIERTO',
    mozo VARCHAR(150) NOT NULL,
    abierto_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    cerrado_en DATETIME(6) NULL,
    -- Vale el id de la mesa mientras el pedido está abierto y NULL cuando ya no.
    -- Con la clave única de abajo, la base de datos no deja tener dos cuentas abiertas en la misma mesa.
    mesa_abierta_id BIGINT GENERATED ALWAYS AS (CASE WHEN estado = 'ABIERTO' THEN mesa_id ELSE NULL END) STORED,
    UNIQUE KEY uk_pedidos_restaurante_id (restaurante_id, id),
    UNIQUE KEY uk_pedidos_mesa_abierta (restaurante_id, mesa_abierta_id),
    KEY ix_pedidos_estado (restaurante_id, estado),
    CONSTRAINT fk_pedidos_restaurante FOREIGN KEY (restaurante_id) REFERENCES restaurantes (id),
    CONSTRAINT fk_pedidos_mesa FOREIGN KEY (restaurante_id, mesa_id) REFERENCES mesas (restaurante_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE pedido_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    restaurante_id BIGINT NOT NULL,
    pedido_id BIGINT NOT NULL,
    producto_id BIGINT NOT NULL,
    -- Copia del nombre y del precio al momento de pedir: si luego cambias la carta, las cuentas no cambian.
    nombre VARCHAR(120) NOT NULL,
    precio_unitario DECIMAL(10,2) NOT NULL,
    cantidad INT NOT NULL,
    nota VARCHAR(150) NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    enviado_en DATETIME(6) NULL,
    creado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    KEY ix_pedido_items_pedido (restaurante_id, pedido_id),
    CONSTRAINT fk_pedido_items_restaurante FOREIGN KEY (restaurante_id) REFERENCES restaurantes (id),
    CONSTRAINT fk_pedido_items_pedido FOREIGN KEY (restaurante_id, pedido_id)
        REFERENCES pedidos (restaurante_id, id),
    CONSTRAINT fk_pedido_items_producto FOREIGN KEY (restaurante_id, producto_id)
        REFERENCES productos (restaurante_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
