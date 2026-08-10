package com.ares.backend.dto;

import com.ares.backend.entity.Negocio;
import com.ares.backend.entity.NegocioSignupCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitarios puros para SignupCodeResponse: en particular, que expone el
 * negocioId (no el objeto Negocio completo) y el código en texto plano
 * (confirmado assumption #3 — entrega manual, sin email automático).
 */
class SignupCodeResponseTest {

    private NegocioSignupCode codigoDeNegocio(Long negocioId, String codigo) {
        Negocio negocio = new Negocio("Negocio Test", null);
        negocio.setId(negocioId);
        NegocioSignupCode signupCode = new NegocioSignupCode(negocio, codigo);
        signupCode.setId(9L);
        return signupCode;
    }

    @Test
    @DisplayName("expone el código en texto plano y el negocioId (no el Negocio completo) de un código recién creado")
    void constructor_conCodigoRecienCreado_exponeCamposDelEstadoInicial() {
        NegocioSignupCode signupCode = codigoDeNegocio(5L, "ABCDEFGHJK");

        SignupCodeResponse response = new SignupCodeResponse(signupCode);

        assertThat(response.getId()).isEqualTo(9L);
        assertThat(response.getCodigo()).isEqualTo("ABCDEFGHJK");
        assertThat(response.getNegocioId()).isEqualTo(5L);
        assertThat(response.getUsado()).isFalse();
        assertThat(response.getActivo()).isTrue();
        assertThat(response.getUsadoPorUsuarioId()).isNull();
        assertThat(response.getFechaCreacion()).isEqualTo(signupCode.getFechaCreacion());
        assertThat(response.getFechaUso()).isNull();
    }

    @Test
    @DisplayName("expone el estado usado/revocado de un código ya consumido (triangulación)")
    void constructor_conCodigoUsado_exponeEstadoUsado() {
        NegocioSignupCode signupCode = codigoDeNegocio(7L, "ZYXWVUTSRQ");
        signupCode.marcarUsado(42L);

        SignupCodeResponse response = new SignupCodeResponse(signupCode);

        assertThat(response.getNegocioId()).isEqualTo(7L);
        assertThat(response.getUsado()).isTrue();
        assertThat(response.getUsadoPorUsuarioId()).isEqualTo(42L);
        assertThat(response.getFechaUso()).isEqualTo(signupCode.getFechaUso());
    }
}
