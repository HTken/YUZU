-- Informes: índices para que las consultas por fecha sigan siendo rápidas cuando haya meses de ventas.

-- Las cuentas cobradas (o anuladas) de un restaurante en un rango de fechas.
CREATE INDEX ix_pedidos_cierre ON pedidos (restaurante_id, estado, cerrado_en);

-- Los movimientos de inventario de un restaurante en un rango de fechas.
CREATE INDEX ix_inventario_movimientos_fecha ON inventario_movimientos (restaurante_id, creado_en);
