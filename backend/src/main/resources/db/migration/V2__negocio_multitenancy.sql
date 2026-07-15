-- V2: multi-tenancy foundation (Negocio).
-- TiDB-safe: no CTAS, only ADD/UPDATE/MODIFY steps (nullable -> backfill -> not null -> FK).
-- Adds a negocio_id discriminator to the 6 tenant-owned tables and provisions
-- a signup-code mechanism to replace the single global BUSSINES_LOGIC_CODE.

-- 1. New tables: Negocio (tenant) and its signup codes.
CREATE TABLE IF NOT EXISTS negocios (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(100) NOT NULL,
    plan VARCHAR(30) NOT NULL DEFAULT 'FREE',
    fecha_alta DATETIME(6) NOT NULL,
    email_contacto VARCHAR(100),
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS negocio_signup_codes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    negocio_id BIGINT NOT NULL,
    codigo VARCHAR(64) NOT NULL,
    usado BIT NOT NULL DEFAULT 0,
    activo BIT NOT NULL DEFAULT 1,
    usado_por_usuario_id BIGINT,
    fecha_creacion DATETIME(6) NOT NULL,
    fecha_uso DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_negocio_signup_codes_codigo UNIQUE (codigo),
    CONSTRAINT fk_negocio_signup_codes_negocio FOREIGN KEY (negocio_id) REFERENCES negocios (id)
);

-- 2. Seed Negocio (id=1): every pre-existing row is backfilled to this tenant.
INSERT INTO negocios (nombre, plan, fecha_alta, email_contacto)
VALUES ('Negocio Semilla', 'FREE', NOW(6), NULL);

-- 3. usuarios: add negocio_id, backfill, enforce NOT NULL + FK.
ALTER TABLE usuarios ADD COLUMN negocio_id BIGINT NULL;
UPDATE usuarios SET negocio_id = 1;
ALTER TABLE usuarios MODIFY negocio_id BIGINT NOT NULL;
ALTER TABLE usuarios ADD CONSTRAINT fk_usuarios_negocio FOREIGN KEY (negocio_id) REFERENCES negocios (id);

-- 4. ingredientes: same pattern.
ALTER TABLE ingredientes ADD COLUMN negocio_id BIGINT NULL;
UPDATE ingredientes SET negocio_id = 1;
ALTER TABLE ingredientes MODIFY negocio_id BIGINT NOT NULL;
ALTER TABLE ingredientes ADD CONSTRAINT fk_ingredientes_negocio FOREIGN KEY (negocio_id) REFERENCES negocios (id);

-- 5. recetas: same pattern.
ALTER TABLE recetas ADD COLUMN negocio_id BIGINT NULL;
UPDATE recetas SET negocio_id = 1;
ALTER TABLE recetas MODIFY negocio_id BIGINT NOT NULL;
ALTER TABLE recetas ADD CONSTRAINT fk_recetas_negocio FOREIGN KEY (negocio_id) REFERENCES negocios (id);

-- 6. alertas: same pattern.
ALTER TABLE alertas ADD COLUMN negocio_id BIGINT NULL;
UPDATE alertas SET negocio_id = 1;
ALTER TABLE alertas MODIFY negocio_id BIGINT NOT NULL;
ALTER TABLE alertas ADD CONSTRAINT fk_alertas_negocio FOREIGN KEY (negocio_id) REFERENCES negocios (id);

-- 7. movimientos_stock: own real negocio_id column (usuarioId stays a raw Long,
-- it cannot ride the Ingrediente association for @Filter purposes).
ALTER TABLE movimientos_stock ADD COLUMN negocio_id BIGINT NULL;
UPDATE movimientos_stock SET negocio_id = 1;
ALTER TABLE movimientos_stock MODIFY negocio_id BIGINT NOT NULL;
ALTER TABLE movimientos_stock ADD CONSTRAINT fk_movimientos_stock_negocio FOREIGN KEY (negocio_id) REFERENCES negocios (id);

-- 8. registro_uso_recetas: same pattern.
ALTER TABLE registro_uso_recetas ADD COLUMN negocio_id BIGINT NULL;
UPDATE registro_uso_recetas SET negocio_id = 1;
ALTER TABLE registro_uso_recetas MODIFY negocio_id BIGINT NOT NULL;
ALTER TABLE registro_uso_recetas ADD CONSTRAINT fk_registro_uso_recetas_negocio FOREIGN KEY (negocio_id) REFERENCES negocios (id);

-- 9. Username uniqueness becomes per-negocio: drop the old global unique
-- constraint and replace it with a composite one.
ALTER TABLE usuarios DROP INDEX uk_usuarios_username;
ALTER TABLE usuarios ADD CONSTRAINT uk_usuarios_negocio_username UNIQUE (negocio_id, username);
