package com.ares.backend.service;

import com.ares.backend.entity.Negocio;
import com.ares.backend.entity.TpvApiKey;
import com.ares.backend.entity.Usuario;
import com.ares.backend.repository.TpvApiKeyRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios para {@link TpvAutenticacionService}: resolución de la
 * clave por prefijo (D-D del diseño) — un único bcrypt.matches TRAS el
 * lookup indexado, nunca un escaneo. Usado por {@code TpvApiKeyFilter}.
 */
@ExtendWith(MockitoExtension.class)
class TpvAutenticacionServiceTest {

    @Mock
    private TpvApiKeyRepository tpvApiKeyRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private TpvAutenticacionService tpvAutenticacionService;

    private TpvApiKey claveActiva(String prefijo, String secretoHash, Usuario usuarioSistema) {
        Negocio negocio = new Negocio("Negocio TPV", "tpv@test.com");
        negocio.setId(1L);
        return new TpvApiKey(negocio, prefijo, secretoHash, usuarioSistema);
    }

    private Usuario usuarioSistema() {
        Usuario u = new Usuario("tpv-system", "sentinel", false);
        u.setId(99L);
        u.setEmail("tpv+negocio-1@tpv.ohmyfreezer.invalid");
        return u;
    }

    @Nested
    @DisplayName("autenticar()")
    class Autenticar {

        @Test
        @DisplayName("clave activa con secreto correcto -> devuelve el usuario sistema")
        void claveActivaSecretoCorrecto_devuelveUsuarioSistema() {
            Usuario usuarioSistema = usuarioSistema();
            TpvApiKey clave = claveActiva("pfx123456789", "hash-bcrypt", usuarioSistema);

            when(tpvApiKeyRepository.findByPrefijoAndActivaTrue("pfx123456789"))
                    .thenReturn(Optional.of(clave));
            when(passwordEncoder.matches("secreto-en-claro", "hash-bcrypt")).thenReturn(true);

            Optional<Usuario> resultado = tpvAutenticacionService.autenticar("omf_tpv_pfx123456789_secreto-en-claro");

            assertThat(resultado).isPresent();
            assertThat(resultado.get()).isEqualTo(usuarioSistema);
        }

        @Test
        @DisplayName("secreto incorrecto -> Optional vacío")
        void secretoIncorrecto_devuelveVacio() {
            Usuario usuarioSistema = usuarioSistema();
            TpvApiKey clave = claveActiva("pfx123456789", "hash-bcrypt", usuarioSistema);

            when(tpvApiKeyRepository.findByPrefijoAndActivaTrue("pfx123456789"))
                    .thenReturn(Optional.of(clave));
            when(passwordEncoder.matches("secreto-incorrecto", "hash-bcrypt")).thenReturn(false);

            Optional<Usuario> resultado = tpvAutenticacionService.autenticar("omf_tpv_pfx123456789_secreto-incorrecto");

            assertThat(resultado).isEmpty();
        }

        @Test
        @DisplayName("prefijo desconocido -> Optional vacío, sin invocar bcrypt (nunca hace un escaneo)")
        void prefijoDesconocido_devuelveVacioSinBcrypt() {
            when(tpvApiKeyRepository.findByPrefijoAndActivaTrue("no-existe"))
                    .thenReturn(Optional.empty());

            Optional<Usuario> resultado = tpvAutenticacionService.autenticar("omf_tpv_no-existe_secreto");

            assertThat(resultado).isEmpty();
            verify(passwordEncoder, never()).matches(anyString(), anyString());
        }

        @Test
        @DisplayName("credencial revocada -> findByPrefijoAndActivaTrue ya no la resuelve -> Optional vacío")
        void credencialRevocada_devuelveVacio() {
            when(tpvApiKeyRepository.findByPrefijoAndActivaTrue("pfx-revocada"))
                    .thenReturn(Optional.empty());

            Optional<Usuario> resultado = tpvAutenticacionService.autenticar("omf_tpv_pfx-revocada_secreto");

            assertThat(resultado).isEmpty();
        }

        @Test
        @DisplayName("formato inválido (sin el prefijo omf_tpv_) -> Optional vacío, sin tocar el repositorio")
        void formatoInvalidoSinPrefijoFijo_devuelveVacioSinRepositorio() {
            Optional<Usuario> resultado = tpvAutenticacionService.autenticar("clave-cualquiera");

            assertThat(resultado).isEmpty();
            verifyNoInteractions(tpvApiKeyRepository, passwordEncoder);
        }

        @Test
        @DisplayName("formato inválido (sin separador entre prefijo y secreto) -> Optional vacío")
        void formatoInvalidoSinSeparador_devuelveVacio() {
            Optional<Usuario> resultado = tpvAutenticacionService.autenticar("omf_tpv_soloprefijosinseparador");

            assertThat(resultado).isEmpty();
            verifyNoInteractions(tpvApiKeyRepository, passwordEncoder);
        }

        @Test
        @DisplayName("clave null -> Optional vacío")
        void claveNull_devuelveVacio() {
            Optional<Usuario> resultado = tpvAutenticacionService.autenticar(null);

            assertThat(resultado).isEmpty();
            verifyNoInteractions(tpvApiKeyRepository, passwordEncoder);
        }
    }
}
