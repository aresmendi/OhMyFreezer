package com.ares.backend.service;

import com.ares.backend.repository.NegocioRepository;
import com.ares.backend.repository.NegocioSignupCodeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba, por reflexión, que {@link NegocioAdminService} SOLO declara como
 * colaboradores los 3 tipos permitidos por el diseño (D1-B de
 * "negocio-onboarding-admin"): {@link NegocioRepository}, {@link
 * NegocioSignupCodeRepository} y {@link SignupCodeGenerator}. El admin de
 * plataforma no es un {@code Usuario} ni pertenece a ningún Negocio, así
 * que este servicio NUNCA debe inyectar servicios tenant-scoped (p. ej.
 * {@code UsuarioService}, {@code IngredienteService}): inyectar cualquier
 * otro colaborador rompe este test y obliga a revisar por qué el admin
 * necesitaría cruzar hacia lógica de negocio tenant-scoped.
 */
class NegocioAdminServiceDependencyTest {

    @Test
    @DisplayName("los únicos campos declarados son NegocioRepository, NegocioSignupCodeRepository y SignupCodeGenerator")
    void soloDeclaraLosTresColaboradoresPermitidos() {
        // Se excluyen los campos static (p. ej. constantes como el número
        // máximo de intentos de generación de código): no son colaboradores
        // inyectados, son configuración interna del propio servicio.
        Set<Class<?>> tiposDeclarados = java.util.Arrays.stream(NegocioAdminService.class.getDeclaredFields())
                .filter(f -> !Modifier.isStatic(f.getModifiers()))
                .map(Field::getType)
                .collect(Collectors.toSet());

        assertThat(tiposDeclarados).containsExactlyInAnyOrder(
                NegocioRepository.class,
                NegocioSignupCodeRepository.class,
                SignupCodeGenerator.class);
    }
}
