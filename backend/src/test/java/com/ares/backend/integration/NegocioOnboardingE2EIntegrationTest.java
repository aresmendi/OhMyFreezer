package com.ares.backend.integration;

import com.ares.backend.service.EmailService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de integración full-stack (Spring real + H2 + Spring Security real)
 * del flujo completo de onboarding de "negocio-onboarding-admin" (PR3):
 * el superadmin de plataforma crea un Negocio + primer código de alta vía la
 * API real, y ese código, sin ningún cambio en {@code UsuarioService.registrar()},
 * es consumido por el primer jefe que se registra. También prueba el
 * escenario espejo del spec: un código REVOCADO por el admin es rechazado en
 * el registro, con el mismo {@code registrar()} sin modificar.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class NegocioOnboardingE2EIntegrationTest {

    private static final String ADMIN_HEADER = "X-Admin-Token";
    /** Debe coincidir con el hash bcrypt configurado en test/resources/application.properties. */
    private static final String ADMIN_TOKEN_VALIDO = "s3cr3t-admin-token-for-tests";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private EmailService emailService;

    @Test
    @DisplayName("flujo completo: el admin crea un Negocio+código vía API, y un jefe se registra con ese código sin tocar registrar()")
    void flujoCompleto_adminCreaNegocioYJefeSeRegistraConElCodigo() throws Exception {
        // 1. El superadmin crea un Negocio + su primer código de alta.
        String crearNegocioBody = objectMapper.writeValueAsString(
                Map.of("nombre", "Cocina E2E", "emailContacto", "e2e@cocina.com"));

        String creadoJson = mockMvc.perform(post("/api/admin/negocios")
                        .header(ADMIN_HEADER, ADMIN_TOKEN_VALIDO)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(crearNegocioBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        JsonNode creado = objectMapper.readTree(creadoJson);
        Long negocioId = creado.get("negocio").get("id").asLong();
        String codigo = creado.get("signupCode").get("codigo").asText();
        assertThat(codigo).isNotBlank();

        // 2. Un jefe se registra con ese código, vía el flujo público SIN cambios (registrar()).
        Map<String, Object> registerBody = Map.of(
                "username", "jefeE2E",
                "password", "password123",
                "esJefeCocina", true,
                "codigoRegistro", codigo,
                "email", "jefeE2E@test.com"
        );
        mockMvc.perform(post("/api/usuarios/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerBody)))
                .andExpect(status().isCreated());

        // 3. GET /api/admin/negocios/{id}/signup-codes muestra el código ya usado.
        String codigosJson = mockMvc.perform(get("/api/admin/negocios/" + negocioId + "/signup-codes")
                        .header(ADMIN_HEADER, ADMIN_TOKEN_VALIDO))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode codigos = objectMapper.readTree(codigosJson);
        assertThat(codigos).hasSize(1);
        assertThat(codigos.get(0).get("usado").asBoolean()).isTrue();
        assertThat(codigos.get(0).get("usadoPorUsuarioId").asLong()).isPositive();
    }

    @Test
    @DisplayName("el admin lista los negocios recién creados por la API")
    void listarNegocios_muestraLosNegociosCreadosPorLaApi() throws Exception {
        mockMvc.perform(post("/api/admin/negocios")
                        .header(ADMIN_HEADER, ADMIN_TOKEN_VALIDO)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("nombre", "Cocina Listada"))))
                .andExpect(status().isCreated());

        String listaJson = mockMvc.perform(get("/api/admin/negocios")
                        .header(ADMIN_HEADER, ADMIN_TOKEN_VALIDO))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode lista = objectMapper.readTree(listaJson);
        boolean encontrado = false;
        for (JsonNode negocio : lista) {
            if ("Cocina Listada".equals(negocio.get("nombre").asText())) {
                encontrado = true;
            }
        }
        assertThat(encontrado).isTrue();
    }

    @Test
    @DisplayName("código revocado vía la API de admin es rechazado en el registro, sin ningún cambio en registrar()/marcarUsadoAtomico")
    void codigoRevocado_esRechazadoEnElRegistro() throws Exception {
        // 1. Admin crea el Negocio + código.
        String creadoJson = mockMvc.perform(post("/api/admin/negocios")
                        .header(ADMIN_HEADER, ADMIN_TOKEN_VALIDO)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("nombre", "Cocina Revocada"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        JsonNode creado = objectMapper.readTree(creadoJson);
        Long signupCodeId = creado.get("signupCode").get("id").asLong();
        String codigo = creado.get("signupCode").get("codigo").asText();

        // 2. Admin revoca el código ANTES de que nadie lo use.
        mockMvc.perform(post("/api/admin/signup-codes/" + signupCodeId + "/revoke")
                        .header(ADMIN_HEADER, ADMIN_TOKEN_VALIDO))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.activo").value(false));

        // 3. Un jefe intenta registrarse con el código ya revocado: 4xx, ningún Usuario creado.
        Map<String, Object> registerBody = Map.of(
                "username", "jefeRevocado",
                "password", "password123",
                "esJefeCocina", true,
                "codigoRegistro", codigo,
                "email", "jefeRevocado@test.com"
        );
        mockMvc.perform(post("/api/usuarios/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerBody)))
                .andExpect(status().isBadRequest());

        // 4. El login con esas credenciales falla: nunca se creó el Usuario.
        Map<String, Object> loginBody = Map.of("email", "jefeRevocado@test.com", "password", "password123");
        mockMvc.perform(post("/api/usuarios/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginBody)))
                .andExpect(status().is4xxClientError());
    }
}
