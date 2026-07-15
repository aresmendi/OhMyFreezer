package com.ares.backend.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NegocioSignupCodeTest {

    private Negocio negocio() {
        return new Negocio("Negocio X", "x@ares.dev");
    }

    @Test
    void codigoRecienCreadoEsActivoNoUsadoYPorTantoValido() {
        NegocioSignupCode code = new NegocioSignupCode(negocio(), "ABC123");

        assertThat(code.getActivo()).isTrue();
        assertThat(code.getUsado()).isFalse();
        assertThat(code.esValido()).isTrue();
    }

    @Test
    void codigoUsadoDejaDeSerValidoAunqueSigaActivo() {
        NegocioSignupCode code = new NegocioSignupCode(negocio(), "USED1");

        code.marcarUsado(42L);

        assertThat(code.getUsado()).isTrue();
        assertThat(code.getUsadoPorUsuarioId()).isEqualTo(42L);
        assertThat(code.getFechaUso()).isNotNull();
        assertThat(code.esValido())
                .as("a used code must not be valid, even though activo is still true")
                .isFalse();
    }

    @Test
    void codigoRevocadoPorAdminDejaDeSerValidoAunqueNoSeHayaUsado() {
        NegocioSignupCode code = new NegocioSignupCode(negocio(), "REVOKED1");

        code.setActivo(false);

        assertThat(code.getUsado()).isFalse();
        assertThat(code.esValido())
                .as("a revoked (activo=false) code must not be valid, even though usado is false")
                .isFalse();
    }
}
