package com.ares.backend.integration;

import com.ares.backend.config.JwtUtil;
import com.ares.backend.dto.ElaborarRecetaRequest;
import com.ares.backend.dto.IngredienteRequest;
import com.ares.backend.dto.PasoRecetaDTO;
import com.ares.backend.dto.RecetaIngredienteRequest;
import com.ares.backend.dto.RecetaRequest;
import com.ares.backend.entity.Negocio;
import com.ares.backend.entity.NegocioSignupCode;
import com.ares.backend.entity.TipoUnidad;
import com.ares.backend.entity.UnidadMedida;
import com.ares.backend.repository.NegocioRepository;
import com.ares.backend.repository.NegocioSignupCodeRepository;
import com.ares.backend.repository.UnidadMedidaRepository;
import com.ares.backend.service.EmailService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de integración full-stack (Spring real + H2 + Spring Security real)
 * de la Fase 8 de multi-tenancy: aislamiento cross-tenant a nivel de
 * controller/HTTP, para los 6 controllers que exponen tablas tenant-owned
 * (usuarios, ingredientes, recetas, alertas, movimientos_stock,
 * registro_uso_recetas).
 * <p>
 * Sigue el mismo patrón que {@link EmpleadoEndpointIntegrationTest}:
 * provisiona un Negocio + código de alta, registra un jefe consumiendo ese
 * código (flujo real de onboarding, PR5) y opera vía MockMvc con el JWT real
 * obtenido en login — sin ningún shim manual de negocio.
 * <p>
 * Nota de alcance por controller: no los 6 controllers exponen la misma
 * forma de acceso por id. Donde existe un endpoint GET/PUT/DELETE por id
 * (Ingrediente, Receta) o un endpoint de mutación por id (Alerta.leer,
 * Usuario.eliminar) se prueba el contrato 404-nunca-403 literal del spec.
 * MovimientoStock y RegistroUso (histórico por recetaId) no tienen endpoint
 * de recurso único por id — su única forma de acceso es una lista scoped por
 * parámetros de request (ingredienteIds / recetaId) — para esos dos se
 * prueba la garantía de aislamiento equivalente: la lista nunca expone datos
 * de otro negocio (respuesta vacía en vez de 404, porque no hay un recurso
 * único que "no encontrar").
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CrossTenantIsolationIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private NegocioRepository negocioRepository;
    @Autowired private NegocioSignupCodeRepository negocioSignupCodeRepository;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private UnidadMedidaRepository unidadMedidaRepository;

    @MockitoBean private EmailService emailService;

    /**
     * Semilla mínima del catálogo global de unidades usada por
     * {@code crearIngrediente()}. Ver nota equivalente en
     * FlujoStockIntegrationTest — esta suite tampoco ejecuta Flyway.
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

    // ─── Helpers ────────────────────────────────────────────────────────────

    private Negocio provisionarNegocio(String nombre, String codigo) {
        Negocio negocio = negocioRepository.save(new Negocio(nombre, nombre + "@test.com"));
        negocioSignupCodeRepository.save(new NegocioSignupCode(negocio, codigo));
        return negocio;
    }

    private String registrarJefeYObtenerToken(String username, String codigo) throws Exception {
        return registrarJefeYObtenerToken(username, codigo, "password123");
    }

    /**
     * Sobrecarga con contraseña explícita. Antes del fix de email-como-login
     * (esta fase), dos negocios distintos con el mismo username Y la misma
     * contraseña eran ambiguos para login() — esa fuga cross-tenant real fue
     * cerrada haciendo de email (único globalmente) el identificador de
     * login. Se mantiene la sobrecarga con contraseña explícita porque varios
     * tests (p.ej. 8.4) siguen queriendo negocios con contraseñas propias por
     * claridad, aunque la ambigüedad que antes forzaba a usarlas ya no exista.
     */
    private String registrarJefeYObtenerToken(String username, String codigo, String password) throws Exception {
        return registrarJefeYObtenerToken(username, codigo, password, username + "@test.com");
    }

    /**
     * Sobrecarga con email explícito. Necesaria cuando dos negocios distintos
     * registran EL MISMO username (p.ej. test 8.4): el email derivado
     * automáticamente de username colisionaría (email es único globalmente
     * desde V3), así que cada negocio necesita su propio email explícito
     * aunque comparta username con el otro.
     */
    private String registrarJefeYObtenerToken(String username, String codigo, String password, String email) throws Exception {
        Map<String, Object> registerBody = Map.of(
                "username", username,
                "password", password,
                "esJefeCocina", true,
                "codigoRegistro", codigo,
                "email", email
        );
        mockMvc.perform(post("/api/usuarios/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerBody)))
                .andExpect(status().isCreated());

        return login(email, password);
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

    private Long idDeUsuarioAutenticado(String token) {
        return jwtUtil.extraerUsuarioId(token);
    }

    private Long crearIngrediente(String token, String nombre, double cantidad, double stockMinimo) throws Exception {
        IngredienteRequest req = new IngredienteRequest();
        req.setNombre(nombre);
        req.setCantidad(cantidad);
        req.setUnidadMedida("kg");
        req.setStockMinimo(stockMinimo);

        String json = mockMvc.perform(post("/api/ingredientes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(json).get("id").asLong();
    }

    private Long crearReceta(String token, Long ingredienteId) throws Exception {
        RecetaRequest req = new RecetaRequest();
        req.setNombre("Receta " + ingredienteId);
        req.setDescripcion("descripcion");
        req.setPasos(List.of(new PasoRecetaDTO(null, 1, "Paso 1", null)));
        req.setIngredientes(List.of(new RecetaIngredienteRequest(ingredienteId, 1.0, null)));

        String json = mockMvc.perform(post("/api/recetas")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(json).get("id").asLong();
    }

    /** Crea un ingrediente con stock por debajo del mínimo (dispara crearAlertaStockBajo) y devuelve el id de la alerta generada. */
    private Long crearAlertaStockBajoYObtenerId(String tokenJefe, String nombreIngrediente) throws Exception {
        crearIngrediente(tokenJefe, nombreIngrediente, 1.0, 5.0);

        String json = mockMvc.perform(get("/api/alertas/usuario")
                        .header("Authorization", "Bearer " + tokenJefe))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(json).get(0).get("id").asLong();
    }

    private List<String> nombres(JsonNode arrayNode) {
        List<String> resultado = new ArrayList<>();
        arrayNode.forEach(n -> resultado.add(n.get("nombre").asText()));
        return resultado;
    }

    // ─── 8.1 — GET/PUT/DELETE cross-tenant -> 404, nunca 403 ────────────────

    @Test
    @DisplayName("8.1 Ingrediente: GET/PUT/DELETE de un id de negocio B, autenticado como negocio A, siempre 404")
    void ingrediente_crossTenant_siempre404() throws Exception {
        provisionarNegocio("Negocio Ingrediente A", "COD_ING_A");
        provisionarNegocio("Negocio Ingrediente B", "COD_ING_B");
        String tokenA = registrarJefeYObtenerToken("jefeIngA", "COD_ING_A");
        String tokenB = registrarJefeYObtenerToken("jefeIngB", "COD_ING_B");

        Long ingredienteDeB = crearIngrediente(tokenB, "Sal", 10.0, 1.0);

        mockMvc.perform(get("/api/ingredientes/" + ingredienteDeB)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound());

        IngredienteRequest actualizar = new IngredienteRequest();
        actualizar.setNombre("Sal Modificada");
        actualizar.setCantidad(5.0);
        actualizar.setUnidadMedida("kg");
        actualizar.setStockMinimo(1.0);
        mockMvc.perform(put("/api/ingredientes/" + ingredienteDeB)
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(actualizar)))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/ingredientes/" + ingredienteDeB)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("8.1 Receta: GET/PUT/DELETE de un id de negocio B, autenticado como negocio A, siempre 404")
    void receta_crossTenant_siempre404() throws Exception {
        provisionarNegocio("Negocio Receta A", "COD_REC_A");
        provisionarNegocio("Negocio Receta B", "COD_REC_B");
        String tokenA = registrarJefeYObtenerToken("jefeRecA", "COD_REC_A");
        String tokenB = registrarJefeYObtenerToken("jefeRecB", "COD_REC_B");

        Long ingredienteDeB = crearIngrediente(tokenB, "Harina Receta", 10.0, 1.0);
        Long recetaDeB = crearReceta(tokenB, ingredienteDeB);

        mockMvc.perform(get("/api/recetas/" + recetaDeB)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound());

        RecetaRequest actualizar = new RecetaRequest();
        actualizar.setNombre("Receta Modificada");
        actualizar.setDescripcion("otra desc");
        actualizar.setPasos(List.of());
        actualizar.setIngredientes(List.of());
        mockMvc.perform(put("/api/recetas/" + recetaDeB)
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(actualizar)))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/recetas/" + recetaDeB)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("8.1 Alerta: PATCH /leer de un id de negocio B, autenticado como negocio A, siempre 404 (único endpoint por id que expone AlertaController)")
    void alerta_crossTenant_siempre404() throws Exception {
        provisionarNegocio("Negocio Alerta A", "COD_ALE_A");
        provisionarNegocio("Negocio Alerta B", "COD_ALE_B");
        String tokenA = registrarJefeYObtenerToken("jefeAleA", "COD_ALE_A");
        String tokenB = registrarJefeYObtenerToken("jefeAleB", "COD_ALE_B");

        Long alertaDeB = crearAlertaStockBajoYObtenerId(tokenB, "Harina Alerta");

        mockMvc.perform(patch("/api/alertas/" + alertaDeB + "/leer")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("8.1 Usuario: DELETE de un empleado de negocio B, autenticado como negocio A, siempre 404 (fuga cross-tenant real, cerrada en esta fase: UsuarioService.eliminar() no estaba scoped por negocio)")
    void usuario_crossTenant_delete_siempre404() throws Exception {
        provisionarNegocio("Negocio Usuario A", "COD_USR_A");
        provisionarNegocio("Negocio Usuario B", "COD_USR_B");
        String tokenA = registrarJefeYObtenerToken("jefeUsrA", "COD_USR_A");
        String tokenB = registrarJefeYObtenerToken("jefeUsrB", "COD_USR_B");

        String empleadoBJson = mockMvc.perform(post("/api/usuarios/empleados")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"empleadoUsrB\",\"password\":\"password123\",\"email\":\"empleadoUsrB@test.com\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long empleadoBId = objectMapper.readTree(empleadoBJson).get("id").asLong();

        mockMvc.perform(delete("/api/usuarios/" + empleadoBId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("8.1 (adaptado) MovimientoStock: sin endpoint GET/PUT/DELETE por id; su única vía de acceso (lista scoped por ingredienteIds) nunca filtra movimientos de otro negocio")
    void movimientoStock_listaScoped_noExponeMovimientosDeOtroNegocio() throws Exception {
        provisionarNegocio("Negocio Mov A", "COD_MOV_A");
        provisionarNegocio("Negocio Mov B", "COD_MOV_B");
        String tokenA = registrarJefeYObtenerToken("jefeMovA", "COD_MOV_A");
        String tokenB = registrarJefeYObtenerToken("jefeMovB", "COD_MOV_B");

        Long ingredienteDeB = crearIngrediente(tokenB, "Azucar", 10.0, 1.0); // genera un movimiento ENTRADA

        String json = mockMvc.perform(get("/api/movimientos")
                        .header("Authorization", "Bearer " + tokenA)
                        .param("ingredienteIds", String.valueOf(ingredienteDeB))
                        .param("fechaDesde", "2000-01-01T00:00:00")
                        .param("fechaHasta", "2100-01-01T00:00:00"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(objectMapper.readTree(json)).isEmpty();
    }

    @Test
    @DisplayName("8.1 (adaptado) RegistroUso: sin endpoint GET/PUT/DELETE por id propio; obtenerPorReceta(recetaId de otro negocio) nunca expone el histórico ajeno")
    void registroUso_porReceta_noExponeHistoricoDeOtroNegocio() throws Exception {
        provisionarNegocio("Negocio Reg A", "COD_REG_A");
        provisionarNegocio("Negocio Reg B", "COD_REG_B");
        String tokenA = registrarJefeYObtenerToken("jefeRegA", "COD_REG_A");
        String tokenB = registrarJefeYObtenerToken("jefeRegB", "COD_REG_B");

        Long ingredienteDeB = crearIngrediente(tokenB, "Tomate Reg", 10.0, 1.0);
        Long recetaDeB = crearReceta(tokenB, ingredienteDeB);

        ElaborarRecetaRequest elaborar = new ElaborarRecetaRequest();
        elaborar.setCompletada(true);
        mockMvc.perform(post("/api/recetas/" + recetaDeB + "/elaborar")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(elaborar)))
                .andExpect(status().isCreated());

        String json = mockMvc.perform(get("/api/registros/receta/" + recetaDeB)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(objectMapper.readTree(json)).isEmpty();
    }

    // ─── 8.2 — id inexistente e id foráneo son indistinguibles ──────────────

    @Test
    @DisplayName("8.2 un id inexistente en el propio negocio y un id real de otro negocio devuelven el mismo status y cuerpo (404 genérico)")
    void idInexistenteEIdForaneo_sonIndistinguibles() throws Exception {
        provisionarNegocio("Negocio Idx A", "COD_IDX_A");
        provisionarNegocio("Negocio Idx B", "COD_IDX_B");
        String tokenA = registrarJefeYObtenerToken("jefeIdxA", "COD_IDX_A");
        String tokenB = registrarJefeYObtenerToken("jefeIdxB", "COD_IDX_B");

        Long ingredienteDeB = crearIngrediente(tokenB, "Pimienta", 10.0, 1.0);
        long idInexistente = 999999L;

        MvcResult foraneo = mockMvc.perform(get("/api/ingredientes/" + ingredienteDeB)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound())
                .andReturn();

        MvcResult inexistente = mockMvc.perform(get("/api/ingredientes/" + idInexistente)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound())
                .andReturn();

        JsonNode foraneoBody = objectMapper.readTree(foraneo.getResponse().getContentAsString());
        JsonNode inexistenteBody = objectMapper.readTree(inexistente.getResponse().getContentAsString());

        assertThat(foraneo.getResponse().getStatus()).isEqualTo(inexistente.getResponse().getStatus());
        assertThat(foraneoBody.get("status").asInt()).isEqualTo(inexistenteBody.get("status").asInt());
        assertThat(foraneoBody.get("error").asText()).isEqualTo(inexistenteBody.get("error").asText());
        assertThat(foraneoBody.get("message").asText()).isEqualTo(inexistenteBody.get("message").asText());
    }

    // ─── 8.3 — el Negocio semilla no tiene visibilidad especial ─────────────

    @Test
    @DisplayName("8.3 el Negocio semilla no tiene visibilidad especial: un Negocio nuevo no ve sus datos y viceversa "
            + "(esta suite corre con spring.flyway.enabled=false + ddl-auto=create-drop — ver application.properties "
            + "de test —, así que el backfill real de la V2 con id=1 se prueba a nivel de esquema en "
            + "FlywayMigrationTest.seedNegocioExisteConId1(); aquí se prueba la garantía de comportamiento "
            + "equivalente: el primer Negocio pre-existente del sistema, cualquiera sea su id, no tiene NINGÚN "
            + "bypass ni visibilidad especial frente a un Negocio nuevo)")
    void negocioSemilla_sinVisibilidadEspecial() throws Exception {
        // Este Negocio juega el rol del "Negocio semilla": es el primero que
        // existe en el sistema para esta prueba, igual que el backfill de la
        // V2 asigna todo lo pre-existente al Negocio id=1 en producción.
        provisionarNegocio("Negocio Semilla 83", "COD_SEMILLA_83");
        String tokenSemilla = registrarJefeYObtenerToken("jefeSemilla83", "COD_SEMILLA_83");

        provisionarNegocio("Negocio Nuevo 83", "COD_NUEVO_83");
        String tokenNuevo = registrarJefeYObtenerToken("jefeNuevo83", "COD_NUEVO_83");

        Long ingredienteSemilla = crearIngrediente(tokenSemilla, "IngredienteSemilla83", 5.0, 1.0);
        Long ingredienteNuevo = crearIngrediente(tokenNuevo, "IngredienteNuevo83", 5.0, 1.0);

        // El Negocio semilla no ve el ingrediente del Negocio nuevo, ni por id ni en su lista
        mockMvc.perform(get("/api/ingredientes/" + ingredienteNuevo)
                        .header("Authorization", "Bearer " + tokenSemilla))
                .andExpect(status().isNotFound());

        // El Negocio nuevo no ve el ingrediente del Negocio semilla, ni por id ni en su lista
        mockMvc.perform(get("/api/ingredientes/" + ingredienteSemilla)
                        .header("Authorization", "Bearer " + tokenNuevo))
                .andExpect(status().isNotFound());

        String listaSemillaJson = mockMvc.perform(get("/api/ingredientes")
                        .header("Authorization", "Bearer " + tokenSemilla))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(nombres(objectMapper.readTree(listaSemillaJson))).doesNotContain("IngredienteNuevo83");

        String listaNuevoJson = mockMvc.perform(get("/api/ingredientes")
                        .header("Authorization", "Bearer " + tokenNuevo))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(nombres(objectMapper.readTree(listaNuevoJson))).doesNotContain("IngredienteSemilla83");
    }

    // ─── 8.4 — unicidad de username por negocio ──────────────────────────────

    @Test
    @DisplayName("8.4 el mismo username se permite en negocios distintos; duplicado dentro del mismo negocio se rechaza (existsByUsernameAndNegocioId)")
    void usernameUnicidad_esPerNegocio() throws Exception {
        provisionarNegocio("Negocio User84 A", "COD_USR84_A");
        provisionarNegocio("Negocio User84 B", "COD_USR84_B");

        // El mismo username "admin84" registrado como jefe en dos negocios
        // distintos: ambos OK. Email EXPLÍCITO y distinto en cada negocio
        // (obligatorio a partir de esta fase): el derivado automático de
        // username colisionaría, porque el username SÍ es el mismo a propósito.
        String tokenAdminA = registrarJefeYObtenerToken("admin84", "COD_USR84_A", "passwordA84", "adminA84@test.com");
        String tokenAdminB = registrarJefeYObtenerToken("admin84", "COD_USR84_B", "passwordB84", "adminB84@test.com");
        assertThat(tokenAdminA).isNotBlank();
        assertThat(tokenAdminB).isNotBlank();

        // Dentro del negocio A ya existe "admin84" (el propio jefe): un alta de
        // empleado con el mismo username en ESE negocio se rechaza (400, vía
        // existsByUsernameAndNegocioId en UsuarioService.crearEmpleado()).
        mockMvc.perform(post("/api/usuarios/empleados")
                        .header("Authorization", "Bearer " + tokenAdminA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin84\",\"password\":\"password123\",\"email\":\"empleado84@test.com\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("8.4 (fuga cross-tenant CRÍTICA, cerrada en esta fase) login por email autentica correctamente a cada jefe en SU propio negocio incluso cuando dos negocios distintos comparten EL MISMO username y LA MISMA contraseña — antes de este fix, login() basado en username elegía arbitrariamente un candidato (findFirst() sin orden) y podía loguear a la persona A como el usuario de la persona B en OTRO negocio")
    void loginPorEmail_resuelveCorrectamenteAunqueUsernameYContrasenaColisionenEntreNegocios() throws Exception {
        provisionarNegocio("Negocio LoginFix A", "COD_LOGINFIX_A");
        provisionarNegocio("Negocio LoginFix B", "COD_LOGINFIX_B");

        // Mismo username ("colisiona") Y misma contraseña en los dos negocios:
        // el escenario EXACTO que antes de este fix producía la fuga cross-tenant
        // (login elegía cualquiera de los dos candidatos que devolviera la BD).
        // Solo el email distingue a cada usuario ahora, y es obligatorio + único
        // globalmente (migración V3).
        Map<String, Object> registerA = Map.of(
                "username", "colisiona", "password", "mismaPassword123", "esJefeCocina", true,
                "codigoRegistro", "COD_LOGINFIX_A", "email", "jefeLoginFixA@test.com");
        Map<String, Object> registerB = Map.of(
                "username", "colisiona", "password", "mismaPassword123", "esJefeCocina", true,
                "codigoRegistro", "COD_LOGINFIX_B", "email", "jefeLoginFixB@test.com");

        mockMvc.perform(post("/api/usuarios/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerA)))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/usuarios/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerB)))
                .andExpect(status().isCreated());

        String tokenA = login("jefeLoginFixA@test.com", "mismaPassword123");
        String tokenB = login("jefeLoginFixB@test.com", "mismaPassword123");

        assertThat(jwtUtil.extraerNegocioId(tokenA)).isNotEqualTo(jwtUtil.extraerNegocioId(tokenB));

        // Prueba positiva de que cada token pertenece de verdad a SU propio
        // negocio: un ingrediente creado con el token A es invisible para B.
        Long ingredienteDeA = crearIngrediente(tokenA, "IngredienteLoginFixA", 5.0, 1.0);
        mockMvc.perform(get("/api/ingredientes/" + ingredienteDeA)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }

    // ─── 8.5 — token legacy sin claim negocioId ─────────────────────────────

    @Test
    @DisplayName("8.5 un token sin claim negocioId (legacy) no autentica en ninguno de los 6 controllers tenant-scoped: cae a anónimo -> 403 (convención ya documentada del codebase: sin AuthenticationEntryPoint propio, ver SecurityConfigCorsTest / EmpleadoEndpointIntegrationTest — la spec pide 401, pero esta app responde 403 a toda petición anónima contra un endpoint protegido, no solo a los legacy-token)")
    void tokenLegacySinNegocioId_403EnLosSeisControllers() throws Exception {
        provisionarNegocio("Negocio Legacy 85", "COD_LEGACY_85");
        String tokenValido = registrarJefeYObtenerToken("jefeLegacy85", "COD_LEGACY_85");
        Long usuarioId = idDeUsuarioAutenticado(tokenValido);

        // Sobrecarga deprecated de 3 argumentos: simula un token legacy sin negocioId.
        String tokenLegacy = jwtUtil.generarToken(usuarioId, "jefeLegacy85", true);

        // Ingrediente
        mockMvc.perform(get("/api/ingredientes/1").header("Authorization", "Bearer " + tokenLegacy))
                .andExpect(status().isForbidden());

        // Receta
        mockMvc.perform(get("/api/recetas/1").header("Authorization", "Bearer " + tokenLegacy))
                .andExpect(status().isForbidden());

        // Alerta
        mockMvc.perform(patch("/api/alertas/1/leer").header("Authorization", "Bearer " + tokenLegacy))
                .andExpect(status().isForbidden());

        // MovimientoStock
        mockMvc.perform(get("/api/movimientos")
                        .header("Authorization", "Bearer " + tokenLegacy)
                        .param("ingredienteIds", "1")
                        .param("fechaDesde", "2000-01-01T00:00:00")
                        .param("fechaHasta", "2100-01-01T00:00:00"))
                .andExpect(status().isForbidden());

        // RegistroUso
        mockMvc.perform(get("/api/registros/receta/1").header("Authorization", "Bearer " + tokenLegacy))
                .andExpect(status().isForbidden());

        // Usuario
        mockMvc.perform(delete("/api/usuarios/1").header("Authorization", "Bearer " + tokenLegacy))
                .andExpect(status().isForbidden());
    }
}
