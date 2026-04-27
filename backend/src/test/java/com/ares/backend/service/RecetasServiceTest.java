package com.ares.backend.service;

import com.ares.backend.dto.PasoRecetaDTO;
import com.ares.backend.dto.IngredienteFaltanteDTO;
import com.ares.backend.dto.RecetaDetailResponse;
import com.ares.backend.dto.RecetaRequest;
import com.ares.backend.dto.RecetaIngredienteRequest;
import com.ares.backend.dto.VerificarRecetaResponse;
import com.ares.backend.dto.RegistroUsoResponse;
import com.ares.backend.dto.ElaborarRecetaRequest;
import com.ares.backend.dto.IngredienteResponse;
import com.ares.backend.entity.Receta;
import com.ares.backend.entity.Usuario;
import com.ares.backend.entity.PasoReceta;
import com.ares.backend.entity.RecetaIngrediente;
import com.ares.backend.entity.Ingrediente;
import com.ares.backend.repository.RecetaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import com.ares.backend.config.SecurityUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para RecetaService.
 * Usan Mockito para aislar el servicio de sus dependencias (BD, encoder).
 */
@ExtendWith(MockitoExtension.class)
class RecetasServiceTest {

    @Mock
    private RecetaRepository recetaRepository;

    @Mock
    private UsuarioService usuarioService;

    @Mock
    private IngredienteService ingredienteService;

    @Mock
    private RegistroUsoService registroUsoService;

    @Mock
    private AlertaService alertaService;

    @InjectMocks
    private RecetaService recetaService;

