-- V4: drop the temporary DEFAULT 1 on negocio_id (6 tenant tables).
-- V2 introduced this default as a deploy-safety net while application code
-- was retrofitted incrementally (PR1-6) to explicitly resolve+set Negocio on
-- every create path. That retrofit is now complete and merged: all 6
-- entities are nullable=false, and every production creation call site sets
-- negocio explicitly before save() (confirmed via grep, no raw/native SQL
-- insert path exists anywhere in the codebase). The default is now dead
-- weight and only masked a NOT NULL violation for inserts that omit the
-- column entirely — no such insert path remains, so removing it is safe.
--
-- NOT NULL and the FK to negocios are left untouched; only the column
-- default is dropped. No existing data is modified.
ALTER TABLE usuarios MODIFY negocio_id BIGINT NOT NULL;
ALTER TABLE ingredientes MODIFY negocio_id BIGINT NOT NULL;
ALTER TABLE recetas MODIFY negocio_id BIGINT NOT NULL;
ALTER TABLE alertas MODIFY negocio_id BIGINT NOT NULL;
ALTER TABLE movimientos_stock MODIFY negocio_id BIGINT NOT NULL;
ALTER TABLE registro_uso_recetas MODIFY negocio_id BIGINT NOT NULL;
