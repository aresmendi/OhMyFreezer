# V5 — Runbook de rollback (`unidades_medida`)

Este archivo NO es una migración de Flyway (no tiene sufijo `.sql`, Flyway lo
ignora). Es el runbook manual de rollback para `V5__unidades_medida.sql`,
separado de `V5_PRE_DEPLOY_CHECKLIST.md` (que cubre la auditoría PRE-despliegue).

## Si la migración abortó a mitad de camino (antes de completarse)

`CREATE TABLE`/`ALTER TABLE` hacen autocommit en MySQL/TiDB: no forman parte
de una transacción reversible. Si la guarda de backfill abortó (el paso 2 del
checklist de pre-despliegue no se hizo, o `sql_mode` no era el esperado), los
pasos previos de `V5__unidades_medida.sql` (creación de `unidades_medida`,
columnas `unidad_base_id`/`unidad_id` como `NULL`, backfill parcial) YA
quedaron aplicados — Flyway marca V5 como `FAILED` pero la base queda en un
estado a medias, no revertido.

**Recuperación** (la migración termina de aplicarse, no se revierte):
1. Resolver los valores `unidad_medida` no mapeados (paso 2 del pre-deploy
   checklist).
2. Ejecutar `./mvnw flyway:repair` (o el equivalente del entorno).
3. Volver a desplegar para que V5 termine de aplicarse desde donde quedó.

## Rollback completo (V5 ya aplicada con éxito, se decide revertir)

V5 es aditiva: una tabla nueva (`unidades_medida`) más dos columnas FK nuevas
(`ingredientes.unidad_base_id`, `receta_ingredientes.unidad_id`). El string
legacy `unidad_medida` NUNCA se elimina.

### Rollback solo-lectura (sin re-desplegar escrituras del jar anterior)

Redesplegar el jar anterior (pre-V5) funciona sin ninguna acción sobre el
esquema: ese jar simplemente ignora las columnas/tabla nuevas al leer.

### Rollback con escrituras del jar anterior

Si además hace falta que el jar anterior pueda **escribir** (`INSERT` en
`ingredientes`/`receta_ingredientes` sin conocer `unidad_base_id`/`unidad_id`),
hay que relajar manualmente esas dos columnas a nullable ANTES de
redesplegar el jar anterior:

```sql
ALTER TABLE ingredientes MODIFY unidad_base_id BIGINT NULL;
ALTER TABLE receta_ingredientes MODIFY unidad_id BIGINT NULL;
```

Estas dos sentencias son un runbook manual, no una migración: deliberadamente
NO se colocan como archivo `.sql` dentro de `db/migration/`, porque Flyway las
aplicaría automáticamente en el siguiente arranque y anularía la constraint
`NOT NULL` que V5 introdujo a propósito.

### Reversión total del esquema (eliminar V5 por completo)

Solo si se decide abandonar la feature de unidades tipadas por completo:

```sql
ALTER TABLE receta_ingredientes DROP FOREIGN KEY receta_ingredientes_unidad_id_fk;
ALTER TABLE receta_ingredientes DROP COLUMN unidad_id;
ALTER TABLE ingredientes DROP FOREIGN KEY ingredientes_unidad_base_id_fk;
ALTER TABLE ingredientes DROP COLUMN unidad_base_id;
DROP TABLE unidades_medida;
DELETE FROM flyway_schema_history WHERE version = '5';
```

Los nombres exactos de las constraints FK pueden variar según cómo Flyway/el
motor los generó automáticamente — confirmar con `SHOW CREATE TABLE
ingredientes;` / `SHOW CREATE TABLE receta_ingredientes;` antes de ejecutar
los `DROP FOREIGN KEY`. Esta reversión total es destructiva e irreversible:
requiere confirmación explícita antes de ejecutarse en cualquier entorno con
datos reales.
