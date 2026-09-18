package com.ares.backend.controller;

import com.ares.backend.entity.Negocio;
import com.ares.backend.entity.TpvApiKey;
import com.ares.backend.entity.Usuario;
import com.ares.backend.service.TpvApiKeyAdminService;
import com.ares.backend.service.TpvApiKeyEmitida;
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
 * Tests unitarios para TpvApiKeyAdminController: mapeo de las respuestas del
 * servicio a los DTOs administrativos y el código de estado HTTP de cada uno
 * de los 3 endpoints. El servicio se mockea (sin Spring context, sin
 * MockMvc): la autenticación/autorización real (hasRole("PLATFORM_ADMIN"))
 * está cubierta por TpvApiKeyAdminEndpointsAuthMatrixIntegrationTest, mismo
 * patrón que {@link NegocioAdminControllerTest}.
 */
class TpvApiKeyAdminControllerTest {

    private Negocio negocioConId(Long id, String nombre) {
        Negocio negocio = new Negocio(nombre, nombre + "@test.com");
        negocio.setId(id);
        return negocio;
    }

    private Usuario usuarioSistema(Long id) {
        Usuario usuario = new Usuario("tpv-system", "sentinel", false);
        usuario.setId(id);
        return usuario;
    }

    private TpvApiKey claveConId(Long id, Negocio negocio, String prefijo, Usuario usuarioSistema) {
        TpvApiKey clave = new TpvApiKey(negocio, prefijo, "hash-" + prefijo, usuarioSistema);
        clave.setId(id);
        return clave;
    }

    @Nested
    @DisplayName("POST /api/admin/negocios/{id}/tpv-api-key")
    class EmitirClave {

        @Test
        @DisplayName("delega en emitir(negocioId) y devuelve 201 con la key en claro")
        void emitir_devuelve201ConLaKeyEnClaro() {
            TpvApiKeyAdminService service = mock(TpvApiKeyAdminService.class);
            TpvApiKeyAdminController controller = new TpvApiKeyAdminController(service);

            Negocio negocio = negocioConId(1L, "Cocina Central");
            Usuario usuarioSistema = usuarioSistema(500L);
            TpvApiKey clave = claveConId(10L, negocio, "pfx-uno", usuarioSistema);
            TpvApiKeyEmitida emitida = new TpvApiKeyEmitida(clave, "omf_tpv_pfx-uno_secreto-en-claro");
            when(service.emitir(1L)).thenReturn(emitida);

            ResponseEntity<com.ares.backend.dto.TpvApiKeyEmitidaResponse> response = controller.emitir(1L);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody().getId()).isEqualTo(10L);
            assertThat(response.getBody().getNegocioId()).isEqualTo(1L);
            assertThat(response.getBody().getPrefijo()).isEqualTo("pfx-uno");
            assertThat(response.getBody().getClaveEnClaro()).isEqualTo("omf_tpv_pfx-uno_secreto-en-claro");
            verify(service).emitir(1L);
        }

        @Test
        @DisplayName("con otro negocio devuelve otra credencial (triangulación)")
        void emitir_conOtroNegocio_devuelveOtraCredencial() {
            TpvApiKeyAdminService service = mock(TpvApiKeyAdminService.class);
            TpvApiKeyAdminController controller = new TpvApiKeyAdminController(service);

            Negocio negocio = negocioConId(2L, "Otra Cocina");
            Usuario usuarioSistema = usuarioSistema(600L);
            TpvApiKey clave = claveConId(20L, negocio, "pfx-dos", usuarioSistema);
            TpvApiKeyEmitida emitida = new TpvApiKeyEmitida(clave, "omf_tpv_pfx-dos_otro-secreto");
            when(service.emitir(2L)).thenReturn(emitida);

            ResponseEntity<com.ares.backend.dto.TpvApiKeyEmitidaResponse> response = controller.emitir(2L);

            assertThat(response.getBody().getId()).isEqualTo(20L);
            assertThat(response.getBody().getNegocioId()).isEqualTo(2L);
            assertThat(response.getBody().getClaveEnClaro()).isEqualTo("omf_tpv_pfx-dos_otro-secreto");
        }
    }

    @Nested
    @DisplayName("GET /api/admin/negocios/{id}/tpv-api-keys")
    class ListarClaves {

        @Test
        @DisplayName("devuelve 200 con las credenciales mapeadas a TpvApiKeyResponse, sin el secreto")
        void listar_devuelve200ConLasCredenciales() {
            TpvApiKeyAdminService service = mock(TpvApiKeyAdminService.class);
            TpvApiKeyAdminController controller = new TpvApiKeyAdminController(service);

            Negocio negocio = negocioConId(3L, "Negocio Con Claves");
            Usuario usuarioSistema = usuarioSistema(700L);
            TpvApiKey c1 = claveConId(30L, negocio, "pfx-a", usuarioSistema);
            TpvApiKey c2 = claveConId(31L, negocio, "pfx-b", usuarioSistema);
            when(service.listar(3L)).thenReturn(List.of(c2, c1));

            ResponseEntity<List<com.ares.backend.dto.TpvApiKeyResponse>> response = controller.listar(3L);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(2);
            assertThat(response.getBody().get(0).getPrefijo()).isEqualTo("pfx-b");
            assertThat(response.getBody().get(1).getPrefijo()).isEqualTo("pfx-a");
        }

        @Test
        @DisplayName("devuelve 200 con lista vacía cuando el negocio no tiene credenciales (triangulación)")
        void listar_sinCredenciales_devuelveListaVacia() {
            TpvApiKeyAdminService service = mock(TpvApiKeyAdminService.class);
            TpvApiKeyAdminController controller = new TpvApiKeyAdminController(service);

            when(service.listar(4L)).thenReturn(List.of());

            ResponseEntity<List<com.ares.backend.dto.TpvApiKeyResponse>> response = controller.listar(4L);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEmpty();
        }
    }

    @Nested
    @DisplayName("POST /api/admin/tpv-api-keys/{id}/revoke")
    class RevocarClave {

        @Test
        @DisplayName("delega en revocar(id) y devuelve 200 con el estado post-revocación")
        void revocar_devuelve200ConElEstadoPostRevocacion() {
            TpvApiKeyAdminService service = mock(TpvApiKeyAdminService.class);
            TpvApiKeyAdminController controller = new TpvApiKeyAdminController(service);

            Negocio negocio = negocioConId(5L, "Negocio Revoca");
            Usuario usuarioSistema = usuarioSistema(800L);
            TpvApiKey clave = claveConId(50L, negocio, "pfx-revocada", usuarioSistema);
            clave.revocar();
            when(service.revocar(50L)).thenReturn(clave);

            ResponseEntity<com.ares.backend.dto.TpvApiKeyResponse> response = controller.revocar(50L);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getActiva()).isFalse();
            verify(service).revocar(50L);
        }
    }
}
