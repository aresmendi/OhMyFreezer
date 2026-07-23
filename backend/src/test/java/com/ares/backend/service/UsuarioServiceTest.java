package com.ares.backend.service;

import com.ares.backend.config.SecurityUtils;
import com.ares.backend.dto.EmpleadoRegisterRequest;
import com.ares.backend.dto.UsuarioLoginRequest;
import com.ares.backend.dto.UsuarioRegisterRequest;
import com.ares.backend.dto.UsuarioResponse;
import com.ares.backend.entity.Negocio;
import com.ares.backend.entity.NegocioSignupCode;
import com.ares.backend.entity.Usuario;
import com.ares.backend.exception.RecursoNoEncontradoException;
import com.ares.backend.repository.NegocioRepository;
import com.ares.backend.repository.NegocioSignupCodeRepository;
import com.ares.backend.repository.RecetaFavoritaRepository;
import com.ares.backend.repository.RegistroUsoRecetaRepository;
import com.ares.backend.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para UsuarioService.
 * Usan Mockito para aislar el servicio de sus dependencias (BD, encoder).
 */
@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RegistroUsoRecetaRepository registroUsoRepository;

    @Mock
    private RecetaFavoritaRepository recetaFavoritaRepository;

    @Mock
    private NegocioRepository negocioRepository;

    @Mock
    private NegocioSignupCodeRepository negocioSignupCodeRepository;

    @InjectMocks
    private UsuarioService usuarioService;

    // ─── Helpers ────────────────────────────────────────────────────────────

    private Negocio negocio(Long id) {
        Negocio n = new Negocio("Negocio " + id, "negocio" + id + "@test.com");
        n.setId(id);
        return n;
    }

    private NegocioSignupCode codigoValido(Negocio negocio, String codigo) {
        return new NegocioSignupCode(negocio, codigo);
    }

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

    // ─── registrar() ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("registrar()")
    class Registrar {

        private UsuarioRegisterRequest requestJefe(String codigo) {
            UsuarioRegisterRequest r = new UsuarioRegisterRequest();
            r.setUsername("jefe1");
            r.setPassword("password123");
            r.setEsJefeCocina(true);
            r.setCodigoRegistro(codigo);
            r.setEmail("jefe1@test.com");
            return r;
        }

        @Test
        @DisplayName("registra un jefe correctamente con un código de alta válido y lo vincula al Negocio del código")
        void registraJefeOkConCodigoValido() {
            Negocio negocio = negocio(10L);
            NegocioSignupCode codigo = codigoValido(negocio, "CODIGO_VALIDO");

            when(negocioSignupCodeRepository.findByCodigo("CODIGO_VALIDO")).thenReturn(Optional.of(codigo));
            when(usuarioRepository.existsByUsernameAndNegocioId("jefe1", 10L)).thenReturn(false);
            when(negocioSignupCodeRepository.marcarUsadoAtomico("CODIGO_VALIDO")).thenReturn(1);
            when(passwordEncoder.encode(anyString())).thenReturn("hashed");
            when(usuarioRepository.save(any())).thenAnswer(inv -> {
                Usuario u = inv.getArgument(0);
                u.setId(1L);
                return u;
            });

            UsuarioResponse response = usuarioService.registrar(requestJefe("CODIGO_VALIDO"));

            assertThat(response.getUsername()).isEqualTo("jefe1");
            assertThat(response.getNegocioId()).isEqualTo(10L);
            assertThat(response.getEsJefeCocina()).isTrue();

            ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
            verify(usuarioRepository).save(captor.capture());
            assertThat(captor.getValue().getNegocio()).isEqualTo(negocio);
            assertThat(captor.getValue().getEsJefeCocina()).isTrue();
        }

        @Test
        @DisplayName("reclama atómicamente el código de alta y registra qué usuario lo consumió tras un registro exitoso")
        void reclamaCodigoAtomicamenteTrasRegistroExitoso() {
            Negocio negocio = negocio(10L);
            NegocioSignupCode codigo = codigoValido(negocio, "CODIGO_VALIDO");

            when(negocioSignupCodeRepository.findByCodigo("CODIGO_VALIDO")).thenReturn(Optional.of(codigo));
            when(usuarioRepository.existsByUsernameAndNegocioId("jefe1", 10L)).thenReturn(false);
            when(negocioSignupCodeRepository.marcarUsadoAtomico("CODIGO_VALIDO")).thenReturn(1);
            when(passwordEncoder.encode(anyString())).thenReturn("hashed");
            when(usuarioRepository.save(any())).thenAnswer(inv -> {
                Usuario u = inv.getArgument(0);
                u.setId(7L);
                return u;
            });

            usuarioService.registrar(requestJefe("CODIGO_VALIDO"));

            verify(negocioSignupCodeRepository).marcarUsadoAtomico("CODIGO_VALIDO");
            verify(negocioSignupCodeRepository).registrarUsuarioQueConsumio("CODIGO_VALIDO", 7L);
            verify(negocioSignupCodeRepository, never()).save(any(NegocioSignupCode.class));
        }

        @Test
        @DisplayName("lanza excepción y no crea el Usuario si pierde la reclamación atómica del código (carrera concurrente)")
        void lanzaExcepcionYNoCreaUsuarioSiPierdeLaReclamacionAtomica() {
            Negocio negocio = negocio(10L);
            NegocioSignupCode codigo = codigoValido(negocio, "CODIGO_VALIDO");

            when(negocioSignupCodeRepository.findByCodigo("CODIGO_VALIDO")).thenReturn(Optional.of(codigo));
            when(usuarioRepository.existsByUsernameAndNegocioId("jefe1", 10L)).thenReturn(false);
            // Simula que otra petición concurrente ganó la reclamación primero.
            when(negocioSignupCodeRepository.marcarUsadoAtomico("CODIGO_VALIDO")).thenReturn(0);

            assertThatThrownBy(() -> usuarioService.registrar(requestJefe("CODIGO_VALIDO")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("inválido");

            verify(usuarioRepository, never()).save(any());
            verify(negocioSignupCodeRepository, never()).registrarUsuarioQueConsumio(anyString(), any());
        }

        @Test
        @DisplayName("lanza excepción si el código de alta no existe")
        void lanzaExcepcionCodigoDesconocido() {
            when(negocioSignupCodeRepository.findByCodigo("NO_EXISTE")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> usuarioService.registrar(requestJefe("NO_EXISTE")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("inválido");

            verify(usuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("lanza excepción si el código de alta ya fue usado (single-use)")
        void lanzaExcepcionCodigoYaUsado() {
            Negocio negocio = negocio(10L);
            NegocioSignupCode codigo = codigoValido(negocio, "CODIGO_USADO");
            codigo.marcarUsado(99L);

            when(negocioSignupCodeRepository.findByCodigo("CODIGO_USADO")).thenReturn(Optional.of(codigo));

            assertThatThrownBy(() -> usuarioService.registrar(requestJefe("CODIGO_USADO")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("inválido");

            verify(usuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("lanza excepción si el código de alta fue revocado por un admin (activo=false)")
        void lanzaExcepcionCodigoRevocado() {
            Negocio negocio = negocio(10L);
            NegocioSignupCode codigo = codigoValido(negocio, "CODIGO_REVOCADO");
            codigo.setActivo(false);

            when(negocioSignupCodeRepository.findByCodigo("CODIGO_REVOCADO")).thenReturn(Optional.of(codigo));

            assertThatThrownBy(() -> usuarioService.registrar(requestJefe("CODIGO_REVOCADO")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("inválido");

            verify(usuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("lanza excepción si no se aporta código de registro")
        void lanzaExcepcionSinCodigo() {
            UsuarioRegisterRequest request = requestJefe(null);

            assertThatThrownBy(() -> usuarioService.registrar(request))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(usuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("lanza excepción si el username ya existe en el negocio resuelto por el código")
        void lanzaExcepcionUsernameExistenteEnEseNegocio() {
            Negocio negocio = negocio(10L);
            NegocioSignupCode codigo = codigoValido(negocio, "CODIGO_VALIDO");

            when(negocioSignupCodeRepository.findByCodigo("CODIGO_VALIDO")).thenReturn(Optional.of(codigo));
            when(usuarioRepository.existsByUsernameAndNegocioId("jefe1", 10L)).thenReturn(true);

            assertThatThrownBy(() -> usuarioService.registrar(requestJefe("CODIGO_VALIDO")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("ya existe");

            verify(usuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("lanza excepción si el jefe no tiene email")
        void lanzaExcepcionJefeSinEmail() {
            Negocio negocio = negocio(10L);
            NegocioSignupCode codigo = codigoValido(negocio, "CODIGO_VALIDO");
            UsuarioRegisterRequest request = requestJefe("CODIGO_VALIDO");
            request.setEmail(null);

            when(negocioSignupCodeRepository.findByCodigo("CODIGO_VALIDO")).thenReturn(Optional.of(codigo));

            assertThatThrownBy(() -> usuarioService.registrar(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("correo electrónico es obligatorio");

            verify(usuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("lanza excepción si el registro público se intenta con esJefeCocina=false (los empleados se crean vía /empleados)")
        void lanzaExcepcionSiNoEsJefe() {
            UsuarioRegisterRequest request = new UsuarioRegisterRequest();
            request.setUsername("empleado1");
            request.setPassword("password123");
            request.setEsJefeCocina(false);

            assertThatThrownBy(() -> usuarioService.registrar(request))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(usuarioRepository, never()).save(any());
            verifyNoInteractions(negocioSignupCodeRepository);
        }

        @Test
        @DisplayName("lanza excepción si el email ya existe en cualquier negocio (unicidad global)")
        void lanzaExcepcionEmailYaExiste() {
            Negocio negocio = negocio(10L);
            NegocioSignupCode codigo = codigoValido(negocio, "CODIGO_VALIDO");

            when(negocioSignupCodeRepository.findByCodigo("CODIGO_VALIDO")).thenReturn(Optional.of(codigo));
            when(usuarioRepository.existsByEmail("jefe1@test.com")).thenReturn(true);

            assertThatThrownBy(() -> usuarioService.registrar(requestJefe("CODIGO_VALIDO")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("correo electrónico ya está en uso");

            verify(usuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("hashea la contraseña antes de guardar")
        void hasheoContrasena() {
            Negocio negocio = negocio(10L);
            NegocioSignupCode codigo = codigoValido(negocio, "CODIGO_VALIDO");

            when(negocioSignupCodeRepository.findByCodigo("CODIGO_VALIDO")).thenReturn(Optional.of(codigo));
            when(usuarioRepository.existsByUsernameAndNegocioId("jefe1", 10L)).thenReturn(false);
            when(negocioSignupCodeRepository.marcarUsadoAtomico("CODIGO_VALIDO")).thenReturn(1);
            when(passwordEncoder.encode("password123")).thenReturn("hashed_password");
            when(usuarioRepository.save(any())).thenAnswer(inv -> {
                Usuario u = inv.getArgument(0);
                u.setId(1L);
                return u;
            });

            usuarioService.registrar(requestJefe("CODIGO_VALIDO"));

            verify(passwordEncoder).encode("password123");
            verify(usuarioRepository).save(argThat(u ->
                    "hashed_password".equals(u.getPassword())
            ));
        }
    }

    // ─── crearEmpleado() ────────────────────────────────────────────────────

    @Nested
    @DisplayName("crearEmpleado()")
    class CrearEmpleado {

        private EmpleadoRegisterRequest request() {
            return new EmpleadoRegisterRequest("empleado1", "password123", "empleado1@test.com");
        }

        @Test
        @DisplayName("crea un empleado heredando el negocioId del jefe autenticado (SecurityUtils), sin código")
        void creaEmpleadoHeredandoNegocioDelJefe() {
            Negocio negocio = negocio(10L);

            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getNegocioId).thenReturn(10L);

                when(usuarioRepository.existsByUsernameAndNegocioId("empleado1", 10L)).thenReturn(false);
                when(negocioRepository.findById(10L)).thenReturn(Optional.of(negocio));
                when(passwordEncoder.encode(anyString())).thenReturn("hashed");
                when(usuarioRepository.save(any())).thenAnswer(inv -> {
                    Usuario u = inv.getArgument(0);
                    u.setId(2L);
                    return u;
                });

                UsuarioResponse response = usuarioService.crearEmpleado(request());

                assertThat(response.getUsername()).isEqualTo("empleado1");
                assertThat(response.getNegocioId()).isEqualTo(10L);
                assertThat(response.getEsJefeCocina()).isFalse();

                ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
                verify(usuarioRepository).save(captor.capture());
                assertThat(captor.getValue().getNegocio()).isEqualTo(negocio);
                assertThat(captor.getValue().getEsJefeCocina()).isFalse();
            }
        }

        @Test
        @DisplayName("triangulación: otro jefe autenticado crea el empleado en SU propio negocio, no en otro")
        void creaEmpleadoEnElNegocioDeOtroJefe() {
            Negocio negocio = negocio(55L);

            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getNegocioId).thenReturn(55L);

                when(usuarioRepository.existsByUsernameAndNegocioId("empleado1", 55L)).thenReturn(false);
                when(negocioRepository.findById(55L)).thenReturn(Optional.of(negocio));
                when(passwordEncoder.encode(anyString())).thenReturn("hashed");
                when(usuarioRepository.save(any())).thenAnswer(inv -> {
                    Usuario u = inv.getArgument(0);
                    u.setId(3L);
                    return u;
                });

                UsuarioResponse response = usuarioService.crearEmpleado(request());

                assertThat(response.getNegocioId()).isEqualTo(55L);
            }
        }

        @Test
        @DisplayName("lanza excepción si el username ya existe en el negocio del jefe")
        void lanzaExcepcionUsernameExistenteEnNegocioDelJefe() {
            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getNegocioId).thenReturn(10L);
                when(usuarioRepository.existsByUsernameAndNegocioId("empleado1", 10L)).thenReturn(true);

                assertThatThrownBy(() -> usuarioService.crearEmpleado(request()))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("ya existe");

                verify(usuarioRepository, never()).save(any());
            }
        }

        @Test
        @DisplayName("lanza excepción si no se aporta email")
        void lanzaExcepcionSinEmail() {
            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getNegocioId).thenReturn(10L);
                EmpleadoRegisterRequest request = new EmpleadoRegisterRequest("empleado1", "password123", null);

                assertThatThrownBy(() -> usuarioService.crearEmpleado(request))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("correo electrónico es obligatorio");

                verify(usuarioRepository, never()).save(any());
            }
        }

        @Test
        @DisplayName("lanza excepción si el email ya existe en cualquier negocio (unicidad global)")
        void lanzaExcepcionEmailExistente() {
            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getNegocioId).thenReturn(10L);
                when(usuarioRepository.existsByEmail("empleado1@test.com")).thenReturn(true);

                assertThatThrownBy(() -> usuarioService.crearEmpleado(request()))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("correo electrónico ya está en uso");

                verify(usuarioRepository, never()).save(any());
            }
        }
    }

    // ─── login() ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("login()")
    class Login {

        @Test
        @DisplayName("devuelve el usuario si las credenciales son correctas")
        void loginCorrecto() {
            Usuario usuario = usuarioEmpleado(1L, "empleado1");
            usuario.setEmail("empleado1@test.com");
            when(usuarioRepository.findByEmail("empleado1@test.com")).thenReturn(Optional.of(usuario));
            when(passwordEncoder.matches("password123", "hashed_password")).thenReturn(true);

            UsuarioLoginRequest request = new UsuarioLoginRequest();
            request.setEmail("empleado1@test.com");
            request.setPassword("password123");

            UsuarioResponse response = usuarioService.login(request);

            assertThat(response.getUsername()).isEqualTo("empleado1");
        }

        @Test
        @DisplayName("lanza excepción si el email no existe")
        void lanzaExcepcionEmailNoExiste() {
            when(usuarioRepository.findByEmail("noexiste@test.com")).thenReturn(Optional.empty());

            UsuarioLoginRequest request = new UsuarioLoginRequest();
            request.setEmail("noexiste@test.com");
            request.setPassword("cualquiera");

            assertThatThrownBy(() -> usuarioService.login(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Usuario o contraseña incorrectos");
        }

        @Test
        @DisplayName("lanza excepción si la contraseña es incorrecta")
        void lanzaExcepcionContrasenaIncorrecta() {
            Usuario usuario = usuarioEmpleado(1L, "empleado1");
            usuario.setEmail("empleado1@test.com");
            when(usuarioRepository.findByEmail("empleado1@test.com")).thenReturn(Optional.of(usuario));
            when(passwordEncoder.matches("wrongpassword", "hashed_password")).thenReturn(false);

            UsuarioLoginRequest request = new UsuarioLoginRequest();
            request.setEmail("empleado1@test.com");
            request.setPassword("wrongpassword");

            assertThatThrownBy(() -> usuarioService.login(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Usuario o contraseña incorrectos");
        }

        @Test
        @DisplayName("no revela si el email existe o no (mismo mensaje de error)")
        void mensajeErrorGenerico() {
            // Seguridad: el mensaje debe ser idéntico tanto si no existe el email
            // como si la contraseña es incorrecta — para no dar pistas a atacantes
            when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.empty());

            UsuarioLoginRequest request = new UsuarioLoginRequest();
            request.setEmail("cualquiera@test.com");
            request.setPassword("cualquiera");

            assertThatThrownBy(() -> usuarioService.login(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Usuario o contraseña incorrectos");
        }

        // NOTA: el antiguo test "desambiguaPorContrasenaSiUsernameColisionaEntreNegocios"
        // (Fase 8) queda OBSOLETO y se elimina: su premisa era que login()
        // desambiguaba por contraseña entre candidatos con el mismo username en
        // negocios distintos. Desde esta migración login() ya NO usa username en
        // absoluto — resuelve directamente por email (único globalmente), así que
        // la ambigüedad que ese test documentaba (y la fuga cross-tenant real que
        // producía si además la contraseña coincidía) es estructuralmente
        // imposible ahora. La prueba equivalente y superadora, a nivel de
        // integración full-stack con dos negocios reales, vive en
        // CrossTenantIsolationIntegrationTest (test 8.4).
    }

    // ─── buscarPorId() ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("buscarPorId()")
    class BuscarPorId {

        @Test
        @DisplayName("devuelve el usuario si existe")
        void devuelveUsuarioExistente() {
            Usuario usuario = usuarioEmpleado(1L, "empleado1");
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

            Usuario resultado = usuarioService.buscarPorId(1L);

            assertThat(resultado.getId()).isEqualTo(1L);
            assertThat(resultado.getUsername()).isEqualTo("empleado1");
        }

        @Test
        @DisplayName("lanza excepción si el usuario no existe")
        void lanzaExcepcionUsuarioNoExiste() {
            when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> usuarioService.buscarPorId(99L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Usuario no encontrado con ID: 99");
        }
    }

    // ─── obtenerTodos() ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("obtenerTodos()")
    class ObtenerTodos {

        @Test
        @DisplayName("devuelve lista con todos los usuarios")
        void devuelveListaDeUsuarios() {
            when(usuarioRepository.findAll()).thenReturn(List.of(
                    usuarioEmpleado(1L, "empleado1"),
                    usuarioJefe(2L, "jefe1")
            ));

            List<UsuarioResponse> response = usuarioService.obtenerTodos();

            assertThat(response).hasSize(2);
            verify(usuarioRepository).findAll();
        }

        @Test
        @DisplayName("devuelve lista vacía si no hay usuarios")
        void devuelveListaVacia() {
            when(usuarioRepository.findAll()).thenReturn(List.of());

            assertThat(usuarioService.obtenerTodos()).isEmpty();
        }
    }

    // ─── esJefeCocina() ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("esJefeCocina()")
    class EsJefeCocina {

        @Test
        @DisplayName("devuelve true si el usuario autenticado es jefe")
        void devuelveTrueSiEsJefe() {
            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getUsuarioId).thenReturn(1L);
                when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioJefe(1L, "jefe1")));

                assertThat(usuarioService.esJefeCocina()).isTrue();
            }
        }

        @Test
        @DisplayName("devuelve false si el usuario autenticado no es jefe")
        void devuelveFalseSiNoEsJefe() {
            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getUsuarioId).thenReturn(1L);
                when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioEmpleado(1L, "empleado1")));

                assertThat(usuarioService.esJefeCocina()).isFalse();
            }
        }
    }

    // ─── eliminar() ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("eliminar()")
    class Eliminar {

        @Test
        @DisplayName("jefe elimina empleado correctamente")
        void jefeEliminaEmpleadoOk() {
            Usuario jefe = usuarioJefe(1L, "jefe1");
            Usuario empleado = usuarioEmpleado(2L, "empleado1");

            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getUsuarioId).thenReturn(1L);
                mocked.when(SecurityUtils::getNegocioId).thenReturn(10L);
                when(usuarioRepository.findById(1L)).thenReturn(Optional.of(jefe));
                when(usuarioRepository.findByIdAndNegocioId(2L, 10L)).thenReturn(Optional.of(empleado));

                usuarioService.eliminar(2L);

                verify(registroUsoRepository).deleteByUsuarioId(2L);
                verify(recetaFavoritaRepository).deleteByUsuarioId(2L);
                verify(usuarioRepository).delete(empleado);
            }
        }

        @Test
        @DisplayName("lanza excepción si el que elimina no es jefe")
        void lanzaExcepcionSiNoEsJefe() {
            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getUsuarioId).thenReturn(1L);
                when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioEmpleado(1L, "empleado1")));

                assertThatThrownBy(() -> usuarioService.eliminar(2L))
                        .isInstanceOf(IllegalArgumentException.class);

                verify(usuarioRepository, never()).delete(any());
            }
        }

        @Test
        @DisplayName("lanza excepción si se intenta eliminar a un jefe de cocina")
        void lanzaExcepcionSiTargetEsJefe() {
            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getUsuarioId).thenReturn(1L);
                mocked.when(SecurityUtils::getNegocioId).thenReturn(10L);
                when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioJefe(1L, "jefe1")));
                when(usuarioRepository.findByIdAndNegocioId(2L, 10L)).thenReturn(Optional.of(usuarioJefe(2L, "jefe2")));

                assertThatThrownBy(() -> usuarioService.eliminar(2L))
                        .isInstanceOf(IllegalArgumentException.class);

                verify(usuarioRepository, never()).delete(any());
            }
        }

        @Test
        @DisplayName("lanza excepción si el usuario a eliminar no existe")
        void lanzaExcepcionUsuarioNoExiste() {
            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getUsuarioId).thenReturn(1L);
                mocked.when(SecurityUtils::getNegocioId).thenReturn(10L);
                when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioJefe(1L, "jefe1")));
                when(usuarioRepository.findByIdAndNegocioId(99L, 10L)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> usuarioService.eliminar(99L))
                        .isInstanceOf(RecursoNoEncontradoException.class);

                verify(usuarioRepository, never()).delete(any());
            }
        }

        @Test
        @DisplayName("lanza excepción (404, no 400) si el usuario a eliminar pertenece a OTRO negocio")
        void lanzaExcepcionSiUsuarioPerteneceAOtroNegocio() {
            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getUsuarioId).thenReturn(1L);
                mocked.when(SecurityUtils::getNegocioId).thenReturn(10L);
                when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioJefe(1L, "jefe1")));
                // El empleado con id=2 existe, pero en otro negocio (20L):
                // findByIdAndNegocioId(2L, 10L) no lo encuentra, igual que un
                // id inexistente — nunca revela que el id existe bajo otro tenant.
                when(usuarioRepository.findByIdAndNegocioId(2L, 10L)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> usuarioService.eliminar(2L))
                        .isInstanceOf(RecursoNoEncontradoException.class);

                verify(usuarioRepository, never()).delete(any());
            }
        }
    }

    // ─── actualizarEmail() ──────────────────────────────────────────────────

    @Nested
    @DisplayName("actualizarEmail()")
    class ActualizarEmail {

        @Test
        @DisplayName("actualiza el email del usuario autenticado")
        void actualizaEmailOk() {
            Usuario usuario = usuarioEmpleado(1L, "empleado1");

            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getUsuarioId).thenReturn(1L);
                when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
                when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

                UsuarioResponse response = usuarioService.actualizarEmail("nuevo@email.com");

                assertThat(response.getEmail()).isEqualTo("nuevo@email.com");
                verify(usuarioRepository).save(any());
            }
        }

        @Test
        @DisplayName("lanza excepción si no se aporta email")
        void lanzaExcepcionSinEmail() {
            Usuario usuario = usuarioEmpleado(1L, "empleado1");
            usuario.setEmail("actual@email.com");

            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getUsuarioId).thenReturn(1L);
                when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

                assertThatThrownBy(() -> usuarioService.actualizarEmail(" "))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("correo electrónico es obligatorio");

                verify(usuarioRepository, never()).save(any());
            }
        }

        @Test
        @DisplayName("lanza excepción si el email ya pertenece a otro usuario")
        void lanzaExcepcionEmailYaUsadoPorOtroUsuario() {
            Usuario usuario = usuarioEmpleado(1L, "empleado1");
            usuario.setEmail("actual@email.com");

            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getUsuarioId).thenReturn(1L);
                when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
                when(usuarioRepository.existsByEmail("otro@email.com")).thenReturn(true);

                assertThatThrownBy(() -> usuarioService.actualizarEmail("otro@email.com"))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("correo electrónico ya está en uso");

                verify(usuarioRepository, never()).save(any());
            }
        }

        @Test
        @DisplayName("permite reenviar el propio email actual sin cambios (no-op)")
        void permiteReenviarPropioEmailSinCambios() {
            Usuario usuario = usuarioEmpleado(1L, "empleado1");
            usuario.setEmail("actual@email.com");

            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getUsuarioId).thenReturn(1L);
                when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
                when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

                UsuarioResponse response = usuarioService.actualizarEmail("actual@email.com");

                assertThat(response.getEmail()).isEqualTo("actual@email.com");
                verify(usuarioRepository, never()).existsByEmail(any());
                verify(usuarioRepository).save(any());
            }
        }
    }
}
