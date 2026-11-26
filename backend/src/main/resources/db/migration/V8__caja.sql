-- Caja: turnos de caja, ingresos/egresos y cobro de las cuentas.
-- Igual que antes, cada fila lleva restaurante_id y las claves foráneas compuestas impiden
-- mezclar datos de dos restaurantes aunque algún día hubiera un error en el código.

CREATE TABLE caja_turnos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    restaurante_id BIGINT NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'ABIERTA',
    abierta_por VARCHAR(150) NOT NULL,
    abierta_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    monto_inicial DECIMAL(10,2) NOT NULL DEFAULT 0,
    cerrada_por VARCHAR(150) NULL,
    cerrada_en DATETIME(6) NULL,
    -- Se llenan al cerrar la caja y quedan guardados como constancia.
    total_ventas DECIMAL(10,2) NULL,
    num_ventas INT NULL,
    efectivo_esperado DECIMAL(10,2) NULL,
    efectivo_contado DECIMAL(10,2) NULL,
    diferencia DECIMAL(10,2) NULL,
    nota_cierre VARCHAR(200) NULL,
    -- Vale el id del restaurante mientras la caja está abierta y NULL cuando ya no.
    -- Con la clave única de abajo, la base de datos no deja tener dos cajas abiertas en el mismo restaurante.
    restaurante_abierta_id BIGINT GENERATED ALWAYS AS (CASE WHEN estado = 'ABIERTA' THEN restaurante_id ELSE NULL END) STORED,
    UNIQUE KEY uk_caja_turnos_restaurante_id (restaurante_id, id),
    UNIQUE KEY uk_caja_turnos_abierta (restaurante_abierta_id),
    KEY ix_caja_turnos_estado (restaurante_id, estado),
    CONSTRAINT fk_caja_turnos_restaurante FOREIGN KEY (restaurante_id) REFERENCES restaurantes (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Entradas y salidas de efectivo que no son ventas (por ejemplo: compra de hielo, retiro de dinero).
CREATE TABLE caja_movimientos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    restaurante_id BIGINT NOT NULL,
    turno_id BIGINT NOT NULL,
    tipo VARCHAR(10) NOT NULL,
    concepto VARCHAR(120) NOT NULL,
    monto DECIMAL(10,2) NOT NULL,
    registrado_por VARCHAR(150) NOT NULL,
    creado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    KEY ix_caja_movimientos_turno (restaurante_id, turno_id),
    CONSTRAINT fk_caja_movimientos_restaurante FOREIGN KEY (restaurante_id) REFERENCES restaurantes (id),
    CONSTRAINT fk_caja_movimientos_turno FOREIGN KEY (restaurante_id, turno_id)
        REFERENCES caja_turnos (restaurante_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Datos del cobro dentro del pedido. Al pasar a PAGADO la mesa queda libre sola
-- (mesa_abierta_id solo vale mientras el pedido está ABIERTO).
ALTER TABLE pedidos
    ADD COLUMN turno_id BIGINT NULL,
    ADD COLUMN numero_ticket INT NULL,
    ADD COLUMN total_cobrado DECIMAL(10,2) NULL,
    ADD COLUMN cobrado_por VARCHAR(150) NULL,
    ADD UNIQUE KEY uk_pedidos_ticket (restaurante_id, numero_ticket),
    ADD KEY ix_pedidos_turno (restaurante_id, turno_id),
    ADD CONSTRAINT fk_pedidos_turno FOREIGN KEY (restaurante_id, turno_id)
        REFERENCES caja_turnos (restaurante_id, id);

-- Cómo se pagó cada cuenta (puede ser en varios métodos a la vez).
-- Se llama pedido_pagos para no confundirse con la tabla "pagos" de la suscripción del restaurante.
CREATE TABLE pedido_pagos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    restaurante_id BIGINT NOT NULL,
    pedido_id BIGINT NOT NULL,
    metodo VARCHAR(20) NOT NULL,
    monto DECIMAL(10,2) NOT NULL,
    creado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    KEY ix_pedido_pagos_pedido (restaurante_id, pedido_id),
    CONSTRAINT fk_pedido_pagos_restaurante FOREIGN KEY (restaurante_id) REFERENCES restaurantes (id),
    CONSTRAINT fk_pedido_pagos_pedido FOREIGN KEY (restaurante_id, pedido_id)
        REFERENCES pedidos (restaurante_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
