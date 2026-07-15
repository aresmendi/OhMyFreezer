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
 * Verifica que las migraciones de Flyway (V1 baseline + V2 multi-tenancy)
 * se aplican limpiamente sobre una base H2 nueva (perfil de test) y que el
 * esquema resultante cumple lo definido en el diseño de multi-tenancy:
 * columna negocio_id NOT NULL con FK en las 6 tablas tenant, tabla negocios
 * sembrada con id=1, y unicidad de username recompuesta como
 * UNIQUE(negocio_id, username).
 */
class FlywayMigrationTest {

    private static final String[] TENANT_TABLES = {
            "USUARIOS", "INGREDIENTES", "RECETAS", "ALERTAS",
            "MOVIMIENTOS_STOCK", "REGISTRO_USO_RECETAS"
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

        assertThat(flyway.info().applied()).hasSizeGreaterThanOrEqualTo(2);
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

            String insertUsuario = "INSERT INTO usuarios (username, password, es_jefe_cocina, fecha_registro, negocio_id) "
                    + "VALUES ('admin', 'x', 0, NOW(6), %d)";

            try (Statement st = conn.createStatement()) {
                st.execute(String.format(insertUsuario, 1));
                // mismo username, distinto negocio -> debe permitirse
                st.execute(String.format(insertUsuario, 2));
            }

            try (Statement st = conn.createStatement()) {
                assertThatThrownBy(() -> st.execute(String.format(insertUsuario, 1)))
                        .as("duplicate username within the same negocio must violate the composite unique constraint")
                        .isInstanceOf(SQLException.class);
            }
        }
    }
}
