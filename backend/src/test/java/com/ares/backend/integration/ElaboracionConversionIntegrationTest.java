package com.ares.backend.integration;

import com.ares.backend.config.CustomUserDetails;
import com.ares.backend.dto.ElaborarRecetaRequest;
import com.ares.backend.dto.IngredienteRequest;
import com.ares.backend.dto.IngredienteResponse;
import com.ares.backend.dto.PasoRecetaDTO;
import com.ares.backend.dto.RecetaIngredienteRequest;
import com.ares.backend.dto.RecetaRequest;
import com.ares.backend.dto.RegistroUsoResponse;
import com.ares.backend.entity.Ingrediente;
import com.ares.backend.entity.MovimientoStock;
import com.ares.backend.entity.Negocio;
import com.ares.backend.entity.NegocioSignupCode;
import com.ares.backend.entity.TipoUnidad;
import com.ares.backend.entity.UnidadMedida;
import com.ares.backend.entity.Usuario;
import com.ares.backend.exception.UnidadesIncompatiblesException;
import com.ares.backend.repository.IngredienteRepository;
import com.ares.backend.repository.MovimientoStockRepository;
import com.ares.backend.repository.NegocioRepository;
import com.ares.backend.repository.NegocioSignupCodeRepository;
import com.ares.backend.repository.UnidadMedidaRepository;
import com.ares.backend.repository.UsuarioRepository;
import com.ares.backend.service.EmailService;
import com.ares.backend.service.IngredienteService;
import com.ares.backend.service.RecetaService;
import com.ares.backend.service.UsuarioService;
import com.ares.backend.dto.UsuarioRegisterRequest;
import com.ares.backend.dto.UsuarioResponse;
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
 * Test de FLUJO completo (Spring real + H2 en memoria) para la conversión de
 * unidades en elaboración de recetas (Fase 2 "unidades-medida", PR3):
 * complementa a {@link FlujoStockIntegrationTest} (que solo cubre el flujo
 * same-unit) cubriendo el caso donde el ingrediente está stockeado en una
 * unidad y el paso de receta pide la cantidad en OTRA unidad del mismo tipo.
 * <p>
 * Ejercita el encadenado real: registrar jefe → crear ingrediente (2 kg) →
 * crear receta (paso: 500 g) → elaborar → se descuenta el stock convertido
 * (1.5 kg, no 500 "kg" crudos) y queda registrado el movimiento SALIDA.
 */
@SpringBootTest
@Transactional
class ElaboracionConversionIntegrationTest {

    @Autowired private UsuarioService usuarioService;
    @Autowired private IngredienteService ingredienteService;
    @Autowired private RecetaService recetaService;

    @Autowired private IngredienteRepository ingredienteRepository;
    @Autowired private MovimientoStockRepository movimientoStockRepository;
    @Autowired private NegocioRepository negocioRepository;
    @Autowired private NegocioSignupCodeRepository negocioSignupCodeRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private UnidadMedidaRepository unidadMedidaRepository;

    @MockitoBean private EmailService emailService;

    private UnidadMedida kg;
    private UnidadMedida g;
    private UnidadMedida litro;

    /**
     * Semilla completa del catálogo global de unidades, igual que el resto
     * de suites @SpringBootTest de esta fase (Flyway deshabilitado en el
     * contexto de test — ver application.properties).
     */
    @BeforeEach
    void sembrarUnidadesMedida() {
        g = crearUnidad("g", "Gramo", TipoUnidad.MASA, 1.0);
        kg = crearUnidad("kg", "Kilogramo", TipoUnidad.MASA, 1000.0);
        crearUnidad("ml", "Mililitro", TipoUnidad.VOLUMEN, 1.0);
        litro = crearUnidad("L", "Litro", TipoUnidad.VOLUMEN, 1000.0);
        crearUnidad("ud", "Unidad", TipoUnidad.UNIDAD, 1.0);
    }

