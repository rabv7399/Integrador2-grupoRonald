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

                        // Registro y login pÃºblicos
                        .requestMatchers("/api/auth/**").permitAll()

                        // CategorÃ­as
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/categorias/**"
                        ).authenticated()

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
                        ).authenticated()

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
                                "CLIENTE"
                        )

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