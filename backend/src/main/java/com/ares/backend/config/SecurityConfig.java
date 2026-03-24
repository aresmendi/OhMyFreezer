package com.ares.backend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuración principal de seguridad de la aplicación.
 * Define las reglas de acceso a los endpoints, el uso de JWT
 * para autenticación sin estado (stateless) y los filtros
 * de seguridad que se aplican a cada petición.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    /**
     * Filtro encargado de validar los tokens JWT en cada petición.
     */
    private final JwtFilter jwtFilter;

    /**
     * Configura la cadena de filtros de Spring Security y las reglas
     * de autorización para los distintos endpoints de la API.
     *
     * @param http objeto de configuración de seguridad HTTP
     * @return cadena de filtros de seguridad configurada
     * @throws Exception si ocurre un error durante la configuración
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Desactiva protección CSRF (no necesaria en APIs REST con JWT)
                .csrf(AbstractHttpConfigurer::disable)

                // Configura la sesión como stateless (sin estado)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Define reglas de acceso a los endpoints
                .authorizeHttpRequests(auth -> auth
                        // Endpoints públicos (no requieren token)
                        .requestMatchers("/api/usuarios/login", "/api/usuarios/register").permitAll()

                        //TODO: Acceso libre a documentación Swagger (solo desarrollo)
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

                        // ✅ Elaborar y verificar: cualquier usuario autenticado
                        .requestMatchers(HttpMethod.POST, "/api/recetas/*/elaborar").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/recetas/*/verificar").authenticated()

                        // Solo el jefe de cocina accede al CRUD de recetas
                        .requestMatchers(HttpMethod.POST, "/api/recetas/**").hasRole("JEFE")
                        .requestMatchers(HttpMethod.PUT, "/api/recetas/**").hasRole("JEFE")
                        .requestMatchers(HttpMethod.DELETE, "/api/recetas/**").hasRole("JEFE")

                        // Solo el jefe de cocina accede al CRUD de ingredientes
                        .requestMatchers(HttpMethod.POST, "/api/ingredientes/**").hasRole("JEFE")
                        .requestMatchers(HttpMethod.PUT, "/api/ingredientes/**").hasRole("JEFE")
                        .requestMatchers(HttpMethod.DELETE, "/api/ingredientes/**").hasRole("JEFE")

                        // Alertas y estadísticas accesibles solo para jefe
                        .requestMatchers("/api/alertas/**").hasRole("JEFE")
                        .requestMatchers("/api/estadisticas/**").hasRole("JEFE")

                        // El resto de endpoints requiere autenticación
                        .anyRequest().authenticated()
                )

                // Añade el filtro JWT antes del filtro de autenticación estándar
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Define el codificador de contraseñas utilizado en la aplicación.
     *
     * BCrypt proporciona almacenamiento seguro mediante hashing.
     *
     * @return instancia de PasswordEncoder basada en BCrypt
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}