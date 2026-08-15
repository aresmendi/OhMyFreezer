package com.ares.backend.service;

import com.ares.backend.config.SecurityUtils;
import com.ares.backend.dto.IngredienteRequest;
import com.ares.backend.dto.IngredienteResponse;
import com.ares.backend.dto.IngredienteUpdateRequest;
import com.ares.backend.entity.Ingrediente;
import com.ares.backend.entity.Negocio;
import com.ares.backend.entity.TipoUnidad;
import com.ares.backend.entity.UnidadMedida;
import com.ares.backend.exception.RecursoNoEncontradoException;
import com.ares.backend.repository.IngredienteRepository;
import com.ares.backend.repository.NegocioRepository;
import com.ares.backend.repository.UnidadMedidaRepository;
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
    @Mock private NegocioRepository negocioRepository;
    @Mock private UnidadMedidaRepository unidadMedidaRepository;

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

    private Negocio negocio(Long id) {
        Negocio n = new Negocio();
        n.setId(id);
        n.setNombre("negocio-" + id);
        n.setFechaAlta(LocalDateTime.now());
        return n;
    }

    private UnidadMedida unidad(Long id, String codigo) {
        UnidadMedida u = new UnidadMedida();
        u.setId(id);
        u.setCodigo(codigo);
        u.setNombre(codigo);
        u.setTipo(TipoUnidad.UNIDAD);
        u.setFactorABase(1.0);
        return u;
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
        @DisplayName("devuelve IngredienteResponse si existe en el negocio del caller")
        void devuelveIngredienteExistente() {
            when(ingredienteRepository.findByIdAndNegocioId(1L, 5L))
                    .thenReturn(Optional.of(ingrediente(1L, 10.0, 5.0)));

            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getNegocioId).thenReturn(5L);

                IngredienteResponse response = ingredienteService.obtenerPorId(1L);

                assertThat(response).isNotNull();
            }
        }

        @Test
        @DisplayName("lanza RecursoNoEncontradoException si el ingrediente no existe")
        void lanzaExcepcionNoExiste() {
            when(ingredienteRepository.findByIdAndNegocioId(99L, 5L)).thenReturn(Optional.empty());

            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getNegocioId).thenReturn(5L);

                assertThatThrownBy(() -> ingredienteService.obtenerPorId(99L))
                        .isInstanceOf(RecursoNoEncontradoException.class);
            }
        }

        @Test
        @DisplayName("CROSS-TENANT: id de un ingrediente de otro negocio devuelve RecursoNoEncontradoException, no el dato")
        void idDeOtroNegocioNoSeFiltra() {
            // El ingrediente 7 existe, pero pertenece al negocio 99 (foráneo); el caller es negocio 1.
            when(ingredienteRepository.findByIdAndNegocioId(7L, 1L)).thenReturn(Optional.empty());

            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getNegocioId).thenReturn(1L);

                assertThatThrownBy(() -> ingredienteService.obtenerPorId(7L))
                        .isInstanceOf(RecursoNoEncontradoException.class);

                // La query siempre se scoped al negocio del caller, nunca al del dueño real.
                verify(ingredienteRepository).findByIdAndNegocioId(7L, 1L);
            }
        }
    }

    // ─── crear() ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("crear()")
    class Crear {

        @Test
        @DisplayName("crea ingrediente correctamente sin stock bajo, asignando el negocio del caller")
        void creaIngredienteOk() {
            Negocio negocioDelCaller = negocio(5L);
            when(ingredienteRepository.existsByNombreIgnoreCaseAndNegocioId("Tomate", 5L)).thenReturn(false);
            when(negocioRepository.findById(5L)).thenReturn(Optional.of(negocioDelCaller));
            when(unidadMedidaRepository.findByCodigo("ud")).thenReturn(Optional.of(unidad(10L, "ud")));
            when(ingredienteRepository.save(any())).thenAnswer(inv -> {
                Ingrediente i = inv.getArgument(0);
                i.setId(1L);
                return i;
            });

            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getUsuarioId).thenReturn(1L);
                mocked.when(SecurityUtils::getNegocioId).thenReturn(5L);

                IngredienteResponse response = ingredienteService.crear(request("Tomate", 10.0, 5.0));

                assertThat(response).isNotNull();
                verify(ingredienteRepository).save(argThat(i -> i.getNegocio() == negocioDelCaller));
                verify(movimientoStockService).registrarMovimiento(any(), eq(0.0), eq(10.0), eq("ENTRADA"), any(), eq(1L));
                verify(alertaService, never()).crearAlertaStockBajo(any());
            }
        }

        @Test
        @DisplayName("crea ingrediente y genera alerta si hay stock bajo")
        void creaIngredienteConStockBajo() {
            when(ingredienteRepository.existsByNombreIgnoreCaseAndNegocioId("Tomate", 5L)).thenReturn(false);
            when(negocioRepository.findById(5L)).thenReturn(Optional.of(negocio(5L)));
            when(unidadMedidaRepository.findByCodigo("ud")).thenReturn(Optional.of(unidad(10L, "ud")));
            when(ingredienteRepository.save(any())).thenAnswer(inv -> {
                Ingrediente i = inv.getArgument(0);
                i.setId(1L);
                return i;
            });

            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getUsuarioId).thenReturn(1L);
                mocked.when(SecurityUtils::getNegocioId).thenReturn(5L);

                // cantidad=2 < stockMinimo=5 → stock bajo
                ingredienteService.crear(request("Tomate", 2.0, 5.0));

                verify(alertaService).crearAlertaStockBajo(any());
            }
        }

        @Test
        @DisplayName("lanza excepción si ya existe un ingrediente con ese nombre EN EL MISMO NEGOCIO")
        void lanzaExcepcionNombreDuplicado() {
            when(ingredienteRepository.existsByNombreIgnoreCaseAndNegocioId("Tomate", 5L)).thenReturn(true);

            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getNegocioId).thenReturn(5L);

                assertThatThrownBy(() -> ingredienteService.crear(request("Tomate", 10.0, 5.0)))
                        .isInstanceOf(IllegalArgumentException.class);

                verify(ingredienteRepository, never()).save(any());
            }
        }

        @Test
        @DisplayName("CROSS-TENANT: dos negocios distintos pueden tener cada uno un ingrediente 'Tomate' sin colisionar")
        void mismoNombreEnDosNegociosNoColisiona() {
            // Negocio 1 ya tiene "Tomate" (hipotético); Negocio 2 NO lo tiene todavía:
            // el check scoped al negocio 2 debe devolver false independientemente de negocio 1.
            when(ingredienteRepository.existsByNombreIgnoreCaseAndNegocioId("Tomate", 2L)).thenReturn(false);
            when(negocioRepository.findById(2L)).thenReturn(Optional.of(negocio(2L)));
            when(unidadMedidaRepository.findByCodigo("ud")).thenReturn(Optional.of(unidad(10L, "ud")));
            when(ingredienteRepository.save(any())).thenAnswer(inv -> {
                Ingrediente i = inv.getArgument(0);
                i.setId(42L);
                return i;
            });

            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getUsuarioId).thenReturn(2L);
                mocked.when(SecurityUtils::getNegocioId).thenReturn(2L);

                // El caller es negocio 2: la creación de "Tomate" debe tener éxito
                // a pesar de que negocio 1 ya tiene un ingrediente con ese nombre.
                IngredienteResponse response = ingredienteService.crear(request("Tomate", 10.0, 5.0));

                assertThat(response).isNotNull();
                verify(ingredienteRepository).existsByNombreIgnoreCaseAndNegocioId("Tomate", 2L);
                verify(ingredienteRepository, never()).existsByNombreIgnoreCase(any());
                verify(ingredienteRepository).save(any());
            }
        }
    }

    // ─── crear(): resolución de unidad de medida (Fase 2 "unidades-medida", PR2) ─────

    @Nested
    @DisplayName("crear() — resolución de unidad de medida")
    class CrearResolucionUnidad {

        @Test
        @DisplayName("resuelve la unidad por unidadBaseId cuando viene informado, ignorando unidadMedida")
        void creaViaUnidadBaseId() {
            when(ingredienteRepository.existsByNombreIgnoreCaseAndNegocioId("Harina", 5L)).thenReturn(false);
            when(negocioRepository.findById(5L)).thenReturn(Optional.of(negocio(5L)));
            when(unidadMedidaRepository.findById(7L)).thenReturn(Optional.of(unidad(7L, "kg")));
            when(ingredienteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            IngredienteRequest req = request("Harina", 10.0, 5.0);
            req.setUnidadBaseId(7L);
            req.setUnidadMedida(null); // el request real no necesita enviar el campo legado

            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getUsuarioId).thenReturn(1L);
                mocked.when(SecurityUtils::getNegocioId).thenReturn(5L);

                IngredienteResponse response = ingredienteService.crear(req);

                assertThat(response.getUnidadBaseId()).isEqualTo(7L);
                assertThat(response.getUnidadMedida()).isEqualTo("kg");
                assertThat(response.getUnidad()).isNotNull();
                assertThat(response.getUnidad().getCodigo()).isEqualTo("kg");
                verify(unidadMedidaRepository, never()).findByCodigo(any());
                verify(ingredienteRepository).save(argThat(i ->
                        i.getUnidadBase() != null
                                && i.getUnidadBase().getId().equals(7L)
                                && "kg".equals(i.getUnidadMedida())));
            }
        }

        @Test
        @DisplayName("sin unidadBaseId, resuelve por el código legado exacto de unidadMedida")
        void creaViaCodigoLegado() {
            when(ingredienteRepository.existsByNombreIgnoreCaseAndNegocioId("Azucar", 5L)).thenReturn(false);
            when(negocioRepository.findById(5L)).thenReturn(Optional.of(negocio(5L)));
            when(unidadMedidaRepository.findByCodigo("kg")).thenReturn(Optional.of(unidad(3L, "kg")));
            when(ingredienteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            IngredienteRequest req = request("Azucar", 10.0, 5.0);
            req.setUnidadMedida("kg"); // cliente legado, sin unidadBaseId

            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getUsuarioId).thenReturn(1L);
                mocked.when(SecurityUtils::getNegocioId).thenReturn(5L);

                IngredienteResponse response = ingredienteService.crear(req);

                assertThat(response.getUnidadBaseId()).isEqualTo(3L);
                assertThat(response.getUnidadMedida()).isEqualTo("kg");
                verify(unidadMedidaRepository).findByCodigo("kg");
                verify(unidadMedidaRepository, never()).findById(any());
            }
        }

        @Test
        @DisplayName("código legado desconocido (sin unidadBaseId) lanza IllegalArgumentException (400), sin guardar nada")
        void codigoLegadoDesconocidoLanza400() {
            when(ingredienteRepository.existsByNombreIgnoreCaseAndNegocioId("Bolsas", 5L)).thenReturn(false);
            when(negocioRepository.findById(5L)).thenReturn(Optional.of(negocio(5L)));
            when(unidadMedidaRepository.findByCodigo("bolsas")).thenReturn(Optional.empty());

            IngredienteRequest req = request("Bolsas", 10.0, 5.0);
            req.setUnidadMedida("bolsas");

            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getNegocioId).thenReturn(5L);

                assertThatThrownBy(() -> ingredienteService.crear(req))
                        .isInstanceOf(IllegalArgumentException.class);

                verify(ingredienteRepository, never()).save(any());
            }
        }

        @Test
        @DisplayName("unidadBaseId desconocido lanza RecursoNoEncontradoException (404), sin guardar nada")
        void unidadBaseIdDesconocidoLanza404() {
            when(ingredienteRepository.existsByNombreIgnoreCaseAndNegocioId("Sal", 5L)).thenReturn(false);
            when(negocioRepository.findById(5L)).thenReturn(Optional.of(negocio(5L)));
            when(unidadMedidaRepository.findById(999L)).thenReturn(Optional.empty());

            IngredienteRequest req = request("Sal", 10.0, 5.0);
            req.setUnidadBaseId(999L);

            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getNegocioId).thenReturn(5L);

                assertThatThrownBy(() -> ingredienteService.crear(req))
                        .isInstanceOf(RecursoNoEncontradoException.class);

                verify(ingredienteRepository, never()).save(any());
            }
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

            when(ingredienteRepository.findByIdAndNegocioId(1L, 5L)).thenReturn(Optional.of(existente));
            when(unidadMedidaRepository.findByCodigo("ud")).thenReturn(Optional.of(unidad(10L, "ud")));
            when(ingredienteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getNegocioId).thenReturn(5L);

                IngredienteResponse response = ingredienteService.actualizar(1L, request("Tomate", 12.0, 5.0));

                assertThat(response).isNotNull();
                verify(ingredienteRepository).save(any());
                verify(alertaService, never()).crearAlertaMerma(any(), any(), any());
            }
        }

        @Test
        @DisplayName("genera alerta escaldaio si el stock desciende")
        void generaAlertaEscaldaioSiDesciende() {
            Ingrediente existente = ingrediente(1L, 10.0, 5.0);
            existente.setNombre("Tomate");

            when(ingredienteRepository.findByIdAndNegocioId(1L, 5L)).thenReturn(Optional.of(existente));
            when(unidadMedidaRepository.findByCodigo("ud")).thenReturn(Optional.of(unidad(10L, "ud")));
            when(ingredienteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getNegocioId).thenReturn(5L);

                // cantidad baja de 10 a 6
                ingredienteService.actualizar(1L, request("Tomate", 6.0, 5.0));

                verify(alertaService).crearAlertaMerma(any(), eq(10.0), eq(6.0));
            }
        }

        @Test
        @DisplayName("lanza excepción si el nombre ya lo tiene otro ingrediente EN EL MISMO NEGOCIO")
        void lanzaExcepcionNombreDuplicadoOtroIngrediente() {
            Ingrediente existente = ingrediente(1L, 10.0, 5.0);
            existente.setNombre("Tomate");

            when(ingredienteRepository.findByIdAndNegocioId(1L, 5L)).thenReturn(Optional.of(existente));
            when(ingredienteRepository.existsByNombreIgnoreCaseAndNegocioId("Queso", 5L)).thenReturn(true);

            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getNegocioId).thenReturn(5L);

                assertThatThrownBy(() -> ingredienteService.actualizar(1L, request("Queso", 10.0, 5.0)))
                        .isInstanceOf(IllegalArgumentException.class);

                verify(ingredienteRepository, never()).save(any());
            }
        }

        @Test
        @DisplayName("lanza RecursoNoEncontradoException si el ingrediente no existe")
        void lanzaExcepcionNoExiste() {
            when(ingredienteRepository.findByIdAndNegocioId(99L, 5L)).thenReturn(Optional.empty());

            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getNegocioId).thenReturn(5L);

                assertThatThrownBy(() -> ingredienteService.actualizar(99L, request("Tomate", 10.0, 5.0)))
                        .isInstanceOf(RecursoNoEncontradoException.class);
            }
        }

        @Test
        @DisplayName("CROSS-TENANT: no puede actualizar un ingrediente de otro negocio, ni siquiera conociendo el id")
        void noPuedeActualizarIngredienteDeOtroNegocio() {
            // El ingrediente 7 pertenece al negocio 99; el caller es negocio 1 → 404, sin mutar nada.
            when(ingredienteRepository.findByIdAndNegocioId(7L, 1L)).thenReturn(Optional.empty());

            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getNegocioId).thenReturn(1L);

                assertThatThrownBy(() -> ingredienteService.actualizar(7L, request("Hackeado", 999.0, 0.0)))
                        .isInstanceOf(RecursoNoEncontradoException.class);

                verify(ingredienteRepository, never()).save(any());
            }
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
            when(ingredienteRepository.findByIdAndNegocioId(1L, 5L)).thenReturn(Optional.of(existente));
            when(ingredienteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            IngredienteUpdateRequest req = new IngredienteUpdateRequest(10.0);

            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getUsuarioId).thenReturn(1L);
                mocked.when(SecurityUtils::getNegocioId).thenReturn(5L);

                ingredienteService.actualizarCantidad(1L, req);

                verify(movimientoStockService).registrarMovimiento(any(), eq(5.0), eq(10.0), eq("ENTRADA"), any(), eq(1L));
                verify(alertaService, never()).crearAlertaMerma(any(), any(), any());
            }
        }

        @Test
        @DisplayName("registra movimiento SALIDA y alerta escaldaio si la cantidad baja")
        void registraSalidaYAlertaSiCantidadBaja() {
            Ingrediente existente = ingrediente(1L, 10.0, 3.0);
            when(ingredienteRepository.findByIdAndNegocioId(1L, 5L)).thenReturn(Optional.of(existente));
            when(ingredienteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            IngredienteUpdateRequest req = new IngredienteUpdateRequest(4.0);

            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getUsuarioId).thenReturn(1L);
                mocked.when(SecurityUtils::getNegocioId).thenReturn(5L);

                ingredienteService.actualizarCantidad(1L, req);

                verify(movimientoStockService).registrarMovimiento(any(), eq(10.0), eq(4.0), eq("SALIDA"), any(), eq(1L));
                verify(alertaService).crearAlertaMerma(any(), eq(10.0), eq(4.0));
            }
        }

        @Test
        @DisplayName("genera alerta stock bajo si la cantidad queda por debajo del mínimo")
        void generaAlertaStockBajo() {
            Ingrediente existente = ingrediente(1L, 10.0, 5.0);
            when(ingredienteRepository.findByIdAndNegocioId(1L, 5L)).thenReturn(Optional.of(existente));
            when(ingredienteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // 2 < 5 → stock bajo
            IngredienteUpdateRequest req = new IngredienteUpdateRequest(2.0);

            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getUsuarioId).thenReturn(1L);
                mocked.when(SecurityUtils::getNegocioId).thenReturn(5L);

                ingredienteService.actualizarCantidad(1L, req);

                verify(alertaService).crearAlertaStockBajo(any());
            }
        }

        @Test
        @DisplayName("lanza RecursoNoEncontradoException si el ingrediente no existe")
        void lanzaExcepcionNoExiste() {
            when(ingredienteRepository.findByIdAndNegocioId(99L, 5L)).thenReturn(Optional.empty());

            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getUsuarioId).thenReturn(1L);
                mocked.when(SecurityUtils::getNegocioId).thenReturn(5L);

                assertThatThrownBy(() -> ingredienteService.actualizarCantidad(99L, new IngredienteUpdateRequest(5.0)))
                        .isInstanceOf(RecursoNoEncontradoException.class);
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
            when(ingredienteRepository.findByIdAndNegocioId(1L, 5L)).thenReturn(Optional.of(existente));

            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getNegocioId).thenReturn(5L);

                ingredienteService.eliminar(1L);

                verify(alertaService).eliminarPorIngrediente(existente);
                verify(ingredienteRepository).delete(existente);
            }
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
        @DisplayName("lanza RecursoNoEncontradoException si el ingrediente no existe")
        void lanzaExcepcionNoExiste() {
            when(usuarioService.esJefeCocina()).thenReturn(true);
            when(ingredienteRepository.findByIdAndNegocioId(99L, 5L)).thenReturn(Optional.empty());

            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getNegocioId).thenReturn(5L);

                assertThatThrownBy(() -> ingredienteService.eliminar(99L))
                        .isInstanceOf(RecursoNoEncontradoException.class);

                verify(ingredienteRepository, never()).delete(any());
            }
        }

        @Test
        @DisplayName("CROSS-TENANT: jefe de un negocio no puede eliminar un ingrediente de otro negocio")
        void jefeNoPuedeEliminarIngredienteDeOtroNegocio() {
            when(usuarioService.esJefeCocina()).thenReturn(true);
            when(ingredienteRepository.findByIdAndNegocioId(7L, 1L)).thenReturn(Optional.empty());

            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getNegocioId).thenReturn(1L);

                assertThatThrownBy(() -> ingredienteService.eliminar(7L))
                        .isInstanceOf(RecursoNoEncontradoException.class);

                verify(ingredienteRepository, never()).delete(any());
                verify(alertaService, never()).eliminarPorIngrediente(any());
            }
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
        @DisplayName("devuelve la entidad Ingrediente si existe en el negocio del caller")
        void devuelveIngredienteExistente() {
            Ingrediente i = ingrediente(1L, 10.0, 5.0);
            when(ingredienteRepository.findByIdAndNegocioId(1L, 5L)).thenReturn(Optional.of(i));

            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getNegocioId).thenReturn(5L);

                Ingrediente result = ingredienteService.buscarPorId(1L);

                assertThat(result.getId()).isEqualTo(1L);
            }
        }

        @Test
        @DisplayName("lanza RecursoNoEncontradoException si no existe")
        void lanzaExcepcionNoExiste() {
            when(ingredienteRepository.findByIdAndNegocioId(99L, 5L)).thenReturn(Optional.empty());

            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getNegocioId).thenReturn(5L);

                assertThatThrownBy(() -> ingredienteService.buscarPorId(99L))
                        .isInstanceOf(RecursoNoEncontradoException.class);
            }
        }

        @Test
        @DisplayName("CROSS-TENANT: un id de otro negocio nunca resuelve, aunque exista en la BD")
        void idDeOtroNegocioNoResuelve() {
            when(ingredienteRepository.findByIdAndNegocioId(7L, 1L)).thenReturn(Optional.empty());

            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getNegocioId).thenReturn(1L);

                assertThatThrownBy(() -> ingredienteService.buscarPorId(7L))
                        .isInstanceOf(RecursoNoEncontradoException.class);
            }
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
