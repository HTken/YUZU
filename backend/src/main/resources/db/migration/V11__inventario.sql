-- Inventario: los insumos (lo que el restaurante compra: arroz, pollo, gaseosas…) con su stock,
-- y el historial de cada entrada, salida, merma o ajuste.
-- Igual que antes, cada fila lleva restaurante_id y las claves foráneas compuestas impiden
-- mezclar datos de dos restaurantes aunque algún día hubiera un error en el código.

ALTER TABLE planes
    ADD COLUMN max_insumos INT NOT NULL DEFAULT 30;

UPDATE planes SET max_insumos = 30  WHERE codigo = 'ESENCIAL';
UPDATE planes SET max_insumos = 100 WHERE codigo = 'ESTANDAR';
UPDATE planes SET max_insumos = 400 WHERE codigo = 'PREMIUM';

CREATE TABLE insumos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    restaurante_id BIGINT NOT NULL,
    nombre VARCHAR(80) NOT NULL,
    -- KG, G, L, ML o UNIDAD. El stock se guarda siempre en esta misma unidad.
    unidad VARCHAR(10) NOT NULL,
    stock_actual DECIMAL(12,3) NOT NULL DEFAULT 0,
    -- Por debajo (o igual) de este número se avisa "stock bajo". 0 = sin aviso.
    stock_minimo DECIMAL(12,3) NOT NULL DEFAULT 0,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE KEY uk_insumos_restaurante_id (restaurante_id, id),
    -- Con el orden de letras de MySQL, "Ají" y "aji" cuentan como el mismo nombre.
    UNIQUE KEY uk_insumos_nombre (restaurante_id, nombre),
    CONSTRAINT fk_insumos_restaurante FOREIGN KEY (restaurante_id) REFERENCES restaurantes (id),
    -- Última defensa: aunque el código fallara, la base no deja guardar un stock negativo.
    CONSTRAINT ck_insumos_stock CHECK (stock_actual >= 0),
    CONSTRAINT ck_insumos_minimo CHECK (stock_minimo >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Cada cambio de stock deja una fila: qué pasó, cuánto, cómo quedó el stock, quién y cuándo.
-- cantidad lleva signo: positiva suma al stock y negativa resta (en AJUSTE es la diferencia con lo que había).
CREATE TABLE inventario_movimientos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    restaurante_id BIGINT NOT NULL,
    insumo_id BIGINT NOT NULL,
    -- ENTRADA (compra), SALIDA (uso), MERMA (se malogró o se perdió) o AJUSTE (conteo físico).
    tipo VARCHAR(10) NOT NULL,
    cantidad DECIMAL(12,3) NOT NULL,
    stock_resultante DECIMAL(12,3) NOT NULL,
    -- Lo que costó la compra completa. Solo se llena en las entradas, y es opcional.
    costo_total DECIMAL(10,2) NULL,
    nota VARCHAR(150) NULL,
    registrado_por VARCHAR(150) NOT NULL,
    creado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    KEY ix_inventario_movimientos_insumo (restaurante_id, insumo_id, id),
    CONSTRAINT fk_inventario_movimientos_restaurante FOREIGN KEY (restaurante_id) REFERENCES restaurantes (id),
    CONSTRAINT fk_inventario_movimientos_insumo FOREIGN KEY (restaurante_id, insumo_id)
        REFERENCES insumos (restaurante_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
