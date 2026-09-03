package com.ares.backend.service;

import com.ares.backend.repository.NegocioRepository;
import com.ares.backend.repository.TpvApiKeyRepository;
import com.ares.backend.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba, por reflexión, que {@link TpvApiKeyAdminService} SOLO declara
 * como colaboradores los 4 tipos permitidos por el diseño (D-G de
 * "tpv-integration"): {@link TpvApiKeyRepository}, {@link UsuarioRepository},
 * {@link NegocioRepository} y {@link PasswordEncoder}. Este servicio debe
 * usar {@code UsuarioRepository} DIRECTAMENTE, nunca {@code UsuarioService}
 * — cuyos métodos llaman a {@code SecurityUtils} y lanzarían
 * {@code ClassCastException} bajo el principal {@code String} plano
 * "platform-admin" que produce {@code AdminApiKeyFilter}. Inyectar
 * cualquier otro colaborador (en particular {@code UsuarioService}) rompe
 * este test — mismo patrón que {@code NegocioAdminServiceDependencyTest}.
 */
class TpvApiKeyAdminServiceDependencyTest {

    @Test
    @DisplayName("los únicos campos declarados son TpvApiKeyRepository, UsuarioRepository, NegocioRepository y PasswordEncoder")
    void soloDeclaraLosCuatroColaboradoresPermitidos() {
        Set<Class<?>> tiposDeclarados = java.util.Arrays.stream(TpvApiKeyAdminService.class.getDeclaredFields())
                .filter(f -> !Modifier.isStatic(f.getModifiers()))
                .map(Field::getType)
                .collect(Collectors.toSet());

        assertThat(tiposDeclarados).containsExactlyInAnyOrder(
                TpvApiKeyRepository.class,
                UsuarioRepository.class,
                NegocioRepository.class,
                PasswordEncoder.class);
    }
}
