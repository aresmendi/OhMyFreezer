package com.ares.backend.service;

import com.ares.backend.dto.UsuarioLoginRequest;
import com.ares.backend.dto.UsuarioRegisterRequest;
import com.ares.backend.dto.UsuarioResponse;
import com.ares.backend.entity.Usuario;
import com.ares.backend.repository.RecetaFavoritaRepository;
import com.ares.backend.repository.RegistroUsoRecetaRepository;
import com.ares.backend.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.ares.backend.config.SecurityUtils;

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

    @InjectMocks
    private UsuarioService usuarioService;

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

    // ─── registrar() ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("registrar()")
    class Registrar {

        private UsuarioRegisterRequest requestEmpleado() {
            UsuarioRegisterRequest r = new UsuarioRegisterRequest();
            r.setUsername("empleado1");
            r.setPassword("password123");
            r.setEsJefeCocina(false);
            return r;
        }

        @Test
        @DisplayName("registra un empleado correctamente")
        void registraEmpleadoOk() {
            when(usuarioRepository.existsByUsername("empleado1")).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("hashed");
            when(usuarioRepository.save(any())).thenAnswer(inv -> {
                Usuario u = inv.getArgument(0);
                u.setId(1L);
                return u;
            });

            UsuarioResponse response = usuarioService.registrar(requestEmpleado());

            assertThat(response.getUsername()).isEqualTo("empleado1");
            verify(usuarioRepository).save(any(Usuario.class));
        }

        @Test
        @DisplayName("lanza excepción si el username ya existe")
        void lanzaExcepcionUsernameExistente() {
            when(usuarioRepository.existsByUsername("empleado1")).thenReturn(true);

            assertThatThrownBy(() -> usuarioService.registrar(requestEmpleado()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("ya existe");

            verify(usuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("lanza excepción si jefe no tiene email")
        void lanzaExcepcionJefeSinEmail() {
            UsuarioRegisterRequest request = new UsuarioRegisterRequest();
            request.setUsername("jefe1");
            request.setPassword("password123");
            request.setEsJefeCocina(true);
            request.setCodigoJefe("CODIGO_VALIDO");
            request.setEmail(null);

            when(usuarioRepository.existsByUsername("jefe1")).thenReturn(false);

            assertThatThrownBy(() -> usuarioService.registrar(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("correo electrónico es obligatorio");

            verify(usuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("lanza excepción si el código de jefe es inválido")
        void lanzaExcepcionCodigoJefeInvalido() {
            UsuarioRegisterRequest request = new UsuarioRegisterRequest();
            request.setUsername("jefe1");
            request.setPassword("password123");
            request.setEsJefeCocina(true);
            request.setCodigoJefe("CODIGO_INCORRECTO");
            request.setEmail("jefe@test.com");

            when(usuarioRepository.existsByUsername("jefe1")).thenReturn(false);

            assertThatThrownBy(() -> usuarioService.registrar(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Código de jefe de cocina inválido");

            verify(usuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("hashea la contraseña antes de guardar")
        void hasheoContrasena() {
            when(usuarioRepository.existsByUsername("empleado1")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("hashed_password");
            when(usuarioRepository.save(any())).thenAnswer(inv -> {
                Usuario u = inv.getArgument(0);
                u.setId(1L);
                return u;
            });

            usuarioService.registrar(requestEmpleado());

            verify(passwordEncoder).encode("password123");
            verify(usuarioRepository).save(argThat(u ->
                    "hashed_password".equals(u.getPassword())
            ));
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
            when(usuarioRepository.findByUsername("empleado1")).thenReturn(Optional.of(usuario));
            when(passwordEncoder.matches("password123", "hashed_password")).thenReturn(true);

            UsuarioLoginRequest request = new UsuarioLoginRequest();
            request.setUsername("empleado1");
            request.setPassword("password123");

            UsuarioResponse response = usuarioService.login(request);

            assertThat(response.getUsername()).isEqualTo("empleado1");
        }

        @Test
        @DisplayName("lanza excepción si el usuario no existe")
        void lanzaExcepcionUsuarioNoExiste() {
            when(usuarioRepository.findByUsername("noexiste")).thenReturn(Optional.empty());

            UsuarioLoginRequest request = new UsuarioLoginRequest();
            request.setUsername("noexiste");
            request.setPassword("cualquiera");

            assertThatThrownBy(() -> usuarioService.login(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Usuario o contraseña incorrectos");
        }

        @Test
        @DisplayName("lanza excepción si la contraseña es incorrecta")
        void lanzaExcepcionContrasenaIncorrecta() {
            Usuario usuario = usuarioEmpleado(1L, "empleado1");
            when(usuarioRepository.findByUsername("empleado1")).thenReturn(Optional.of(usuario));
            when(passwordEncoder.matches("wrongpassword", "hashed_password")).thenReturn(false);

            UsuarioLoginRequest request = new UsuarioLoginRequest();
            request.setUsername("empleado1");
            request.setPassword("wrongpassword");

            assertThatThrownBy(() -> usuarioService.login(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Usuario o contraseña incorrectos");
        }

        @Test
        @DisplayName("no revela si el usuario existe o no (mismo mensaje de error)")
        void mensajeErrorGenerico() {
            // Seguridad: el mensaje debe ser idéntico tanto si no existe el usuario
            // como si la contraseña es incorrecta — para no dar pistas a atacantes
            when(usuarioRepository.findByUsername(anyString())).thenReturn(Optional.empty());

            UsuarioLoginRequest request = new UsuarioLoginRequest();
            request.setUsername("cualquiera");
            request.setPassword("cualquiera");

            assertThatThrownBy(() -> usuarioService.login(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Usuario o contraseña incorrectos");
        }
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
                when(usuarioRepository.findById(1L)).thenReturn(Optional.of(jefe));
                when(usuarioRepository.findById(2L)).thenReturn(Optional.of(empleado));

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
                when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioJefe(1L, "jefe1")));
                when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuarioJefe(2L, "jefe2")));

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
                when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioJefe(1L, "jefe1")));
                when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> usuarioService.eliminar(99L))
                        .isInstanceOf(IllegalArgumentException.class);

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
    }
}
