package com.ares.backend.dto;

import com.ares.backend.entity.Negocio;
import com.ares.backend.entity.NegocioSignupCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitarios puros para NegocioCreadoResponse: envuelve el Negocio recién
 * creado y su primer código de alta en una única respuesta (contrato de
 * {@code POST /api/admin/negocios}). Se construye a partir del
 * NegocioSignupCode devuelto por NegocioAdminService.crearNegocio(), cuyo
 * getNegocio() expone el Negocio recién creado.
 */
class NegocioCreadoResponseTest {

    @Test
    @DisplayName("expone tanto el negocio como su primer código de alta a partir del NegocioSignupCode devuelto por el servicio")
    void constructor_conPrimerCodigo_exponeNegocioYCodigo() {
        Negocio negocio = new Negocio("Cocina Nueva", "hola@cocinanueva.com");
        negocio.setId(3L);
        NegocioSignupCode signupCode = new NegocioSignupCode(negocio, "PRIMERCODIGO1");
        signupCode.setId(11L);

        NegocioCreadoResponse response = new NegocioCreadoResponse(signupCode);

        assertThat(response.getNegocio().getId()).isEqualTo(3L);
        assertThat(response.getNegocio().getNombre()).isEqualTo("Cocina Nueva");
        assertThat(response.getSignupCode().getId()).isEqualTo(11L);
        assertThat(response.getSignupCode().getCodigo()).isEqualTo("PRIMERCODIGO1");
        assertThat(response.getSignupCode().getNegocioId()).isEqualTo(3L);
    }

    @Test
    @DisplayName("distingue otro negocio y otro código (triangulación)")
    void constructor_conOtroPrimerCodigo_exponeOtrosValores() {
        Negocio negocio = new Negocio("Otra Cocina", null);
        negocio.setId(4L);
        NegocioSignupCode signupCode = new NegocioSignupCode(negocio, "SEGUNDOCODIGO2");
        signupCode.setId(12L);

        NegocioCreadoResponse response = new NegocioCreadoResponse(signupCode);

        assertThat(response.getNegocio().getId()).isEqualTo(4L);
        assertThat(response.getSignupCode().getCodigo()).isEqualTo("SEGUNDOCODIGO2");
    }
}
