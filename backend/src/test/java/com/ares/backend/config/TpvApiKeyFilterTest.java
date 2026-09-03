package com.ares.backend.config;

import com.ares.backend.entity.Usuario;
import com.ares.backend.service.TpvAutenticacionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios de {@link TpvApiKeyFilter}: path-scoping ({@code
 * shouldNotFilter}) y la decisión de autenticación — mismo patrón que
 * {@link AdminApiKeyFilterTest}, pero resolviendo su colaborador
 * ({@link TpvAutenticacionService}) LAZY vía {@link ApplicationContext}
 * (mismo patrón que {@link JwtFilter}, para no arrastrar JPA a la
 * inicialización de {@link SecurityConfig}).
 */
class TpvApiKeyFilterTest {

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    private Usuario usuarioSistema() {
        Usuario u = new Usuario("tpv-system", "sentinel", false);
        u.setId(500L);
        u.setEmail("tpv+negocio-10@tpv.ohmyfreezer.invalid");
        return u;
    }

    // ─── shouldNotFilter: path-scoping ────────────────────────────────────────

    @Test
    @DisplayName("shouldNotFilter es true fuera de /api/tpv/ (el filtro no interviene ahí)")
    void shouldNotFilter_fueraDeTpv_true() {
        TpvApiKeyFilter filter = new TpvApiKeyFilter(mock(ApplicationContext.class));
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/ingredientes");

        assertThat(filter.shouldNotFilter(request)).isTrue();
    }

    @Test
    @DisplayName("shouldNotFilter es true para /api/tpv-mapeos (D-F: NO es un subpath de /api/tpv/)")
    void shouldNotFilter_tpvMapeos_true() {
        TpvApiKeyFilter filter = new TpvApiKeyFilter(mock(ApplicationContext.class));
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/tpv-mapeos");

        assertThat(filter.shouldNotFilter(request)).isTrue();
    }

    @Test
    @DisplayName("shouldNotFilter es false dentro de /api/tpv/ (el filtro SÍ interviene ahí)")
    void shouldNotFilter_dentroDeTpv_false() {
        TpvApiKeyFilter filter = new TpvApiKeyFilter(mock(ApplicationContext.class));
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/tpv/ventas");

        assertThat(filter.shouldNotFilter(request)).isFalse();
    }

    // ─── Decisión de autenticación ────────────────────────────────────────────

    @Test
    @DisplayName("clave válida -> autentica con principal CustomUserDetails real + authority ROLE_TPV explícita")
    void doFilterInternal_claveValida_autenticaConRoleTpv() throws Exception {
        ApplicationContext context = mock(ApplicationContext.class);
        TpvAutenticacionService autenticacionService = mock(TpvAutenticacionService.class);
        when(context.getBean(TpvAutenticacionService.class)).thenReturn(autenticacionService);

        Usuario usuarioSistema = usuarioSistema();
        when(autenticacionService.autenticar("omf_tpv_pfx_secreto")).thenReturn(Optional.of(usuarioSistema));

        TpvApiKeyFilter filter = new TpvApiKeyFilter(context);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getHeader(TpvConstantes.HEADER_API_KEY)).thenReturn("omf_tpv_pfx_secreto");

        filter.doFilterInternal(request, response, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isInstanceOf(CustomUserDetails.class);
        assertThat(((CustomUserDetails) auth.getPrincipal()).getId()).isEqualTo(500L);
        // Authority EXPLÍCITA ROLE_TPV: NUNCA la de CustomUserDetails.getAuthorities()
        // (que devolvería ROLE_COCINERO, porque esJefeCocina=false en el usuario sistema).
        assertThat(auth.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_TPV");
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("clave inválida/desconocida -> NO autentica, el contexto de seguridad queda vacío")
    void doFilterInternal_claveInvalida_noAutentica() throws Exception {
        ApplicationContext context = mock(ApplicationContext.class);
        TpvAutenticacionService autenticacionService = mock(TpvAutenticacionService.class);
        when(context.getBean(TpvAutenticacionService.class)).thenReturn(autenticacionService);
        when(autenticacionService.autenticar("clave-invalida")).thenReturn(Optional.empty());

        TpvApiKeyFilter filter = new TpvApiKeyFilter(context);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getHeader(TpvConstantes.HEADER_API_KEY)).thenReturn("clave-invalida");

        filter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("sin cabecera X-Tpv-Api-Key -> NO autentica, no resuelve el colaborador lazy")
    void doFilterInternal_sinCabecera_noAutenticaNiResuelveColaborador() throws Exception {
        ApplicationContext context = mock(ApplicationContext.class);

        TpvApiKeyFilter filter = new TpvApiKeyFilter(context);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getHeader(TpvConstantes.HEADER_API_KEY)).thenReturn(null);

        filter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(context, org.mockito.Mockito.never()).getBean(TpvAutenticacionService.class);
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("limpia cualquier autenticación previa en el contexto (una petición a /api/tpv/** admite una sola identidad)")
    void doFilterInternal_limpiaContextoPrevio() throws Exception {
        UsernamePasswordAuthenticationToken authPrevia =
                new UsernamePasswordAuthenticationToken("otro-principal", null);
        SecurityContextHolder.getContext().setAuthentication(authPrevia);

        ApplicationContext context = mock(ApplicationContext.class);
        TpvAutenticacionService autenticacionService = mock(TpvAutenticacionService.class);
        when(context.getBean(TpvAutenticacionService.class)).thenReturn(autenticacionService);
        when(autenticacionService.autenticar("clave-invalida")).thenReturn(Optional.empty());

        TpvApiKeyFilter filter = new TpvApiKeyFilter(context);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getHeader(TpvConstantes.HEADER_API_KEY)).thenReturn("clave-invalida");

        filter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