    private UnidadMedida crearUnidad(String codigo, String nombre, TipoUnidad tipo, double factorABase) {
        UnidadMedida u = new UnidadMedida();
        u.setCodigo(codigo);
        u.setNombre(nombre);
        u.setTipo(tipo);
        u.setFactorABase(factorABase);
        return unidadMedidaRepository.save(u);
    }

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    private void autenticar(Usuario usuario) {
        CustomUserDetails principal = new CustomUserDetails(usuario);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private Usuario registrarJefe() {
        Negocio negocio = negocioRepository.save(new Negocio("Negocio conversion", "negocio-conversion@test.com"));
        negocioSignupCodeRepository.save(new NegocioSignupCode(negocio, "CODIGO_CONVERSION"));

        UsuarioRegisterRequest reg = new UsuarioRegisterRequest();
        reg.setUsername("jefe_conversion");
        reg.setPassword("password123");
        reg.setEsJefeCocina(true);
        reg.setCodigoRegistro("CODIGO_CONVERSION");
        reg.setEmail("jefe-conversion@test.com");
        UsuarioResponse creado = usuarioService.registrar(reg);

        return usuarioService.buscarPorId(creado.getId());
    }

    private Long crearIngrediente(String nombre, double cantidad, double stockMinimo, UnidadMedida unidad) {
        IngredienteRequest req = new IngredienteRequest();
        req.setNombre(nombre);
        req.setCantidad(cantidad);
        req.setUnidadBaseId(unidad.getId());
        req.setStockMinimo(stockMinimo);
        IngredienteResponse creado = ingredienteService.crear(req);
        assertThat(creado.getUnidadMedida()).isNotNull(); // compat: sigue derivándose (task 7.4)
        return creado.getId();
    }

    private Long crearReceta(Long ingredienteId, double cantidadNecesaria, UnidadMedida unidadPaso) {
        RecetaRequest req = new RecetaRequest();
        req.setNombre("Receta conversion");
        req.setDescripcion("descripcion");
        req.setPasos(List.of(new PasoRecetaDTO(null, 1, "Paso 1", null)));
        req.setIngredientes(List.of(
                new RecetaIngredienteRequest(ingredienteId, cantidadNecesaria, unidadPaso.getId())));
        return recetaService.crear(req).getId();
    }

    @Test
    @DisplayName("elaborar() convierte 500 g (paso de receta) a la unidad del ingrediente (kg) y descuenta 0.5 kg, no 500")
    void elaborarDescuentaLaCantidadConvertida() {
        autenticar(registrarJefe());

        // Ingrediente stockeado en kg, receta pide el paso en g (mismo tipo MASA, unidad distinta)
        Long ingId = crearIngrediente("Harina", 2.0, 0.5, kg);
        Long recetaId = crearReceta(ingId, 500.0, g);

        ElaborarRecetaRequest elaborar = new ElaborarRecetaRequest();
        elaborar.setCompletada(true);
        RegistroUsoResponse registro = recetaService.elaborar(recetaId, elaborar);

        assertThat(registro).isNotNull();
        assertThat(registro.getCompletada()).isTrue();

        // 2 kg - 500 g (=0.5 kg) = 1.5 kg — el stock queda en la unidad del ingrediente, no en gramos
        Ingrediente persistido = ingredienteRepository.findById(ingId).orElseThrow();
        assertThat(persistido.getCantidad()).isEqualTo(1.5);

        // El movimiento de stock registrado también refleja la cantidad convertida
        List<MovimientoStock> movimientos = movimientoStockRepository.findByIngredienteIdOrderByFechaDesc(ingId);
        assertThat(movimientos).isNotEmpty();
        MovimientoStock ultimo = movimientos.get(0);
        assertThat(ultimo.getTipo()).isEqualTo("SALIDA");
        assertThat(ultimo.getCantidadAnterior()).isEqualTo(2.0);
        assertThat(ultimo.getCantidadNueva()).isEqualTo(1.5);
    }

    @Test
    @DisplayName("crear() rechaza un paso de receta en una unidad de tipo distinto al del ingrediente (400 UnidadesIncompatiblesException)")
    void crearRecetaConUnidadDeTipoDistintoLanzaExcepcion() {
        autenticar(registrarJefe());

        // Ingrediente stockeado en kg (MASA); receta intenta usar L (VOLUMEN) — tipos incompatibles
        Long ingId = crearIngrediente("Aceite", 5.0, 1.0, kg);

        assertThatThrownBy(() -> crearReceta(ingId, 1.0, litro))
                .isInstanceOf(UnidadesIncompatiblesException.class);
    }
}
