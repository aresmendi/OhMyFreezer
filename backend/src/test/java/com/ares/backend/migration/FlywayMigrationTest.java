package com.ares.backend.migration;

import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifica que las migraciones de Flyway (V1 baseline + V2 multi-tenancy +
 * V3 email único + V4 sin DEFAULT en negocio_id + V5 unidades de medida +
 * V6 integración TPV) se aplican limpiamente sobre una base H2 nueva
 * (perfil de test) y que el esquema resultante cumple lo definido en el
 * diseño de multi-tenancy: columna negocio_id NOT NULL con FK en las 6
 * tablas tenant, tabla negocios sembrada con id=1, unicidad de username
 * recompuesta como UNIQUE(negocio_id, username), y (desde V4) sin DEFAULT 1
 * residual una vez que todo el código de aplicación resuelve y asigna
 * negocio_id explícitamente en cada creación.
 * <p>
 * Las pruebas específicas de V6 (tablas TPV, constraints únicas) verifican
 * el SQL crudo de la migración de forma independiente de las entidades
 * JPA — {@code TenantFilterMappingTest}/los repository tests cubren el
 * mapeo Hibernate por separado, sobre {@code ddl-auto=create-drop}.
 */
class FlywayMigrationTest {

    private static final String[] TENANT_TABLES = {
            "USUARIOS", "INGREDIENTES", "RECETAS", "ALERTAS",
            "MOVIMIENTOS_STOCK", "REGISTRO_USO_RECETAS"
    };

    private static final String[] TPV_TABLES = {
            "TPV_API_KEYS", "TPV_SKU_MAPPING", "VENTAS_TPV", "VENTAS_TPV_LINEAS"
    };

    private DataSource freshH2DataSource() {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:flyway_test_" + System.nanoTime() + ";DB_CLOSE_DELAY=-1;MODE=MySQL");
        ds.setUser("sa");
        ds.setPassword("");
        return ds;
    }

    private Flyway migratedFlyway() {
        Flyway flyway = Flyway.configure()
                .dataSource(freshH2DataSource())
                .locations("classpath:db/migration")
                .load();
        flyway.migrate();
        return flyway;
    }

    @Test
    void migracionesSeAplicanLimpiamenteSobreBaseNueva() {
        Flyway flyway = migratedFlyway();

        assertThat(flyway.info().applied()).hasSizeGreaterThanOrEqualTo(4);
        assertThat(flyway.info().pending()).isEmpty();
    }

    @Test
    void seedNegocioExisteConId1() throws SQLException {
        Flyway flyway = migratedFlyway();

        try (Connection conn = flyway.getConfiguration().getDataSource().getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT id, nombre, plan FROM negocios WHERE id = 1")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getLong("id")).isEqualTo(1L);
            assertThat(rs.getString("nombre")).isEqualTo("Negocio Semilla");
            assertThat(rs.getString("plan")).isEqualTo("FREE");
        }
    }

    @Test
    void las6TablasTenantTienenNegocioIdNotNull() throws SQLException {
        Flyway flyway = migratedFlyway();

        try (Connection conn = flyway.getConfiguration().getDataSource().getConnection()) {
            for (String table : TENANT_TABLES) {
                try (Statement st = conn.createStatement();
                     ResultSet rs = st.executeQuery(
                             "SELECT is_nullable FROM information_schema.columns "
                                     + "WHERE table_name = '" + table + "' AND column_name = 'NEGOCIO_ID'")) {
                    assertThat(rs.next())
                            .as("negocio_id column must exist on " + table)
                            .isTrue();
                    assertThat(rs.getString("is_nullable"))
                            .as("negocio_id must be NOT NULL on " + table)
                            .isEqualTo("NO");
                }
            }
        }
    }