    // ─── Helpers ────────────────────────────────────────────────────────────
    private Usuario usuarioEmpleado(Long id, String username) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setUsername(username);
        u.setPassword("hashed_password");
        u.setEsJefeCocina(false);
        u.setFechaRegistro(LocalDateTime.now());
        return u;
    }

    private Usuario usuarioJefe(Long id, String username) {
        Usuario u = usuarioEmpleado(id, username);
        u.setEsJefeCocina(true);
        u.setEmail("jefe@cocina.com");
        return u;
    }
    private PasoReceta pasoReceta(Long id, int orden, Receta receta) {
        PasoReceta p = new PasoReceta();
        p.setId(id);
        p.setOrden(orden);
        p.setDescripcion("descripcion paso " + orden);
        p.setReceta(receta);
        return p;
    }
    private Ingrediente ingrediente(Long id, double cantidad, double stockMinimo) {
        Ingrediente i = new Ingrediente();
        i.setId(id);
        i.setNombre("ingrediente de prueba " + id);
        i.setCantidad(cantidad);
        i.setUnidadMedida("ud");
        i.setStockMinimo(stockMinimo);
        i.setFechaActualizacion(LocalDateTime.now());
        return i;
    }
    private RecetaIngrediente recetaIngrediente(Long id, Receta receta, Ingrediente ingrediente, double cantidadNecesaria) {
        RecetaIngrediente r = new RecetaIngrediente();
        r.setId(id);
        r.setReceta(receta);
        r.setIngrediente(ingrediente);
        r.setCantidadNecesaria(cantidadNecesaria);
        return r;
    }


    private Receta recetaPrueba(Long id, String name) {
        Receta receta = new Receta();
        //Lista de pasos
        List<PasoReceta> pasos = new ArrayList<>();
        pasos.add(pasoReceta(1L,1,receta));
        pasos.add(pasoReceta(2L,2,receta));
        pasos.add(pasoReceta(3L,3,receta));
        //Lista de Ingredientes por Receta
        List<RecetaIngrediente> recetaIngredientes = new ArrayList<>();
        recetaIngredientes.add(recetaIngrediente(1L,receta,ingrediente(1L,10,5),2));
        recetaIngredientes.add(recetaIngrediente(2L,receta,ingrediente(2L,15,7),5));

        receta.setId(id);
        receta.setNombre(name);
        receta.setDescripcion("decripcion prueba");
        receta.setFechaCreacion(LocalDateTime.now());
        receta.setCreadaPor(usuarioJefe(1L,"usuarioJefe"));
        receta.setPasos(pasos);
        receta.setIngredientes(recetaIngredientes);
        return receta;
    }

    private RecetaRequest recetaRequest(){
        RecetaRequest request = new RecetaRequest();
        request.setNombre("receta de prueba");
        request.setDescripcion("descripcion prueba");

        //pasos
        List<PasoRecetaDTO> pasos = List.of(new PasoRecetaDTO(null,1,"descripcion paso 1", null),
                new PasoRecetaDTO(null,2,"descripcion paso 2", null));
        request.setPasos(pasos);

        //ingredientes
        List<RecetaIngredienteRequest> ingredientes = List.of(
                new RecetaIngredienteRequest(1L, 2.0)
        );
        request.setIngredientes(ingredientes);
        return request;
    }


    // ─── crear() ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("crear()")
    class Crear {

        @Test
        @DisplayName("registra una receta correctamente")
        void crearRecetaOk() {
            //1.Given
            //Mock usuario jefe
            Usuario jefe = usuarioJefe(1L,"usuarioJefe");

            try(MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getUsuarioId).thenReturn(1L);
                when(usuarioService.buscarPorId(1L)).thenReturn(jefe);

                //Mock ingrediente
                Ingrediente ingrediente = ingrediente(1L,10,5);
                when(ingredienteService.buscarPorId(1L)).thenReturn(ingrediente);

                //Simulamos que guardamos
                when(recetaRepository.save(any())).thenAnswer(inv -> {
                    Receta receta = (Receta) inv.getArguments()[0];
                    receta.setId(1L);
                    return receta;
                });

                //Request
                RecetaRequest request = recetaRequest();

                //2.When

                RecetaDetailResponse response = recetaService.crear(request);

                //3.Then
                assertThat(response.getNombre()).isEqualTo("receta de prueba");
                assertThat(response.getDescripcion()).isEqualTo("descripcion prueba");

                verify(recetaRepository).save(any());

                verify(ingredienteService).buscarPorId(1L);
            }
        }
        @Test
        @DisplayName("lanza excepción si la receta la intenta crear alguien que NO es jefe")
        void lanzarExcepcionCodigoJefeInvalido() {
            //1.Given
            //Mock usuario normal
            Usuario usuario = usuarioEmpleado(1L,"usuarioEmpleado");

            try(MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getUsuarioId).thenReturn(1L);
                when(usuarioService.buscarPorId(1L)).thenReturn(usuario);

                //Request
                RecetaRequest request = recetaRequest();

                //2.When And Then
                assertThatThrownBy(() -> recetaService.crear(request)).isInstanceOf(IllegalArgumentException.class);

                //Verificar
                verify(recetaRepository, never()).save(any());
                verify(ingredienteService, never()).buscarPorId(anyLong());
            }
        }
    }
    // ─── obtenerPorId() ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("obtenerPorId()")
    class ObtenerPorId {

        @Test
        @DisplayName("devuelve la receta si exite")
        void devuelveRecetaExistente() {
            Receta receta = recetaPrueba(1L,"recetaPrueba");
            when(recetaRepository.findByIdWithIngredientes(1L)).thenReturn(Optional.of(receta));
            when(recetaRepository.findByIdWithPasos(1L)).thenReturn(Optional.of(receta));

            Receta resultado = recetaService.buscarPorId(1L);

            assertThat(resultado.getId()).isEqualTo(1L);
            assertThat(resultado.getNombre()).isEqualTo("recetaPrueba");
        }

        @Test
        @DisplayName("lanza excepción si la receta no existe")
        void lanzaExcepcionRecetaNoExiste(){
            when(recetaRepository.findByIdWithIngredientes(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> recetaService.buscarPorId(99L))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
    // ─── eliminarReceta() ────────────────────────────────────────────────────────
    @Nested
    @DisplayName("eliminarReceta()")
    class Eliminar {

        @Test
        @DisplayName("elimina receta")
        void eliminarReceta() {
            //GIVEN
            Receta receta = recetaPrueba(1L,"recetaPrueba");

            //1. Comprobamos que sea jefe
            when(usuarioService.esJefeCocina()).thenReturn(true);
            //2. Comprobamos que existe la receta
            when(recetaRepository.findById(1L)).thenReturn(Optional.of(receta));

            //WHEN
            recetaService.eliminar(1L);

            //THEN
            verify(alertaService).eliminarPorReceta(receta);
            verify(recetaRepository).delete(receta);
            verify(registroUsoService).eliminarPorReceta(receta);
        }

        @Test
        @DisplayName("lanza excepción cuando un No jefe intenta borrar recetas")
        void lanzaExcepcionNoJefe() {
            //1. Comprobamos que no sea jefe
            when(usuarioService.esJefeCocina()).thenReturn(false);


            //When and Then
            assertThatThrownBy(() -> recetaService.eliminar(1L)).isInstanceOf(IllegalArgumentException.class);

            //Verify
            verify(recetaRepository, never()).delete(any());
            verify(recetaRepository, never()).findById(any());
        }

        @Test
        @DisplayName("Lanza excepción cuando la receta no existe")
        void lanzaExcepcionRecetaNoExiste() {
            //Given
            //1. Comprobamos que si sea jefe
            when(usuarioService.esJefeCocina()).thenReturn(true);
            when(recetaRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> recetaService.eliminar(99L)).isInstanceOf(IllegalArgumentException.class);
            verify(recetaRepository, never()).delete(any());
        }
    }
    // ─── verificarDisponibilidadYNotificar() ────────────────────────────────────────────────────────
    @Nested
    @DisplayName("verificarDisponibilidadYNotificar()")
    class VerificarDisponibilidadYNotificar {

        @Test
        @DisplayName("devuelve true si hay stock")
        void devuelveTrue() {
            //Given
            Receta receta = recetaPrueba(1L,"recetaPrueba");

            when(recetaRepository.findById(1L)).thenReturn(Optional.of(receta));

            VerificarRecetaResponse response = recetaService.verificarDisponibilidadYNotificar(1L);

            assertThat(response.getDisponible()).isTrue();
            assertThat(response.getIngredientesFaltantes()).isEmpty();

            verify(alertaService,never()).crearAlertaRecetaNoDisponible(any(),any());
        }
        @Test
        @DisplayName("devuelve false si no hay stock")
        void devuelveFalse() {
            //Given
            Receta receta = recetaPrueba(1L,"recetaPrueba");
            Ingrediente ingrediente = ingrediente(1L,4,3);
            RecetaIngrediente recetaIngrediente = recetaIngrediente(2L,receta,ingrediente,6);

            List<RecetaIngrediente> ingredientes = new ArrayList<>();
            ingredientes.add(recetaIngrediente);
            receta.setIngredientes(ingredientes);

            //When
            when(recetaRepository.findById(1L)).thenReturn(Optional.of(receta));
            VerificarRecetaResponse response = recetaService.verificarDisponibilidadYNotificar(1L);

            //Then
            assertThat(response.getDisponible()).isFalse();
            assertThat(response.getIngredientesFaltantes()).size().isGreaterThanOrEqualTo(1);

            verify(alertaService).crearAlertaRecetaNoDisponible(any(),any());
        }
    }

    @Nested
    @DisplayName("elaborar()")
    class Elaborar {

        @Test
        @DisplayName("receta completada")
        void recetaCompletada() {
            //Given
            Receta receta = recetaPrueba(1L,"recetaPrueba");
            when(recetaRepository.findById(1L)).thenReturn(Optional.of(receta));
            when(registroUsoService.crear(receta,true)).thenReturn(new RegistroUsoResponse());
            ElaborarRecetaRequest request = new ElaborarRecetaRequest();
            request.setCompletada(true);

            //when + then
            try(MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getUsuarioId).thenReturn(1L);
                RegistroUsoResponse response = recetaService.elaborar(1L,request);
                assertThat(response).isNotNull();
            }

            //verify
            verify(ingredienteService, times(receta.getIngredientes().size())).reducirCantidad(any(),any());
            verify(registroUsoService).crear(receta,true);
        }

        @Test
        @DisplayName("receta fallida")
        void recetaFallida() {
            //Given
            Receta receta = recetaPrueba(1L,"recetaPrueba");
            when(recetaRepository.findById(1L)).thenReturn(Optional.of(receta));
            when(registroUsoService.crear(receta,false)).thenReturn(new RegistroUsoResponse());
            ElaborarRecetaRequest request = new ElaborarRecetaRequest();
            request.setCompletada(false);

            try(MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)){
                mocked.when(SecurityUtils::getUsuarioId).thenReturn(1L);
                //when
                RegistroUsoResponse response = recetaService.elaborar(1L,request);
                //then
                assertThat(response).isNotNull();
            }

            //verify
            verify(ingredienteService, never()).reducirCantidad(any(),any());
            verify(registroUsoService).crear(receta,false);
        }

        @Test
        @DisplayName("lanza excepción si completada=true pero no hay stock suficiente")
        void lanzaExcepcionSinStockSuficiente() {
            // GIVEN — receta con un ingrediente sin stock (cantidad=1, necesaria=5)
            Receta receta = new Receta();
            receta.setId(1L);
            receta.setNombre("recetaSinStock");
            receta.setDescripcion("desc");
            receta.setFechaCreacion(LocalDateTime.now());
            receta.setCreadaPor(usuarioJefe(1L, "usuarioJefe"));
            receta.setPasos(new ArrayList<>());

            Ingrediente sinStock = ingrediente(1L, 1, 3);
            RecetaIngrediente ri = recetaIngrediente(1L, receta, sinStock, 5);
            receta.setIngredientes(new ArrayList<>(List.of(ri)));

            ElaborarRecetaRequest request = new ElaborarRecetaRequest();
            request.setCompletada(true);

            when(recetaRepository.findById(1L)).thenReturn(Optional.of(receta));

            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getUsuarioId).thenReturn(1L);
                when(usuarioService.buscarPorId(1L)).thenReturn(usuarioJefe(1L, "usuarioJefe"));

                // WHEN & THEN
                assertThatThrownBy(() -> recetaService.elaborar(1L, request))
                        .isInstanceOf(IllegalArgumentException.class);

                verify(registroUsoService).crear(receta, false);
                verify(ingredienteService, never()).reducirCantidad(any(), any());
            }
        }
    }

    // ─── actualizar() ────────────────────────────────────────────────────────
    @Nested
    @DisplayName("actualizar()")
    class Actualizar {

        @Test
        @DisplayName("actualiza una receta correctamente si es jefe")
        void actualizarRecetaOk() {
            // GIVEN
            Usuario jefe = usuarioJefe(1L, "usuarioJefe");
            Receta recetaExistente = recetaPrueba(1L, "nombre original");

            RecetaRequest request = new RecetaRequest();
            request.setNombre("nombre actualizado");
            request.setDescripcion("descripcion actualizada");
            request.setPasos(List.of(new PasoRecetaDTO(null, 1, "paso nuevo", null)));
            request.setIngredientes(List.of(new RecetaIngredienteRequest(1L, 3.0)));

            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getUsuarioId).thenReturn(1L);
                when(usuarioService.buscarPorId(1L)).thenReturn(jefe);
                when(recetaRepository.findById(1L)).thenReturn(Optional.of(recetaExistente));
                when(ingredienteService.buscarPorId(1L)).thenReturn(ingrediente(1L, 10, 5));
                when(recetaRepository.save(any())).thenAnswer(inv -> inv.getArguments()[0]);

                // WHEN
                RecetaDetailResponse response = recetaService.actualizar(1L, request);

                // THEN
                assertThat(response.getNombre()).isEqualTo("nombre actualizado");
                assertThat(response.getDescripcion()).isEqualTo("descripcion actualizada");
                verify(recetaRepository).save(any());
                verify(ingredienteService).buscarPorId(1L);
            }
        }

        @Test
        @DisplayName("lanza excepción si el usuario no es jefe de cocina")
        void lanzaExcepcionNoJefe() {
            // GIVEN
            Usuario empleado = usuarioEmpleado(2L, "usuarioEmpleado");

            RecetaRequest request = new RecetaRequest();
            request.setNombre("nombre");
            request.setDescripcion("desc");
            request.setPasos(List.of());
            request.setIngredientes(List.of());

            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getUsuarioId).thenReturn(2L);
                when(usuarioService.buscarPorId(2L)).thenReturn(empleado);

                // WHEN & THEN
                assertThatThrownBy(() -> recetaService.actualizar(1L, request))
                        .isInstanceOf(IllegalArgumentException.class);

                verify(recetaRepository, never()).findById(any());
                verify(recetaRepository, never()).save(any());
            }
        }

        @Test
        @DisplayName("lanza excepción si la receta no existe")
        void lanzaExcepcionRecetaNoExiste() {
            // GIVEN
            Usuario jefe = usuarioJefe(1L, "usuarioJefe");

            RecetaRequest request = new RecetaRequest();
            request.setNombre("nombre");
            request.setDescripcion("desc");
            request.setPasos(List.of());
            request.setIngredientes(List.of());

            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getUsuarioId).thenReturn(1L);
                when(usuarioService.buscarPorId(1L)).thenReturn(jefe);
                when(recetaRepository.findById(99L)).thenReturn(Optional.empty());

                // WHEN & THEN
                assertThatThrownBy(() -> recetaService.actualizar(99L, request))
                        .isInstanceOf(IllegalArgumentException.class);

                verify(recetaRepository, never()).save(any());
            }
        }
    }

    // ─── obtenerTodas() ────────────────────────────────────────────────────────
    @Nested
    @DisplayName("obtenerTodas()")
    class ObtenerTodas {

        @Test
        @DisplayName("devuelve lista con todas las recetas")
        void devuelveListaConRecetas() {
            // GIVEN
            Receta receta1 = recetaPrueba(1L, "receta uno");
            Receta receta2 = recetaPrueba(2L, "receta dos");
            List<Receta> recetas = List.of(receta1, receta2);

            when(recetaRepository.findAllWithIngredientes()).thenReturn(recetas);
            when(recetaRepository.findAllWithPasos()).thenReturn(recetas);

            // WHEN
            List<RecetaDetailResponse> response = recetaService.obtenerTodas();

            // THEN
            assertThat(response).hasSize(2);
            assertThat(response.get(0).getNombre()).isEqualTo("receta uno");
            assertThat(response.get(1).getNombre()).isEqualTo("receta dos");
            verify(recetaRepository).findAllWithIngredientes();
            verify(recetaRepository).findAllWithPasos();
        }

        @Test
        @DisplayName("devuelve lista vacía si no hay recetas")
        void devuelveListaVacia() {
            // GIVEN
            when(recetaRepository.findAllWithIngredientes()).thenReturn(List.of());
            when(recetaRepository.findAllWithPasos()).thenReturn(List.of());

            // WHEN
            List<RecetaDetailResponse> response = recetaService.obtenerTodas();

            // THEN
            assertThat(response).isEmpty();
        }
    }

    // ─── obtenerPorId() ────────────────────────────────────────────────────────
    @Nested
    @DisplayName("obtenerPorId() — RecetaDetailResponse")
    class ObtenerPorIdDetalle {

        @Test
        @DisplayName("devuelve RecetaDetailResponse con disponible=true si hay stock")
        void devuelveDetailResponseConStock() {
            // GIVEN — recetaPrueba tiene stock suficiente en todos sus ingredientes
            Receta receta = recetaPrueba(1L, "recetaPrueba");

            when(recetaRepository.findByIdWithIngredientes(1L)).thenReturn(Optional.of(receta));
            when(recetaRepository.findByIdWithPasos(1L)).thenReturn(Optional.of(receta));

            // WHEN
            RecetaDetailResponse response = recetaService.obtenerPorId(1L);

            // THEN
            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getNombre()).isEqualTo("recetaPrueba");
            assertThat(response.getDisponible()).isTrue();
        }

        @Test
        @DisplayName("lanza excepción si la receta no existe")
        void lanzaExcepcionRecetaNoExiste() {
            // GIVEN
            when(recetaRepository.findByIdWithIngredientes(99L)).thenReturn(Optional.empty());

            // WHEN & THEN
            assertThatThrownBy(() -> recetaService.obtenerPorId(99L))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ─── obtenerPasos() ────────────────────────────────────────────────────────
    @Nested
    @DisplayName("obtenerPasos()")
    class ObtenerPasos {

        @Test
        @DisplayName("devuelve los pasos ordenados por orden")
        void devuelvePasosOrdenados() {
            // GIVEN — recetaPrueba tiene pasos con orden 1, 2, 3
            Receta receta = recetaPrueba(1L, "recetaPrueba");
            when(recetaRepository.findById(1L)).thenReturn(Optional.of(receta));

            // WHEN
            List<PasoRecetaDTO> pasos = recetaService.obtenerPasos(1L);

            // THEN
            assertThat(pasos).hasSize(3);
            assertThat(pasos.get(0).getOrden()).isEqualTo(1);
            assertThat(pasos.get(1).getOrden()).isEqualTo(2);
            assertThat(pasos.get(2).getOrden()).isEqualTo(3);
        }

        @Test
        @DisplayName("lanza excepción si la receta no existe")
        void lanzaExcepcionRecetaNoExiste() {
            // GIVEN
            when(recetaRepository.findById(99L)).thenReturn(Optional.empty());

            // WHEN & THEN
            assertThatThrownBy(() -> recetaService.obtenerPasos(99L))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
