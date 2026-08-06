# V5 — Checklist manual de pre-despliegue (`unidades_medida`)

Este archivo NO es una migración de Flyway (no tiene sufijo `.sql`, Flyway lo
ignora). Es un runbook manual que un humano debe ejecutar y confirmar ANTES
de desplegar `V5__unidades_medida.sql` contra staging/producción.

## 1. Auditar los valores reales de `unidad_medida`

Ejecutar contra la base real (staging primero, luego producción) ANTES del
despliegue:

```sql
SELECT DISTINCT unidad_medida, COUNT(*) FROM ingredientes GROUP BY unidad_medida;
```

El código y los fixtures solo contemplan `kg`, `g`, `L`, `ml`, `ud` (el
formulario de Flutter nunca emitió otro valor), pero escrituras directas por
API pueden contener typos, variantes de mayúsculas/minúsculas u otros
valores no previstos.

## 2. Resolver cualquier valor fuera del conjunto sembrado

Si el `SELECT` anterior devuelve algún `unidad_medida` que NO sea exactamente
`kg`, `g`, `L`, `ml` o `ud`:

- Añadir una línea de mapeo explícita adicional en `V5__unidades_medida.sql`
  (un `INSERT` de esa unidad extra en `unidades_medida`, o un `UPDATE` de
  corrección de dato), **o**
- Limpiar el dato en origen ANTES de desplegar V5.

**Nunca** ampliar el `JOIN` de backfill con `LOWER()`/`TRIM()` para "tragarse"
variantes silenciosamente: la migración está deliberadamente diseñada para
fallar de forma ruidosa (ver mecanismo de guarda en `V5__unidades_medida.sql`)
en vez de asumir un mapeo que nadie confirmó.

## 3. Confirmar el `sql_mode`

Confirmar que el `sql_mode` del servidor objetivo incluye
`STRICT_TRANS_TABLES` (es el valor por defecto en TiDB). Si no lo incluye,
el mecanismo de guarda de la migración degrada de un error duro a un mero
warning y el backfill podría dejar filas con `unidad_base_id` no resuelto
sin abortar el despliegue.

## Nota de rollback (no ejecutar salvo necesidad real)

V5 es aditiva: una tabla nueva más dos columnas FK nuevas. `unidad_medida`
(el string legacy) NUNCA se elimina, así que un rollback de solo-lectura
(redesplegar el jar anterior) funciona sin ninguna acción sobre el esquema.

Si el rollback necesita restaurar también las ESCRITURAS del jar anterior
(que no conoce `unidad_base_id`/`unidad_id` y por tanto los omite en el
`INSERT`), hace falta relajar manualmente las dos columnas a nullable ANTES
de redesplegar el jar anterior:

```sql
ALTER TABLE ingredientes MODIFY unidad_base_id BIGINT NULL;
ALTER TABLE receta_ingredientes MODIFY unidad_id BIGINT NULL;
```

Estas dos sentencias son un runbook manual, no una migración: deliberadamente
NO se colocan como archivo dentro de `db/migration/`, porque Flyway las
aplicaría automáticamente en el siguiente arranque y anularía la constraint
`NOT NULL` que V5 introdujo a propósito.
