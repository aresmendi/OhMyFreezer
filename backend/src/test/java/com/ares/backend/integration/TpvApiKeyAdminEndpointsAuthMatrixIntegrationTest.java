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
import org.springframework.security.crypto.password.PasswordEncoder;
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
    @Autowired private PasswordEncoder passwordEncoder;

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
        String json = mockMvc.perform(post("/api/admin/negocios/" + negocioId + "/tpv-api-keys")
                        .header(ADMIN_HEADER, ADMIN_TOKEN_VALIDO))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("id").asLong();
    }

    /**
     * Emite una clave TPV real usando una credencial {@code X-Tpv-Api-Key}
     * válida (PR2's {@code ROLE_TPV}), mismo patrón de construcción de
     * cabecera que {@code SecurityConfigAllowlistTest.provisionarClaveTpvYObtenerKeyEnClaro}.
     * Reutiliza el usuario sintético si ya existe (algunos tests de esta
     * clase emiten una clave de admin para el mismo negocio antes de llamar
     * aquí, lo que ya lo provisiona), igual que
     * {@code TpvApiKeyAdminService.obtenerOProvisionarUsuarioSistema}.
     */
    private String provisionarClaveTpvYObtenerKeyEnClaro(Negocio negocio) {
        String email = "tpv+negocio-" + negocio.getId() + "@tpv.ohmyfreezer.invalid";
        Usuario usuarioSistema = usuarioRepository.findByEmail(email).orElseGet(() -> {
            Usuario nuevo = new Usuario("tpv-system", "sentinel-no-bcrypt", false);
            nuevo.setEmail(email);
            nuevo.setNegocio(negocio);
            return usuarioRepository.save(nuevo);
        });

        String prefijo = "pfxrolematrix" + negocio.getId();
        String secreto = "secretorolematrix" + negocio.getId();
        TpvApiKey clave = new TpvApiKey(negocio, prefijo, passwordEncoder.encode(secreto), usuarioSistema);
        tpvApiKeyRepository.save(clave);

        return "omf_tpv_" + prefijo + "_" + secreto;
    }

    // ─── POST /api/admin/negocios/{id}/tpv-api-keys ──────────────────────

    @Nested
    @DisplayName("POST /api/admin/negocios/{id}/tpv-api-keys")
    class EmitirClaveMatrix {

        @Test
        @DisplayName("sin credencial -> 403")
        void sinCredencial_403() throws Exception {
            Long negocioId = crearNegocioComoAdminYObtenerId("Negocio Emitir Sin Cred");

            mockMvc.perform(post("/api/admin/negocios/" + negocioId + "/tpv-api-keys"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("credencial incorrecta -> 403")
        void credencialIncorrecta_403() throws Exception {
            Long negocioId = crearNegocioComoAdminYObtenerId("Negocio Emitir Cred Mala");

            mockMvc.perform(post("/api/admin/negocios/" + negocioId + "/tpv-api-keys")
                            .header(ADMIN_HEADER, "credencial-incorrecta"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("JWT de tenant (jefe y cocinero) -> 403")
        void jwtDeTenant_403() throws Exception {
            Long negocioId = crearNegocioComoAdminYObtenerId("Negocio Emitir Jwt");
            CredencialesTenant tenant = credencialesTenantValidas("EC");

            mockMvc.perform(post("/api/admin/negocios/" + negocioId + "/tpv-api-keys")
                            .header("Authorization", "Bearer " + tenant.tokenJefe()))
                    .andExpect(status().isForbidden());

            mockMvc.perform(post("/api/admin/negocios/" + negocioId + "/tpv-api-keys")
                            .header("Authorization", "Bearer " + tenant.tokenCocinero()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("credencial TPV válida (ROLE_TPV) -> 403 (frontera de rol, no alcanza /api/admin/**)")
        void credencialTpvValida_403() throws Exception {
            Negocio negocio = provisionarNegocio("Negocio Emitir Tpv Role", "COD_TPV_ADMIN_EMITIR_ROLE");
            String claveTpv = provisionarClaveTpvYObtenerKeyEnClaro(negocio);

            mockMvc.perform(post("/api/admin/negocios/" + negocio.getId() + "/tpv-api-keys")
                            .header("X-Tpv-Api-Key", claveTpv))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("credencial de admin válida -> 201 con la key en claro")
        void credencialValida_201() throws Exception {
            Long negocioId = crearNegocioComoAdminYObtenerId("Negocio Emitir Valido");

            mockMvc.perform(post("/api/admin/negocios/" + negocioId + "/tpv-api-keys")
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
        @DisplayName("credencial incorrecta -> 403")
        void credencialIncorrecta_403() throws Exception {
            Long negocioId = crearNegocioComoAdminYObtenerId("Negocio Listar Cred Mala");

            mockMvc.perform(get("/api/admin/negocios/" + negocioId + "/tpv-api-keys")
                            .header(ADMIN_HEADER, "credencial-incorrecta"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("JWT de tenant (jefe y cocinero) -> 403")
        void jwtDeTenant_403() throws Exception {
            Long negocioId = crearNegocioComoAdminYObtenerId("Negocio Listar Jwt");
            CredencialesTenant tenant = credencialesTenantValidas("LC");

            mockMvc.perform(get("/api/admin/negocios/" + negocioId + "/tpv-api-keys")
                            .header("Authorization", "Bearer " + tenant.tokenJefe()))
                    .andExpect(status().isForbidden());

            mockMvc.perform(get("/api/admin/negocios/" + negocioId + "/tpv-api-keys")
                            .header("Authorization", "Bearer " + tenant.tokenCocinero()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("credencial TPV válida (ROLE_TPV) -> 403 (frontera de rol, no alcanza /api/admin/**)")
        void credencialTpvValida_403() throws Exception {
            Negocio negocio = provisionarNegocio("Negocio Listar Tpv Role", "COD_TPV_ADMIN_LISTAR_ROLE");
            String claveTpv = provisionarClaveTpvYObtenerKeyEnClaro(negocio);

            mockMvc.perform(get("/api/admin/negocios/" + negocio.getId() + "/tpv-api-keys")
                            .header("X-Tpv-Api-Key", claveTpv))
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
        @DisplayName("credencial incorrecta -> 403")
        void credencialIncorrecta_403() throws Exception {
            Long negocioId = crearNegocioComoAdminYObtenerId("Negocio Revoke Cred Mala");
            Long claveId = emitirClaveComoAdminYObtenerId(negocioId);

            mockMvc.perform(post("/api/admin/tpv-api-keys/" + claveId + "/revoke")
                            .header(ADMIN_HEADER, "credencial-incorrecta"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("JWT de tenant (jefe y cocinero) -> 403")
        void jwtDeTenant_403() throws Exception {
            Long negocioId = crearNegocioComoAdminYObtenerId("Negocio Revoke Jwt");
            Long claveId = emitirClaveComoAdminYObtenerId(negocioId);
            CredencialesTenant tenant = credencialesTenantValidas("RC");

            mockMvc.perform(post("/api/admin/tpv-api-keys/" + claveId + "/revoke")
                            .header("Authorization", "Bearer " + tenant.tokenJefe()))
                    .andExpect(status().isForbidden());

            mockMvc.perform(post("/api/admin/tpv-api-keys/" + claveId + "/revoke")
                            .header("Authorization", "Bearer " + tenant.tokenCocinero()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("credencial TPV válida (ROLE_TPV) -> 403 (frontera de rol, no alcanza /api/admin/**)")
        void credencialTpvValida_403() throws Exception {
            Long negocioId = crearNegocioComoAdminYObtenerId("Negocio Revoke Tpv Role");
            Long claveId = emitirClaveComoAdminYObtenerId(negocioId);

            // Credencial TPV de OTRO negocio: emitirClaveComoAdminYObtenerId
            // ya deja una credencial activa para negocioId, y
            // provisionarClaveTpvYObtenerKeyEnClaro insertaría una segunda
            // fila activa para el MISMO negocio, violando el índice único
            // uk_tpv_api_keys_negocio_activo (V7). Da igual de qué negocio
            // sea la clave: aquí solo se prueba que ROLE_TPV no alcanza el
            // boundary de admin, no que sea del mismo tenant.
            Negocio otroNegocio = provisionarNegocio("Negocio Revoke Tpv Role Otro", "COD_TPV_ADMIN_REVOKE_ROLE");
            String claveTpv = provisionarClaveTpvYObtenerKeyEnClaro(otroNegocio);

            mockMvc.perform(post("/api/admin/tpv-api-keys/" + claveId + "/revoke")
                            .header("X-Tpv-Api-Key", claveTpv))
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

    // ─── POST /api/admin/negocios/{id}/tpv-api-keys — backstop de BD (V7) ──

    @Nested
    @DisplayName("POST /api/admin/negocios/{id}/tpv-api-keys — violación del backstop de BD (D3, R4-001)")
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

            mockMvc.perform(post("/api/admin/negocios/" + negocioId + "/tpv-api-keys")
                            .header(ADMIN_HEADER, ADMIN_TOKEN_VALIDO))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.status").value(500))
                    .andExpect(jsonPath("$.error").value("Internal Server Error"));
        }
    }

    // ─── Path variable {id} no numérico en los 3 endpoints (R4-003) ───────

    @Nested
    @DisplayName("Path variable {id} no numérico -> 400, no 404 (GlobalExceptionHandler, R4-003)")
    class PathVariableNoNumericoMatrix {

        @Test
        @DisplayName("POST /api/admin/negocios/{id}/tpv-api-keys con id no numérico -> 400")
        void emitirConIdNoNumerico_400() throws Exception {
            mockMvc.perform(post("/api/admin/negocios/abc/tpv-api-keys")
                            .header(ADMIN_HEADER, ADMIN_TOKEN_VALIDO))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }

        @Test
        @DisplayName("GET /api/admin/negocios/{id}/tpv-api-keys con id no numérico -> 400")
        void listarConIdNoNumerico_400() throws Exception {
            mockMvc.perform(get("/api/admin/negocios/abc/tpv-api-keys")
                            .header(ADMIN_HEADER, ADMIN_TOKEN_VALIDO))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }

        @Test
        @DisplayName("POST /api/admin/tpv-api-keys/{id}/revoke con id no numérico -> 400")
        void revocarConIdNoNumerico_400() throws Exception {
            mockMvc.perform(post("/api/admin/tpv-api-keys/abc/revoke")
                            .header(ADMIN_HEADER, ADMIN_TOKEN_VALIDO))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }
    }
}
