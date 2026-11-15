-- Planes de ejemplo: los nombres, precios y límites se cambiarán desde el panel de plataforma.
CREATE TABLE planes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    codigo VARCHAR(30) NOT NULL,
    nombre VARCHAR(80) NOT NULL,
    precio_mensual DECIMAL(10,2) NOT NULL,
    max_usuarios INT NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE KEY uk_planes_codigo (codigo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO planes (codigo, nombre, precio_mensual, max_usuarios) VALUES
    ('ESENCIAL', 'Esencial', 59.00, 3),
    ('ESTANDAR', 'Estándar', 129.00, 10),
    ('PREMIUM', 'Premium', 249.00, 30);

ALTER TABLE restaurantes
    ADD COLUMN plan_id BIGINT NULL,
    ADD COLUMN vence_en DATE NULL;

UPDATE restaurantes
SET plan_id = (SELECT id FROM planes WHERE codigo = 'ESTANDAR'),
    vence_en = DATE_ADD(CURRENT_DATE, INTERVAL 30 DAY);

ALTER TABLE restaurantes
    MODIFY COLUMN plan_id BIGINT NOT NULL,
    MODIFY COLUMN vence_en DATE NOT NULL,
    ADD CONSTRAINT fk_restaurantes_plan FOREIGN KEY (plan_id) REFERENCES planes (id);
