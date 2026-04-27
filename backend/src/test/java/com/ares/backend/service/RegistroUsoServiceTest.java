package com.ares.backend.service;

import com.ares.backend.config.SecurityUtils;
import com.ares.backend.dto.RegistroUsoResponse;
import com.ares.backend.entity.Receta;
import com.ares.backend.entity.RegistroUsoReceta;
import com.ares.backend.entity.Usuario;
import com.ares.backend.repository.RegistroUsoRecetaRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistroUsoServiceTest {

    @Mock private RegistroUsoRecetaRepository registroUsoRecetaRepository;
    @Mock private UsuarioService usuarioService;

    @InjectMocks
    private RegistroUsoService registroUsoService;

    // ─── Helpers ────────────────────────────────────────────────────────────

    private Usuario usuario(Long id) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setUsername("usuario-" + id);
        u.setPassword("hashed");
        u.setEsJefeCocina(false);
        u.setFechaRegistro(LocalDateTime.now());
        return u;
    }

    private Receta receta(Long id) {
        Receta r = new Receta();
        r.setId(id);
        r.setNombre("receta-" + id);
        r.setDescripcion("desc");
        r.setFechaCreacion(LocalDateTime.now());
        r.setCreadaPor(usuario(99L));
        r.setPasos(new java.util.ArrayList<>());
        r.setIngredientes(new java.util.ArrayList<>());
        return r;
    }

    private RegistroUsoReceta registroGuardado(Receta receta, Usuario usuario, boolean completada) {
        RegistroUsoReceta reg = new RegistroUsoReceta();
        reg.setId(1L);
        reg.setReceta(receta);
        reg.setUsuario(usuario);
        reg.setFechaElaboracion(LocalDateTime.now());
        reg.setCompletada(completada);
        return reg;
    }

    // ─── crear() ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("crear()")
    class Crear {

        @Test
        @DisplayName("crea registro de uso completado correctamente")
        void creaRegistroCompletado() {
            Receta receta = receta(1L);
            Usuario usuario = usuario(1L);

            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getUsuarioId).thenReturn(1L);
                when(usuarioService.buscarPorId(1L)).thenReturn(usuario);
                when(registroUsoRecetaRepository.save(any()))
                        .thenReturn(registroGuardado(receta, usuario, true));

                RegistroUsoResponse response = registroUsoService.crear(receta, true);

                assertThat(response).isNotNull();
                assertThat(response.getCompletada()).isTrue();
                verify(registroUsoRecetaRepository).save(any());
            }
        }

        @Test
        @DisplayName("crea registro de uso fallido correctamente")
        void creaRegistroFallido() {
            Receta receta = receta(1L);
            Usuario usuario = usuario(1L);

            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getUsuarioId).thenReturn(1L);
                when(usuarioService.buscarPorId(1L)).thenReturn(usuario);
                when(registroUsoRecetaRepository.save(any()))
                        .thenReturn(registroGuardado(receta, usuario, false));

                RegistroUsoResponse response = registroUsoService.crear(receta, false);

                assertThat(response.getCompletada()).isFalse();
            }
        }
    }

    // ─── obtenerPorReceta() ─────────────────────────────────────────────────

    @Nested
    @DisplayName("obtenerPorReceta()")
    class ObtenerPorReceta {

        @Test
        @DisplayName("devuelve los registros de la receta indicada")
        void devuelveRegistrosDeLaReceta() {
            Receta receta = receta(1L);
            Usuario usuario = usuario(1L);
            when(registroUsoRecetaRepository.findByRecetaId(1L))
                    .thenReturn(List.of(registroGuardado(receta, usuario, true)));

            List<RegistroUsoResponse> result = registroUsoService.obtenerPorReceta(1L);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("devuelve lista vacía si no hay registros para esa receta")
        void devuelveVacioSinRegistros() {
            when(registroUsoRecetaRepository.findByRecetaId(99L)).thenReturn(List.of());

            assertThat(registroUsoService.obtenerPorReceta(99L)).isEmpty();
        }
    }

    // ─── eliminarPorReceta() ─────────────────────────────────────────────────

    @Nested
    @DisplayName("eliminarPorReceta()")
    class EliminarPorReceta {

        @Test
        @DisplayName("delega la eliminación al repositorio")
        void eliminaRegistrosDeLaReceta() {
            Receta receta = receta(1L);

            registroUsoService.eliminarPorReceta(receta);

            verify(registroUsoRecetaRepository).deleteByReceta(receta);
        }
    }
}
