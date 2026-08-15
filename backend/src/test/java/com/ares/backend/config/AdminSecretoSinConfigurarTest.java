package com.ares.backend.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fail-closed: cuando {@code app.admin.token-hash} está en blanco (secreto
 * nunca configurado en el entorno), {@code /api/admin/**} debe rechazar
 * SIEMPRE, incluso presentando el token que sería válido en cualquier otro
 * entorno con el hash configurado (ver
 * {@link com.ares.backend.integration.AdminContainmentIntegrationTest}).
 * Mismo patrón que {@code SwaggerGatingEnabledTest}: sobreescribe una
 * propiedad concreta vía {@code @TestPropertySource} en un contexto Spring
 * separado.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "app.admin.token-hash=")
class AdminSecretoSinConfigurarTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("app.admin.token-hash en blanco -> 403 en /api/admin/**, con o sin cabecera X-Admin-Token")
    void secretoEnBlanco_403SiempreEnAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/negocios")
                        .header("X-Admin-Token", "s3cr3t-admin-token-for-tests"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/negocios"))
                .andExpect(status().isForbidden());
    }
}
