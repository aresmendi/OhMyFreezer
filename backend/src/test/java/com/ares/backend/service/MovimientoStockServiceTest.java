package com.ares.backend.service;

import com.ares.backend.dto.MovimientoStockResponse;
import com.ares.backend.entity.Ingrediente;
import com.ares.backend.entity.MovimientoStock;
import com.ares.backend.repository.MovimientoStockRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MovimientoStockServiceTest {

    @Mock private MovimientoStockRepository movimientoStockRepository;

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

    // ─── registrarMovimiento() ──────────────────────────────────────────────

    @Nested
    @DisplayName("registrarMovimiento()")
    class RegistrarMovimiento {

        @Test
        @DisplayName("persiste un movimiento con los datos recibidos")
        void persisteMovimiento() {
            Ingrediente ing = ingrediente(1L);

            movimientoStockService.registrarMovimiento(ing, 10.0, 7.0, "SALIDA", "Elaboración", 3L);

            ArgumentCaptor<MovimientoStock> captor = ArgumentCaptor.forClass(MovimientoStock.class);
            verify(movimientoStockRepository).save(captor.capture());

            MovimientoStock guardado = captor.getValue();
            assertThat(guardado.getIngrediente()).isEqualTo(ing);
            assertThat(guardado.getCantidadAnterior()).isEqualTo(10.0);
            assertThat(guardado.getCantidadNueva()).isEqualTo(7.0);
            assertThat(guardado.getTipo()).isEqualTo("SALIDA");
            assertThat(guardado.getMotivo()).isEqualTo("Elaboración");
        }
    }

    // ─── obtenerMovimientos() ───────────────────────────────────────────────

    @Nested
    @DisplayName("obtenerMovimientos()")
    class ObtenerMovimientos {

        @Test
        @DisplayName("mapea las entidades a MovimientoStockResponse")
        void mapeaAResponse() {
            Ingrediente ing = ingrediente(1L);
            MovimientoStock mov = new MovimientoStock(ing, 10.0, 7.0, "SALIDA", "Elaboración", 3L);
            mov.setId(1L);

            LocalDateTime desde = LocalDateTime.of(2026, 6, 1, 0, 0);
            LocalDateTime hasta = LocalDateTime.of(2026, 6, 30, 23, 59);

            when(movimientoStockRepository.findByIngredientesAndFechaBetween(any(), eq(desde), eq(hasta)))
                    .thenReturn(List.of(mov));

            List<MovimientoStockResponse> result =
                    movimientoStockService.obtenerMovimientos(List.of(1L), desde, hasta);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getIngredienteId()).isEqualTo(1L);
            assertThat(result.get(0).getTipo()).isEqualTo("SALIDA");
        }

        @Test
        @DisplayName("devuelve lista vacía si no hay movimientos en el rango")
        void devuelveVacia() {
            LocalDateTime desde = LocalDateTime.of(2026, 6, 1, 0, 0);
            LocalDateTime hasta = LocalDateTime.of(2026, 6, 30, 23, 59);

            when(movimientoStockRepository.findByIngredientesAndFechaBetween(any(), eq(desde), eq(hasta)))
                    .thenReturn(List.of());

            assertThat(movimientoStockService.obtenerMovimientos(List.of(1L), desde, hasta)).isEmpty();
        }
    }
}
