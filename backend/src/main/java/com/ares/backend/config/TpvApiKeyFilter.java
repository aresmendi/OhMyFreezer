package com.ares.backend.config;

import com.ares.backend.entity.Usuario;
import com.ares.backend.service.TpvAutenticacionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filtro de autenticación de un TPV, separado por completo del JWT de
 * tenant ({@link JwtFilter}) y del superadmin de plataforma ({@link
 * AdminApiKeyFilter}). Autentica la cabecera {@link TpvConstantes#HEADER_API_KEY}
 * contra {@link TpvAutenticacionService} (D-D del diseño: lookup O(1) por
 * prefijo + un único bcrypt), y produce una {@code Authentication} cuyo
 * principal es un {@link CustomUserDetails} REAL (el usuario sintético del
 * negocio), pero con una authority EXPLÍCITA {@code ROLE_TPV} — NUNCA la
 * de {@code CustomUserDetails.getAuthorities()}, que devolvería
 * {@code ROLE_COCINERO} (el usuario sintético tiene
 * {@code esJefeCocina=false}). Spring Security lee los roles del
 * {@code Authentication} construido aquí, no de {@code UserDetails}
 * directamente — de ahí que la lista explícita sea la única fuente de
 * verdad del rol en esta petición.
 * <p>
 * Colaborador resuelto LAZY vía {@link ApplicationContext#getBean}, mismo
 * patrón que {@link JwtFilter} (línea 82 de su Javadoc): evita arrastrar
 * JPA (repositorio-backed) a la inicialización de {@link SecurityConfig}.
 * <p>
 * <b>Importante</b>: esta clase NO debe anotarse {@code @Component} —
 * mismo razonamiento que {@link AdminApiKeyFilter}: un bean {@code Filter}
 * de Spring Boot se auto-registra también en la cadena de filtros del
 * servlet container, duplicando su ejecución. Se instancia con {@code new}
 * dentro de {@link SecurityConfig}.
 */
public class TpvApiKeyFilter extends OncePerRequestFilter {

    private static final String RUTA_TPV = "/api/tpv/";

    private final ApplicationContext applicationContext;

    public TpvApiKeyFilter(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * Este filtro solo se ejecuta para rutas bajo {@code /api/tpv/}. Nota
     * D-F del diseño: {@code /api/tpv-mapeos/**} NO empieza por
     * {@code "/api/tpv/"} (falta la barra tras {@code tpv}), así que este
     * filtro NUNCA interviene ahí — la frontera con la ruta hermana de
     * mapeo (autenticada por JWT de tenant, rol JEFE) queda limpia.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(RUTA_TPV);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain)
            throws ServletException, IOException {

        // Limpia cualquier autenticación previa (p. ej. un JWT de tenant que
        // también llegase en esta petición): en /api/tpv/** la única
        // identidad válida es la que resuelve este filtro, nunca la heredada.
        SecurityContextHolder.clearContext();

        String apiKey = request.getHeader(TpvConstantes.HEADER_API_KEY);

        if (StringUtils.hasText(apiKey)) {
            TpvAutenticacionService autenticacionService =
                    applicationContext.getBean(TpvAutenticacionService.class);

            autenticacionService.autenticar(apiKey).ifPresent(this::autenticar);
        }

        filterChain.doFilter(request, response);
    }

    private void autenticar(Usuario usuarioSistema) {
        CustomUserDetails principal = new CustomUserDetails(usuarioSistema);

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(() -> "ROLE_TPV"));

        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