    @Test
    void usernameEsUnicoPorNegocioNoGlobalmente() throws SQLException {
        Flyway flyway = migratedFlyway();

        try (Connection conn = flyway.getConfiguration().getDataSource().getConnection()) {
            // Negocio B adicional para probar unicidad compuesta.
            try (Statement st = conn.createStatement()) {
                st.execute("INSERT INTO negocios (nombre, plan, fecha_alta) VALUES ('Negocio B', 'FREE', NOW(6))");
            }

            // email distinto en cada INSERT (a propósito): desde V3 email es
            // NOT NULL + UNIQUE global, así que hace falta un valor propio por
            // fila para que el fallo esperado en el tercer INSERT se deba
            // inequívocamente a la constraint compuesta de username, no a la
            // de email.
            String insertUsuario = "INSERT INTO usuarios (username, password, es_jefe_cocina, fecha_registro, negocio_id, email) "
                    + "VALUES ('admin', 'x', 0, NOW(6), %d, '%s')";

            try (Statement st = conn.createStatement()) {
                st.execute(String.format(insertUsuario, 1, "admin1@test.com"));
                // mismo username, distinto negocio -> debe permitirse
                st.execute(String.format(insertUsuario, 2, "admin2@test.com"));
            }

            try (Statement st = conn.createStatement()) {
                assertThatThrownBy(() -> st.execute(String.format(insertUsuario, 1, "admin3@test.com")))
                        .as("duplicate username within the same negocio must violate the composite unique constraint")
                        .isInstanceOf(SQLException.class);
            }
        }
    }

    @Test
    void emailEsObligatorioYUnicoGlobalmenteTrasV3() throws SQLException {
        Flyway flyway = migratedFlyway();

        try (Connection conn = flyway.getConfiguration().getDataSource().getConnection()) {
            try (Statement st = conn.createStatement()) {
                assertThatThrownBy(() -> st.execute(
                        "INSERT INTO usuarios (username, password, es_jefe_cocina, fecha_registro, negocio_id) "
                                + "VALUES ('sinemail', 'x', 0, NOW(6), 1)"))
                        .as("email debe ser NOT NULL tras V3")
                        .isInstanceOf(SQLException.class);
            }

            try (Statement st = conn.createStatement()) {
                st.execute("INSERT INTO usuarios (username, password, es_jefe_cocina, fecha_registro, negocio_id, email) "
                        + "VALUES ('conemail1', 'x', 0, NOW(6), 1, 'unico@test.com')");
            }

            try (Statement st = conn.createStatement()) {
                assertThatThrownBy(() -> st.execute(
                        "INSERT INTO usuarios (username, password, es_jefe_cocina, fecha_registro, negocio_id, email) "
                                + "VALUES ('conemail2', 'x', 0, NOW(6), 1, 'unico@test.com')"))
                        .as("email debe ser UNIQUE globalmente (no por negocio, a diferencia de username) tras V3")
                        .isInstanceOf(SQLException.class);
            }
        }
    }

    @Test
    void backfillDeEmailNuloEsDefensivoAntesDeAplicarV3() throws SQLException {
        DataSource ds = freshH2DataSource();

        Flyway flywayHastaV2 = Flyway.configure()
                .dataSource(ds)
                .locations("classpath:db/migration")
                .target("2")
                .load();
        flywayHastaV2.migrate();

        // Simula una fila preexistente (previa a V3) con email NULL: exactamente
        // el estado que el backfill defensivo de V3 debe cubrir para que la
        // migración nunca falle sobre datos reales de dev/staging.
        try (Connection conn = ds.getConnection(); Statement st = conn.createStatement()) {
            st.execute("INSERT INTO usuarios (id, username, password, es_jefe_cocina, fecha_registro, negocio_id, email) "
                    + "VALUES (999, 'legacySinEmail', 'x', 0, NOW(6), 1, NULL)");
        }

        Flyway flywayCompleto = Flyway.configure()
                .dataSource(ds)
                .locations("classpath:db/migration")
                .load();
        flywayCompleto.migrate();

        try (Connection conn = ds.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT email FROM usuarios WHERE id = 999")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("email"))
                    .as("email NULL preexistente debe backfillearse con un placeholder derivado del id (único), nunca fallar la migración")
                    .isEqualTo("sin-email+999@ohmyfreezer.invalid");
        }
    }

