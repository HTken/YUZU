-- Cobro recurrente con Culqi: el plan interno se asocia a un Plan de Culqi (se crea solo, la primera vez
-- que se necesita) y cada restaurante puede guardar una tarjeta y una Suscripción de Culqi que Culqi cobra
-- solo cada mes. El resultado de cada cobro llega por webhook (ver CulqiWebhookController).

ALTER TABLE planes
    ADD COLUMN culqi_plan_id VARCHAR(40) NULL;

CREATE TABLE culqi_suscripciones (
    restaurante_id BIGINT NOT NULL PRIMARY KEY,
    culqi_customer_id VARCHAR(40) NOT NULL,
    culqi_card_id VARCHAR(40) NOT NULL,
    marca_tarjeta VARCHAR(30) NULL,
    ultimos_cuatro VARCHAR(4) NULL,
    culqi_plan_id VARCHAR(40) NOT NULL,
    culqi_suscripcion_id VARCHAR(40) NOT NULL,
    activa BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    actualizado_en DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_culqi_suscripciones_restaurante FOREIGN KEY (restaurante_id) REFERENCES restaurantes (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
