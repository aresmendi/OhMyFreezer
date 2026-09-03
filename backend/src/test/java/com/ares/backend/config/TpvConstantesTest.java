package com.ares.backend.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fija el contrato de {@link TpvConstantes}: es la ÚNICA fuente de verdad
 * para el dominio de email reservado del usuario sintético TPV (consumido
 * por provisioning, rechazo de login y exclusión de listados — ver diseño,
 * sección "Synthetic-User Containment"), el prefijo del formato de API key
 * (D-D del diseño) y los nombres de cabecera HTTP del endpoint de ingesta.
 * Un cambio accidental en cualquiera de estos valores rompe silenciosamente
 * la contención del usuario sintético o la autenticación TPV; este test
 * existe para que ese cambio falle ruidosamente en CI.
 */
class TpvConstantesTest {

    @Test
    void dominioEmailTpvEsElReservado() {
        assertThat(TpvConstantes.DOMINIO_EMAIL_TPV).isEqualTo("tpv.ohmyfreezer.invalid");
    }

    @Test
    void prefijoApiKeyEsElEsperado() {
        assertThat(TpvConstantes.PREFIJO_API_KEY).isEqualTo("omf_tpv_");
    }

    @Test
    void headersHttpSonLosEsperados() {
        assertThat(TpvConstantes.HEADER_API_KEY).isEqualTo("X-Tpv-Api-Key");
        assertThat(TpvConstantes.HEADER_PROVEEDOR).isEqualTo("X-Tpv-Proveedor");
    }
}
