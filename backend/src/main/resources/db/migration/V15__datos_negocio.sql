-- Datos del negocio para el ticket de venta: razón social, RUC, dirección, teléfono, mensaje al pie
-- y el ancho del papel de la impresora. Una fila por restaurante; si todavía no hay fila, el sistema usa
-- los valores de siempre (sin RUC ni dirección, papel de 80 mm).
-- La clave foránea impide que una fila quede apuntando a un restaurante que no existe.

CREATE TABLE negocio_datos (
    restaurante_id BIGINT NOT NULL PRIMARY KEY,
    razon_social VARCHAR(150) NULL,
    -- 11 dígitos. Se valida en el servidor (dígito verificador de SUNAT).
    ruc CHAR(11) NULL,
    direccion VARCHAR(200) NULL,
    telefono VARCHAR(30) NULL,
    mensaje_ticket VARCHAR(120) NULL,
    -- Ancho del papel de la impresora de tickets: 58 u 80 milímetros.
    ancho_ticket_mm SMALLINT NOT NULL DEFAULT 80,
    actualizado_en DATETIME(6) NOT NULL,
    CONSTRAINT fk_negocio_datos_restaurante FOREIGN KEY (restaurante_id) REFERENCES restaurantes (id),
    CONSTRAINT ck_negocio_ancho CHECK (ancho_ticket_mm IN (58, 80))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
