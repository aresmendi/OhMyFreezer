package com.ares.backend.config;

import com.ares.backend.entity.Negocio;
import com.ares.backend.entity.Usuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitarios puros (sin contexto de Spring) para CustomUserDetails.
 * Verifican, en particular, que expone el negocioId del Usuario que envuelve
 * (multi-tenancy).
 */
class CustomUserDetailsTest {

    @Test
    @DisplayName("getNegocioId() devuelve el id del Negocio del Usuario envuelto")
    void getNegocioId_devuelveIdDelNegocioDelUsuario() {
        Negocio negocio = new Negocio("Negocio A", null);
        negocio.setId(5L);
        Usuario usuario = new Usuario("jefe", "hash", true);
        usuario.setNegocio(negocio);

        CustomUserDetails userDetails = new CustomUserDetails(usuario);

        assertThat(userDetails.getNegocioId()).isEqualTo(5L);
    }

    @Test
    @DisplayName("getNegocioId() distingue negocios distintos (triangulación)")
    void getNegocioId_conOtroNegocio_devuelveOtroId() {
        Negocio negocio = new Negocio("Negocio B", null);
        negocio.setId(99L);
        Usuario usuario = new Usuario("empleado", "hash", false);
        usuario.setNegocio(negocio);

        CustomUserDetails userDetails = new CustomUserDetails(usuario);

        assertThat(userDetails.getNegocioId()).isEqualTo(99L);
    }
}
