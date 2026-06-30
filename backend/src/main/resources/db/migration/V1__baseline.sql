-- V1 baseline: snapshot of the current OhMyFreezer schema (9 tables).
-- Baseline-only: zero structural change vs the live schema.
-- TiDB-compatible: no ENGINE/charset clauses. Types mirror Hibernate 6 (MySQLDialect).

CREATE TABLE IF NOT EXISTS usuarios (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(255) NOT NULL,
    es_jefe_cocina BIT NOT NULL,
    fecha_registro DATETIME(6) NOT NULL,
    email VARCHAR(100),
    PRIMARY KEY (id),
    CONSTRAINT uk_usuarios_username UNIQUE (username)
);

CREATE TABLE IF NOT EXISTS ingredientes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(100) NOT NULL,
    cantidad FLOAT(53) NOT NULL,
    unidad_medida VARCHAR(50) NOT NULL,
    stock_minimo FLOAT(53) NOT NULL,
    fecha_actualizacion DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS recetas (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(100) NOT NULL,
    descripcion VARCHAR(500),
    fecha_creacion DATETIME(6) NOT NULL,
    creada_por_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_recetas_usuario FOREIGN KEY (creada_por_id) REFERENCES usuarios (id)
);

CREATE TABLE IF NOT EXISTS pasos_receta (
    id BIGINT NOT NULL AUTO_INCREMENT,
    orden INTEGER NOT NULL,
    descripcion VARCHAR(500) NOT NULL,
    receta_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_pasos_receta FOREIGN KEY (receta_id) REFERENCES recetas (id)
);

CREATE TABLE IF NOT EXISTS receta_ingredientes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    receta_id BIGINT NOT NULL,
    ingrediente_id BIGINT NOT NULL,
    cantidad_necesaria FLOAT(53) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_ri_receta FOREIGN KEY (receta_id) REFERENCES recetas (id),
    CONSTRAINT fk_ri_ingrediente FOREIGN KEY (ingrediente_id) REFERENCES ingredientes (id)
);

CREATE TABLE IF NOT EXISTS recetas_favoritas (
    id BIGINT NOT NULL AUTO_INCREMENT,
    usuario_id BIGINT NOT NULL,
    receta_id BIGINT NOT NULL,
    fecha_marcado DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_fav_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id),
    CONSTRAINT fk_fav_receta FOREIGN KEY (receta_id) REFERENCES recetas (id)
);

CREATE TABLE IF NOT EXISTS registro_uso_recetas (
    id BIGINT NOT NULL AUTO_INCREMENT,
    receta_id BIGINT NOT NULL,
    usuario_id BIGINT NOT NULL,
    fecha_elaboracion DATETIME(6) NOT NULL,
    completada BIT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_uso_receta FOREIGN KEY (receta_id) REFERENCES recetas (id),
    CONSTRAINT fk_uso_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id)
);

CREATE TABLE IF NOT EXISTS movimientos_stock (
    id BIGINT NOT NULL AUTO_INCREMENT,
    ingrediente_id BIGINT NOT NULL,
    cantidad_anterior FLOAT(53) NOT NULL,
    cantidad_nueva FLOAT(53) NOT NULL,
    cantidad_cambio FLOAT(53) NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    fecha DATETIME(6) NOT NULL,
    motivo VARCHAR(255),
    usuario_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_mov_ingrediente FOREIGN KEY (ingrediente_id) REFERENCES ingredientes (id)
);

CREATE TABLE IF NOT EXISTS alertas (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tipo VARCHAR(50) NOT NULL,
    mensaje VARCHAR(500) NOT NULL,
    receta_id BIGINT,
    ingrediente_id BIGINT,
    destinatario_id BIGINT NOT NULL,
    fecha_creacion DATETIME(6) NOT NULL,
    leida BIT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_alertas_receta FOREIGN KEY (receta_id) REFERENCES recetas (id),
    CONSTRAINT fk_alertas_ingrediente FOREIGN KEY (ingrediente_id) REFERENCES ingredientes (id),
    CONSTRAINT fk_alertas_destinatario FOREIGN KEY (destinatario_id) REFERENCES usuarios (id)
);
