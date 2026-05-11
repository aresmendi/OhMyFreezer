package com.ares.backend.service;

import com.ares.backend.config.SecurityUtils;
import com.ares.backend.dto.IngredienteRequest;
import com.ares.backend.dto.IngredienteResponse;
import com.ares.backend.dto.IngredienteUpdateRequest;
import com.ares.backend.entity.Ingrediente;
import com.ares.backend.repository.IngredienteRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IngredienteServiceTest {

    @Mock private IngredienteRepository ingredienteRepository;
    @Mock private AlertaService alertaService;
    @Mock private UsuarioService usuarioService;
    @Mock private MovimientoStockService movimientoStockService;

    @InjectMocks
    private IngredienteService ingredienteService;

    // ─── Helpers ────────────────────────────────────────────────────────────

    private Ingrediente ingrediente(Long id, Double cantidad, Double stockMinimo) {
        Ingrediente i = new Ingrediente();
        i.setId(id);
        i.setNombre("ingrediente-" + id);
        i.setCantidad(cantidad);
        i.setUnidadMedida("ud");
        i.setStockMinimo(stockMinimo);
        i.setFechaActualizacion(LocalDateTime.now());
        return i;
    }

    private IngredienteRequest request(String nombre, Double cantidad, Double stockMinimo) {
        IngredienteRequest r = new IngredienteRequest();
        r.setNombre(nombre);
        r.setCantidad(cantidad);
        r.setUnidadMedida("ud");
        r.setStockMinimo(stockMinimo);
        return r;
    }

    // ─── obtenerTodos() ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("obtenerTodos()")
    class ObtenerTodos {

        @Test
        @DisplayName("devuelve lista de ingredientes")
        void devuelveListaDeIngredientes() {
            when(ingredienteRepository.findAll()).thenReturn(List.of(
                    ingrediente(1L, 10.0, 5.0),
                    ingrediente(2L, 3.0, 5.0)
            ));

            List<IngredienteResponse> result = ingredienteService.obtenerTodos();

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("devuelve lista vacía si no hay ingredientes")
        void devuelveListaVacia() {
            when(ingredienteRepository.findAll()).thenReturn(List.of());

            assertThat(ingredienteService.obtenerTodos()).isEmpty();
        }
    }

    // ─── obtenerPorId() ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("obtenerPorId()")
    class ObtenerPorId {

        @Test
        @DisplayName("devuelve IngredienteResponse si existe")
        void devuelveIngredienteExistente() {
            when(ingredienteRepository.findById(1L)).thenReturn(Optional.of(ingrediente(1L, 10.0, 5.0)));

            IngredienteResponse response = ingredienteService.obtenerPorId(1L);

            assertThat(response).isNotNull();
        }

        @Test
        @DisplayName("lanza excepción si el ingrediente no existe")
        void lanzaExcepcionNoExiste() {
            when(ingredienteRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> ingredienteService.obtenerPorId(99L))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ─── crear() ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("crear()")
    class Crear {

        @Test
        @DisplayName("crea ingrediente correctamente sin stock bajo")
        void creaIngredienteOk() {
            when(ingredienteRepository.existsByNombreIgnoreCase("Tomate")).thenReturn(false);
            when(ingredienteRepository.save(any())).thenAnswer(inv -> {
                Ingrediente i = inv.getArgument(0);
                i.setId(1L);
                return i;
            });

            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getUsuarioId).thenReturn(1L);

                IngredienteResponse response = ingredienteService.crear(request("Tomate", 10.0, 5.0));

                assertThat(response).isNotNull();
                verify(ingredienteRepository).save(any());
                verify(movimientoStockService).registrarMovimiento(any(), eq(0.0), eq(10.0), eq("ENTRADA"), any(), eq(1L));
                verify(alertaService, never()).crearAlertaStockBajo(any());
            }
        }

        @Test
        @DisplayName("crea ingrediente y genera alerta si hay stock bajo")
        void creaIngredienteConStockBajo() {
            when(ingredienteRepository.existsByNombreIgnoreCase("Tomate")).thenReturn(false);
            when(ingredienteRepository.save(any())).thenAnswer(inv -> {
                Ingrediente i = inv.getArgument(0);
                i.setId(1L);
                return i;
            });

            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getUsuarioId).thenReturn(1L);

                // cantidad=2 < stockMinimo=5 → stock bajo
                ingredienteService.crear(request("Tomate", 2.0, 5.0));

                verify(alertaService).crearAlertaStockBajo(any());
            }
        }

        @Test
        @DisplayName("lanza excepción si ya existe un ingrediente con ese nombre")
        void lanzaExcepcionNombreDuplicado() {
            when(ingredienteRepository.existsByNombreIgnoreCase("Tomate")).thenReturn(true);

            assertThatThrownBy(() -> ingredienteService.crear(request("Tomate", 10.0, 5.0)))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(ingredienteRepository, never()).save(any());
        }
    }

    // ─── actualizar() ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("actualizar()")
    class Actualizar {

        @Test
        @DisplayName("actualiza ingrediente correctamente manteniendo mismo nombre")
        void actualizaIngredienteOk() {
            Ingrediente existente = ingrediente(1L, 10.0, 5.0);
            existente.setNombre("Tomate");

            when(ingredienteRepository.findById(1L)).thenReturn(Optional.of(existente));
            when(ingredienteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            IngredienteResponse response = ingredienteService.actualizar(1L, request("Tomate", 12.0, 5.0));

            assertThat(response).isNotNull();
            verify(ingredienteRepository).save(any());
            verify(alertaService, never()).crearAlertaMerma(any(), any(), any());
        }

        @Test
        @DisplayName("genera alerta escaldaio si el stock desciende")
        void generaAlertaEscaldaioSiDesciende() {
            Ingrediente existente = ingrediente(1L, 10.0, 5.0);
            existente.setNombre("Tomate");

            when(ingredienteRepository.findById(1L)).thenReturn(Optional.of(existente));
            when(ingredienteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // cantidad baja de 10 a 6
            ingredienteService.actualizar(1L, request("Tomate", 6.0, 5.0));

            verify(alertaService).crearAlertaMerma(any(), eq(10.0), eq(6.0));
        }

        @Test
        @DisplayName("lanza excepción si el nombre ya lo tiene otro ingrediente")
        void lanzaExcepcionNombreDuplicadoOtroIngrediente() {
            Ingrediente existente = ingrediente(1L, 10.0, 5.0);
            existente.setNombre("Tomate");

            when(ingredienteRepository.findById(1L)).thenReturn(Optional.of(existente));
            when(ingredienteRepository.existsByNombreIgnoreCase("Queso")).thenReturn(true);

            assertThatThrownBy(() -> ingredienteService.actualizar(1L, request("Queso", 10.0, 5.0)))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(ingredienteRepository, never()).save(any());
        }

        @Test
        @DisplayName("lanza excepción si el ingrediente no existe")
        void lanzaExcepcionNoExiste() {
            when(ingredienteRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> ingredienteService.actualizar(99L, request("Tomate", 10.0, 5.0)))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ─── actualizarCantidad() ───────────────────────────────────────────────

    @Nested
    @DisplayName("actualizarCantidad()")
    class ActualizarCantidad {

        @Test
        @DisplayName("registra movimiento ENTRADA si la cantidad sube")
        void registraEntradaSiCantidadSube() {
            Ingrediente existente = ingrediente(1L, 5.0, 3.0);
            when(ingredienteRepository.findById(1L)).thenReturn(Optional.of(existente));
            when(ingredienteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            IngredienteUpdateRequest req = new IngredienteUpdateRequest(10.0);

            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getUsuarioId).thenReturn(1L);

                ingredienteService.actualizarCantidad(1L, req);

                verify(movimientoStockService).registrarMovimiento(any(), eq(5.0), eq(10.0), eq("ENTRADA"), any(), eq(1L));
                verify(alertaService, never()).crearAlertaMerma(any(), any(), any());
            }
        }

        @Test
        @DisplayName("registra movimiento SALIDA y alerta escaldaio si la cantidad baja")
        void registraSalidaYAlertaSiCantidadBaja() {
            Ingrediente existente = ingrediente(1L, 10.0, 3.0);
            when(ingredienteRepository.findById(1L)).thenReturn(Optional.of(existente));
            when(ingredienteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            IngredienteUpdateRequest req = new IngredienteUpdateRequest(4.0);

            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getUsuarioId).thenReturn(1L);

                ingredienteService.actualizarCantidad(1L, req);

                verify(movimientoStockService).registrarMovimiento(any(), eq(10.0), eq(4.0), eq("SALIDA"), any(), eq(1L));
                verify(alertaService).crearAlertaMerma(any(), eq(10.0), eq(4.0));
            }
        }

        @Test
        @DisplayName("genera alerta stock bajo si la cantidad queda por debajo del mínimo")
        void generaAlertaStockBajo() {
            Ingrediente existente = ingrediente(1L, 10.0, 5.0);
            when(ingredienteRepository.findById(1L)).thenReturn(Optional.of(existente));
            when(ingredienteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // 2 < 5 → stock bajo
            IngredienteUpdateRequest req = new IngredienteUpdateRequest(2.0);

            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getUsuarioId).thenReturn(1L);

                ingredienteService.actualizarCantidad(1L, req);

                verify(alertaService).crearAlertaStockBajo(any());
            }
        }

        @Test
        @DisplayName("lanza excepción si el ingrediente no existe")
        void lanzaExcepcionNoExiste() {
            when(ingredienteRepository.findById(99L)).thenReturn(Optional.empty());

            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getUsuarioId).thenReturn(1L);

                assertThatThrownBy(() -> ingredienteService.actualizarCantidad(99L, new IngredienteUpdateRequest(5.0)))
                        .isInstanceOf(IllegalArgumentException.class);
            }
        }
    }

    // ─── eliminar() ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("eliminar()")
    class Eliminar {

        @Test
        @DisplayName("jefe elimina ingrediente correctamente")
        void eliminaIngredienteOk() {
            Ingrediente existente = ingrediente(1L, 10.0, 5.0);
            when(usuarioService.esJefeCocina()).thenReturn(true);
            when(ingredienteRepository.findById(1L)).thenReturn(Optional.of(existente));

            ingredienteService.eliminar(1L);

            verify(alertaService).eliminarPorIngrediente(existente);
            verify(ingredienteRepository).delete(existente);
        }

        @Test
        @DisplayName("lanza excepción si el usuario no es jefe")
        void lanzaExcepcionSiNoEsJefe() {
            when(usuarioService.esJefeCocina()).thenReturn(false);

            assertThatThrownBy(() -> ingredienteService.eliminar(1L))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(ingredienteRepository, never()).delete(any());
        }

        @Test
        @DisplayName("lanza excepción si el ingrediente no existe")
        void lanzaExcepcionNoExiste() {
            when(usuarioService.esJefeCocina()).thenReturn(true);
            when(ingredienteRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> ingredienteService.eliminar(99L))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(ingredienteRepository, never()).delete(any());
        }
    }

    // ─── obtenerConStockBajo() ──────────────────────────────────────────────

    @Nested
    @DisplayName("obtenerConStockBajo()")
    class ObtenerConStockBajo {

        @Test
        @DisplayName("devuelve solo ingredientes con stock bajo")
        void devuelveSoloIngredientesConStockBajo() {
            // ingrediente-1: 10 >= 5 → OK; ingrediente-2: 2 < 5 → bajo
            when(ingredienteRepository.findAll()).thenReturn(List.of(
                    ingrediente(1L, 10.0, 5.0),
                    ingrediente(2L, 2.0, 5.0)
            ));

            List<IngredienteResponse> result = ingredienteService.obtenerConStockBajo();

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("devuelve lista vacía si todos tienen stock suficiente")
        void devuelveVaciaSiTodosTienenStock() {
            when(ingredienteRepository.findAll()).thenReturn(List.of(
                    ingrediente(1L, 10.0, 5.0),
                    ingrediente(2L, 8.0, 5.0)
            ));

            assertThat(ingredienteService.obtenerConStockBajo()).isEmpty();
        }
    }

    // ─── buscarPorId() ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("buscarPorId()")
    class BuscarPorId {

        @Test
        @DisplayName("devuelve la entidad Ingrediente si existe")
        void devuelveIngredienteExistente() {
            Ingrediente i = ingrediente(1L, 10.0, 5.0);
            when(ingredienteRepository.findById(1L)).thenReturn(Optional.of(i));

            Ingrediente result = ingredienteService.buscarPorId(1L);

            assertThat(result.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("lanza excepción si no existe")
        void lanzaExcepcionNoExiste() {
            when(ingredienteRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> ingredienteService.buscarPorId(99L))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ─── reducirCantidad() ──────────────────────────────────────────────────

    @Nested
    @DisplayName("reducirCantidad()")
    class ReducirCantidad {

        @Test
        @DisplayName("reduce la cantidad y registra movimiento SALIDA")
        void reduceCantidadOk() {
            Ingrediente i = ingrediente(1L, 10.0, 5.0);
            when(ingredienteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getUsuarioId).thenReturn(1L);

                ingredienteService.reducirCantidad(i, 3.0);

                assertThat(i.getCantidad()).isEqualTo(7.0);
                verify(ingredienteRepository).save(i);
                verify(movimientoStockService).registrarMovimiento(eq(i), eq(10.0), eq(7.0), eq("SALIDA"), any(), eq(1L));
                verify(alertaService, never()).crearAlertaStockBajo(any());
            }
        }

        @Test
        @DisplayName("genera alerta stock bajo si la cantidad queda por debajo del mínimo")
        void generaAlertaStockBajoAlReducir() {
            Ingrediente i = ingrediente(1L, 6.0, 5.0);
            when(ingredienteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getUsuarioId).thenReturn(1L);

                // 6 - 4 = 2 < 5 → stock bajo
                ingredienteService.reducirCantidad(i, 4.0);

                verify(alertaService).crearAlertaStockBajo(any());
            }
        }
    }
}
