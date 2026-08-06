-- V5: catálogo global de unidades de medida + FKs tipadas en ingredientes y
-- receta_ingredientes.
--
-- unidades_medida es una tabla de REFERENCIA, no tenant-owned: sin
-- negocio_id, sin @Filter en la entidad. Es idéntica para todos los negocios
-- (constantes físicas de conversión), a diferencia de las 6 tablas
-- tenant-owned introducidas en V2.
--
-- IMPORTANTE: antes de ejecutar esta migración contra staging/producción,
-- completar el checklist manual en V5_PRE_DEPLOY_CHECKLIST.md (auditoría de
-- valores reales de unidad_medida). Ese archivo no es una migración de
-- Flyway (sin sufijo .sql), es un runbook.

CREATE TABLE IF NOT EXISTS unidades_medida (
    id BIGINT NOT NULL AUTO_INCREMENT,
    codigo VARCHAR(10) NOT NULL,
    nombre VARCHAR(50) NOT NULL,
    tipo VARCHAR(20) NOT NULL,          -- MASA | VOLUMEN | UNIDAD
    factor_a_base DOUBLE NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_unidades_medida_codigo UNIQUE (codigo)
);

INSERT INTO unidades_medida (codigo, nombre, tipo, factor_a_base) VALUES
    ('g',  'Gramo',      'MASA',    1),
    ('kg', 'Kilogramo',  'MASA',    1000),
    ('ml', 'Mililitro',  'VOLUMEN', 1),
    ('L',  'Litro',      'VOLUMEN', 1000),
    ('ud', 'Unidad',     'UNIDAD',  1);

-- ingredientes.unidad_base_id: backfill por coincidencia exacta de código
-- contra el string legacy unidad_medida (que se RETIENE, no se elimina).
-- Subconsulta correlacionada en vez de UPDATE ... JOIN: la sintaxis JOIN de
-- UPDATE multi-tabla es propietaria de MySQL/TiDB y H2 (usado en
-- FlywayMigrationTest, incluso en MODE=MySQL) no la soporta. La subconsulta
-- correlacionada es ANSI estándar y funciona igual en ambos motores.
ALTER TABLE ingredientes ADD COLUMN unidad_base_id BIGINT NULL;
UPDATE ingredientes
   SET unidad_base_id = (SELECT id FROM unidades_medida WHERE codigo = ingredientes.unidad_medida);

-- Guarda: aborta la migración si algún ingrediente quedó sin unidad mapeada
-- (es decir, su unidad_medida no coincide con ningún código sembrado arriba).
-- TiDB no soporta procedimientos almacenados ni SIGNAL, así que la guarda se
-- implementa con una tabla auxiliar NOT NULL: insertar un NULL en una
-- columna NOT NULL provoca el error 1364 en modo estricto (sql_mode incluye
-- STRICT_TRANS_TABLES, valor por defecto en TiDB) y aborta la migración. Si
-- el SELECT no devuelve filas (no hay ingredientes sin mapear), no se
-- inserta nada y la migración continúa con normalidad.
CREATE TABLE _v5_abort_unidad_medida_sin_mapear (unidad_no_mapeada VARCHAR(50) NOT NULL);
INSERT INTO _v5_abort_unidad_medida_sin_mapear (unidad_no_mapeada)
SELECT NULL FROM ingredientes WHERE unidad_base_id IS NULL;
DROP TABLE _v5_abort_unidad_medida_sin_mapear;

-- Segunda barrera: si por lo que sea la guarda anterior no abortó (por
-- ejemplo, sql_mode no estricto en el entorno objetivo), este MODIFY sigue
-- fallando ante cualquier fila con unidad_base_id NULL. Su mensaje de error
-- es más opaco, por eso la guarda de arriba es la primera línea de defensa.
ALTER TABLE ingredientes MODIFY unidad_base_id BIGINT NOT NULL;
ALTER TABLE ingredientes ADD CONSTRAINT fk_ingredientes_unidad_base
    FOREIGN KEY (unidad_base_id) REFERENCES unidades_medida (id);

-- receta_ingredientes.unidad_id: por defecto, la propia unidad del
-- ingrediente al que pertenece el paso de receta. Misma subconsulta
-- correlacionada por portabilidad H2/MySQL/TiDB (ver nota arriba).
ALTER TABLE receta_ingredientes ADD COLUMN unidad_id BIGINT NULL;
UPDATE receta_ingredientes
   SET unidad_id = (SELECT unidad_base_id FROM ingredientes WHERE id = receta_ingredientes.ingrediente_id);
ALTER TABLE receta_ingredientes MODIFY unidad_id BIGINT NOT NULL;
ALTER TABLE receta_ingredientes ADD CONSTRAINT fk_receta_ingredientes_unidad
    FOREIGN KEY (unidad_id) REFERENCES unidades_medida (id);
