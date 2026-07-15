package com.ares.backend.service;

import com.ares.backend.config.SecurityUtils;
import com.ares.backend.dto.MovimientoStockResponse;
import com.ares.backend.entity.Ingrediente;
import com.ares.backend.entity.MovimientoStock;
import com.ares.backend.entity.Negocio;
import com.ares.backend.repository.MovimientoStockRepository;
import com.ares.backend.repository.NegocioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MovimientoStockServiceTest {

    @Mock private MovimientoStockRepository movimientoStockRepository;
    @Mock private NegocioRepository negocioRepository;

    @InjectMocks
    private MovimientoStockService movimientoStockService;

    private Ingrediente ingrediente(Long id) {
        Ingrediente i = new Ingrediente();
        i.setId(id);
        i.setNombre("Tomate");
        i.setCantidad(10.0);
        i.setUnidadMedida("kg");
        i.setStockMinimo(5.0);
        i.setFechaActualizacion(LocalDateTime.now());
        return i;
    }

    private Negocio negocio(Long id) {
        Negocio n = new Negocio("negocio-" + id, "negocio" + id + "@test.com");
        n.setId(id);
        return n;
    }

    // ─── registrarMovimiento() ──────────────────────────────────────────────

    @Nested
    @DisplayName("registrarMovimiento()")
    class RegistrarMovimiento {

        @Test
        @DisplayName("persiste un movimiento con los datos recibidos y el negocio del caller")
        void persisteMovimiento() {
            Ingrediente ing = ingrediente(1L);
            Negocio negocio = negocio(10L);

            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getNegocioId).thenReturn(10L);
                when(negocioRepository.findById(10L)).thenReturn(Optional.of(negocio));

                movimientoStockService.registrarMovimiento(ing, 10.0, 7.0, "SALIDA", "Elaboración", 3L);

                ArgumentCaptor<MovimientoStock> captor = ArgumentCaptor.forClass(MovimientoStock.class);
                verify(movimientoStockRepository).save(captor.capture());

                MovimientoStock guardado = captor.getValue();
                assertThat(guardado.getIngrediente()).isEqualTo(ing);
                assertThat(guardado.getCantidadAnterior()).isEqualTo(10.0);
                assertThat(guardado.getCantidadNueva()).isEqualTo(7.0);
                assertThat(guardado.getTipo()).isEqualTo("SALIDA");
                assertThat(guardado.getMotivo()).isEqualTo("Elaboración");
                // Cierra el hueco DEFAULT 1: negocio_id se setea explícitamente, nunca por defecto de BD.
                assertThat(guardado.getNegocio()).isEqualTo(negocio);
            }
        }
    }

    // ─── obtenerMovimientos() ───────────────────────────────────────────────

    @Nested
    @DisplayName("obtenerMovimientos()")
    class ObtenerMovimientos {

        @Test
        @DisplayName("mapea las entidades a MovimientoStockResponse, scoped al negocio del caller")
        void mapeaAResponse() {
            Ingrediente ing = ingrediente(1L);
            MovimientoStock mov = new MovimientoStock(ing, 10.0, 7.0, "SALIDA", "Elaboración", 3L);
            mov.setId(1L);

            LocalDateTime desde = LocalDateTime.of(2026, 6, 1, 0, 0);
            LocalDateTime hasta = LocalDateTime.of(2026, 6, 30, 23, 59);

            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getNegocioId).thenReturn(10L);
                when(movimientoStockRepository.findByIngredientesAndFechaBetweenAndNegocioId(any(), eq(desde), eq(hasta), eq(10L)))
                        .thenReturn(List.of(mov));

                List<MovimientoStockResponse> result =
                        movimientoStockService.obtenerMovimientos(List.of(1L), desde, hasta);

                assertThat(result).hasSize(1);
                assertThat(result.get(0).getIngredienteId()).isEqualTo(1L);
                assertThat(result.get(0).getTipo()).isEqualTo("SALIDA");
            }
        }

        @Test
        @DisplayName("devuelve lista vacía si no hay movimientos en el rango")
        void devuelveVacia() {
            LocalDateTime desde = LocalDateTime.of(2026, 6, 1, 0, 0);
            LocalDateTime hasta = LocalDateTime.of(2026, 6, 30, 23, 59);

            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getNegocioId).thenReturn(10L);
                when(movimientoStockRepository.findByIngredientesAndFechaBetweenAndNegocioId(any(), eq(desde), eq(hasta), eq(10L)))
                        .thenReturn(List.of());

                assertThat(movimientoStockService.obtenerMovimientos(List.of(1L), desde, hasta)).isEmpty();
            }
        }

        @Test
        @DisplayName("CROSS-TENANT: pasar ids de ingredientes de otro negocio no devuelve sus movimientos")
        void noDevuelveMovimientosDeIngredientesDeOtroNegocio() {
            LocalDateTime desde = LocalDateTime.of(2026, 6, 1, 0, 0);
            LocalDateTime hasta = LocalDateTime.of(2026, 6, 30, 23, 59);

            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                // El caller pertenece al negocio 10; pasa ids de ingredientes que en
                // realidad son de otro negocio. El query scoped por negocioId=10
                // simplemente no los encuentra (no existe combinación id+negocio=10).
                mocked.when(SecurityUtils::getNegocioId).thenReturn(10L);
                when(movimientoStockRepository.findByIngredientesAndFechaBetweenAndNegocioId(
                        eq(List.of(999L)), eq(desde), eq(hasta), eq(10L)))
                        .thenReturn(List.of());

                List<MovimientoStockResponse> result =
                        movimientoStockService.obtenerMovimientos(List.of(999L), desde, hasta);

                assertThat(result).isEmpty();
            }
        }
    }
}
