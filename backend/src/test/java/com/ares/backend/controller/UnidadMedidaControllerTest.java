package com.ares.backend.controller;

import com.ares.backend.dto.UnidadMedidaResponse;
import com.ares.backend.entity.TipoUnidad;
import com.ares.backend.entity.UnidadMedida;
import com.ares.backend.repository.UnidadMedidaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios para UnidadMedidaController.
 * El catálogo es global (no scoped por negocio): no requiere ningún negocioId
 * de contexto, a diferencia del resto de controllers tenant-owned.
 */
class UnidadMedidaControllerTest {

    @Test
    @DisplayName("obtenerTodas() devuelve el catálogo completo sin filtrar por negocio")
    void obtenerTodas_devuelveCatalogoCompleto() {
        UnidadMedidaRepository repository = mock(UnidadMedidaRepository.class);
        UnidadMedidaController controller = new UnidadMedidaController(repository);

        UnidadMedida kg = new UnidadMedida();
        kg.setId(1L);
        kg.setCodigo("kg");
        kg.setNombre("Kilogramo");
        kg.setTipo(TipoUnidad.MASA);
        kg.setFactorABase(1000.0);

        UnidadMedida ud = new UnidadMedida();
        ud.setId(2L);
        ud.setCodigo("ud");
        ud.setNombre("Unidad");
        ud.setTipo(TipoUnidad.UNIDAD);
        ud.setFactorABase(1.0);

        when(repository.findAll()).thenReturn(List.of(kg, ud));

        ResponseEntity<List<UnidadMedidaResponse>> response = controller.obtenerTodas();

        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody().get(0).getCodigo()).isEqualTo("kg");
        assertThat(response.getBody().get(0).getTipo()).isEqualTo(TipoUnidad.MASA);
        assertThat(response.getBody().get(0).getFactorABase()).isEqualTo(1000.0);
    }
}
