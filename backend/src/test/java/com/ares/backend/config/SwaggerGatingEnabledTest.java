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
 * Regresión: cuando springdoc está explícitamente habilitado (equivalente al
 * perfil dev), los endpoints de documentación deben seguir siendo accesibles
 * sin autenticación.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "springdoc.api-docs.enabled=true",
        "springdoc.swagger-ui.enabled=true"
})
class SwaggerGatingEnabledTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Con Swagger habilitado, /v3/api-docs es accesible sin autenticación")
    void swaggerHabilitado_accesibleSinAutenticacion() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
    }
}
