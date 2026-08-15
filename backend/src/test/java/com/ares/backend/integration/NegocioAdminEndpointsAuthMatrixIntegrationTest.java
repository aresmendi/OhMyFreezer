package com.ares.backend.integration;

import com.ares.backend.entity.Negocio;
import com.ares.backend.entity.NegocioSignupCode;
import com.ares.backend.repository.NegocioRepository;
import com.ares.backend.repository.NegocioSignupCodeRepository;
import com.ares.backend.service.EmailService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Matriz de autenticación/autorización del spec ("admin authentication
 * boundary"), pero ahora ejercitada contra los 5 endpoints REALES de
 * {@link com.ares.backend.controller.NegocioAdminController} (PR3), no solo
 * contra {@code GET /api/admin/negocios} como hacía
 * {@link AdminContainmentIntegrationTest} en PR1 (cuando el controller
 * todavía no existía). Cada uno de los 5 endpoints debe:
 * <ul>
 *   <li>rechazar (403) sin credencial, con credencial incorrecta, y con un
 *       JWT de tenant válido (jefe o cocinero);</li>
 *   <li>aceptar (2xx) una credencial de admin válida.</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class NegocioAdminEndpointsAuthMatrixIntegrationTest {

    private static final String ADMIN_HEADER = "X-Admin-Token";
    /** Debe coincidir con el hash bcrypt configurado en test/resources/application.properties. */
    private static final String ADMIN_TOKEN_VALIDO = "s3cr3t-admin-token-for-tests";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private NegocioRepository negocioRepository;
    @Autowired private NegocioSignupCodeRepository negocioSignupCodeRepository;

    @MockitoBean private EmailService emailService;

    // ─── Helpers ────────────────────────────────────────────────────────────

    private Negocio provisionarNegocio(String nombre, String codigo) {
        Negocio negocio = negocioRepository.save(new Negocio(nombre, nombre + "@test.com"));
        negocioSignupCodeRepository.save(new NegocioSignupCode(negocio, codigo));
        return negocio;
    }

    private String registrarJefeYObtenerToken(String username, String codigo) throws Exception {
        Map<String, Object> registerBody = Map.of(
                "username", username,
                "password", "password123",
                "esJefeCocina", true,
                "codigoRegistro", codigo,
                "email", username + "@test.com"
        );
        mockMvc.perform(post("/api/usuarios/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerBody)))
                .andExpect(status().isCreated());

        return login(username + "@test.com", "password123");
    }

    private String login(String email, String password) throws Exception {
        Map<String, Object> loginBody = Map.of("email", email, "password", password);
        String responseJson = mockMvc.perform(post("/api/usuarios/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginBody)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(responseJson).get("token").asText();
    }

    private Long crearNegocioComoAdminYObtenerId() throws Exception {
        String json = mockMvc.perform(post("/api/admin/negocios")
                        .header(ADMIN_HEADER, ADMIN_TOKEN_VALIDO)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("nombre", "Negocio Matriz"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("negocio").get("id").asLong();
    }

    private Long crearSignupCodeComoAdminYObtenerId(Long negocioId) throws Exception {
        String json = mockMvc.perform(post("/api/admin/negocios/" + negocioId + "/signup-codes")
                        .header(ADMIN_HEADER, ADMIN_TOKEN_VALIDO))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("id").asLong();
    }

    private record CredencialesTenant(String tokenJefe, String tokenCocinero) {}

    private CredencialesTenant credencialesTenantValidas(String sufijo) throws Exception {
        provisionarNegocio("Negocio Matriz Tenant " + sufijo, "COD_MATRIZ_" + sufijo);
        String tokenJefe = registrarJefeYObtenerToken("jefeMatriz" + sufijo, "COD_MATRIZ_" + sufijo);

        String bodyCocinero = "{\"username\":\"cocineroMatriz" + sufijo + "\",\"password\":\"password123\","
                + "\"email\":\"cocineroMatriz" + sufijo + "@test.com\"}";
        mockMvc.perform(post("/api/usuarios/empleados")
                        .header("Authorization", "Bearer " + tokenJefe)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyCocinero))
                .andExpect(status().isCreated());
        String tokenCocinero = login("cocineroMatriz" + sufijo + "@test.com", "password123");

        return new CredencialesTenant(tokenJefe, tokenCocinero);
    }

    // ─── POST /api/admin/negocios ────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/admin/negocios")
    class CrearNegocioMatrix {

        @Test
        @DisplayName("sin credencial -> 403")
        void sinCredencial_403() throws Exception {
            mockMvc.perform(post("/api/admin/negocios")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("nombre", "X"))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("credencial incorrecta -> 403")
        void credencialIncorrecta_403() throws Exception {
            mockMvc.perform(post("/api/admin/negocios")
                            .header(ADMIN_HEADER, "credencial-incorrecta")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("nombre", "X"))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("JWT de tenant (jefe y cocinero) -> 403")
        void jwtDeTenant_403() throws Exception {
            CredencialesTenant tenant = credencialesTenantValidas("CN");

            mockMvc.perform(post("/api/admin/negocios")
                            .header("Authorization", "Bearer " + tenant.tokenJefe())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("nombre", "X"))))
                    .andExpect(status().isForbidden());

            mockMvc.perform(post("/api/admin/negocios")
                            .header("Authorization", "Bearer " + tenant.tokenCocinero())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("nombre", "X"))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("credencial de admin válida -> 201")
        void credencialValida_201() throws Exception {
            mockMvc.perform(post("/api/admin/negocios")
                            .header(ADMIN_HEADER, ADMIN_TOKEN_VALIDO)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("nombre", "Negocio Válido"))))
                    .andExpect(status().isCreated());
        }
    }

    // ─── POST /api/admin/negocios/{id}/signup-codes ──────────────────────

    @Nested
    @DisplayName("POST /api/admin/negocios/{id}/signup-codes")
    class GenerarCodigoMatrix {

        @Test
        @DisplayName("sin credencial -> 403")
        void sinCredencial_403() throws Exception {
            Long negocioId = crearNegocioComoAdminYObtenerId();

            mockMvc.perform(post("/api/admin/negocios/" + negocioId + "/signup-codes"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("JWT de tenant (jefe) -> 403")
        void jwtDeTenant_403() throws Exception {
            Long negocioId = crearNegocioComoAdminYObtenerId();
            CredencialesTenant tenant = credencialesTenantValidas("GC");

            mockMvc.perform(post("/api/admin/negocios/" + negocioId + "/signup-codes")
                            .header("Authorization", "Bearer " + tenant.tokenJefe()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("credencial de admin válida -> 201")
        void credencialValida_201() throws Exception {
            Long negocioId = crearNegocioComoAdminYObtenerId();

            mockMvc.perform(post("/api/admin/negocios/" + negocioId + "/signup-codes")
                            .header(ADMIN_HEADER, ADMIN_TOKEN_VALIDO))
                    .andExpect(status().isCreated());
        }
    }

    // ─── GET /api/admin/negocios ──────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/admin/negocios")
    class ListarNegociosMatrix {

        @Test
        @DisplayName("sin credencial -> 403")
        void sinCredencial_403() throws Exception {
            mockMvc.perform(get("/api/admin/negocios"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("JWT de tenant (cocinero) -> 403")
        void jwtDeTenant_403() throws Exception {
            CredencialesTenant tenant = credencialesTenantValidas("LN");

            mockMvc.perform(get("/api/admin/negocios")
                            .header("Authorization", "Bearer " + tenant.tokenCocinero()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("credencial de admin válida -> 200")
        void credencialValida_200() throws Exception {
            mockMvc.perform(get("/api/admin/negocios")
                            .header(ADMIN_HEADER, ADMIN_TOKEN_VALIDO))
                    .andExpect(status().isOk());
        }
    }

    // ─── GET /api/admin/negocios/{id}/signup-codes ────────────────────────

    @Nested
    @DisplayName("GET /api/admin/negocios/{id}/signup-codes")
    class ListarCodigosMatrix {

        @Test
        @DisplayName("sin credencial -> 403")
        void sinCredencial_403() throws Exception {
            Long negocioId = crearNegocioComoAdminYObtenerId();

            mockMvc.perform(get("/api/admin/negocios/" + negocioId + "/signup-codes"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("JWT de tenant (jefe) -> 403")
        void jwtDeTenant_403() throws Exception {
            Long negocioId = crearNegocioComoAdminYObtenerId();
            CredencialesTenant tenant = credencialesTenantValidas("LC");

            mockMvc.perform(get("/api/admin/negocios/" + negocioId + "/signup-codes")
                            .header("Authorization", "Bearer " + tenant.tokenJefe()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("credencial de admin válida -> 200")
        void credencialValida_200() throws Exception {
            Long negocioId = crearNegocioComoAdminYObtenerId();

            mockMvc.perform(get("/api/admin/negocios/" + negocioId + "/signup-codes")
                            .header(ADMIN_HEADER, ADMIN_TOKEN_VALIDO))
                    .andExpect(status().isOk());
        }
    }

    // ─── POST /api/admin/signup-codes/{id}/revoke ─────────────────────────

    @Nested
    @DisplayName("POST /api/admin/signup-codes/{id}/revoke")
    class RevocarCodigoMatrix {

        @Test
        @DisplayName("sin credencial -> 403")
        void sinCredencial_403() throws Exception {
            Long negocioId = crearNegocioComoAdminYObtenerId();
            Long signupCodeId = crearSignupCodeComoAdminYObtenerId(negocioId);

            mockMvc.perform(post("/api/admin/signup-codes/" + signupCodeId + "/revoke"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("JWT de tenant (cocinero) -> 403")
        void jwtDeTenant_403() throws Exception {
            Long negocioId = crearNegocioComoAdminYObtenerId();
            Long signupCodeId = crearSignupCodeComoAdminYObtenerId(negocioId);
            CredencialesTenant tenant = credencialesTenantValidas("RC");

            mockMvc.perform(post("/api/admin/signup-codes/" + signupCodeId + "/revoke")
                            .header("Authorization", "Bearer " + tenant.tokenCocinero()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("credencial de admin válida -> 200")
        void credencialValida_200() throws Exception {
            Long negocioId = crearNegocioComoAdminYObtenerId();
            Long signupCodeId = crearSignupCodeComoAdminYObtenerId(negocioId);

            mockMvc.perform(post("/api/admin/signup-codes/" + signupCodeId + "/revoke")
                            .header(ADMIN_HEADER, ADMIN_TOKEN_VALIDO))
                    .andExpect(status().isOk());
        }
    }
}
