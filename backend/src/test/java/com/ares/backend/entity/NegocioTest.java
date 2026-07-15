package com.ares.backend.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class NegocioTest {

    @Test
    void constructorConParametrosAsignaPlanFreePorDefectoYFechaAlta() {
        LocalDateTime antes = LocalDateTime.now();

        Negocio negocio = new Negocio("Restaurante Ares", "contacto@ares.dev");

        assertThat(negocio.getNombre()).isEqualTo("Restaurante Ares");
        assertThat(negocio.getEmailContacto()).isEqualTo("contacto@ares.dev");
        assertThat(negocio.getPlan()).isEqualTo("FREE");
        assertThat(negocio.getFechaAlta()).isAfterOrEqualTo(antes);
    }

    @Test
    void permiteEmailContactoNuloSinRomperElAlta() {
        Negocio negocio = new Negocio("Negocio Sin Email", null);

        assertThat(negocio.getEmailContacto()).isNull();
        assertThat(negocio.getNombre()).isEqualTo("Negocio Sin Email");
        assertThat(negocio.getPlan()).isEqualTo("FREE");
    }
}
