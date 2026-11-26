-- Cocina: cada plato enviado pasa por ENVIADO (en cocina) -> LISTO (listo para servir) -> SERVIDO.
-- Las categorías pueden marcarse como "no pasan por cocina" (por ejemplo, las bebidas): esos productos
-- quedan LISTOS apenas se envía la cuenta, sin aparecer en la pantalla de cocina.

ALTER TABLE categorias
    ADD COLUMN pasa_por_cocina BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE pedido_items
    ADD COLUMN pasa_por_cocina BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN listo_en DATETIME(6) NULL,
    ADD COLUMN servido_en DATETIME(6) NULL,
    ADD KEY ix_pedido_items_estado (restaurante_id, estado, enviado_en);