    @Test
    void las6TablasTenantYaNoTienenDefaultEnNegocioIdTrasV4() throws SQLException {
        Flyway flyway = migratedFlyway();

        try (Connection conn = flyway.getConfiguration().getDataSource().getConnection()) {
            for (String table : TENANT_TABLES) {
                try (Statement st = conn.createStatement();
                     ResultSet rs = st.executeQuery(
                             "SELECT column_default FROM information_schema.columns "
                                     + "WHERE table_name = '" + table + "' AND column_name = 'NEGOCIO_ID'")) {
                    assertThat(rs.next())
                            .as("negocio_id column must exist on " + table)
                            .isTrue();
                    assertThat(rs.getString("column_default"))
                            .as("negocio_id must no longer have a DEFAULT on " + table + " after V4")
                            .isNull();
                }
            }
        }
    }

    @Test
    void insertarIngredienteSinNegocioIdFallaTrasV4() throws SQLException {
        Flyway flyway = migratedFlyway();

        try (Connection conn = flyway.getConfiguration().getDataSource().getConnection()) {
            try (Statement st = conn.createStatement()) {
                assertThatThrownBy(() -> st.execute(
                        "INSERT INTO ingredientes (nombre, cantidad, unidad_medida, stock_minimo, fecha_actualizacion) "
                                + "VALUES ('sin negocio', 1, 'kg', 1, NOW(6))"))
                        .as("omitting negocio_id must violate NOT NULL now that the temporary DEFAULT 1 is gone (V4)")
                        .isInstanceOf(SQLException.class);
            }

            try (Statement st = conn.createStatement()) {
                // unidad_base_id (V5) también es NOT NULL sin DEFAULT: se resuelve
                // aquí vía subconsulta contra el catálogo sembrado, en vez de un id
                // literal, para no acoplar el test al orden de inserción de V5.
                st.execute("INSERT INTO ingredientes (nombre, cantidad, unidad_medida, stock_minimo, fecha_actualizacion, negocio_id, unidad_base_id) "
                        + "VALUES ('con negocio', 1, 'kg', 1, NOW(6), 1, (SELECT id FROM unidades_medida WHERE codigo = 'kg'))");
            }
        }
    }

