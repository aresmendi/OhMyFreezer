package com.ares.backend.integration;

import com.ares.backend.entity.Negocio;
import com.ares.backend.entity.NegocioSignupCode;
import com.ares.backend.repository.NegocioRepository;
import com.ares.backend.repository.NegocioSignupCodeRepository;
import com.ares.backend.service.EmailService;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Suite de contención del límite de autenticación de superadmin (fase 1 de
 * "negocio-onboarding-admin", PR1). Es la red de regresión de la CAPA 2
 * (inversión de allowlist de autorización) descrita en el diseño: la única
 * capa autoritativa de contención.
 * <p>
 * En este PR aún no existe {@code NegocioAdminController} (PR2/PR3), así que
 * ninguna petición a {@code /api/admin/**} puede devolver datos reales. Todo
 * lo que se prueba aquí es la decisión de autorización, que ocurre en el
 * filtro de seguridad ANTES de que la petición llegue (o no) a un
 * {@code DispatcherServlet} con un handler mapeado.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminContainmentIntegrationTest {

    /** Debe coincidir con el hash bcrypt configurado en test/resources/application.properties. */
    private static final String ADMIN_TOKEN_VALIDO = "s3cr3t-admin-token-for-tests";
    private static final String ADMIN_HEADER = "X-Admin-Token";

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

    /** Crea un cocinero (empleado, no jefe) autenticándose con el token de un jefe ya existente. */
    private String registrarCocineroYObtenerToken(String tokenJefe, String username) throws Exception {
        String body = "{\"username\":\"" + username + "\",\"password\":\"password123\","
                + "\"email\":\"" + username + "@test.com\"}";
        mockMvc.perform(post("/api/usuarios/empleados")
                        .header("Authorization", "Bearer " + tokenJefe)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
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

    // ─── Rechazo de /api/admin/** sin credencial válida ──────────────────────

    @Test
    @DisplayName("sin credencial de admin -> 403 en /api/admin/**")
    void sinCredencial_403() throws Exception {
        mockMvc.perform(get("/api/admin/negocios"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("credencial de admin incorrecta -> 403 en /api/admin/**")
    void credencialIncorrecta_403() throws Exception {
        mockMvc.perform(get("/api/admin/negocios")
                        .header(ADMIN_HEADER, "esto-no-es-el-secreto-correcto"))
                .andExpect(status().isForbidden());
    }

    // ─── Un JWT de tenant válido NUNCA autentica en /api/admin/** ───────────

    @Test
    @DisplayName("un JWT de tenant válido (jefe) presentado en /api/admin/** -> 403, nunca tratado como identidad admin")
    void jwtDeJefeValido_403EnAdmin() throws Exception {
        provisionarNegocio("Negocio Admin Jefe", "COD_ADMIN_JEFE");
        String tokenJefe = registrarJefeYObtenerToken("jefeAdminTest", "COD_ADMIN_JEFE");

        mockMvc.perform(get("/api/admin/negocios")
                        .header("Authorization", "Bearer " + tokenJefe))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("un JWT de tenant válido (cocinero) presentado en /api/admin/** -> 403, nunca tratado como identidad admin")
    void jwtDeCocineroValido_403EnAdmin() throws Exception {
        provisionarNegocio("Negocio Admin Cocinero", "COD_ADMIN_COC");
        String tokenJefe = registrarJefeYObtenerToken("jefeAdminCocTest", "COD_ADMIN_COC");
        String tokenCocinero = registrarCocineroYObtenerToken(tokenJefe, "cocineroAdminTest");

        mockMvc.perform(get("/api/admin/negocios")
                        .header("Authorization", "Bearer " + tokenCocinero))
                .andExpect(status().isForbidden());
    }

    // ─── La prueba MÁS IMPORTANTE del PR: contención de la credencial admin fuera de /api/admin/** ──

    @Test
    @DisplayName("[CRÍTICO] una credencial de admin válida presentada en GET /api/ingredientes -> 403, jamás se filtra a una ruta de tenant")
    void credencialAdminValida_403EnIngredientes() throws Exception {
        mockMvc.perform(get("/api/ingredientes")
                        .header(ADMIN_HEADER, ADMIN_TOKEN_VALIDO))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("[CRÍTICO] una credencial de admin válida presentada en GET /api/recetas -> 403")
    void credencialAdminValida_403EnRecetas() throws Exception {
        mockMvc.perform(get("/api/recetas")
                        .header(ADMIN_HEADER, ADMIN_TOKEN_VALIDO))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("[CRÍTICO] una credencial de admin válida presentada en POST /api/recetas/1/elaborar -> 403")
    void credencialAdminValida_403EnElaborar() throws Exception {
        mockMvc.perform(post("/api/recetas/1/elaborar")
                        .header(ADMIN_HEADER, ADMIN_TOKEN_VALIDO)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"completada\":true}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("[CRÍTICO] una credencial de admin válida presentada en GET /api/estadisticas/recetas -> 403")
    void credencialAdminValida_403EnEstadisticas() throws Exception {
        mockMvc.perform(get("/api/estadisticas/recetas")
                        .header(ADMIN_HEADER, ADMIN_TOKEN_VALIDO))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("[CRÍTICO] una credencial de admin válida presentada en POST /api/usuarios/empleados -> 403")
    void credencialAdminValida_403EnCrearEmpleado() throws Exception {
        mockMvc.perform(post("/api/usuarios/empleados")
                        .header(ADMIN_HEADER, ADMIN_TOKEN_VALIDO)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"x\",\"password\":\"password123\",\"email\":\"x@test.com\"}"))
                .andExpect(status().isForbidden());
    }

    // ─── Documentación de comportamiento: sin controller admin todavía (PR1) ────

    @Test
    @DisplayName("(documental, PR1) una credencial de admin válida en /api/admin/** hoy da 500, no 404: no existe "
            + "NegocioAdminController hasta PR2/PR3, y NoResourceFoundException NO es una RuntimeException "
            + "(extiende ServletException) por lo que GlobalExceptionHandler.handleNotFound(RuntimeException) "
            + "nunca la atrapa — cae al handler genérico de Exception -> 500. Es una peculiaridad PREEXISTENTE "
            + "de GlobalExceptionHandler, no introducida por este PR, y fuera de su alcance arreglarla. Esta "
            + "expectativa DEBE actualizarse cuando el controller aterrice en PR2/PR3 (entonces será 200/201).")
    void credencialAdminValida_500EnAdminPorFaltaDeController() throws Exception {
        mockMvc.perform(get("/api/admin/negocios")
                        .header(ADMIN_HEADER, ADMIN_TOKEN_VALIDO))
                .andExpect(status().isInternalServerError());
    }
}
