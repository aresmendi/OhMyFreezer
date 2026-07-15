package com.ares.backend.dto;

import com.ares.backend.entity.Negocio;
import com.ares.backend.entity.Usuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitarios puros para UsuarioResponse: verifican, en particular, que
 * expone el negocioId del Usuario origen (multi-tenancy).
 */
class UsuarioResponseTest {

    private Usuario usuarioConNegocio(Long negocioId) {
        Negocio negocio = new Negocio("Negocio Test", null);
        negocio.setId(negocioId);
        Usuario usuario = new Usuario("usuario", "hash", false);
        usuario.setId(1L);
        usuario.setFechaRegistro(LocalDateTime.now());
        usuario.setNegocio(negocio);
        return usuario;
    }

    @Test
    @DisplayName("expone el negocioId del Usuario origen")
    void constructor_conUsuarioConNegocio_exponeNegocioId() {
        UsuarioResponse response = new UsuarioResponse(usuarioConNegocio(6L));

        assertThat(response.getNegocioId()).isEqualTo(6L);
    }

    @Test
    @DisplayName("distingue negocios distintos (triangulación)")
    void constructor_conOtroNegocio_exponeOtroNegocioId() {
        UsuarioResponse response = new UsuarioResponse(usuarioConNegocio(30L));

        assertThat(response.getNegocioId()).isEqualTo(30L);
    }
}
