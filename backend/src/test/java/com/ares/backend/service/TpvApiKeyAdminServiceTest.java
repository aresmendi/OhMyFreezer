package com.ares.backend.service;

import com.ares.backend.entity.Negocio;
import com.ares.backend.entity.TpvApiKey;
import com.ares.backend.entity.Usuario;
import com.ares.backend.exception.RecursoNoEncontradoException;
import com.ares.backend.repository.NegocioRepository;
import com.ares.backend.repository.TpvApiKeyRepository;
import com.ares.backend.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios para {@link TpvApiKeyAdminService}: provisioning
 * atómico del usuario sintético, revocación de la credencial anterior al
 * reemitir (D3), y ciclo de vida completo (D-G del diseño: SOLO
 * repositorios + PasswordEncoder, NUNCA UsuarioService).
 */
@ExtendWith(MockitoExtension.class)
class TpvApiKeyAdminServiceTest {

    @Mock
    private TpvApiKeyRepository tpvApiKeyRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private NegocioRepository negocioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private TpvApiKeyAdminService tpvApiKeyAdminService;

    private Negocio negocio(Long id) {
        Negocio n = new Negocio("Negocio " + id, "negocio" + id + "@test.com");
        n.setId(id);
        return n;
    }

    // ─── emitir() ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("emitir()")
    class Emitir {

        @Test
        @DisplayName("lanza excepción si el negocio no existe")
        void lanzaExcepcionSiNegocioNoExiste() {
            when(negocioRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> tpvApiKeyAdminService.emitir(99L))
                    .isInstanceOf(RecursoNoEncontradoException.class);

            verify(tpvApiKeyRepository, never()).save(any());
        }

