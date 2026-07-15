package com.ares.backend.service;

import com.ares.backend.config.SecurityUtils;
import com.ares.backend.dto.FavoritoRequest;
import com.ares.backend.dto.RecetaDetailResponse;
import com.ares.backend.dto.RecetaFavoritaResponse;
import com.ares.backend.entity.Receta;
import com.ares.backend.entity.RecetaFavorita;
import com.ares.backend.entity.Usuario;
import com.ares.backend.exception.RecursoNoEncontradoException;
import com.ares.backend.repository.RecetaFavoritaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FavoritoServiceTest {

    @Mock private RecetaFavoritaRepository favoritoRepository;
    @Mock private UsuarioService usuarioService;
    @Mock private RecetaService recetaService;

    @InjectMocks
    private FavoritoService favoritoService;

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
        r.setPasos(new ArrayList<>());
        r.setIngredientes(new ArrayList<>());
        return r;
    }

    private FavoritoRequest favoritoRequest(Long recetaId) {
        FavoritoRequest req = new FavoritoRequest();
        req.setRecetaId(recetaId);
        return req;
    }

    // ─── marcarFavorito() ───────────────────────────────────────────────────

    @Nested
    @DisplayName("marcarFavorito()")
    class MarcarFavorito {

        @Test
        @DisplayName("marca receta como favorita correctamente")
        void marcaFavoritoOk() {
            Usuario usuario = usuario(1L);
            Receta receta = receta(1L);

            RecetaFavorita favoritoGuardado = new RecetaFavorita(usuario, receta);
            favoritoGuardado.setId(1L);

            RecetaDetailResponse detalle = new RecetaDetailResponse();
            detalle.setId(1L);
            detalle.setNombre("receta-1");

            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getUsuarioId).thenReturn(1L);
                when(usuarioService.buscarPorId(1L)).thenReturn(usuario);
                when(recetaService.buscarPorId(1L)).thenReturn(receta);
                when(favoritoRepository.existsByUsuarioAndReceta(usuario, receta)).thenReturn(false);
                when(favoritoRepository.save(any())).thenReturn(favoritoGuardado);
                when(recetaService.obtenerPorId(1L)).thenReturn(detalle);

                RecetaFavoritaResponse response = favoritoService.marcarFavorito(favoritoRequest(1L));

                assertThat(response).isNotNull();
                verify(favoritoRepository).save(any());
                verify(recetaService).obtenerPorId(1L);
            }
        }

        @Test
        @DisplayName("lanza excepción si la receta ya es favorita")
        void lanzaExcepcionYaEsFavorita() {
            Usuario usuario = usuario(1L);
            Receta receta = receta(1L);

            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getUsuarioId).thenReturn(1L);
                when(usuarioService.buscarPorId(1L)).thenReturn(usuario);
                when(recetaService.buscarPorId(1L)).thenReturn(receta);
                when(favoritoRepository.existsByUsuarioAndReceta(usuario, receta)).thenReturn(true);

                assertThatThrownBy(() -> favoritoService.marcarFavorito(favoritoRequest(1L)))
                        .isInstanceOf(IllegalArgumentException.class);

                verify(favoritoRepository, never()).save(any());
            }
        }

        @Test
        @DisplayName("CROSS-TENANT: no se puede marcar como favorita una receta de otro negocio")
        void noPuedeMarcarRecetaDeOtroNegocioComoFavorita() {
            Usuario usuario = usuario(1L);

            // La receta 42 pertenece a otro negocio: RecetaService la scoped-resuelve
            // como inexistente para este caller, y FavoritoService propaga esa excepción
            // sin llegar a tocar el repositorio de favoritos.
            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getUsuarioId).thenReturn(1L);
                when(usuarioService.buscarPorId(1L)).thenReturn(usuario);
                when(recetaService.buscarPorId(42L))
                        .thenThrow(new RecursoNoEncontradoException("Receta no encontrada con ID: 42"));

                assertThatThrownBy(() -> favoritoService.marcarFavorito(favoritoRequest(42L)))
                        .isInstanceOf(RecursoNoEncontradoException.class);

                verify(favoritoRepository, never()).existsByUsuarioAndReceta(any(), any());
                verify(favoritoRepository, never()).save(any());
            }
        }
    }

    // ─── desmarcarFavorito() ────────────────────────────────────────────────

    @Nested
    @DisplayName("desmarcarFavorito()")
    class DesmarcarFavorito {

        @Test
        @DisplayName("desmarca receta favorita correctamente")
        void desmarcarFavoritoOk() {
            Usuario usuario = usuario(1L);
            Receta receta = receta(1L);

            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getUsuarioId).thenReturn(1L);
                when(usuarioService.buscarPorId(1L)).thenReturn(usuario);
                when(recetaService.buscarPorId(1L)).thenReturn(receta);
                when(favoritoRepository.existsByUsuarioAndReceta(usuario, receta)).thenReturn(true);

                favoritoService.desmarcarFavorito(favoritoRequest(1L));

                verify(favoritoRepository).deleteByUsuarioAndReceta(usuario, receta);
            }
        }

        @Test
        @DisplayName("lanza excepción si la receta no estaba marcada como favorita")
        void lanzaExcepcionNoEsFavorita() {
            Usuario usuario = usuario(1L);
            Receta receta = receta(1L);

            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getUsuarioId).thenReturn(1L);
                when(usuarioService.buscarPorId(1L)).thenReturn(usuario);
                when(recetaService.buscarPorId(1L)).thenReturn(receta);
                when(favoritoRepository.existsByUsuarioAndReceta(usuario, receta)).thenReturn(false);

                assertThatThrownBy(() -> favoritoService.desmarcarFavorito(favoritoRequest(1L)))
                        .isInstanceOf(IllegalArgumentException.class);

                verify(favoritoRepository, never()).deleteByUsuarioAndReceta(any(), any());
            }
        }

        @Test
        @DisplayName("CROSS-TENANT: no se puede desmarcar una receta de otro negocio")
        void noPuedeDesmarcarRecetaDeOtroNegocio() {
            Usuario usuario = usuario(1L);

            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getUsuarioId).thenReturn(1L);
                when(usuarioService.buscarPorId(1L)).thenReturn(usuario);
                when(recetaService.buscarPorId(42L))
                        .thenThrow(new RecursoNoEncontradoException("Receta no encontrada con ID: 42"));

                assertThatThrownBy(() -> favoritoService.desmarcarFavorito(favoritoRequest(42L)))
                        .isInstanceOf(RecursoNoEncontradoException.class);

                verify(favoritoRepository, never()).deleteByUsuarioAndReceta(any(), any());
            }
        }
    }

    // ─── esFavorita() ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("esFavorita()")
    class EsFavorita {

        @Test
        @DisplayName("devuelve true si la receta es favorita del usuario")
        void devuelveTrueSiEsFavorita() {
            Usuario usuario = usuario(1L);
            Receta receta = receta(1L);

            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getUsuarioId).thenReturn(1L);
                when(usuarioService.buscarPorId(1L)).thenReturn(usuario);
                when(recetaService.buscarPorId(1L)).thenReturn(receta);
                when(favoritoRepository.existsByUsuarioAndReceta(usuario, receta)).thenReturn(true);

                assertThat(favoritoService.esFavorita(1L)).isTrue();
            }
        }

        @Test
        @DisplayName("devuelve false si la receta no es favorita del usuario")
        void devuelveFalseSiNoEsFavorita() {
            Usuario usuario = usuario(1L);
            Receta receta = receta(1L);

            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getUsuarioId).thenReturn(1L);
                when(usuarioService.buscarPorId(1L)).thenReturn(usuario);
                when(recetaService.buscarPorId(1L)).thenReturn(receta);
                when(favoritoRepository.existsByUsuarioAndReceta(usuario, receta)).thenReturn(false);

                assertThat(favoritoService.esFavorita(1L)).isFalse();
            }
        }

        @Test
        @DisplayName("CROSS-TENANT: preguntar por una receta de otro negocio lanza RecursoNoEncontradoException, no 'false'")
        void preguntarPorRecetaDeOtroNegocioLanzaExcepcion() {
            Usuario usuario = usuario(1L);

            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getUsuarioId).thenReturn(1L);
                when(usuarioService.buscarPorId(1L)).thenReturn(usuario);
                when(recetaService.buscarPorId(42L))
                        .thenThrow(new RecursoNoEncontradoException("Receta no encontrada con ID: 42"));

                assertThatThrownBy(() -> favoritoService.esFavorita(42L))
                        .isInstanceOf(RecursoNoEncontradoException.class);

                verify(favoritoRepository, never()).existsByUsuarioAndReceta(any(), any());
            }
        }
    }

    // ─── obtenerFavoritosUsuario() ──────────────────────────────────────────

    @Nested
    @DisplayName("obtenerFavoritosUsuario()")
    class ObtenerFavoritosUsuario {

        @Test
        @DisplayName("devuelve lista de favoritos del usuario autenticado")
        void devuelveListaDeFavoritos() {
            Usuario usuario = usuario(1L);
            Receta receta = receta(1L);
            RecetaFavorita favorito = new RecetaFavorita(usuario, receta);
            favorito.setId(1L);

            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getUsuarioId).thenReturn(1L);
                when(favoritoRepository.findByUsuarioIdWithIngredientes(1L)).thenReturn(List.of(favorito));
                when(favoritoRepository.findByUsuarioIdWithPasos(1L)).thenReturn(List.of(favorito));

                List<RecetaFavoritaResponse> result = favoritoService.obtenerFavoritosUsuario();

                assertThat(result).hasSize(1);
                verify(favoritoRepository).findByUsuarioIdWithIngredientes(1L);
                verify(favoritoRepository).findByUsuarioIdWithPasos(1L);
            }
        }

        @Test
        @DisplayName("devuelve lista vacía si el usuario no tiene favoritos")
        void devuelveListaVacia() {
            try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
                mocked.when(SecurityUtils::getUsuarioId).thenReturn(1L);
                when(favoritoRepository.findByUsuarioIdWithIngredientes(1L)).thenReturn(List.of());
                when(favoritoRepository.findByUsuarioIdWithPasos(1L)).thenReturn(List.of());

                assertThat(favoritoService.obtenerFavoritosUsuario()).isEmpty();
            }
        }
    }
}
