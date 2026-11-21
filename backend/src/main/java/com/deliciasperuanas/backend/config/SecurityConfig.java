package com.deliciasperuanas.backend.config;

import com.deliciasperuanas.backend.security.JwtAuthenticationFilter;

import jakarta.servlet.DispatcherType;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .authorizeHttpRequests(auth -> auth

                        // Permitir despacho interno de errores
                        .dispatcherTypeMatchers(
                                DispatcherType.ERROR
                        ).permitAll()

                        .requestMatchers("/error").permitAll()

                        // Frontend público de demostración APF2
                        .requestMatchers(
                                "/",
                                "/index.html",
                                "/styles.css",
                                "/app.js",
                                "/favicon.ico"
                        ).permitAll()

                        // Portal gastronómico público APF3, sin acceso a operaciones protegidas
                        .requestMatchers("/portal/**", "/soa-demo/**").permitAll()

                        // Documentación de la API; las operaciones conservan sus permisos
                        .requestMatchers(
                                HttpMethod.GET,
                                "/v3/api-docs",
                                "/v3/api-docs/**",
                                "/swagger-ui.html",
                                "/swagger-ui/**"
                        ).permitAll()

                        // Registro y login públicos
                        .requestMatchers("/api/auth/**").permitAll()

                        // Categorías
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/categorias/**"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/categorias/**"
                        ).hasRole("ADMIN")

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/categorias/**"
                        ).hasRole("ADMIN")

                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/categorias/**"
                        ).hasRole("ADMIN")

                        // Productos
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/productos/**"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/productos/**"
                        ).hasAnyRole("ADMIN", "OPERADOR")

                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/productos/**"
                        ).hasAnyRole("ADMIN", "OPERADOR")

                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/productos/**"
                        ).hasRole("ADMIN")

                        // Mesas
                        .requestMatchers(
                                "/api/mesas/**"
                        ).hasAnyRole("ADMIN", "OPERADOR")

                        // Reservas
                        .requestMatchers(
                                "/api/reservas/**"
                        ).hasAnyRole(
                                "ADMIN",
                                "OPERADOR",
                                "MOZO",
                                "SUPERVISOR",
                                "CLIENTE"
                        )

                        // APF3 SOA: lectura interna de porciones, ajustes solo por ADMIN
                        .requestMatchers(HttpMethod.GET, "/api/v1/inventario/**")
                        .hasAnyRole("ADMIN", "OPERADOR", "MOZO", "SUPERVISOR")

                        .requestMatchers(HttpMethod.PUT, "/api/v1/inventario/**")
                        .hasRole("ADMIN")

                        // El personal confirma pedidos; clientes registran, consultan y cancelan los suyos
                        .requestMatchers(HttpMethod.POST, "/api/v1/pedidos/*/confirmacion")
                        .hasAnyRole("ADMIN", "OPERADOR", "MOZO", "SUPERVISOR")

                        .requestMatchers(HttpMethod.POST, "/api/v1/pedidos/**")
                        .hasAnyRole("ADMIN", "OPERADOR", "MOZO", "SUPERVISOR", "CLIENTE")

                        .requestMatchers(HttpMethod.GET, "/api/v1/pedidos/**")
                        .authenticated()

                        .anyRequest().authenticated()
                )

                .exceptionHandling(exceptions -> exceptions

                        .authenticationEntryPoint(
                                (request, response, ex) -> {
                                    response.setStatus(401);
                                    response.setContentType(
                                            MediaType.APPLICATION_JSON_VALUE
                                    );
                                    response.getWriter().write(
                                            "{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Token JWT requerido o invalido\"}"
                                    );
                                }
                        )

                        .accessDeniedHandler(
                                (request, response, ex) -> {
                                    response.setStatus(403);
                                    response.setContentType(
                                            MediaType.APPLICATION_JSON_VALUE
                                    );
                                    response.getWriter().write(
                                            "{\"status\":403,\"error\":\"Forbidden\",\"message\":\"No tiene permisos suficientes\"}"
                                    );
                                }
                        )
                )

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}
