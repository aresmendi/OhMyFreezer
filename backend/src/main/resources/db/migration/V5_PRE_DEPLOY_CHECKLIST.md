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

## Si la migración aborta a mitad de camino

`CREATE TABLE`/`ALTER TABLE` hacen autocommit en MySQL/TiDB: no forman parte
de una transacción reversible. Si la guarda aborta (paso 2 no se hizo bien,
o `sql_mode` no es el esperado), los pasos previos de `V5__unidades_medida.sql`
(creación de `unidades_medida`, columnas `unidad_base_id`/`unidad_id` como
`NULL`, backfill parcial) YA quedaron aplicados — Flyway marca V5 como
`FAILED` pero la base queda en un estado a medias, no revertido.

Recuperación: resolver los valores no mapeados (paso 2 de este checklist),
luego `./mvnw flyway:repair` (o el equivalente del entorno) y volver a
desplegar para que V5 termine de aplicarse desde donde quedó.

## Rollback

Ver `V5_ROLLBACK_RUNBOOK.md` (mismo directorio) para el procedimiento
completo de rollback — recuperación de un aborto a mitad de camino, rollback
solo-lectura, rollback con escrituras del jar anterior y reversión total del
esquema.
