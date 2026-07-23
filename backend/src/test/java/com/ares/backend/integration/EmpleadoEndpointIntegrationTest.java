package com.ares.backend.integration;

import com.ares.backend.entity.Negocio;
import com.ares.backend.entity.NegocioSignupCode;
import com.ares.backend.repository.NegocioRepository;
import com.ares.backend.repository.NegocioSignupCodeRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de integración full-stack (Spring real + H2 + Spring Security real)
 * del endpoint de onboarding {@code POST /api/usuarios/empleados}: quién
 * puede llamarlo y que el negocio del empleado creado se hereda SIEMPRE del
 * jefe autenticado, nunca del body del request.
 * <p>
 * También ejercita, de punta a punta, el nuevo flujo de alta de jefe vía
 * código de registro ({@code POST /api/usuarios/register} con
 * {@code codigoRegistro}), que reemplaza al antiguo BUSSINES_LOGIC_CODE.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EmpleadoEndpointIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private NegocioRepository negocioRepository;
    @Autowired private NegocioSignupCodeRepository negocioSignupCodeRepository;

    @MockitoBean private EmailService emailService;

    private Negocio provisionarNegocioConCodigo(String codigo) {
        Negocio negocio = negocioRepository.save(new Negocio("Negocio Empleados", "negocio@test.com"));
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

    private String login(String email) throws Exception {
        Map<String, Object> loginBody = Map.of("email", email, "password", "password123");
        String responseJson = mockMvc.perform(post("/api/usuarios/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginBody)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(responseJson).get("token").asText();
    }

    @Test
    @DisplayName("un jefe autenticado crea un empleado que hereda SU negocioId, aunque el body proponga otro")
    void jefeCreaEmpleado_hereda_negocioId_delCaller() throws Exception {
        Negocio negocio = provisionarNegocioConCodigo("CODIGO_EMP_1");
        String tokenJefe = registrarJefeYObtenerToken("jefe_emp1", "CODIGO_EMP_1");

        // El DTO de empleado no tiene campo negocioId, pero probamos que aunque
        // el cliente cuele uno en el JSON crudo, se ignora por completo.
        String bodyConNegocioIdColado =
                "{\"username\":\"empleado_emp1\",\"password\":\"password123\","
                        + "\"email\":\"empleado_emp1@test.com\",\"negocioId\":999999}";

        String responseJson = mockMvc.perform(post("/api/usuarios/empleados")
                        .header("Authorization", "Bearer " + tokenJefe)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyConNegocioIdColado))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        JsonNode empleadoCreado = objectMapper.readTree(responseJson);
        assertThat(empleadoCreado.get("negocioId").asLong()).isEqualTo(negocio.getId());
        assertThat(empleadoCreado.get("esJefeCocina").asBoolean()).isFalse();
    }

    @Test
    @DisplayName("un empleado (no jefe) NO puede crear otros empleados")
    void empleadoNoJefe_noPuedeCrearEmpleados() throws Exception {
        provisionarNegocioConCodigo("CODIGO_EMP_2");
        String tokenJefe = registrarJefeYObtenerToken("jefe_emp2", "CODIGO_EMP_2");

        String bodyEmpleado =
                "{\"username\":\"empleado_emp2\",\"password\":\"password123\",\"email\":\"empleado_emp2@test.com\"}";
        mockMvc.perform(post("/api/usuarios/empleados")
                        .header("Authorization", "Bearer " + tokenJefe)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyEmpleado))
                .andExpect(status().isCreated());

        String tokenEmpleado = login("empleado_emp2@test.com");

        String bodyOtroEmpleado =
                "{\"username\":\"empleado_emp2_b\",\"password\":\"password123\",\"email\":\"empleado_emp2_b@test.com\"}";
        mockMvc.perform(post("/api/usuarios/empleados")
                        .header("Authorization", "Bearer " + tokenEmpleado)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyOtroEmpleado))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("una petición sin autenticar NO puede crear empleados")
    void sinAutenticar_noPuedeCrearEmpleados() throws Exception {
        String body = "{\"username\":\"empleado_anonimo\",\"password\":\"password123\"}";

        // Esta app no registra un AuthenticationEntryPoint propio (ver
        // SecurityConfigCorsTest): Spring Security responde 403, no 401, a
        // peticiones anónimas contra un endpoint protegido. Documentado aquí
        // porque el enunciado de la tarea original asumía 401.
        mockMvc.perform(post("/api/usuarios/empleados")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }
}