    @Test
    void v6CreaLasCuatroTablasTpv() throws SQLException {
        Flyway flyway = migratedFlyway();

        try (Connection conn = flyway.getConfiguration().getDataSource().getConnection()) {
            for (String table : TPV_TABLES) {
                try (Statement st = conn.createStatement();
                     ResultSet rs = st.executeQuery(
                             "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = '" + table + "'")) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getInt(1)).as(table + " debe existir tras V6").isEqualTo(1);
                }
            }
        }
    }

    @Test
    void skuTpvEsUnicoPorNegocioPeroNoGlobalmenteTrasV6() throws SQLException {
        Flyway flyway = migratedFlyway();

        try (Connection conn = flyway.getConfiguration().getDataSource().getConnection()) {
            // Negocio B adicional (id=2, único insertado hasta ahora en esta BD fresca)
            // para probar que sku_tpv solo es único DENTRO de un negocio (D-B del diseño).
            try (Statement st = conn.createStatement()) {
                st.execute("INSERT INTO negocios (nombre, plan, fecha_alta) VALUES ('Negocio B', 'FREE', NOW(6))");
            }

            String insertMapping = "INSERT INTO tpv_sku_mapping (negocio_id, sku_tpv, fecha_creacion, fecha_actualizacion) "
                    + "VALUES (%d, '%s', NOW(6), NOW(6))";

            try (Statement st = conn.createStatement()) {
                st.execute(String.format(insertMapping, 1, "101"));
                // mismo sku_tpv, negocio distinto -> debe permitirse (sin colisión cross-tenant)
                st.execute(String.format(insertMapping, 2, "101"));
            }

            try (Statement st = conn.createStatement()) {
                assertThatThrownBy(() -> st.execute(String.format(insertMapping, 1, "101")))
                        .as("el mismo sku_tpv repetido dentro del mismo negocio debe violar uk_tpv_sku_mapping")
                        .isInstanceOf(SQLException.class);
            }
        }
    }

    @Test
    void externalIdEsUnicoPorNegocioEnVentasTpvTrasV6() throws SQLException {
        Flyway flyway = migratedFlyway();

        try (Connection conn = flyway.getConfiguration().getDataSource().getConnection()) {
            try (Statement st = conn.createStatement()) {
                st.execute("INSERT INTO negocios (nombre, plan, fecha_alta) VALUES ('Negocio B', 'FREE', NOW(6))");
            }

            String insertVenta = "INSERT INTO ventas_tpv (negocio_id, tipo, external_id, sku_tpv, cantidad, estado, fecha_recepcion) "
                    + "VALUES (%d, 'VENTA', '%s', '101', 1, 'RECIBIDA', NOW(6))";

            try (Statement st = conn.createStatement()) {
                st.execute(String.format(insertVenta, 1, "ext-1"));
                // mismo external_id, negocio distinto -> debe permitirse
                st.execute(String.format(insertVenta, 2, "ext-1"));
            }

            try (Statement st = conn.createStatement()) {
                assertThatThrownBy(() -> st.execute(String.format(insertVenta, 1, "ext-1")))
                        .as("el mismo external_id repetido dentro del mismo negocio debe violar uk_ventas_tpv_external "
                                + "(replay de una venta ya procesada)")
                        .isInstanceOf(SQLException.class);
            }
        }
    }

    @Test
    void externalIdOriginalEsUnicoPorNegocioEnVentasTpvTrasV6() throws SQLException {
        Flyway flyway = migratedFlyway();

        try (Connection conn = flyway.getConfiguration().getDataSource().getConnection()) {
            String insertAnulacion = "INSERT INTO ventas_tpv (negocio_id, tipo, external_id, external_id_original, sku_tpv, cantidad, estado, fecha_recepcion) "
                    + "VALUES (1, 'ANULACION', '%s', 'orig-1', '101', 1, 'ANULACION_APLICADA', NOW(6))";

            try (Statement st = conn.createStatement()) {
                st.execute(String.format(insertAnulacion, "anul-1"));
            }

            try (Statement st = conn.createStatement()) {
                assertThatThrownBy(() -> st.execute(String.format(insertAnulacion, "anul-2")))
                        .as("un segundo external_id_original repetido dentro del mismo negocio debe violar "
                                + "uk_ventas_tpv_original (una venta no puede anularse dos veces)")
                        .isInstanceOf(SQLException.class);
            }
        }
    }

    @Test
    void nullEsDistintoDeNullEnExternalIdOriginalDeVentasTpvTrasV6() throws SQLException {
        Flyway flyway = migratedFlyway();

        try (Connection conn = flyway.getConfiguration().getDataSource().getConnection()) {
            // Ventas normales (tipo=VENTA) dejan external_id_original en NULL: MySQL y
            // H2 tratan NULL como distinto en un índice único, así que dos ventas
            // normales en el mismo negocio nunca colisionan por esta columna.
            String insertVentaNormal = "INSERT INTO ventas_tpv (negocio_id, tipo, external_id, sku_tpv, cantidad, estado, fecha_recepcion) "
                    + "VALUES (1, 'VENTA', '%s', '101', 1, 'PROCESADA', NOW(6))";

            try (Statement st = conn.createStatement()) {
                st.execute(String.format(insertVentaNormal, "venta-a"));
                st.execute(String.format(insertVentaNormal, "venta-b"));
            }

            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM ventas_tpv WHERE external_id_original IS NULL")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isEqualTo(2);
            }
        }
    }

    @Test
    void prefijoDeTpvApiKeyEsUnicoGlobalmenteTrasV6() throws SQLException {
        Flyway flyway = migratedFlyway();

        try (Connection conn = flyway.getConfiguration().getDataSource().getConnection()) {
            try (Statement st = conn.createStatement()) {
                st.execute("INSERT INTO negocios (nombre, plan, fecha_alta) VALUES ('Negocio B', 'FREE', NOW(6))");
                st.execute("INSERT INTO usuarios (username, password, es_jefe_cocina, fecha_registro, negocio_id, email) "
                        + "VALUES ('tpv-system-1', 'x', 0, NOW(6), 1, 'tpv+negocio-1@tpv.ohmyfreezer.invalid')");
                st.execute("INSERT INTO usuarios (username, password, es_jefe_cocina, fecha_registro, negocio_id, email) "
                        + "VALUES ('tpv-system-2', 'x', 0, NOW(6), 2, 'tpv+negocio-2@tpv.ohmyfreezer.invalid')");
            }

            String insertKey = "INSERT INTO tpv_api_keys (negocio_id, prefijo, secreto_hash, usuario_sistema_id, activa, fecha_creacion) "
                    + "VALUES (%d, '%s', 'hash-de-prueba', %d, 1, NOW(6))";

            try (Statement st = conn.createStatement()) {
                st.execute(String.format(insertKey, 1, "pfx-aaaaaaaaaa", 1));
            }

            try (Statement st = conn.createStatement()) {
                assertThatThrownBy(() -> st.execute(String.format(insertKey, 2, "pfx-aaaaaaaaaa", 2)))
                        .as("prefijo debe ser único GLOBALMENTE (D-D del diseño: el filtro hace un lookup indexado "
                                + "por prefijo antes de conocer el negocio), incluso entre negocios distintos")
                        .isInstanceOf(SQLException.class);
            }
        }
    }

    @Test
    void ventasTpvLineasSeBorranEnCascadaAlBorrarLaVentaTrasV6() throws SQLException {
        Flyway flyway = migratedFlyway();

        try (Connection conn = flyway.getConfiguration().getDataSource().getConnection()) {
            long ventaId;
            try (Statement st = conn.createStatement()) {
                st.execute("INSERT INTO ingredientes (nombre, cantidad, unidad_medida, stock_minimo, fecha_actualizacion, negocio_id, unidad_base_id) "
                        + "VALUES ('Harina TPV', 10, 'kg', 1, NOW(6), 1, (SELECT id FROM unidades_medida WHERE codigo = 'kg'))");
                st.execute("INSERT INTO ventas_tpv (negocio_id, tipo, external_id, sku_tpv, cantidad, estado, fecha_recepcion) "
                        + "VALUES (1, 'VENTA', 'ext-cascade', '101', 1, 'PROCESADA', NOW(6))");
            }
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT id FROM ventas_tpv WHERE external_id = 'ext-cascade'")) {
                assertThat(rs.next()).isTrue();
                ventaId = rs.getLong(1);
            }
            try (Statement st = conn.createStatement()) {
                st.execute("INSERT INTO ventas_tpv_lineas (venta_tpv_id, ingrediente_id, cantidad_descontada) "
                        + "VALUES (" + ventaId + ", (SELECT id FROM ingredientes WHERE nombre = 'Harina TPV'), 2.5)");
            }
            try (Statement st = conn.createStatement()) {
                st.execute("DELETE FROM ventas_tpv WHERE id = " + ventaId);
            }
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM ventas_tpv_lineas WHERE venta_tpv_id = " + ventaId)) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1))
                        .as("ON DELETE CASCADE debe borrar las líneas huérfanas al borrar la venta")
                        .isEqualTo(0);
            }
        }
    }
}
