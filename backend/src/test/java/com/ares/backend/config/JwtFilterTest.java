package com.ares.backend.config;

import com.ares.backend.entity.Negocio;
import com.ares.backend.entity.Usuario;
import com.ares.backend.service.UsuarioService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para JwtFilter, en particular el comportamiento de
 * multi-tenancy: un token sin claim negocioId (legacy, emitido antes de esta
 * fase) NO debe autenticar al usuario, ni siquiera contra el negocio semilla
 * al que apuntan los datos pre-existentes tras el backfill de la V2. Esto
 * fuerza un 401 en cualquier endpoint protegido (fail closed), en vez de
 * resolver silenciosamente al negocio por defecto (ver spec: "Pre-migration
 * token without negocioId claim").
 */
class JwtFilterTest {

    private static final String SECRET = "clave-de-test-para-jwt-con-mas-de-treinta-y-dos-bytes-1234567890";
    private static final long EXPIRATION_MS = 86400000L;

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    private Usuario usuarioConNegocio(Long id, Long negocioId) {
        Negocio negocio = new Negocio("Negocio Test", null);
        negocio.setId(negocioId);
        Usuario usuario = new Usuario("usuario", "hash", false);
        usuario.setId(id);
        usuario.setNegocio(negocio);
        return usuario;
    }

    @Test
    @DisplayName("Token válido CON negocioId autentica al usuario en el contexto de seguridad")
    void doFilterInternal_tokenConNegocioId_autentica() throws Exception {
        JwtUtil jwtUtil = new JwtUtil(SECRET, EXPIRATION_MS);
        String token = jwtUtil.generarToken(1L, "usuario", false, 5L);

        UsuarioService usuarioService = mock(UsuarioService.class);
        when(usuarioService.buscarPorId(1L)).thenReturn(usuarioConNegocio(1L, 5L));

        ApplicationContext applicationContext = mock(ApplicationContext.class);
        when(applicationContext.getBean(UsuarioService.class)).thenReturn(usuarioService);

        JwtFilter filter = new JwtFilter(jwtUtil, applicationContext);

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

        filter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        CustomUserDetails principal =
                (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        assertThat(principal.getNegocioId()).isEqualTo(5L);
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("Token legacy SIN negocioId no autentica (fail closed, no cae al negocio semilla)")
    void doFilterInternal_tokenLegacySinNegocioId_noAutentica() throws Exception {
        JwtUtil jwtUtil = new JwtUtil(SECRET, EXPIRATION_MS);
        // Sobrecarga de 3 argumentos: simula un token legacy sin claim negocioId.
        String tokenLegacy = jwtUtil.generarToken(1L, "usuario", false);

        UsuarioService usuarioService = mock(UsuarioService.class);
        // Aunque el usuario recargado SÍ tiene negocio (backfill de la V2 al
        // negocio semilla id=1), el filtro no debe llegar a consultarlo:
        // la ausencia de la claim debe cortar antes de reautenticar.
        when(usuarioService.buscarPorId(1L)).thenReturn(usuarioConNegocio(1L, 1L));

        ApplicationContext applicationContext = mock(ApplicationContext.class);
        when(applicationContext.getBean(UsuarioService.class)).thenReturn(usuarioService);

        JwtFilter filter = new JwtFilter(jwtUtil, applicationContext);

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer " + tokenLegacy);

        filter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(usuarioService, never()).buscarPorId(any());
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("Dos negocios distintos autentican con su propio negocioId (triangulación)")
    void doFilterInternal_conOtroNegocioId_autenticaConEseNegocio() throws Exception {
        JwtUtil jwtUtil = new JwtUtil(SECRET, EXPIRATION_MS);
        String token = jwtUtil.generarToken(2L, "otroUsuario", true, 42L);

        UsuarioService usuarioService = mock(UsuarioService.class);
        when(usuarioService.buscarPorId(2L)).thenReturn(usuarioConNegocio(2L, 42L));

        ApplicationContext applicationContext = mock(ApplicationContext.class);
        when(applicationContext.getBean(UsuarioService.class)).thenReturn(usuarioService);

        JwtFilter filter = new JwtFilter(jwtUtil, applicationContext);

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

        filter.doFilterInternal(request, response, chain);

        CustomUserDetails principal =
                (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        assertThat(principal.getNegocioId()).isEqualTo(42L);
    }
}
