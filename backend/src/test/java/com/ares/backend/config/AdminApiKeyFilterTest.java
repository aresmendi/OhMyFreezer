package com.ares.backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios de {@link AdminApiKeyFilter}: la validación fail-loud del
 * hash configurado en el constructor, el path-scoping ({@code shouldNotFilter}),
 * y la decisión de autenticación en sí (sin arrancar contexto Spring — mismo
 * patrón que {@link JwtFilterTest}).
 */
class AdminApiKeyFilterTest {

    private static final PasswordEncoder ENCODER = new BCryptPasswordEncoder();
    private static final String SECRETO = "s3cr3t-admin-token-for-tests-unit";
    private static final String HASH_VALIDO = ENCODER.encode(SECRETO);

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    // ─── Validación fail-loud del constructor ────────────────────────────────

    @Test
    @DisplayName("hash en blanco no lanza excepción (fail closed, no fail to start)")
    void constructor_hashEnBlanco_noLanza() {
        assertThatCode(() -> new AdminApiKeyFilter(ENCODER, ""))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("hash null se trata igual que blanco: no lanza excepción")
    void constructor_hashNull_noLanza() {
        assertThatCode(() -> new AdminApiKeyFilter(ENCODER, null))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("hash con formato bcrypt válido no lanza excepción")
    void constructor_hashValido_noLanza() {
        assertThatCode(() -> new AdminApiKeyFilter(ENCODER, HASH_VALIDO))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("hash no vacío pero mal formado (p. ej. contraseña en texto plano) -> IllegalStateException, aborta arranque")
    void constructor_hashMalformado_lanzaIllegalState() {
        assertThatThrownBy(() -> new AdminApiKeyFilter(ENCODER, "esto-no-es-un-hash-bcrypt"))
                .isInstanceOf(IllegalStateException.class);
    }

    // ─── shouldNotFilter: path-scoping ────────────────────────────────────────

    @Test
    @DisplayName("shouldNotFilter es true fuera de /api/admin/ (el filtro no interviene ahí)")
    void shouldNotFilter_fueraDeAdmin_true() throws Exception {
        AdminApiKeyFilter filter = new AdminApiKeyFilter(ENCODER, HASH_VALIDO);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/ingredientes");

        assertThat(filter.shouldNotFilter(request)).isTrue();
    }

    @Test
    @DisplayName("shouldNotFilter es false dentro de /api/admin/ (el filtro SÍ interviene ahí)")
    void shouldNotFilter_dentroDeAdmin_false() throws Exception {
        AdminApiKeyFilter filter = new AdminApiKeyFilter(ENCODER, HASH_VALIDO);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/admin/negocios");

        assertThat(filter.shouldNotFilter(request)).isFalse();
    }

    // ─── Decisión de autenticación ────────────────────────────────────────────

    @Test
    @DisplayName("token correcto -> autentica con principal String \"platform-admin\" y ROLE_PLATFORM_ADMIN")
    void doFilterInternal_tokenCorrecto_autentica() throws Exception {
        AdminApiKeyFilter filter = new AdminApiKeyFilter(ENCODER, HASH_VALIDO);

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getHeader("X-Admin-Token")).thenReturn(SECRETO);

        filter.doFilterInternal(request, response, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isInstanceOf(String.class).isEqualTo("platform-admin");
        assertThat(auth.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_PLATFORM_ADMIN");
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("token incorrecto -> NO autentica, el contexto de seguridad queda vacío")
    void doFilterInternal_tokenIncorrecto_noAutentica() throws Exception {
        AdminApiKeyFilter filter = new AdminApiKeyFilter(ENCODER, HASH_VALIDO);

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getHeader("X-Admin-Token")).thenReturn("token-incorrecto");

        filter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("hash sin configurar (blank) -> NUNCA autentica, ni con el token que sería válido en otro entorno")
    void doFilterInternal_hashSinConfigurar_nuncaAutentica() throws Exception {
        AdminApiKeyFilter filter = new AdminApiKeyFilter(ENCODER, "");

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getHeader("X-Admin-Token")).thenReturn(SECRETO);

        filter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }
}
