package com.ares.backend.config;

import com.ares.backend.entity.Negocio;
import com.ares.backend.entity.Usuario;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests unitarios para SecurityUtils.getNegocioId(): debe leer el negocioId
 * del principal autenticado (CustomUserDetails) y lanzar si no hay
 * autenticación en el contexto de seguridad (mismo comportamiento que
 * getUsuarioId()).
 */
class SecurityUtilsTest {

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    private void autenticarComo(Long negocioId) {
        Negocio negocio = new Negocio("Negocio Test", null);
        negocio.setId(negocioId);
        Usuario usuario = new Usuario("usuario", "hash", false);
        usuario.setNegocio(negocio);
        CustomUserDetails principal = new CustomUserDetails(usuario);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @Test
    @DisplayName("getNegocioId() devuelve el negocioId del usuario autenticado")
    void getNegocioId_conUsuarioAutenticado_devuelveSuNegocioId() {
        autenticarComo(4L);

        assertThat(SecurityUtils.getNegocioId()).isEqualTo(4L);
    }

    @Test
    @DisplayName("getNegocioId() distingue negocios distintos (triangulación)")
    void getNegocioId_conOtroUsuarioAutenticado_devuelveOtroNegocioId() {
        autenticarComo(77L);

        assertThat(SecurityUtils.getNegocioId()).isEqualTo(77L);
    }

    @Test
    @DisplayName("getNegocioId() lanza si no hay autenticación en el contexto")
    void getNegocioId_sinAutenticacion_lanza() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(SecurityUtils::getNegocioId).isInstanceOf(RuntimeException.class);
    }
}