        @Test
        @DisplayName("primera emisión: provisiona un Usuario sintético con el email reservado del negocio")
        void primeraEmision_provisionaUsuarioSistema() {
            Negocio negocio = negocio(10L);
            when(negocioRepository.findById(10L)).thenReturn(Optional.of(negocio));
            when(usuarioRepository.findByEmail("tpv+negocio-10@tpv.ohmyfreezer.invalid"))
                    .thenReturn(Optional.empty());
            when(usuarioRepository.save(any())).thenAnswer(inv -> {
                Usuario u = inv.getArgument(0);
                u.setId(500L);
                return u;
            });
            when(passwordEncoder.encode(anyString())).thenReturn("hash-bcrypt");
            when(tpvApiKeyRepository.findByNegocioIdAndActivaTrue(10L)).thenReturn(Optional.empty());
            when(tpvApiKeyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            TpvApiKeyEmitida resultado = tpvApiKeyAdminService.emitir(10L);

            ArgumentCaptor<Usuario> captorUsuario = ArgumentCaptor.forClass(Usuario.class);
            verify(usuarioRepository).save(captorUsuario.capture());
            Usuario usuarioCreado = captorUsuario.getValue();
            assertThat(usuarioCreado.getEmail()).isEqualTo("tpv+negocio-10@tpv.ohmyfreezer.invalid");
            assertThat(usuarioCreado.getEsJefeCocina()).isFalse();
            assertThat(usuarioCreado.getNegocio()).isEqualTo(negocio);

            assertThat(resultado.clave().getUsuarioSistema()).isEqualTo(usuarioCreado);
            assertThat(resultado.clave().getSecretoHash()).isEqualTo("hash-bcrypt");
        }

        @Test
        @DisplayName("re-emisión: reutiliza el Usuario sintético ya provisionado, no crea uno nuevo")
        void reemision_reutilizaUsuarioSistemaExistente() {
            Negocio negocio = negocio(10L);
            Usuario usuarioExistente = new Usuario("tpv-system", "sentinel", false);
            usuarioExistente.setId(500L);
            usuarioExistente.setEmail("tpv+negocio-10@tpv.ohmyfreezer.invalid");
            usuarioExistente.setNegocio(negocio);

            when(negocioRepository.findById(10L)).thenReturn(Optional.of(negocio));
            when(usuarioRepository.findByEmail("tpv+negocio-10@tpv.ohmyfreezer.invalid"))
                    .thenReturn(Optional.of(usuarioExistente));
            when(passwordEncoder.encode(anyString())).thenReturn("hash-bcrypt");
            when(tpvApiKeyRepository.findByNegocioIdAndActivaTrue(10L)).thenReturn(Optional.empty());
            when(tpvApiKeyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            TpvApiKeyEmitida resultado = tpvApiKeyAdminService.emitir(10L);

            verify(usuarioRepository, never()).save(any());
            assertThat(resultado.clave().getUsuarioSistema()).isEqualTo(usuarioExistente);
        }

        @Test
        @DisplayName("re-emisión: revoca la credencial activa anterior del negocio")
        void reemision_revocaCredencialActivaAnterior() {
            Negocio negocio = negocio(10L);
            Usuario usuarioExistente = new Usuario("tpv-system", "sentinel", false);
            usuarioExistente.setId(500L);
            usuarioExistente.setEmail("tpv+negocio-10@tpv.ohmyfreezer.invalid");
            TpvApiKey claveAnterior = new TpvApiKey(negocio, "pfx-anterior", "hash-anterior", usuarioExistente);
            assertThat(claveAnterior.getActiva()).isTrue();

            when(negocioRepository.findById(10L)).thenReturn(Optional.of(negocio));
            when(usuarioRepository.findByEmail("tpv+negocio-10@tpv.ohmyfreezer.invalid"))
                    .thenReturn(Optional.of(usuarioExistente));
            when(passwordEncoder.encode(anyString())).thenReturn("hash-nueva");
            when(tpvApiKeyRepository.findByNegocioIdAndActivaTrue(10L)).thenReturn(Optional.of(claveAnterior));
            when(tpvApiKeyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            tpvApiKeyAdminService.emitir(10L);

            assertThat(claveAnterior.getActiva()).isFalse();
            assertThat(claveAnterior.getFechaRevocacion()).isNotNull();
        }

        @Test
        @DisplayName("devuelve la key completa en claro con el formato omf_tpv_<prefijo>_<secreto>")
        void devuelveKeyEnClaroConFormatoEsperado() {
            Negocio negocio = negocio(10L);
            when(negocioRepository.findById(10L)).thenReturn(Optional.of(negocio));
            when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.empty());
            when(usuarioRepository.save(any())).thenAnswer(inv -> {
                Usuario u = inv.getArgument(0);
                u.setId(500L);
                return u;
            });
            when(passwordEncoder.encode(anyString())).thenReturn("hash-bcrypt");
            when(tpvApiKeyRepository.findByNegocioIdAndActivaTrue(10L)).thenReturn(Optional.empty());
            when(tpvApiKeyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            TpvApiKeyEmitida resultado = tpvApiKeyAdminService.emitir(10L);

            assertThat(resultado.claveEnClaro()).matches("omf_tpv_[0-9A-Za-z]+_[0-9A-Za-z]+");
            assertThat(resultado.clave().getPrefijo()).isNotBlank();
            // El secreto en claro nunca se persiste: solo su hash (mockeado arriba)
            verify(passwordEncoder).encode(anyString());
        }
    }

    // ─── revocar() ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("revocar()")
    class Revocar {

        @Test
        @DisplayName("revoca la credencial existente")
        void revocaCredencialExistente() {
            Negocio negocio = negocio(10L);
            Usuario usuarioSistema = new Usuario("tpv-system", "sentinel", false);
            TpvApiKey clave = new TpvApiKey(negocio, "pfx-a-revocar", "hash", usuarioSistema);

            when(tpvApiKeyRepository.findById(5L)).thenReturn(Optional.of(clave));
            when(tpvApiKeyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            tpvApiKeyAdminService.revocar(5L);

            assertThat(clave.getActiva()).isFalse();
            assertThat(clave.getFechaRevocacion()).isNotNull();
        }

        @Test
        @DisplayName("lanza excepción si la credencial no existe")
        void lanzaExcepcionSiNoExiste() {
            when(tpvApiKeyRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> tpvApiKeyAdminService.revocar(99L))
                    .isInstanceOf(RecursoNoEncontradoException.class);
        }

        @Test
        @DisplayName("es idempotente: revocar dos veces la misma credencial no lanza excepción")
        void esIdempotente() {
            Negocio negocio = negocio(10L);
            Usuario usuarioSistema = new Usuario("tpv-system", "sentinel", false);
            TpvApiKey clave = new TpvApiKey(negocio, "pfx-idempotente", "hash", usuarioSistema);
            clave.revocar();

            when(tpvApiKeyRepository.findById(5L)).thenReturn(Optional.of(clave));
            when(tpvApiKeyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            tpvApiKeyAdminService.revocar(5L);

            assertThat(clave.getActiva()).isFalse();
            verify(tpvApiKeyRepository, times(1)).save(any());
        }
    }

    // ─── listar() ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("listar()")
    class Listar {

        @Test
        @DisplayName("lanza excepción si el negocio no existe")
        void lanzaExcepcionSiNegocioNoExiste() {
            when(negocioRepository.existsById(99L)).thenReturn(false);

            assertThatThrownBy(() -> tpvApiKeyAdminService.listar(99L))
                    .isInstanceOf(RecursoNoEncontradoException.class);
        }

        @Test
        @DisplayName("devuelve las credenciales del negocio, más reciente primero")
        void devuelveCredencialesDelNegocio() {
            Negocio negocio = negocio(10L);
            Usuario usuarioSistema = new Usuario("tpv-system", "sentinel", false);
            TpvApiKey c1 = new TpvApiKey(negocio, "pfx-1", "hash1", usuarioSistema);
            TpvApiKey c2 = new TpvApiKey(negocio, "pfx-2", "hash2", usuarioSistema);

            when(negocioRepository.existsById(10L)).thenReturn(true);
            when(tpvApiKeyRepository.findByNegocioIdOrderByFechaCreacionDesc(10L))
                    .thenReturn(List.of(c2, c1));

            List<TpvApiKey> resultado = tpvApiKeyAdminService.listar(10L);

            assertThat(resultado).containsExactly(c2, c1);
        }
    }
}
