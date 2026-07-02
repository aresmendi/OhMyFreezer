package com.ares.backend.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifica, con el perfil de test (Swagger deshabilitado por defecto), que:
 * - los endpoints de Swagger/OpenAPI NO tienen un permitAll incondicional
 *   (caen bajo anyRequest().authenticated(), 401 sin token);
 * - la lista de cabeceras CORS permitidas es explícita, nunca "*".
 */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigCorsTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Sin Swagger habilitado, /v3/api-docs no autenticado no tiene permitAll (403, igual que el resto de endpoints protegidos)")
    void swaggerSinHabilitar_requiereAutenticacion() throws Exception {
        // Esta app no configura un AuthenticationEntryPoint propio, así que
        // Spring Security responde 403 (no 401) a peticiones anónimas
        // denegadas por anyRequest().authenticated() — mismo comportamiento
        // que cualquier otro endpoint protegido de la API. Lo relevante aquí
        // es que YA NO hay un permitAll incondicional (antes daba 500 al
        // colar la petición hasta el DispatcherServlet sin doc registrado).
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("El preflight CORS expone solo las cabeceras explícitas, nunca '*'")
    void preflightCors_soloCabecerasExplicitas() throws Exception {
        mockMvc.perform(options("/api/usuarios/login")
                        .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.POST.name())
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.is("*"))))
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
                        org.hamcrest.Matchers.containsString("Authorization")));
    }
}
