-- Historial de pagos de suscripción. Hoy se registran a mano desde la consola;
-- más adelante los registrará el cobro automático con la misma estructura.
CREATE TABLE pagos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    restaurante_id BIGINT NOT NULL,
    plan_id BIGINT NOT NULL,
    monto DECIMAL(10,2) NOT NULL,
    meses INT NOT NULL,
    metodo VARCHAR(30) NOT NULL,
    referencia VARCHAR(100) NULL,
    registrado_por VARCHAR(60) NOT NULL,
    vence_antes DATE NOT NULL,
    vence_despues DATE NOT NULL,
    creado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE KEY uk_pagos_metodo_referencia (metodo, referencia),
    KEY ix_pagos_restaurante (restaurante_id, creado_en),
    CONSTRAINT fk_pagos_restaurante FOREIGN KEY (restaurante_id) REFERENCES restaurantes (id),
    CONSTRAINT fk_pagos_plan FOREIGN KEY (plan_id) REFERENCES planes (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
