-- V6: fundación de la integración TPV (Fase 6). Puramente aditiva: cuatro
-- tablas nuevas, cero ALTER sobre tablas existentes.
--
-- tpv_api_keys, tpv_sku_mapping y ventas_tpv son tenant-owned: negocio_id
-- NOT NULL + @Filter(negocioFilter) en la entidad, igual que Receta/
-- Ingrediente (ver V2). ventas_tpv_lineas se scopea transitivamente a
-- través de su padre ventas_tpv (sin columna propia), igual que
-- receta_ingredientes se scopea a través de Receta.
--
-- ANSI-portable a propósito: FlywayMigrationTest ejecuta esta migración
-- sobre H2 en MODE=MySQL, que rechaza extensiones propietarias de MySQL
-- (ver nota de V5). No hay ninguna aquí: solo CREATE TABLE + FK estándar.

-- 1. tpv_api_keys: una credencial activa por negocio (D-D del diseño:
-- prefijo indexado + secreto bcrypt). El prefijo es único GLOBALMENTE, no
-- por negocio: el filtro resuelve el negocio a partir del prefijo, así que
-- dos negocios no pueden compartir el mismo valor.
CREATE TABLE IF NOT EXISTS tpv_api_keys (
    id BIGINT NOT NULL AUTO_INCREMENT,
    negocio_id BIGINT NOT NULL,
    prefijo VARCHAR(16) NOT NULL,
    secreto_hash VARCHAR(72) NOT NULL,
    usuario_sistema_id BIGINT NOT NULL,
    activa BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion DATETIME(6) NOT NULL,
    fecha_revocacion DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_tpv_api_keys_prefijo UNIQUE (prefijo),
    CONSTRAINT fk_tpv_api_keys_negocio FOREIGN KEY (negocio_id) REFERENCES negocios (id),
    CONSTRAINT fk_tpv_api_keys_usuario FOREIGN KEY (usuario_sistema_id) REFERENCES usuarios (id)
);

-- 2. tpv_sku_mapping: mapeo SKU-de-TPV -> receta, un negocio puede tener
-- N SKUs para 1 receta (D-B del diseño). receta_id NULLABLE: una fila con
-- receta_id NULL ES la fila "pendiente de mapear" que ve el jefe de cocina
-- en pantalla — no hay una tabla ni un estado separados para eso.
-- UNIQUE(negocio_id, sku_tpv), NUNCA global: dos negocios distintos pueden
-- usar el mismo código de producto sin colisionar entre sí.
CREATE TABLE IF NOT EXISTS tpv_sku_mapping (
    id BIGINT NOT NULL AUTO_INCREMENT,
    negocio_id BIGINT NOT NULL,
    sku_tpv VARCHAR(64) NOT NULL,
    nombre_tpv VARCHAR(120) NULL,
    receta_id BIGINT NULL,
    fecha_creacion DATETIME(6) NOT NULL,
    fecha_actualizacion DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_tpv_sku_mapping UNIQUE (negocio_id, sku_tpv),
    CONSTRAINT fk_tpv_sku_mapping_negocio FOREIGN KEY (negocio_id) REFERENCES negocios (id),
    CONSTRAINT fk_tpv_sku_mapping_receta FOREIGN KEY (receta_id) REFERENCES recetas (id)
);

-- 3. ventas_tpv: registro de cada evento de venta/anulación ingerido desde
-- el TPV. fecha_tpv es meramente informativa (viene del payload del
-- proveedor); fecha_recepcion es la autoritativa (server receipt time,
-- usada para datar la reversión de stock en una anulación, ver diseño).
--
-- Dos constraints únicas compuestas por negocio, ambas a propósito NO
-- globales:
--   - uk_ventas_tpv_external: bloquea el replay de una MISMA venta/anulación
--     (misma external_id) dentro del mismo negocio -> respuesta DUPLICADA.
--   - uk_ventas_tpv_original: bloquea que una venta ya procesada se anule
--     DOS VECES (dos anulaciones con distinto external_id pero el mismo
--     external_id_original) dentro del mismo negocio.
-- MySQL y H2 tratan NULL como valor distinto en un índice único, así que
-- las ventas normales (external_id_original siempre NULL) nunca colisionan
-- entre sí por la segunda constraint — no hace falta un índice parcial.
CREATE TABLE IF NOT EXISTS ventas_tpv (
    id BIGINT NOT NULL AUTO_INCREMENT,
    negocio_id BIGINT NOT NULL,
    tipo VARCHAR(12) NOT NULL,
    external_id VARCHAR(128) NOT NULL,
    external_id_original VARCHAR(128) NULL,
    sku_tpv VARCHAR(64) NOT NULL,
    cantidad INT NOT NULL,
    receta_id BIGINT NULL,
    estado VARCHAR(24) NOT NULL,
    fecha_tpv DATETIME(6) NULL,
    fecha_recepcion DATETIME(6) NOT NULL,
    detalle VARCHAR(500) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_ventas_tpv_external UNIQUE (negocio_id, external_id),
    CONSTRAINT uk_ventas_tpv_original UNIQUE (negocio_id, external_id_original),
    CONSTRAINT fk_ventas_tpv_negocio FOREIGN KEY (negocio_id) REFERENCES negocios (id),
    CONSTRAINT fk_ventas_tpv_receta FOREIGN KEY (receta_id) REFERENCES recetas (id)
);

-- 4. ventas_tpv_lineas: snapshot medido (delta real aplicado por
-- elaborar()) de cada ingrediente descontado por una venta, en la unidad
-- base del ingrediente (D-C del diseño). Es lo único que una anulación
-- restaura — nunca una recomputación desde la receta, que pudo editarse
-- entre la venta y su anulación. Sin negocio_id propio ni @Filter: se
-- scopea transitivamente a través de venta_tpv_id, igual que
-- receta_ingredientes se scopea a través de receta_id.
-- ON DELETE CASCADE: borrar una venta (caso excepcional, no un flujo de
-- negocio normal) no debe dejar líneas huérfanas.
CREATE TABLE IF NOT EXISTS ventas_tpv_lineas (
    id BIGINT NOT NULL AUTO_INCREMENT,
    venta_tpv_id BIGINT NOT NULL,
    ingrediente_id BIGINT NOT NULL,
    cantidad_descontada DOUBLE NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_ventas_tpv_lineas_venta FOREIGN KEY (venta_tpv_id) REFERENCES ventas_tpv (id) ON DELETE CASCADE,
    CONSTRAINT fk_ventas_tpv_lineas_ingrediente FOREIGN KEY (ingrediente_id) REFERENCES ingredientes (id)
);
