package com.ares.backend.config;

import com.ares.backend.entity.Usuario;
import com.ares.backend.service.UsuarioService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
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
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final ApplicationContext applicationContext;

    @Autowired
    public JwtFilter(JwtUtil jwtUtil, ApplicationContext applicationContext) {
        this.jwtUtil = jwtUtil;
        this.applicationContext = applicationContext;
    }

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

                // Multi-tenancy: un token sin la claim negocioId es un token
                // legacy (emitido antes de esta fase) o inválido. Se rechaza
                // en frío (fail closed) SIN autenticar: no debe resolver de
                // forma silenciosa al negocio semilla ni a ningún otro negocio,
                // aunque el Usuario recargado desde BD ya tenga uno asignado
                // (ver spec: "Pre-migration token without negocioId claim").
                // El request continúa sin autenticación → 401 en cualquier
                // endpoint protegido.
                if (jwtUtil.extraerNegocioId(token) == null) {
                    filterChain.doFilter(request, response);
                    return;
                }

                Long id = jwtUtil.extraerUsuarioId(token);

                //cargar usuario desde BD (obtenido de forma lazy para evitar ciclo)
                UsuarioService usuarioService = applicationContext.getBean(UsuarioService.class);
                Usuario usuario = usuarioService.buscarPorId(id);

                //crear CustomUserDetails
                CustomUserDetails userDetails = new CustomUserDetails(usuario);

                //crear auth con usuario completo
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );


                // Guarda la autenticación en el contexto de seguridad
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        // Continúa con la cadena de filtros
        filterChain.doFilter(request, response);
    }
}