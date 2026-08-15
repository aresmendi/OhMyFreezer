package com.ares.backend.integration;

import com.ares.backend.config.CustomUserDetails;
import com.ares.backend.dto.*;
import com.ares.backend.entity.Ingrediente;
import com.ares.backend.entity.Negocio;
import com.ares.backend.entity.NegocioSignupCode;
import com.ares.backend.entity.TipoUnidad;
import com.ares.backend.entity.UnidadMedida;
import com.ares.backend.entity.Usuario;
import com.ares.backend.repository.AlertaRepository;
import com.ares.backend.repository.IngredienteRepository;
import com.ares.backend.repository.NegocioRepository;
import com.ares.backend.repository.NegocioSignupCodeRepository;
import com.ares.backend.repository.UnidadMedidaRepository;
import com.ares.backend.repository.UsuarioRepository;
import com.ares.backend.service.EmailService;
import com.ares.backend.service.IngredienteService;
import com.ares.backend.service.RecetaService;
import com.ares.backend.service.RegistroUsoService;
import com.ares.backend.service.UsuarioService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
    @Autowired private NegocioSignupCodeRepository negocioSignupCodeRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private UnidadMedidaRepository unidadMedidaRepository;

    @MockitoBean private EmailService emailService;

    /**
     * Semilla completa del catálogo global de unidades (Fase 2
     * "unidades-medida", PR3), replicando exactamente los 5 códigos y
     * factores sembrados por la migración V5. Esta suite corre con Flyway
     * deshabilitado (ver application.properties de test), así que la seed
     * real de V5 nunca se ejecuta aquí — hay que sembrarla manualmente.
     */
    @BeforeEach
    void sembrarUnidadesMedida() {
        crearUnidad("g", "Gramo", TipoUnidad.MASA, 1.0);
        crearUnidad("kg", "Kilogramo", TipoUnidad.MASA, 1000.0);
        crearUnidad("ml", "Mililitro", TipoUnidad.VOLUMEN, 1.0);
        crearUnidad("L", "Litro", TipoUnidad.VOLUMEN, 1000.0);
        crearUnidad("ud", "Unidad", TipoUnidad.UNIDAD, 1.0);
    }

    private void crearUnidad(String codigo, String nombre, TipoUnidad tipo, double factorABase) {
        UnidadMedida u = new UnidadMedida();
        u.setCodigo(codigo);
        u.setNombre(nombre);
        u.setTipo(tipo);
        u.setFactorABase(factorABase);
        unidadMedidaRepository.save(u);
    }

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
     * Provisiona un Negocio + código de alta y registra su primer jefe
     * consumiendo ese código — el flujo real de onboarding (PR5). El negocio
     * queda vinculado al jefe automáticamente por
     * {@code UsuarioService.registrar()}, sin ningún shim manual.
     */
    private Usuario registrarJefe() {
        Negocio negocio = negocioRepository.save(new Negocio("Negocio de prueba", "negocio@test.com"));
        negocioSignupCodeRepository.save(new NegocioSignupCode(negocio, "CODIGO_FLUJO_STOCK"));

        UsuarioRegisterRequest reg = new UsuarioRegisterRequest();
        reg.setUsername("jefe_test");
        reg.setPassword("password123");
        reg.setEsJefeCocina(true);
        reg.setCodigoRegistro("CODIGO_FLUJO_STOCK");
        reg.setEmail("jefe@test.com");
        UsuarioResponse creado = usuarioService.registrar(reg);

        return usuarioService.buscarPorId(creado.getId());
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
        req.setIngredientes(List.of(new RecetaIngredienteRequest(ingredienteId, cantidadNecesaria, null)));
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
        // El empleado ya no se autoregistra vía /register (esJefeCocina=false
        // fue rechazado en la fase de onboarding): lo crea el jefe autenticado
        // vía crearEmpleado(), heredando el negocio del jefe.
        autenticar(registrarJefe());

        EmpleadoRegisterRequest empleadoReq =
                new EmpleadoRegisterRequest("empleado_test", "password123", "empleado_test@test.com");
        UsuarioResponse creado = usuarioService.crearEmpleado(empleadoReq);
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
