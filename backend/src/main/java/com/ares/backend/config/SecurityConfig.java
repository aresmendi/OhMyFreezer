package com.ares.backend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

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

    private final JwtFilter jwtFilter;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    // Misma propiedad que activa springdoc: si está deshabilitado, el
    // matcher permitAll ni siquiera se registra (defensa en profundidad,
    // no depende únicamente de que springdoc esté apagado).
    @Value("${springdoc.swagger-ui.enabled:false}")
    private boolean swaggerEnabled;

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
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Configura la sesión como stateless (sin estado)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Define reglas de acceso a los endpoints
                .authorizeHttpRequests(auth -> {
                    auth
                            // Endpoints públicos (no requieren token)
                            .requestMatchers("/ping").permitAll()
                            .requestMatchers("/api/usuarios/login", "/api/usuarios/register").permitAll();

                    // Los endpoints de Swagger/OpenAPI solo se registran como
                    // permitAll cuando springdoc está explícitamente habilitado
                    // (perfil dev). Si está deshabilitado, no existe matcher
                    // alguno para esas rutas: caen en anyRequest().authenticated()
                    // (401 sin token) aunque una propiedad de springdoc cambiase
                    // por error — defensa en profundidad, no solo config.
                    if (swaggerEnabled) {
                        auth.requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll();
                    }

                    auth
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
                            .anyRequest().authenticated();
                })

                // Añade el filtro JWT antes del filtro de autenticación estándar
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}