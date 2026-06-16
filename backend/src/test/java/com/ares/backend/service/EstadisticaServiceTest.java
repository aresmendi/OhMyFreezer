package com.ares.backend.service;

import com.ares.backend.dto.EstadisticaRecetaResponse;
import com.ares.backend.entity.Receta;
import com.ares.backend.entity.RegistroUsoReceta;
import com.ares.backend.entity.Usuario;
import com.ares.backend.repository.RecetaRepository;
import com.ares.backend.repository.RegistroUsoRecetaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EstadisticaServiceTest {

    @Mock private RegistroUsoRecetaRepository registroUsoRecetaRepository;
    @Mock private RecetaRepository recetaRepository;

    @InjectMocks
    private EstadisticaService estadisticaService;

    private final LocalDateTime inicio = LocalDateTime.of(2026, 6, 1, 0, 0);
    private final LocalDateTime fin = LocalDateTime.of(2026, 6, 30, 23, 59);

    // ─── Helpers ────────────────────────────────────────────────────────────

    private Receta receta(Long id, String nombre) {
        Receta r = new Receta();
        r.setId(id);
        r.setNombre(nombre);
        r.setDescripcion("desc");
        r.setFechaCreacion(LocalDateTime.now());
        r.setPasos(new ArrayList<>());
        r.setIngredientes(new ArrayList<>());
        return r;
    }

    private RegistroUsoReceta registro(Receta receta, LocalDateTime fecha, boolean completada) {
        RegistroUsoReceta reg = new RegistroUsoReceta();
        reg.setReceta(receta);
        reg.setUsuario(new Usuario());
        reg.setFechaElaboracion(fecha);
        reg.setCompletada(completada);
        return reg;
    }

    // ─── obtenerEstadisticasReceta() ────────────────────────────────────────

    @Nested
    @DisplayName("obtenerEstadisticasReceta()")
    class ObtenerEstadisticasReceta {

        @Test
        @DisplayName("cuenta total y completadas, y agrupa solo las completadas por fecha")
        void cuentaYAgrupa() {
            Receta receta = receta(1L, "Pizza");
            // 3 elaboraciones: 2 completadas el día 5, 1 fallida el día 6
            List<RegistroUsoReceta> registros = List.of(
                    registro(receta, LocalDateTime.of(2026, 6, 5, 12, 0), true),
                    registro(receta, LocalDateTime.of(2026, 6, 5, 18, 0), true),
                    registro(receta, LocalDateTime.of(2026, 6, 6, 12, 0), false)
            );

            when(recetaRepository.findById(1L)).thenReturn(Optional.of(receta));
            when(registroUsoRecetaRepository.findByRecetaIdAndFechaElaboracionBetween(1L, inicio, fin))
                    .thenReturn(registros);

            EstadisticaRecetaResponse response =
                    estadisticaService.obtenerEstadisticasReceta(1L, inicio, fin);

            assertThat(response.getRecetaNombre()).isEqualTo("Pizza");
            assertThat(response.getTotalElaboraciones()).isEqualTo(3);
            assertThat(response.getElaboracionesCompletadas()).isEqualTo(2);
            // Solo hay datos del día 5 (las completadas); el día 6 fallida no cuenta
            assertThat(response.getDatos()).hasSize(1);
            assertThat(response.getDatos().get(0).getUsos()).isEqualTo(2);
        }

        @Test
        @DisplayName("devuelve datos vacíos si no hay registros en el rango")
        void sinRegistros() {
            Receta receta = receta(1L, "Pizza");
            when(recetaRepository.findById(1L)).thenReturn(Optional.of(receta));
            when(registroUsoRecetaRepository.findByRecetaIdAndFechaElaboracionBetween(1L, inicio, fin))
                    .thenReturn(List.of());

            EstadisticaRecetaResponse response =
                    estadisticaService.obtenerEstadisticasReceta(1L, inicio, fin);

            assertThat(response.getTotalElaboraciones()).isZero();
            assertThat(response.getElaboracionesCompletadas()).isZero();
            assertThat(response.getDatos()).isEmpty();
        }

        @Test
        @DisplayName("lanza excepción si la receta no existe")
        void recetaNoExiste() {
            when(recetaRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> estadisticaService.obtenerEstadisticasReceta(99L, inicio, fin))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("ordena los datos por fecha ascendente")
        void ordenaPorFecha() {
            Receta receta = receta(1L, "Pizza");
            // Llegan desordenadas (día 10 antes que día 3)
            List<RegistroUsoReceta> registros = List.of(
                    registro(receta, LocalDateTime.of(2026, 6, 10, 12, 0), true),
                    registro(receta, LocalDateTime.of(2026, 6, 3, 12, 0), true)
            );

            when(recetaRepository.findById(1L)).thenReturn(Optional.of(receta));
            when(registroUsoRecetaRepository.findByRecetaIdAndFechaElaboracionBetween(1L, inicio, fin))
                    .thenReturn(registros);

            EstadisticaRecetaResponse response =
                    estadisticaService.obtenerEstadisticasReceta(1L, inicio, fin);

            assertThat(response.getDatos()).hasSize(2);
            assertThat(response.getDatos().get(0).getFecha())
                    .isBefore(response.getDatos().get(1).getFecha());
        }
    }

    // ─── obtenerEstadisticasTodasRecetas() ──────────────────────────────────

    @Nested
    @DisplayName("obtenerEstadisticasTodasRecetas()")
    class ObtenerEstadisticasTodasRecetas {

        @Test
        @DisplayName("agrupa los registros por receta correctamente")
        void agrupaPorReceta() {
            Receta pizza = receta(1L, "Pizza");
            Receta pasta = receta(2L, "Pasta");

            when(recetaRepository.findAll()).thenReturn(List.of(pizza, pasta));
            when(registroUsoRecetaRepository.findByRecetaIdsAndFechaBetween(any(), eq(inicio), eq(fin)))
                    .thenReturn(List.of(
                            registro(pizza, LocalDateTime.of(2026, 6, 5, 12, 0), true),
                            registro(pizza, LocalDateTime.of(2026, 6, 5, 13, 0), false),
                            registro(pasta, LocalDateTime.of(2026, 6, 5, 14, 0), true)
                    ));

            List<EstadisticaRecetaResponse> response =
                    estadisticaService.obtenerEstadisticasTodasRecetas(inicio, fin);

            assertThat(response).hasSize(2);
            EstadisticaRecetaResponse statPizza = response.stream()
                    .filter(r -> r.getRecetaNombre().equals("Pizza")).findFirst().orElseThrow();
            assertThat(statPizza.getTotalElaboraciones()).isEqualTo(2);
            assertThat(statPizza.getElaboracionesCompletadas()).isEqualTo(1);
        }

        @Test
        @DisplayName("devuelve una entrada por receta aunque no tenga registros")
        void recetaSinRegistros() {
            Receta pizza = receta(1L, "Pizza");
            when(recetaRepository.findAll()).thenReturn(List.of(pizza));
            when(registroUsoRecetaRepository.findByRecetaIdsAndFechaBetween(any(), eq(inicio), eq(fin)))
                    .thenReturn(List.of());

            List<EstadisticaRecetaResponse> response =
                    estadisticaService.obtenerEstadisticasTodasRecetas(inicio, fin);

            assertThat(response).hasSize(1);
            assertThat(response.get(0).getTotalElaboraciones()).isZero();
            assertThat(response.get(0).getDatos()).isEmpty();
        }
    }
}
