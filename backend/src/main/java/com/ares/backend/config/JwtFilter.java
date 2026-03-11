package com.ares.backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filtro de seguridad que intercepta cada petición HTTP para validar
 * el token JWT incluido en la cabecera Authorization.
 * Si el token es válido, se autentica al usuario en el contexto de
 * seguridad de Spring Security con su rol correspondiente.
 */
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    /**
     * Utilidad para validar y extraer información del token JWT.
     */
    private final JwtUtil jwtUtil;

    /**
     * Función que se ejecuta una vez por cada petición HTTP.
     * Comprueba si existe un token JWT en la cabecera Authorization,
     * lo valida y establece la autenticación en el contexto de seguridad
     * de Spring si el token es correcto.
     *
     * @param request petición HTTP entrante
     * @param response respuesta HTTP
     * @param filterChain cadena de filtros de Spring Security
     * @throws ServletException si ocurre un error en el procesamiento del servlet
     * @throws IOException si ocurre un error de entrada/salida
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // Obtiene la cabecera Authorization de la petición
        String authHeader = request.getHeader("Authorization");

        // Comprueba que exista y que tenga el formato "Bearer token"
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            // Valida el token JWT
            if (jwtUtil.validarToken(token)) {

                // Extrae el nombre de usuario del token
                String username = jwtUtil.extraerUsername(token);

                // Comprueba si el usuario tiene rol de jefe de cocina
                boolean esJefe = jwtUtil.extraerEsJefeCocina(token);

                // Asigna el rol correspondiente
                String rol = esJefe ? "ROLE_JEFE" : "ROLE_COCINERO";

                // Crea el objeto de autenticación para Spring Security
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                username,
                                null,
                                List.of(new SimpleGrantedAuthority(rol))
                        );

                // Guarda la autenticación en el contexto de seguridad
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        // Continúa con la cadena de filtros
        filterChain.doFilter(request, response);
    }
}