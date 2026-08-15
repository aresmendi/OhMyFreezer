package com.ares.backend.dto;

import com.ares.backend.entity.Negocio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitarios puros para NegocioAdminResponse: verifica que expone todos
 * los campos administrativos del Negocio origen (sin ningún dato tenant-owned,
 * este DTO solo representa el propio Negocio).
 */
class NegocioAdminResponseTest {

    private Negocio negocioConId(Long id, String nombre, String emailContacto) {
        Negocio negocio = new Negocio(nombre, emailContacto);
        negocio.setId(id);
        return negocio;
    }

    @Test
    @DisplayName("expone id, nombre, plan, fechaAlta y emailContacto del Negocio origen")
    void constructor_conNegocio_exponeTodosLosCampos() {
        Negocio negocio = negocioConId(1L, "Cocina Central", "contacto@cocinacentral.com");
        LocalDateTime fechaAlta = negocio.getFechaAlta();

        NegocioAdminResponse response = new NegocioAdminResponse(negocio);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getNombre()).isEqualTo("Cocina Central");
        assertThat(response.getPlan()).isEqualTo("FREE");
        assertThat(response.getFechaAlta()).isEqualTo(fechaAlta);
        assertThat(response.getEmailContacto()).isEqualTo("contacto@cocinacentral.com");
    }

    @Test
    @DisplayName("distingue negocios distintos (triangulación)")
    void constructor_conOtroNegocio_exponeOtrosValores() {
        Negocio negocio = negocioConId(2L, "Otro Negocio", null);

        NegocioAdminResponse response = new NegocioAdminResponse(negocio);

        assertThat(response.getId()).isEqualTo(2L);
        assertThat(response.getNombre()).isEqualTo("Otro Negocio");
        assertThat(response.getEmailContacto()).isNull();
    }
}
