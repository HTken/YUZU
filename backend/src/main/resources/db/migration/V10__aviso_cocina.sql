-- Aviso a cocina cuando se anula un plato (o toda la cuenta) que cocina todavía tenía en pantalla.
-- aviso_cocina = TRUE significa "cocina todavía no vio esta anulación"; al tocar "Entendido" pasa a FALSE.
ALTER TABLE pedido_items
    ADD COLUMN aviso_cocina BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN anulado_en DATETIME(6) NULL;
