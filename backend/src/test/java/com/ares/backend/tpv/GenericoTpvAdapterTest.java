package com.ares.backend.tpv;

import com.ares.backend.dto.VentaTpvDto;
import com.ares.backend.entity.TipoEventoTpv;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests unitarios para {@link GenericoTpvAdapter}: parseo de payloads JSON
 * genéricos, validación de campos obligatorios y opcionales.
 */
class GenericoTpvAdapterTest {

    private GenericoTpvAdapter adapter;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        adapter = new GenericoTpvAdapter(objectMapper);
    }

    // ─── getNombre() ────────────────────────────────────────────────────────

    @Test
    @DisplayName("getNombre() devuelve 'generico'")
    void getNombreDevuelveGenerico() {
        assertThat(adapter.getNombre()).isEqualTo("generico");
    }

    // ─── parsear() — casos OK ───────────────────────────────────────────────

    @Nested
    @DisplayName("parsear() — casos válidos")
    class ParsearOk {

        @Test
        @DisplayName("parsea una VENTA con todos los campos presentes")
        void parseaVentaCompleta() {
            String payload = """
                    {
                      "tipo": "VENTA",
                      "externalId": "tx-001",
                      "externalIdOriginal": null,
                      "skuTpv": "CAFE-001",
                      "cantidad": 2,
                      "fechaTpv": "2026-11-11T14:30:00"
                    }
                    """;

            VentaTpvDto dto = adapter.parsear(payload);

            assertThat(dto.tipo()).isEqualTo(TipoEventoTpv.VENTA);
            assertThat(dto.externalId()).isEqualTo("tx-001");
            assertThat(dto.externalIdOriginal()).isNull();
            assertThat(dto.skuTpv()).isEqualTo("CAFE-001");
            assertThat(dto.cantidad()).isEqualTo(2);
            assertThat(dto.fechaTpv()).isEqualTo(LocalDateTime.of(2026, 11, 11, 14, 30, 0));
        }

        @Test
        @DisplayName("parsea una ANULACION con externalIdOriginal presente")
        void parseaAnulacionConExternalIdOriginal() {
            String payload = """
                    {
                      "tipo": "ANULACION",
                      "externalId": "tx-002",
                      "externalIdOriginal": "tx-001",
                      "skuTpv": "CAFE-001",
                      "cantidad": 1,
                      "fechaTpv": null
                    }
                    """;

            VentaTpvDto dto = adapter.parsear(payload);

            assertThat(dto.tipo()).isEqualTo(TipoEventoTpv.ANULACION);
            assertThat(dto.externalId()).isEqualTo("tx-002");
            assertThat(dto.externalIdOriginal()).isEqualTo("tx-001");
            assertThat(dto.fechaTpv()).isNull();
        }

        @Test
        @DisplayName("parsea correctamente cuando fechaTpv está ausente en el JSON")
        void parseaFechaTpvOpcional() {
            String payload = """
                    {
                      "tipo": "VENTA",
                      "externalId": "tx-003",
                      "skuTpv": "CAFE-002",
                      "cantidad": 3
                    }
                    """;

            VentaTpvDto dto = adapter.parsear(payload);

            assertThat(dto.fechaTpv()).isNull();
            assertThat(dto.externalIdOriginal()).isNull();
        }
    }

    // ─── parsear() — casos de error ─────────────────────────────────────────

    @Nested
    @DisplayName("parsear() — casos inválidos")
    class ParsearError {

        @Test
        @DisplayName("lanza IllegalArgumentException si el JSON es inválido")
        void lanzaExcepcionSiJsonInvalido() {
            assertThatThrownBy(() -> adapter.parsear("esto no es json {{{"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("JSON inválido");
        }

        @Test
        @DisplayName("lanza IllegalArgumentException si falta el campo 'tipo'")
        void lanzaExcepcionSiFaltaTipo() {
            String payload = """
                    {
                      "externalId": "tx-001",
                      "skuTpv": "CAFE-001",
                      "cantidad": 1
                    }
                    """;

            assertThatThrownBy(() -> adapter.parsear(payload))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("tipo");
        }

        @Test
        @DisplayName("lanza IllegalArgumentException si falta el campo 'externalId'")
        void lanzaExcepcionSiFaltaExternalId() {
            String payload = """
                    {
                      "tipo": "VENTA",
                      "skuTpv": "CAFE-001",
                      "cantidad": 1
                    }
                    """;

            assertThatThrownBy(() -> adapter.parsear(payload))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("externalId");
        }

        @Test
        @DisplayName("lanza IllegalArgumentException si falta el campo 'skuTpv'")
        void lanzaExcepcionSiFaltaSkuTpv() {
            String payload = """
                    {
                      "tipo": "VENTA",
                      "externalId": "tx-001",
                      "cantidad": 1
                    }
                    """;

            assertThatThrownBy(() -> adapter.parsear(payload))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("skuTpv");
        }

        @Test
        @DisplayName("lanza IllegalArgumentException si falta el campo 'cantidad'")
        void lanzaExcepcionSiFaltaCantidad() {
            String payload = """
                    {
                      "tipo": "VENTA",
                      "externalId": "tx-001",
                      "skuTpv": "CAFE-001"
                    }
                    """;

            assertThatThrownBy(() -> adapter.parsear(payload))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cantidad");
        }
    }
}
