package com.ares.backend.controller;

import com.ares.backend.dto.TpvSkuMappingRequest;
import com.ares.backend.dto.TpvSkuMappingResponse;
import com.ares.backend.entity.Negocio;
import com.ares.backend.entity.Receta;
import com.ares.backend.entity.TpvSkuMapping;
import com.ares.backend.service.TpvSkuMappingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios para {@link TpvSkuMappingController}: mapeo de las
 * respuestas del servicio a los DTOs y el código HTTP de cada endpoint.
 * El servicio se mockea (sin Spring context, sin MockMvc); mismo patrón
 * que {@link TpvApiKeyAdminControllerTest}.
 */
class TpvSkuMappingControllerTest {

    // ─── Helpers ────────────────────────────────────────────────────────────

    private Negocio negocio(Long id) {
        Negocio n = new Negocio();
        n.setId(id);
        return n;
    }

    private TpvSkuMapping mappingPendiente(Long id, Negocio negocio) {
        TpvSkuMapping m = new TpvSkuMapping(negocio, "SKU-" + id, "Producto " + id, null);
        m.setId(id);
        return m;
    }

    private TpvSkuMapping mappingMapeado(Long id, Negocio negocio, Long recetaId, String recetaNombre) {
        Receta receta = new Receta();
        receta.setId(recetaId);
        receta.setNombre(recetaNombre);
        TpvSkuMapping m = new TpvSkuMapping(negocio, "SKU-" + id, "Producto " + id, receta);
        m.setId(id);
        return m;
    }

    // ─── GET /api/tpv/sku-mappings ──────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/tpv/sku-mappings")
    class Listar {

        @Test
        @DisplayName("devuelve 200 con la lista de mapeos mapeada a TpvSkuMappingResponse")
        void listar_devuelve200ConLaLista() {
            TpvSkuMappingService service = mock(TpvSkuMappingService.class);
            TpvSkuMappingController controller = new TpvSkuMappingController(service);

            Negocio negocio = negocio(1L);
            TpvSkuMapping m1 = mappingPendiente(10L, negocio);
            TpvSkuMapping m2 = mappingMapeado(11L, negocio, 20L, "Pasta Carbonara");
            when(service.listar()).thenReturn(List.of(m2, m1));

            ResponseEntity<List<TpvSkuMappingResponse>> response = controller.listar();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(2);
            assertThat(response.getBody().get(0).getId()).isEqualTo(11L);
            assertThat(response.getBody().get(0).isMapeado()).isTrue();
            assertThat(response.getBody().get(0).getRecetaId()).isEqualTo(20L);
            assertThat(response.getBody().get(0).getRecetaNombre()).isEqualTo("Pasta Carbonara");
            assertThat(response.getBody().get(1).getId()).isEqualTo(10L);
            assertThat(response.getBody().get(1).isMapeado()).isFalse();
            assertThat(response.getBody().get(1).getRecetaId()).isNull();
            verify(service).listar();
        }

        @Test
        @DisplayName("devuelve 200 con lista vacía cuando no hay mapeos (triangulación)")
        void listar_sinMapeos_devuelveListaVacia() {
            TpvSkuMappingService service = mock(TpvSkuMappingService.class);
            TpvSkuMappingController controller = new TpvSkuMappingController(service);

            when(service.listar()).thenReturn(List.of());

            ResponseEntity<List<TpvSkuMappingResponse>> response = controller.listar();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEmpty();
        }
    }

    // ─── PUT /api/tpv/sku-mappings/{id} ─────────────────────────────────────

    @Nested
    @DisplayName("PUT /api/tpv/sku-mappings/{id}")
    class AsignarReceta {

        @Test
        @DisplayName("delega en asignarReceta() y devuelve 200 con el mapeo actualizado")
        void asignarReceta_devuelve200ConMappingActualizado() {
            TpvSkuMappingService service = mock(TpvSkuMappingService.class);
            TpvSkuMappingController controller = new TpvSkuMappingController(service);

            Negocio negocio = negocio(1L);
            TpvSkuMapping mappingActualizado = mappingMapeado(10L, negocio, 20L, "Ensalada César");
            when(service.asignarReceta(10L, 20L)).thenReturn(mappingActualizado);

            TpvSkuMappingRequest request = new TpvSkuMappingRequest(20L);
            ResponseEntity<TpvSkuMappingResponse> response = controller.asignarReceta(10L, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getId()).isEqualTo(10L);
            assertThat(response.getBody().isMapeado()).isTrue();
            assertThat(response.getBody().getRecetaId()).isEqualTo(20L);
            assertThat(response.getBody().getRecetaNombre()).isEqualTo("Ensalada César");
            verify(service).asignarReceta(10L, 20L);
        }

        @Test
        @DisplayName("con otro ID devuelve la receta correspondiente (triangulación)")
        void asignarReceta_conOtroId_devuelveOtraReceta() {
            TpvSkuMappingService service = mock(TpvSkuMappingService.class);
            TpvSkuMappingController controller = new TpvSkuMappingController(service);

            Negocio negocio = negocio(2L);
            TpvSkuMapping mappingActualizado = mappingMapeado(30L, negocio, 40L, "Gazpacho");
            when(service.asignarReceta(30L, 40L)).thenReturn(mappingActualizado);

            TpvSkuMappingRequest request = new TpvSkuMappingRequest(40L);
            ResponseEntity<TpvSkuMappingResponse> response = controller.asignarReceta(30L, request);

            assertThat(response.getBody().getId()).isEqualTo(30L);
            assertThat(response.getBody().getRecetaId()).isEqualTo(40L);
            assertThat(response.getBody().getRecetaNombre()).isEqualTo("Gazpacho");
        }
    }

    // ─── DELETE /api/tpv/sku-mappings/{id}/receta ───────────────────────────

    @Nested
    @DisplayName("DELETE /api/tpv/sku-mappings/{id}/receta")
    class Desmapear {

        @Test
        @DisplayName("delega en desmapear() y devuelve 204 NO_CONTENT")
        void desmapear_devuelve204() {
            TpvSkuMappingService service = mock(TpvSkuMappingService.class);
            TpvSkuMappingController controller = new TpvSkuMappingController(service);

            Negocio negocio = negocio(1L);
            TpvSkuMapping mappingDesmapeado = mappingPendiente(10L, negocio);
            when(service.desmapear(10L)).thenReturn(mappingDesmapeado);

            ResponseEntity<Void> response = controller.desmapear(10L);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            assertThat(response.getBody()).isNull();
            verify(service).desmapear(10L);
        }

        @Test
        @DisplayName("con otro ID también devuelve 204 (triangulación)")
        void desmapear_conOtroId_devuelve204() {
            TpvSkuMappingService service = mock(TpvSkuMappingService.class);
            TpvSkuMappingController controller = new TpvSkuMappingController(service);

            Negocio negocio = negocio(2L);
            when(service.desmapear(30L)).thenReturn(mappingPendiente(30L, negocio));

            ResponseEntity<Void> response = controller.desmapear(30L);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            verify(service).desmapear(30L);
        }
    }
}
