package com.ares.backend.controller;

import com.ares.backend.dto.ResultadoIngestaResponse;
import com.ares.backend.entity.EstadoVentaTpv;
import com.ares.backend.exception.ProveedorDesconocidoException;
import com.ares.backend.tpv.VentaTpvIngestaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para {@link VentaTpvController}: verifica que el controller
 * delega en el servicio y mapea correctamente la respuesta. Sin Spring context
 * — mismo patrón que {@link TpvSkuMappingControllerTest}.
 */
class VentaTpvControllerTest {

    // ─── POST /api/tpv/ventas con proveedor OK ────────────────────────────────

    @Nested
    @DisplayName("POST /api/tpv/ventas")
    class Ingestar {

        @Test
        @DisplayName("devuelve 200 con body ResultadoIngestaResponse cuando el servicio procesa OK")
        void ingestar_proveedorOk_devuelve200ConBody() {
            VentaTpvIngestaService serviceMock = mock(VentaTpvIngestaService.class);
            VentaTpvController controller = new VentaTpvController(serviceMock);

            ResultadoIngestaResponse esperado = new ResultadoIngestaResponse(
                    42L, "ext-001", EstadoVentaTpv.PROCESADA, null);
            when(serviceMock.ingestar("{}", "generico")).thenReturn(esperado);

            ResponseEntity<ResultadoIngestaResponse> resp = controller.ingestar("{}", "generico");

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(resp.getBody()).isNotNull();
            assertThat(resp.getBody().estado()).isEqualTo(EstadoVentaTpv.PROCESADA);
            assertThat(resp.getBody().ventaTpvId()).isEqualTo(42L);
            verify(serviceMock).ingestar("{}", "generico");
        }

        @Test
        @DisplayName("sin header X-Tpv-Proveedor delega con proveedor 'generico' (defaultValue del controller)")
        void ingestar_sinHeaderProveedor_delegaConGenerico() {
            VentaTpvIngestaService serviceMock = mock(VentaTpvIngestaService.class);
            VentaTpvController controller = new VentaTpvController(serviceMock);

            ResultadoIngestaResponse esperado = new ResultadoIngestaResponse(
                    10L, "ext-002", EstadoVentaTpv.PROCESADA, null);
            when(serviceMock.ingestar(any(), eq("generico"))).thenReturn(esperado);

            // Simula que Spring inyecta el defaultValue "generico" cuando falta la cabecera
            ResponseEntity<ResultadoIngestaResponse> resp = controller.ingestar("{}", "generico");

            assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(serviceMock).ingestar(any(), eq("generico"));
        }

        @Test
        @DisplayName("proveedor desconocido que lanza ProveedorDesconocidoException se propaga al caller")
        void ingestar_proveedorDesconocido_propagaExcepcion() {
            VentaTpvIngestaService serviceMock = mock(VentaTpvIngestaService.class);
            VentaTpvController controller = new VentaTpvController(serviceMock);

            when(serviceMock.ingestar(any(), eq("desconocido")))
                    .thenThrow(new ProveedorDesconocidoException("desconocido"));

            assertThatThrownBy(() -> controller.ingestar("{}", "desconocido"))
                    .isInstanceOf(ProveedorDesconocidoException.class);
        }
    }
}
