package com.ares.backend.controller;

import com.ares.backend.dto.CrearNegocioRequest;
import com.ares.backend.dto.NegocioAdminResponse;
import com.ares.backend.dto.NegocioCreadoResponse;
import com.ares.backend.dto.SignupCodeResponse;
import com.ares.backend.entity.Negocio;
import com.ares.backend.entity.NegocioSignupCode;
import com.ares.backend.service.NegocioAdminService;
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
 * Tests unitarios para NegocioAdminController: mapeo de las respuestas del
 * servicio a los DTOs administrativos y el código de estado HTTP de cada uno
 * de los 5 endpoints. El servicio se mockea (sin Spring context, sin
 * MockMvc): la autenticación/autorización real (hasRole("PLATFORM_ADMIN"))
 * ya está cubierta por AdminContainmentIntegrationTest.
 */
class NegocioAdminControllerTest {

    private Negocio negocioConId(Long id, String nombre) {
        Negocio negocio = new Negocio(nombre, null);
        negocio.setId(id);
        return negocio;
    }

    private NegocioSignupCode codigoDe(Negocio negocio, Long codigoId, String codigo) {
        NegocioSignupCode signupCode = new NegocioSignupCode(negocio, codigo);
        signupCode.setId(codigoId);
        return signupCode;
    }

    @Nested
    @DisplayName("POST /api/admin/negocios")
    class CrearNegocio {

        @Test
        @DisplayName("crea el negocio y devuelve 201 con el negocio + su primer código")
        void crearNegocio_devuelve201ConNegocioYCodigo() {
            NegocioAdminService service = mock(NegocioAdminService.class);
            NegocioAdminController controller = new NegocioAdminController(service);

            Negocio negocio = negocioConId(1L, "Cocina Central");
            NegocioSignupCode primerCodigo = codigoDe(negocio, 10L, "PRIMERCOD1");
            when(service.crearNegocio("Cocina Central", "hola@cocina.com")).thenReturn(primerCodigo);

            CrearNegocioRequest request = new CrearNegocioRequest("Cocina Central", "hola@cocina.com");
            ResponseEntity<NegocioCreadoResponse> response = controller.crearNegocio(request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody().getNegocio().getId()).isEqualTo(1L);
            assertThat(response.getBody().getSignupCode().getCodigo()).isEqualTo("PRIMERCOD1");
        }

        @Test
        @DisplayName("con otro negocio devuelve otro id/código (triangulación)")
        void crearNegocio_conOtroNegocio_devuelveOtrosValores() {
            NegocioAdminService service = mock(NegocioAdminService.class);
            NegocioAdminController controller = new NegocioAdminController(service);

            Negocio negocio = negocioConId(2L, "Otra Cocina");
            NegocioSignupCode primerCodigo = codigoDe(negocio, 20L, "SEGUNDOCOD2");
            when(service.crearNegocio("Otra Cocina", null)).thenReturn(primerCodigo);

            CrearNegocioRequest request = new CrearNegocioRequest("Otra Cocina", null);
            ResponseEntity<NegocioCreadoResponse> response = controller.crearNegocio(request);

            assertThat(response.getBody().getNegocio().getId()).isEqualTo(2L);
            assertThat(response.getBody().getSignupCode().getCodigo()).isEqualTo("SEGUNDOCOD2");
        }
    }

    @Nested
    @DisplayName("POST /api/admin/negocios/{id}/signup-codes")
    class GenerarCodigo {

        @Test
        @DisplayName("delega en generarCodigo(negocioId) y devuelve 201 con el código nuevo")
        void generarCodigo_devuelve201ConElCodigoNuevo() {
            NegocioAdminService service = mock(NegocioAdminService.class);
            NegocioAdminController controller = new NegocioAdminController(service);

            Negocio negocio = negocioConId(3L, "Negocio Existente");
            NegocioSignupCode nuevoCodigo = codigoDe(negocio, 30L, "CODIGOADICIONAL3");
            when(service.generarCodigo(3L)).thenReturn(nuevoCodigo);

            ResponseEntity<SignupCodeResponse> response = controller.generarCodigo(3L);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody().getCodigo()).isEqualTo("CODIGOADICIONAL3");
            assertThat(response.getBody().getNegocioId()).isEqualTo(3L);
            verify(service).generarCodigo(3L);
        }
    }

    @Nested
    @DisplayName("GET /api/admin/negocios")
    class ListarNegocios {

        @Test
        @DisplayName("devuelve 200 con la lista de negocios mapeados a NegocioAdminResponse")
        void listarNegocios_devuelve200ConLaLista() {
            NegocioAdminService service = mock(NegocioAdminService.class);
            NegocioAdminController controller = new NegocioAdminController(service);

            when(service.listarNegocios()).thenReturn(List.of(
                    negocioConId(1L, "Negocio Uno"),
                    negocioConId(2L, "Negocio Dos")
            ));

            ResponseEntity<List<NegocioAdminResponse>> response = controller.listarNegocios();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(2);
            assertThat(response.getBody().get(0).getNombre()).isEqualTo("Negocio Uno");
            assertThat(response.getBody().get(1).getNombre()).isEqualTo("Negocio Dos");
        }

        @Test
        @DisplayName("devuelve 200 con lista vacía cuando no hay negocios (triangulación)")
        void listarNegocios_sinNegocios_devuelveListaVacia() {
            NegocioAdminService service = mock(NegocioAdminService.class);
            NegocioAdminController controller = new NegocioAdminController(service);

            when(service.listarNegocios()).thenReturn(List.of());

            ResponseEntity<List<NegocioAdminResponse>> response = controller.listarNegocios();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEmpty();
        }
    }

    @Nested
    @DisplayName("GET /api/admin/negocios/{id}/signup-codes")
    class ListarCodigos {

        @Test
        @DisplayName("devuelve 200 con los códigos del negocio mapeados a SignupCodeResponse")
        void listarCodigos_devuelve200ConLosCodigos() {
            NegocioAdminService service = mock(NegocioAdminService.class);
            NegocioAdminController controller = new NegocioAdminController(service);

            Negocio negocio = negocioConId(4L, "Negocio Con Codigos");
            when(service.listarCodigos(4L)).thenReturn(List.of(
                    codigoDe(negocio, 40L, "COD_A"),
                    codigoDe(negocio, 41L, "COD_B")
            ));

            ResponseEntity<List<SignupCodeResponse>> response = controller.listarCodigos(4L);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(2);
            assertThat(response.getBody().get(0).getCodigo()).isEqualTo("COD_A");
            assertThat(response.getBody().get(1).getCodigo()).isEqualTo("COD_B");
        }
    }

    @Nested
    @DisplayName("POST /api/admin/signup-codes/{id}/revoke")
    class RevocarCodigo {

        @Test
        @DisplayName("delega en revocarCodigo(id) y devuelve 200 con el estado post-revocación")
        void revocarCodigo_devuelve200ConElEstadoPostRevocacion() {
            NegocioAdminService service = mock(NegocioAdminService.class);
            NegocioAdminController controller = new NegocioAdminController(service);

            Negocio negocio = negocioConId(5L, "Negocio Revoca");
            NegocioSignupCode revocado = codigoDe(negocio, 50L, "COD_REVOCADO");
            revocado.setActivo(false);
            when(service.revocarCodigo(50L)).thenReturn(revocado);

            ResponseEntity<SignupCodeResponse> response = controller.revocarCodigo(50L);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getActivo()).isFalse();
            verify(service).revocarCodigo(50L);
        }
    }
}
