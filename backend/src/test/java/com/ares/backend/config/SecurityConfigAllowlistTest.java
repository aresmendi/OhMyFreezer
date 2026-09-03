package com.ares.backend.config;

import com.ares.backend.dto.ElaborarRecetaRequest;
import com.ares.backend.dto.IngredienteRequest;
import com.ares.backend.dto.PasoRecetaDTO;
import com.ares.backend.dto.RecetaIngredienteRequest;
import com.ares.backend.dto.RecetaRequest;
import com.ares.backend.entity.Negocio;
import com.ares.backend.entity.NegocioSignupCode;
import com.ares.backend.entity.TipoUnidad;
import com.ares.backend.entity.TpvApiKey;
import com.ares.backend.entity.UnidadMedida;
import com.ares.backend.entity.Usuario;
import com.ares.backend.repository.NegocioRepository;
import com.ares.backend.repository.NegocioSignupCodeRepository;
import com.ares.backend.repository.TpvApiKeyRepository;
import com.ares.backend.repository.UnidadMedidaRepository;
import com.ares.backend.repository.UsuarioRepository;
import com.ares.backend.service.EmailService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Regresión de la inversión de allowlist de autorización (diseño D1-A, capa
 * 2): {@code anyRequest().authenticated()} pasó a
 * {@code hasAnyRole("JEFE","COCINERO")}, y lo mismo para los dos matchers de
 * {@code /api/recetas/*}{@code /elaborar} y {@code /verificar}. Esta suite
 * prueba que NINGUNA ruta de tenant preexistente quedó bloqueada para jefe o
 * cocinero — {@link CustomUserDetails#getAuthorities()} solo devuelve una de
 * esas dos autoridades para cualquier {@code Usuario} que pueda existir, así
 * que el allowlist debe ser conductualmente idéntico a {@code authenticated()}
 * para todo principal preexistente.
 * <p>
 * IMPORTANTE: si en el futuro se añade un tercer rol de tenant, debe
 * incorporarse también a este allowlist en {@link SecurityConfig} o quedará
 * bloqueado por completo — este test es la red que debe fallar en ese caso.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SecurityConfigAllowlistTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private NegocioRepository negocioRepository;
    @Autowired private NegocioSignupCodeRepository negocioSignupCodeRepository;
    @Autowired private UnidadMedidaRepository unidadMedidaRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private TpvApiKeyRepository tpvApiKeyRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @MockitoBean private EmailService emailService;

    /**
     * Semilla mínima del catálogo global de unidades (no tenant-scoped, ver
     * "unidades-medida" fase 2 PR3), necesaria porque esta suite no ejecuta
     * Flyway — mismo patrón que {@code CrossTenantIsolationIntegrationTest}.
     */
    @BeforeEach
    void sembrarUnidadesMedida() {
        UnidadMedida kg = new UnidadMedida();
        kg.setCodigo("kg");
        kg.setNombre("Kilogramo");
        kg.setTipo(TipoUnidad.MASA);
        kg.setFactorABase(1000.0);
        unidadMedidaRepository.save(kg);
    }

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

        return login(username + "@test.com");
    }

    private String registrarCocineroYObtenerToken(String tokenJefe, String username) throws Exception {
        String body = "{\"username\":\"" + username + "\",\"password\":\"password123\","
                + "\"email\":\"" + username + "@test.com\"}";
        mockMvc.perform(post("/api/usuarios/empleados")
                        .header("Authorization", "Bearer " + tokenJefe)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        return login(username + "@test.com");
    }

    private String login(String email) throws Exception {
        Map<String, Object> loginBody = Map.of("email", email, "password", "password123");
        String responseJson = mockMvc.perform(post("/api/usuarios/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginBody)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(responseJson).get("token").asText();
    }

    private Long crearIngrediente(String tokenJefe, String nombre) throws Exception {
        IngredienteRequest req = new IngredienteRequest();
        req.setNombre(nombre);
        req.setCantidad(10.0);
        req.setUnidadMedida("kg");
        req.setStockMinimo(1.0);

        String json = mockMvc.perform(post("/api/ingredientes")
                        .header("Authorization", "Bearer " + tokenJefe)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(json).get("id").asLong();
    }

    private Long crearReceta(String tokenJefe, Long ingredienteId) throws Exception {
        RecetaRequest req = new RecetaRequest();
        req.setNombre("Receta allowlist " + ingredienteId);
        req.setDescripcion("descripcion");
        req.setPasos(List.of(new PasoRecetaDTO(null, 1, "Paso 1", null)));
        req.setIngredientes(List.of(new RecetaIngredienteRequest(ingredienteId, 1.0, null)));

        String json = mockMvc.perform(post("/api/recetas")
                        .header("Authorization", "Bearer " + tokenJefe)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(json).get("id").asLong();
    }

    @Test
    @DisplayName("jefe: rutas de lectura genéricas (antes bajo anyRequest().authenticated()) siguen accesibles tras el allowlist")
    void jefe_sigueAccediendoARutasGenericas() throws Exception {
        provisionarNegocio("Negocio Allowlist Jefe", "COD_ALLOW_JEFE");
        String tokenJefe = registrarJefeYObtenerToken("jefeAllow", "COD_ALLOW_JEFE");

        mockMvc.perform(get("/api/ingredientes").header("Authorization", "Bearer " + tokenJefe))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/recetas").header("Authorization", "Bearer " + tokenJefe))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/unidades").header("Authorization", "Bearer " + tokenJefe))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("cocinero: rutas de lectura genéricas (antes bajo anyRequest().authenticated()) siguen accesibles tras el allowlist")
    void cocinero_sigueAccediendoARutasGenericas() throws Exception {
        provisionarNegocio("Negocio Allowlist Cocinero", "COD_ALLOW_COC");
        String tokenJefe = registrarJefeYObtenerToken("jefeAllowCoc", "COD_ALLOW_COC");
        String tokenCocinero = registrarCocineroYObtenerToken(tokenJefe, "cocineroAllow");

        mockMvc.perform(get("/api/ingredientes").header("Authorization", "Bearer " + tokenCocinero))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/recetas").header("Authorization", "Bearer " + tokenCocinero))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/unidades").header("Authorization", "Bearer " + tokenCocinero))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/movimientos")
                        .header("Authorization", "Bearer " + tokenCocinero)
                        .param("ingredienteIds", "1")
                        .param("fechaDesde", "2000-01-01T00:00:00")
                        .param("fechaHasta", "2100-01-01T00:00:00"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("cocinero: /api/recetas/{id}/verificar y /elaborar (matchers movidos explícitamente) siguen accesibles tras el allowlist")
    void cocinero_sigueAccediendoAVerificarYElaborar() throws Exception {
        provisionarNegocio("Negocio Allowlist Elaborar", "COD_ALLOW_ELAB");
        String tokenJefe = registrarJefeYObtenerToken("jefeAllowElab", "COD_ALLOW_ELAB");
        String tokenCocinero = registrarCocineroYObtenerToken(tokenJefe, "cocineroAllowElab");

        Long ingredienteId = crearIngrediente(tokenJefe, "Harina Allowlist");
        Long recetaId = crearReceta(tokenJefe, ingredienteId);

        mockMvc.perform(post("/api/recetas/" + recetaId + "/verificar")
                        .header("Authorization", "Bearer " + tokenCocinero))
                .andExpect(status().isOk());

        ElaborarRecetaRequest elaborar = new ElaborarRecetaRequest();
        elaborar.setCompletada(true);
        mockMvc.perform(post("/api/recetas/" + recetaId + "/elaborar")
                        .header("Authorization", "Bearer " + tokenCocinero)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(elaborar)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("jefe: rutas restringidas a hasRole(JEFE) (no tocadas por este cambio) siguen exigiendo rol jefe")
    void jefe_siguenExigiendoRolJefeLasRutasRestringidas() throws Exception {
        provisionarNegocio("Negocio Allowlist Restringido", "COD_ALLOW_REST");
        String tokenJefe = registrarJefeYObtenerToken("jefeAllowRest", "COD_ALLOW_REST");

        mockMvc.perform(get("/api/estadisticas/recetas").header("Authorization", "Bearer " + tokenJefe))
                .andExpect(status().isOk());
    }

    // ─── /api/tpv/** y /api/tpv-mapeos/** (tpv-integration, tarea 4.2) ─────────
    //
    // Provisiona la credencial TPV directamente por repositorio (bypass del
    // admin service/controller, que llegan en PR3): a este nivel solo se
    // prueba el ALLOWLIST de SecurityConfig y el filtro TpvApiKeyFilter, no
    // el flujo de emisión. Sin controller detrás de /api/tpv/** ni de
    // /api/tpv-mapeos/**, una autorización CONCEDIDA se manifiesta como 500
    // "Ha ocurrido un error inesperado" (NoResourceFoundException cae en
    // GlobalExceptionHandler#handleGeneral, no en un 404 de Spring puro —
    // comportamiento preexistente de esta app, verificado empíricamente),
    // nunca 403 — es la señal de que el allowlist dejó pasar la petición.

    private String provisionarClaveTpvYObtenerKeyEnClaro(Negocio negocio) {
        Usuario usuarioSistema = new Usuario("tpv-system", "sentinel-no-bcrypt", false);
        usuarioSistema.setEmail("tpv+negocio-" + negocio.getId() + "@tpv.ohmyfreezer.invalid");
        usuarioSistema.setFechaRegistro(LocalDateTime.now());
        usuarioSistema.setNegocio(negocio);
        usuarioRepository.save(usuarioSistema);

        String prefijo = "pfxallow" + negocio.getId();
        String secreto = "secretoallow" + negocio.getId();
        TpvApiKey clave = new TpvApiKey(negocio, prefijo, passwordEncoder.encode(secreto), usuarioSistema);
        tpvApiKeyRepository.save(clave);

        return "omf_tpv_" + prefijo + "_" + secreto;
    }

    @Test
    @DisplayName("TPV: clave activa válida accede a /api/tpv/** (allowlist concede, 500 por falta de controller aún)")
    void claveTpvValida_accedeARutaTpv() throws Exception {
        Negocio negocio = provisionarNegocio("Negocio Allowlist TPV", "COD_ALLOW_TPV");
        String claveTpv = provisionarClaveTpvYObtenerKeyEnClaro(negocio);

        mockMvc.perform(get("/api/tpv/ventas").header("X-Tpv-Api-Key", claveTpv))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("TPV: sin cabecera X-Tpv-Api-Key, /api/tpv/** rechaza (403)")
    void sinCabeceraTpv_rechazaRutaTpv() throws Exception {
        mockMvc.perform(get("/api/tpv/ventas"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TPV: JWT de tenant (jefe) NO puede alcanzar /api/tpv/** (frontera de rol)")
    void jwtTenant_noAlcanzaRutaTpv() throws Exception {
        provisionarNegocio("Negocio Allowlist TPV Jefe", "COD_ALLOW_TPV_JEFE");
        String tokenJefe = registrarJefeYObtenerToken("jefeAllowTpv", "COD_ALLOW_TPV_JEFE");

        mockMvc.perform(get("/api/tpv/ventas").header("Authorization", "Bearer " + tokenJefe))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TPV: ROLE_TPV NO puede alcanzar rutas de tenant ajenas a /api/tpv/** (frontera de rol)")
    void roleTpv_noAlcanzaRutasDeTenant() throws Exception {
        Negocio negocio = provisionarNegocio("Negocio Allowlist TPV Cruzado", "COD_ALLOW_TPV_CRUZ");
        String claveTpv = provisionarClaveTpvYObtenerKeyEnClaro(negocio);

        mockMvc.perform(get("/api/recetas").header("X-Tpv-Api-Key", claveTpv))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("tpv-mapeos: jefe accede a /api/tpv-mapeos/** (allowlist concede, 500 por falta de controller aún)")
    void jefe_accedeARutaTpvMapeos() throws Exception {
        provisionarNegocio("Negocio Allowlist Mapeos Jefe", "COD_ALLOW_MAP_JEFE");
        String tokenJefe = registrarJefeYObtenerToken("jefeAllowMap", "COD_ALLOW_MAP_JEFE");

        mockMvc.perform(get("/api/tpv-mapeos").header("Authorization", "Bearer " + tokenJefe))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("tpv-mapeos: cocinero (no jefe) NO puede alcanzar /api/tpv-mapeos/**")
    void cocinero_noAlcanzaRutaTpvMapeos() throws Exception {
        provisionarNegocio("Negocio Allowlist Mapeos Cocinero", "COD_ALLOW_MAP_COC");
        String tokenJefe = registrarJefeYObtenerToken("jefeAllowMapCoc", "COD_ALLOW_MAP_COC");
        String tokenCocinero = registrarCocineroYObtenerToken(tokenJefe, "cocineroAllowMap");

        mockMvc.perform(get("/api/tpv-mapeos").header("Authorization", "Bearer " + tokenCocinero))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("tpv-mapeos: D-F — una clave TPV válida NO puede alcanzar /api/tpv-mapeos/** (fronteras disjuntas)")
    void claveTpv_noAlcanzaRutaTpvMapeos() throws Exception {
        Negocio negocio = provisionarNegocio("Negocio Allowlist Mapeos TPV", "COD_ALLOW_MAP_TPV");
        String claveTpv = provisionarClaveTpvYObtenerKeyEnClaro(negocio);

        mockMvc.perform(get("/api/tpv-mapeos").header("X-Tpv-Api-Key", claveTpv))
                .andExpect(status().isForbidden());
    }
}
