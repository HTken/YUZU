-- Recetas: qué insumos gasta cada producto. Al cobrar una cuenta, el sistema descuenta del inventario
-- los insumos de lo que se vendió. Un producto sin receta simplemente no descuenta nada.

CREATE TABLE producto_insumos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    restaurante_id BIGINT NOT NULL,
    producto_id BIGINT NOT NULL,
    insumo_id BIGINT NOT NULL,
    -- Cuánto de este insumo gasta UNA unidad del producto, en la unidad del insumo.
    -- Ejemplo: un 1/4 de pollo gasta 0.25 del insumo "Pollo" (medido en unidades).
    cantidad DECIMAL(12,3) NOT NULL,
    -- Un insumo aparece una sola vez por producto.
    UNIQUE KEY uk_producto_insumos (restaurante_id, producto_id, insumo_id),
    KEY ix_producto_insumos_insumo (restaurante_id, insumo_id),
    CONSTRAINT fk_producto_insumos_restaurante FOREIGN KEY (restaurante_id) REFERENCES restaurantes (id),
    CONSTRAINT fk_producto_insumos_producto FOREIGN KEY (restaurante_id, producto_id)
        REFERENCES productos (restaurante_id, id),
    CONSTRAINT fk_producto_insumos_insumo FOREIGN KEY (restaurante_id, insumo_id)
        REFERENCES insumos (restaurante_id, id),
    CONSTRAINT ck_producto_insumos_cantidad CHECK (cantidad > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Los descuentos por venta son un tipo más de movimiento (VENTA) y guardan de qué cuenta vinieron.
-- faltante: lo que la venta necesitaba y el stock registrado no tenía. La venta se hace igual y el stock
-- queda en 0, pero queda anotado para saber que el inventario estaba desactualizado.
ALTER TABLE inventario_movimientos
    ADD COLUMN pedido_id BIGINT NULL,
    ADD COLUMN faltante DECIMAL(12,3) NULL;

ALTER TABLE inventario_movimientos
    ADD CONSTRAINT fk_inventario_movimientos_pedido FOREIGN KEY (restaurante_id, pedido_id)
        REFERENCES pedidos (restaurante_id, id);
