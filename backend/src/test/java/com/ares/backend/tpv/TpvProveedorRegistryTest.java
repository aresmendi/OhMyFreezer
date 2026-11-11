package com.ares.backend.tpv;

import com.ares.backend.exception.ProveedorDesconocidoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios para {@link TpvProveedorRegistry}: resolución de adaptador
 * por nombre, fallback al genérico y rechazo de proveedores desconocidos.
 */
class TpvProveedorRegistryTest {

    private TpvProveedorAdapter genericoAdapter;
    private TpvProveedorRegistry registry;

    @BeforeEach
    void setUp() {
        genericoAdapter = mock(TpvProveedorAdapter.class);
        when(genericoAdapter.getNombre()).thenReturn("generico");
        registry = new TpvProveedorRegistry(List.of(genericoAdapter));
    }

    // ─── resolverAdaptador() — fallback genérico ─────────────────────────────

    @Nested
    @DisplayName("resolverAdaptador() — fallback al genérico")
    class FallbackGenerico {

        @Test
        @DisplayName("devuelve el adaptador genérico cuando nombreProveedor es null")
        void devuelveGenericoSiNull() {
            TpvProveedorAdapter resultado = registry.resolverAdaptador(null);

            assertThat(resultado).isSameAs(genericoAdapter);
        }

        @Test
        @DisplayName("devuelve el adaptador genérico cuando nombreProveedor es blank")
        void devuelveGenericoSiBlank() {
            TpvProveedorAdapter resultado = registry.resolverAdaptador("   ");

            assertThat(resultado).isSameAs(genericoAdapter);
        }

        @Test
        @DisplayName("devuelve el adaptador genérico cuando nombreProveedor es 'generico'")
        void devuelveGenericoSiNombreGenerico() {
            TpvProveedorAdapter resultado = registry.resolverAdaptador("generico");

            assertThat(resultado).isSameAs(genericoAdapter);
        }
    }

    // ─── resolverAdaptador() — proveedor desconocido ─────────────────────────

    @Nested
    @DisplayName("resolverAdaptador() — proveedor desconocido")
    class ProveedorDesconocido {

        @Test
        @DisplayName("lanza ProveedorDesconocidoException para un nombre no registrado")
        void lanzaExcepcionParaNombreDesconocido() {
            assertThatThrownBy(() -> registry.resolverAdaptador("square"))
                    .isInstanceOf(ProveedorDesconocidoException.class)
                    .hasMessageContaining("square");
        }
    }
}
