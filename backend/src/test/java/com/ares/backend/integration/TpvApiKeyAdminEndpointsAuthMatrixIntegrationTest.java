package com.ares.backend.integration;

import com.ares.backend.entity.Negocio;
import com.ares.backend.entity.NegocioSignupCode;
import com.ares.backend.entity.TpvApiKey;
import com.ares.backend.entity.Usuario;
import com.ares.backend.repository.NegocioRepository;
import com.ares.backend.repository.NegocioSignupCodeRepository;
import com.ares.backend.repository.TpvApiKeyRepository;
import com.ares.backend.repository.UsuarioRepository;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Matriz de autenticación/autorización de los 3 endpoints REALES de
 * {@link com.ares.backend.controller.TpvApiKeyAdminController} (PR3),
 * mismo patrón que {@code NegocioAdminEndpointsAuthMatrixIntegrationTest}.
 * Cada endpoint debe:
 * <ul>
 *   <li>rechazar (403) sin credencial, con credencial incorrecta, y con un
 *       JWT de tenant válido (jefe o cocinero);</li>
 *   <li>aceptar (2xx) una credencial de admin válida.</li>
 * </ul>
 * La resolución de la key TPV en sí ({@code ROLE_TPV} sobre
 * {@code /api/tpv/**}) ya está cubierta por
 * {@code SecurityConfigAllowlistTest} (PR2); aquí solo se ejercita el
 * boundary del superadmin sobre {@code /api/admin/**}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TpvApiKeyAdminEndpointsAuthMatrixIntegrationTest {

    private static final String ADMIN_HEADER = "X-Admin-Token";
    /** Debe coincidir con el hash bcrypt configurado en test/resources/application.properties. */
    private static final String ADMIN_TOKEN_VALIDO = "s3cr3t-admin-token-for-tests";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private NegocioRepository negocioRepository;
    @Autowired private NegocioSignupCodeRepository negocioSignupCodeRepository;
    @Autowired private TpvApiKeyRepository tpvApiKeyRepository;
    @Autowired private UsuarioRepository usuarioRepository;

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

    private record CredencialesTenant(String tokenJefe, String tokenCocinero) {}

    private CredencialesTenant credencialesTenantValidas(String sufijo) throws Exception {
        provisionarNegocio("Negocio Tpv Admin Tenant " + sufijo, "COD_TPV_ADMIN_" + sufijo);
        String tokenJefe = registrarJefeYObtenerToken("jefeTpvAdmin" + sufijo, "COD_TPV_ADMIN_" + sufijo);

        String bodyCocinero = "{\"username\":\"cocineroTpvAdmin" + sufijo + "\",\"password\":\"password123\","
                + "\"email\":\"cocineroTpvAdmin" + sufijo + "@test.com\"}";
        mockMvc.perform(post("/api/usuarios/empleados")
                        .header("Authorization", "Bearer " + tokenJefe)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyCocinero))
                .andExpect(status().isCreated());
        String tokenCocinero = login("cocineroTpvAdmin" + sufijo + "@test.com", "password123");

        return new CredencialesTenant(tokenJefe, tokenCocinero);
    }

    private Long crearNegocioComoAdminYObtenerId(String nombre) throws Exception {
        String json = mockMvc.perform(post("/api/admin/negocios")
                        .header(ADMIN_HEADER, ADMIN_TOKEN_VALIDO)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("nombre", nombre))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("negocio").get("id").asLong();
    }

    private Long emitirClaveComoAdminYObtenerId(Long negocioId) throws Exception {
        String json = mockMvc.perform(post("/api/admin/negocios/" + negocioId + "/tpv-api-key")
                        .header(ADMIN_HEADER, ADMIN_TOKEN_VALIDO))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("id").asLong();
    }

    // ─── POST /api/admin/negocios/{id}/tpv-api-key ───────────────────────

    @Nested
    @DisplayName("POST /api/admin/negocios/{id}/tpv-api-key")
    class EmitirClaveMatrix {

        @Test
        @DisplayName("sin credencial -> 403")
        void sinCredencial_403() throws Exception {
            Long negocioId = crearNegocioComoAdminYObtenerId("Negocio Emitir Sin Cred");

            mockMvc.perform(post("/api/admin/negocios/" + negocioId + "/tpv-api-key"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("credencial incorrecta -> 403")
        void credencialIncorrecta_403() throws Exception {
            Long negocioId = crearNegocioComoAdminYObtenerId("Negocio Emitir Cred Mala");

            mockMvc.perform(post("/api/admin/negocios/" + negocioId + "/tpv-api-key")
                            .header(ADMIN_HEADER, "credencial-incorrecta"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("JWT de tenant (jefe y cocinero) -> 403")
        void jwtDeTenant_403() throws Exception {
            Long negocioId = crearNegocioComoAdminYObtenerId("Negocio Emitir Jwt");
            CredencialesTenant tenant = credencialesTenantValidas("EC");

            mockMvc.perform(post("/api/admin/negocios/" + negocioId + "/tpv-api-key")
                            .header("Authorization", "Bearer " + tenant.tokenJefe()))
                    .andExpect(status().isForbidden());

            mockMvc.perform(post("/api/admin/negocios/" + negocioId + "/tpv-api-key")
                            .header("Authorization", "Bearer " + tenant.tokenCocinero()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("credencial de admin válida -> 201 con la key en claro")
        void credencialValida_201() throws Exception {
            Long negocioId = crearNegocioComoAdminYObtenerId("Negocio Emitir Valido");

            mockMvc.perform(post("/api/admin/negocios/" + negocioId + "/tpv-api-key")
                            .header(ADMIN_HEADER, ADMIN_TOKEN_VALIDO))
                    .andExpect(status().isCreated());
        }
    }

    // ─── GET /api/admin/negocios/{id}/tpv-api-keys ───────────────────────

    @Nested
    @DisplayName("GET /api/admin/negocios/{id}/tpv-api-keys")
    class ListarClavesMatrix {

        @Test
        @DisplayName("sin credencial -> 403")
        void sinCredencial_403() throws Exception {
            Long negocioId = crearNegocioComoAdminYObtenerId("Negocio Listar Sin Cred");

            mockMvc.perform(get("/api/admin/negocios/" + negocioId + "/tpv-api-keys"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("JWT de tenant (cocinero) -> 403")
        void jwtDeTenant_403() throws Exception {
            Long negocioId = crearNegocioComoAdminYObtenerId("Negocio Listar Jwt");
            CredencialesTenant tenant = credencialesTenantValidas("LC");

            mockMvc.perform(get("/api/admin/negocios/" + negocioId + "/tpv-api-keys")
                            .header("Authorization", "Bearer " + tenant.tokenCocinero()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("credencial de admin válida -> 200")
        void credencialValida_200() throws Exception {
            Long negocioId = crearNegocioComoAdminYObtenerId("Negocio Listar Valido");

            mockMvc.perform(get("/api/admin/negocios/" + negocioId + "/tpv-api-keys")
                            .header(ADMIN_HEADER, ADMIN_TOKEN_VALIDO))
                    .andExpect(status().isOk());
        }
    }

    // ─── POST /api/admin/tpv-api-keys/{id}/revoke ─────────────────────────

    @Nested
    @DisplayName("POST /api/admin/tpv-api-keys/{id}/revoke")
    class RevocarClaveMatrix {

        @Test
        @DisplayName("sin credencial -> 403")
        void sinCredencial_403() throws Exception {
            Long negocioId = crearNegocioComoAdminYObtenerId("Negocio Revoke Sin Cred");
            Long claveId = emitirClaveComoAdminYObtenerId(negocioId);

            mockMvc.perform(post("/api/admin/tpv-api-keys/" + claveId + "/revoke"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("JWT de tenant (cocinero) -> 403")
        void jwtDeTenant_403() throws Exception {
            Long negocioId = crearNegocioComoAdminYObtenerId("Negocio Revoke Jwt");
            Long claveId = emitirClaveComoAdminYObtenerId(negocioId);
            CredencialesTenant tenant = credencialesTenantValidas("RC");

            mockMvc.perform(post("/api/admin/tpv-api-keys/" + claveId + "/revoke")
                            .header("Authorization", "Bearer " + tenant.tokenCocinero()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("credencial de admin válida -> 200 idempotente")
        void credencialValida_200Idempotente() throws Exception {
            Long negocioId = crearNegocioComoAdminYObtenerId("Negocio Revoke Valido");
            Long claveId = emitirClaveComoAdminYObtenerId(negocioId);

            mockMvc.perform(post("/api/admin/tpv-api-keys/" + claveId + "/revoke")
                            .header(ADMIN_HEADER, ADMIN_TOKEN_VALIDO))
                    .andExpect(status().isOk());

            // Idempotente: revocar dos veces la misma credencial sigue devolviendo 200
            mockMvc.perform(post("/api/admin/tpv-api-keys/" + claveId + "/revoke")
                            .header(ADMIN_HEADER, ADMIN_TOKEN_VALIDO))
                    .andExpect(status().isOk());
        }
    }

    // ─── POST /api/admin/negocios/{id}/tpv-api-key — backstop de BD (V7) ──

    @Nested
    @DisplayName("POST /api/admin/negocios/{id}/tpv-api-key — violación del backstop de BD (D3, R4-001)")
    class EmitirClaveConstraintBackstop {

        @Test
        @DisplayName("negocio_id_activo ya ocupado por otra fila -> 500, no 404 (GlobalExceptionHandler)")
        void emitirConNegocioIdActivoYaOcupado_devuelve500() throws Exception {
            Long negocioId = crearNegocioComoAdminYObtenerId("Negocio Emitir Constraint Violado");
            Negocio negocio = negocioRepository.findById(negocioId).orElseThrow();

            Usuario usuarioSistema = new Usuario("tpv-system", "!TPV_SYSTEM_USER_NO_LOGIN!", false);
            usuarioSistema.setEmail("tpv+negocio-" + negocioId + "@tpv.ohmyfreezer.invalid");
            usuarioSistema.setNegocio(negocio);
            usuarioRepository.save(usuarioSistema);

            // Fila "corrupta" que simula el estado que dejaría una
            // transacción perdedora de la carrera de R4-001: activa=false
            // (así que findByNegocioIdAndActivaTrue no la ve, y emitir()
            // sigue su camino normal de "primera emisión"), pero
            // negocioIdActivo sigue apuntando al negocio. El INSERT de la
            // nueva credencial activa que emitir() hace a continuación
            // colisiona con este valor en el índice único
            // uk_tpv_api_keys_negocio_activo (V7), reproduciendo
            // determinísticamente (sin hilos reales) la misma
            // DataIntegrityViolationException que la carrera real dispara.
            TpvApiKey filaCorrupta = new TpvApiKey(negocio, "pfx-corrupta1", "hash-corrupta", usuarioSistema);
            filaCorrupta.setActiva(false);
            filaCorrupta.setNegocioIdActivo(negocioId);
            tpvApiKeyRepository.saveAndFlush(filaCorrupta);

            mockMvc.perform(post("/api/admin/negocios/" + negocioId + "/tpv-api-key")
                            .header(ADMIN_HEADER, ADMIN_TOKEN_VALIDO))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.status").value(500))
                    .andExpect(jsonPath("$.error").value("Internal Server Error"));
        }
    }
}
