-- Límite de intentos de login y verificación en dos pasos (2FA con app autenticadora, TOTP).
-- totp_secret queda NULL mientras el usuario no active el 2FA (es opcional, solo para ADMIN y SUPERADMIN).

ALTER TABLE usuarios
    ADD COLUMN intentos_fallidos INT NOT NULL DEFAULT 0,
    ADD COLUMN bloqueado_hasta DATETIME NULL,
    ADD COLUMN totp_secret VARCHAR(64) NULL;

ALTER TABLE administradores_plataforma
    ADD COLUMN intentos_fallidos INT NOT NULL DEFAULT 0,
    ADD COLUMN bloqueado_hasta DATETIME NULL,
    ADD COLUMN totp_secret VARCHAR(64) NULL;
