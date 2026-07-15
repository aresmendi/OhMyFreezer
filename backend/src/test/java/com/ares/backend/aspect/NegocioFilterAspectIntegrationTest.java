package com.ares.backend.aspect;

import com.ares.backend.config.CustomUserDetails;
import com.ares.backend.dto.IngredienteRequest;
import com.ares.backend.dto.IngredienteResponse;
import com.ares.backend.dto.UsuarioRegisterRequest;
import com.ares.backend.dto.UsuarioResponse;
import com.ares.backend.entity.Negocio;
import com.ares.backend.entity.Usuario;
import com.ares.backend.repository.NegocioRepository;
import com.ares.backend.repository.UsuarioRepository;
import com.ares.backend.service.EmailService;
import com.ares.backend.service.IngredienteService;
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

/**
 * Prueba de integración (Spring real, H2 en memoria) de la Fase 6:
 * {@link NegocioFilterAspect} habilitando el filtro Hibernate
 * {@code negocioFilter} a través del proxy de Spring en una llamada de
 * servicio real — a diferencia de {@code NegocioFilterAspectTest} (unitario,
 * sin AOP real), esta prueba SÍ ejercita el pointcut de AspectJ y demuestra
 * que una lectura findAll-style (IngredienteService.obtenerTodos(), que
 * sigue llamando a {@code ingredienteRepository.findAll()} sin negocioId
 * explícito) queda automáticamente scoped al negocio del caller.
 */
@SpringBootTest
@Transactional
class NegocioFilterAspectIntegrationTest {

    @Autowired private UsuarioService usuarioService;
    @Autowired private IngredienteService ingredienteService;
    @Autowired private NegocioRepository negocioRepository;
    @Autowired private UsuarioRepository usuarioRepository;

    @MockitoBean private EmailService emailService;

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    private void autenticar(Usuario usuario) {
        CustomUserDetails principal = new CustomUserDetails(usuario);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    /**
     * Registra un jefe y le asigna un Negocio (tenant) manualmente — shim
     * idéntico al de FlujoStockIntegrationTest, necesario porque
     * UsuarioService.registrar() todavía no asigna negocio (PR5).
     */
    private Usuario registrarJefeConNegocio(String username, String nombreNegocio) {
        UsuarioRegisterRequest reg = new UsuarioRegisterRequest();
        reg.setUsername(username);
        reg.setPassword("password123");
        reg.setEsJefeCocina(true);
        reg.setCodigoJefe("TEST_JEFE_CODE");
        reg.setEmail(username + "@test.com");
        UsuarioResponse creado = usuarioService.registrar(reg);

        Negocio negocio = negocioRepository.save(new Negocio(nombreNegocio, nombreNegocio + "@test.com"));
        Usuario usuario = usuarioService.buscarPorId(creado.getId());
        usuario.setNegocio(negocio);
        return usuarioRepository.save(usuario);
    }

    private Long crearIngrediente(String nombre) {
        IngredienteRequest req = new IngredienteRequest();
        req.setNombre(nombre);
        req.setCantidad(10.0);
        req.setUnidadMedida("kg");
        req.setStockMinimo(1.0);
        return ingredienteService.crear(req).getId();
    }

    @Test
    @DisplayName("obtenerTodos() (findAll-style) queda scoped por el filtro Hibernate al negocio del caller")
    void obtenerTodosScopedPorFiltroNegocio() {
        Usuario jefeA = registrarJefeConNegocio("jefeA_aspecto", "Negocio Aspecto A");
        Usuario jefeB = registrarJefeConNegocio("jefeB_aspecto", "Negocio Aspecto B");

        autenticar(jefeA);
        crearIngrediente("IngredienteDeA");

        autenticar(jefeB);
        crearIngrediente("IngredienteDeB");

        // El filtro se habilita en la MISMA llamada externa a un método de
        // servicio (vía el aspecto) — findAll() adentro de obtenerTodos()
        // queda scoped automáticamente, sin que IngredienteService pase
        // negocioId explícito.
        autenticar(jefeA);
        List<IngredienteResponse> comoJefeA = ingredienteService.obtenerTodos();

        autenticar(jefeB);
        List<IngredienteResponse> comoJefeB = ingredienteService.obtenerTodos();

        assertThat(comoJefeA).extracting(IngredienteResponse::getNombre).containsExactly("IngredienteDeA");
        assertThat(comoJefeB).extracting(IngredienteResponse::getNombre).containsExactly("IngredienteDeB");
    }
}
