package com.ares.backend.integration;

import com.ares.backend.config.CustomUserDetails;
import com.ares.backend.dto.*;
import com.ares.backend.entity.Ingrediente;
import com.ares.backend.entity.Negocio;
import com.ares.backend.entity.Usuario;
import com.ares.backend.repository.AlertaRepository;
import com.ares.backend.repository.IngredienteRepository;
import com.ares.backend.repository.NegocioRepository;
import com.ares.backend.repository.UsuarioRepository;
import com.ares.backend.service.EmailService;
import com.ares.backend.service.IngredienteService;
import com.ares.backend.service.RecetaService;
import com.ares.backend.service.RegistroUsoService;
import com.ares.backend.service.UsuarioService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test de FLUJO completo contra una base de datos real (H2 en memoria).
 *
 * A diferencia de los tests unitarios con Mockito, aquí se ejercita el
 * encadenado real de servicios + repositorios + persistencia:
 *   registrar jefe → crear ingrediente → crear receta → elaborar →
 *   se descuenta el stock, se registra el uso y salta la alerta de stock bajo.
 *
 * - EmailService se mockea para no llamar a SendGrid durante los tests.
 * - @Transactional aísla cada test (rollback al terminar). El flujo feliz
 *   probado aquí no usa transacciones REQUIRES_NEW, por lo que el rollback
 *   es limpio. El caso "sin stock suficiente" se cubre en RecetasServiceTest.
 */
@SpringBootTest
@Transactional
class FlujoStockIntegrationTest {

    @Autowired private UsuarioService usuarioService;
    @Autowired private IngredienteService ingredienteService;
    @Autowired private RecetaService recetaService;
    @Autowired private RegistroUsoService registroUsoService;

    @Autowired private IngredienteRepository ingredienteRepository;
    @Autowired private AlertaRepository alertaRepository;
    @Autowired private NegocioRepository negocioRepository;
    @Autowired private UsuarioRepository usuarioRepository;

    @MockitoBean private EmailService emailService;

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    /**
     * Registra un usuario y lo deja autenticado en el SecurityContext,
     * imitando lo que haría el JwtFilter tras un login real.
     */
    private void autenticar(Usuario usuario) {
        CustomUserDetails principal = new CustomUserDetails(usuario);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    /**
     * Registra un jefe y le asigna un Negocio (tenant) manualmente.
     *
     * NOTA: UsuarioService.registrar() todavía NO asigna negocio (eso llega
     * en la fase de onboarding, PR5) — este helper simula ese paso para que
     * IngredienteService/RecetaService (ya retrofitteados, PR3) puedan
     * resolver SecurityUtils.getNegocioId() sin romper el flujo de test.
     */
    private Usuario registrarJefe() {
        UsuarioRegisterRequest reg = new UsuarioRegisterRequest();
        reg.setUsername("jefe_test");
        reg.setPassword("password123");
        reg.setEsJefeCocina(true);
        reg.setCodigoJefe("TEST_JEFE_CODE"); // coincide con BUSSINES_LOGIC_CODE de test
        reg.setEmail("jefe@test.com");
        UsuarioResponse creado = usuarioService.registrar(reg);

        Negocio negocio = negocioRepository.save(new Negocio("Negocio de prueba", "negocio@test.com"));
        Usuario usuario = usuarioService.buscarPorId(creado.getId());
        usuario.setNegocio(negocio);
        return usuarioRepository.save(usuario);
    }

    private Long crearIngrediente(String nombre, double cantidad, double stockMinimo) {
        IngredienteRequest req = new IngredienteRequest();
        req.setNombre(nombre);
        req.setCantidad(cantidad);
        req.setUnidadMedida("kg");
        req.setStockMinimo(stockMinimo);
        return ingredienteService.crear(req).getId();
    }

    private Long crearReceta(Long ingredienteId, double cantidadNecesaria) {
        RecetaRequest req = new RecetaRequest();
        req.setNombre("Receta de prueba");
        req.setDescripcion("descripcion");
        req.setPasos(List.of(new PasoRecetaDTO(null, 1, "Paso 1", null)));
        req.setIngredientes(List.of(new RecetaIngredienteRequest(ingredienteId, cantidadNecesaria)));
        return recetaService.crear(req).getId();
    }

    @Test
    @DisplayName("elaborar una receta descuenta el stock, registra el uso y genera alerta de stock bajo")
    void flujoElaboracionDescuentaStockYGeneraAlerta() {
        autenticar(registrarJefe());

        // Ingrediente con 10 kg, mínimo 5. La receta consume 6 → quedará en 4 (< 5 → alerta)
        Long ingId = crearIngrediente("Tomate", 10.0, 5.0);
        Long recetaId = crearReceta(ingId, 6.0);

        ElaborarRecetaRequest elaborar = new ElaborarRecetaRequest();
        elaborar.setCompletada(true);
        RegistroUsoResponse registro = recetaService.elaborar(recetaId, elaborar);

        // 1. El registro de uso se ha creado como completado
        assertThat(registro).isNotNull();
        assertThat(registro.getCompletada()).isTrue();

        // 2. El stock se ha descontado en la BD real (10 - 6 = 4)
        Ingrediente persistido = ingredienteRepository.findById(ingId).orElseThrow();
        assertThat(persistido.getCantidad()).isEqualTo(4.0);

        // 3. Ha saltado una alerta de stock bajo para ese ingrediente
        assertThat(alertaRepository.findByIngredienteIdAndLeidaFalse(ingId)).isNotEmpty();

        // 4. La receta aparece en el histórico de uso
        assertThat(registroUsoService.obtenerPorReceta(recetaId)).hasSize(1);
    }

    @Test
    @DisplayName("un empleado (no jefe) no puede crear recetas")
    void empleadoNoPuedeCrearRecetas() {
        UsuarioRegisterRequest reg = new UsuarioRegisterRequest();
        reg.setUsername("empleado_test");
        reg.setPassword("password123");
        reg.setEsJefeCocina(false);
        UsuarioResponse creado = usuarioService.registrar(reg);
        autenticar(usuarioService.buscarPorId(creado.getId()));

        RecetaRequest req = new RecetaRequest();
        req.setNombre("Receta prohibida");
        req.setDescripcion("desc");
        req.setPasos(List.of());
        req.setIngredientes(List.of());

        assertThatThrownBy(() -> recetaService.crear(req))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
