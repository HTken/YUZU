-- Anular una venta ya cobrada. La cuenta NO se borra: pasa a ANULADO y conserva su ticket, su turno, su total
-- y quién la cobró, para que quede constancia. Aquí se guarda además quién la anuló, cuándo y por qué.
-- Los números de ticket no se reutilizan: el siguiente cobro sigue contando desde el mayor que existe.

ALTER TABLE pedidos
    ADD COLUMN anulado_cobro_por VARCHAR(150) NULL,
    ADD COLUMN anulado_cobro_en DATETIME(6) NULL,
    ADD COLUMN motivo_anulacion VARCHAR(200) NULL;
