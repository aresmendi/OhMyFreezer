package com.ares.backend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
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
    private final ApplicationContext applicationContext;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    // Misma propiedad que activa springdoc: si está deshabilitado, el
    // matcher permitAll ni siquiera se registra (defensa en profundidad,
    // no depende únicamente de que springdoc esté apagado).
    @Value("${springdoc.swagger-ui.enabled:false}")
    private boolean swaggerEnabled;

    // Hash bcrypt del credencial de superadmin de plataforma. Vacío por
    // defecto — NUNCA un hash real ni una contraseña en texto plano. En
    // blanco, AdminApiKeyFilter jamás autentica nada (fail closed): ver su
    // Javadoc y el diseño D2 de "negocio-onboarding-admin".
    @Value("${app.admin.token-hash:}")
    private String adminTokenHash;

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
                            // Superadmin de plataforma (AdminApiKeyFilter, path-scoped a
                            // /api/admin/**): PRIMER matcher tras el bloque permitAll, y
                            // ANTES del allowlist de tenant. Es la capa 2 (autoritativa) de
                            // contención descrita en el diseño de "negocio-onboarding-admin":
                            // un principal PLATFORM_ADMIN nunca satisface hasAnyRole(JEFE,COCINERO)
                            // más abajo, así que jamás alcanza una ruta de tenant.
                            .requestMatchers("/api/admin/**").hasRole("PLATFORM_ADMIN")

                            // Ingesta TPV (TpvApiKeyFilter, path-scoped a /api/tpv/**):
                            // SEGUNDO matcher, mismo criterio que el anterior. ROLE_TPV
                            // nunca satisface hasAnyRole(JEFE,COCINERO) más abajo (D-A del
                            // diseño de "tpv-integration"), así que un JWT de tenant jamás
                            // alcanza esta ruta y viceversa.
                            .requestMatchers("/api/tpv/**").hasRole("TPV")

                            // CRUD de mapeo SKU-TPV↔receta (D-F del diseño): NO es un
                            // subpath de /api/tpv/** (falta la barra tras "tpv"), así que
                            // TpvApiKeyFilter nunca interviene aquí — se autentica por JWT
                            // de tenant normal, restringido al jefe de cocina (mismo criterio
                            // que el resto del CRUD de dominio, más abajo).
                            .requestMatchers("/api/tpv-mapeos/**").hasRole("JEFE")

                            // ✅ Elaborar y verificar: cualquier usuario de tenant autenticado
                            // (jefe o cocinero). NOTA: si se añade un tercer rol de tenant en
                            // el futuro, debe incorporarse AQUÍ TAMBIÉN o quedará bloqueado
                            // (ver SecurityConfigAllowlistTest).
                            .requestMatchers(HttpMethod.POST, "/api/recetas/*/elaborar").hasAnyRole("JEFE", "COCINERO")
                            .requestMatchers(HttpMethod.POST, "/api/recetas/*/verificar").hasAnyRole("JEFE", "COCINERO")

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

                            // Solo el jefe de cocina puede dar de alta empleados de SU
                            // propio negocio (negocioId se hereda del caller, nunca del body)
                            .requestMatchers(HttpMethod.POST, "/api/usuarios/empleados").hasRole("JEFE")

                            // El resto de endpoints requiere ser un usuario de tenant
                            // (jefe o cocinero). ANTES era anyRequest().authenticated():
                            // se invirtió a un allowlist explícito porque un principal
                            // autenticado pero NO tenant-scoped (p. ej. PLATFORM_ADMIN)
                            // pasaba authenticated() igualmente, y NegocioFilterAspect
                            // falla ABIERTO (sin scoping) para cualquier principal que no
                            // sea CustomUserDetails — ver diseño D1-A. Esto es
                            // conductualmente idéntico a authenticated() para todo
                            // Usuario preexistente: CustomUserDetails.getAuthorities()
                            // siempre devuelve exactamente ROLE_JEFE o ROLE_COCINERO.
                            // Un tercer rol de tenant futuro DEBE añadirse aquí también.
                            .anyRequest().hasAnyRole("JEFE", "COCINERO");
                })

                // Añade el filtro JWT antes del filtro de autenticación estándar
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)

                // Filtro del superadmin de plataforma: path-scoped a /api/admin/**
                // (shouldNotFilter), se registra DESPUÉS del JWT de tenant para que
                // limpie cualquier contexto de seguridad que este último hubiese
                // podido establecer en esa misma petición. NO es un @Component: un
                // bean Filter se auto-registraría también en la cadena de filtros
                // del servlet container, fuera de Spring Security — se construye
                // aquí con "new", igual que se documenta en su propio Javadoc.
                .addFilterAfter(new AdminApiKeyFilter(passwordEncoder(), adminTokenHash), JwtFilter.class)

                // Filtro TPV: path-scoped a /api/tpv/** (shouldNotFilter). Sus
                // colaboradores (repositorio-backed) se resuelven LAZY vía
                // ApplicationContext dentro de doFilterInternal — mismo patrón que
                // JwtFilter — para no arrastrar JPA a la inicialización de este
                // @Configuration. Tampoco es un @Component, por la misma razón que
                // AdminApiKeyFilter. El orden relativo entre ambos filtros es
                // irrelevante: sus rutas (/api/admin/ vs /api/tpv/) son disjuntas.
                .addFilterAfter(new TpvApiKeyFilter(applicationContext), JwtFilter.class);

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