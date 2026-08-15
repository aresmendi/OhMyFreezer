package com.ares.backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Filtro de autenticación del superadmin de plataforma, separado por
 * completo de la autenticación JWT de tenant ({@link JwtFilter}). Autentica
 * la cabecera {@code X-Admin-Token} contra un hash bcrypt configurado por
 * entorno, y produce una {@code Authentication} cuyo principal es un
 * {@link String} plano ("platform-admin"), NUNCA un {@link CustomUserDetails}
 * — así, si este principal llegase por error a código de servicio
 * tenant-scoped, {@code SecurityUtils.getUsuarioId()}/{@code getNegocioId()}
 * lanzarían {@code ClassCastException} en vez de resolver silenciosamente.
 * <p>
 * Contención (ver diseño D1-A, tres capas):
 * <ol>
 *   <li><b>Esta clase (capa 1)</b>: {@link #shouldNotFilter} limita el filtro
 *       a rutas bajo {@code /api/admin/}. Fuera de ahí, este filtro NUNCA
 *       produce una autenticación admin.</li>
 *   <li><b>{@link SecurityConfig} (capa 2, AUTORITATIVA)</b>: el allowlist de
 *       autorización invertido (
 *       {@code hasAnyRole("JEFE","COCINERO")} en vez de {@code authenticated()})
 *       deniega explícitamente a un principal {@code PLATFORM_ADMIN} en
 *       cualquier ruta de tenant, aunque la capa 1 fallase.</li>
 *   <li><b>Tipo del principal (capa 3)</b>: ver arriba.</li>
 * </ol>
 * <b>Importante</b>: esta clase NO debe anotarse {@code @Component}. Un bean
 * {@code Filter} de Spring Boot se auto-registra también en la cadena de
 * filtros del servlet container (fuera de Spring Security), duplicando su
 * ejecución. Se instancia con {@code new} dentro de {@link SecurityConfig},
 * igual que {@link JwtFilter} evita ese problema gracias a que
 * {@code OncePerRequestFilter} deduplica por petición — pero aquí preferimos
 * no depender de esa garantía y simplemente no registrarlo como bean.
 */
public class AdminApiKeyFilter extends OncePerRequestFilter {

    private static final String HEADER = "X-Admin-Token";
    private static final String RUTA_ADMIN = "/api/admin/";

    /** Formato estándar de un hash bcrypt: $2a$/$2b$/$2y$ + coste de 2 dígitos + 53 caracteres de sal+hash. */
    private static final Pattern FORMATO_BCRYPT = Pattern.compile("^\\$2[aby]\\$\\d{2}\\$[./0-9A-Za-z]{53}$");

    private final PasswordEncoder passwordEncoder;
    private final String tokenHash;

    /**
     * @param passwordEncoder bean {@code BCryptPasswordEncoder} existente (mismo que usa {@code UsuarioService})
     * @param tokenHash hash bcrypt del credencial admin, o cadena vacía si no está configurado
     * @throws IllegalStateException si {@code tokenHash} no está en blanco pero tampoco tiene forma de hash bcrypt
     *         válido — falla el arranque en frío (fail loud) en vez de dejar un secreto mal configurado que
     *         nunca podría hacer match silenciosamente.
     */
    public AdminApiKeyFilter(PasswordEncoder passwordEncoder, String tokenHash) {
        this.passwordEncoder = passwordEncoder;
        this.tokenHash = tokenHash == null ? "" : tokenHash;

        if (StringUtils.hasText(this.tokenHash) && !FORMATO_BCRYPT.matcher(this.tokenHash).matches()) {
            throw new IllegalStateException(
                    "app.admin.token-hash está configurado pero no tiene formato de hash bcrypt válido. "
                            + "Debe ser un hash generado con BCryptPasswordEncoder (p. ej. $2a$10$...), "
                            + "nunca la contraseña en texto plano.");
        }
    }

    /**
     * Este filtro solo se ejecuta para rutas bajo {@code /api/admin/}. Es la
     * capa 1 de contención (ver Javadoc de la clase): fuera de esa ruta, el
     * filtro no interviene en absoluto y no puede producir autenticación.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(RUTA_ADMIN);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain)
            throws ServletException, IOException {

        // Limpia cualquier autenticación previa (p. ej. un JWT de tenant que
        // también llegase en esta petición): en /api/admin/** la única
        // identidad válida es la de este filtro, nunca la heredada de otro.
        SecurityContextHolder.clearContext();

        String token = request.getHeader(HEADER);

        // Fail closed: si el hash no está configurado (blank), NUNCA se
        // autentica nada aquí, sea cual sea el token presentado.
        if (StringUtils.hasText(token) && StringUtils.hasText(tokenHash)
                && passwordEncoder.matches(token, tokenHash)) {

            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    "platform-admin",
                    null,
                    List.of(() -> "ROLE_PLATFORM_ADMIN")
            );
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        filterChain.doFilter(request, response);
    }
}
