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

                        .requestMatchers(HttpMethod.POST,"/api/productos/*/imagen")
                        .hasRole("ADMIN")

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
                        ).hasAnyRole("ADMIN", "OPERADOR", "MOZO", "SUPERVISOR", "CAJA")

                        // Reservas: el cliente puede crear; consultas globales y cancelaciones son internas.
                        // Se evita que un cliente lea o cancele reservas ajenas sin identidad vinculada.
                        .requestMatchers(HttpMethod.POST, "/api/reservas/**")
                        .hasAnyRole("ADMIN", "OPERADOR", "MOZO", "SUPERVISOR", "CLIENTE")

                        .requestMatchers(HttpMethod.GET, "/api/reservas/**")
                        .hasAnyRole("ADMIN", "OPERADOR", "MOZO", "SUPERVISOR", "CAJA")

                        .requestMatchers(HttpMethod.PATCH, "/api/reservas/**")
                        .hasAnyRole("ADMIN", "OPERADOR", "MOZO", "SUPERVISOR")

                        // Auditoría del inventario: NO expuesta al Mozo.
                        .requestMatchers(HttpMethod.GET,"/api/v1/inventario/movimientos")
                        .hasAnyRole("ADMIN","SUPERVISOR","CAJA")

                        // APF3 SOA: lectura interna de porciones, ajustes solo por ADMIN
                        .requestMatchers(HttpMethod.GET, "/api/v1/inventario", "/api/v1/inventario/**")
                        .hasAnyRole("ADMIN", "OPERADOR", "MOZO", "SUPERVISOR", "CAJA")

                        .requestMatchers(HttpMethod.PATCH,"/api/v1/inventario/*/salida")
                        .hasAnyRole("ADMIN","SUPERVISOR","CAJA")

                        .requestMatchers(HttpMethod.PATCH, "/api/v1/inventario/*/reponer")
                        .hasAnyRole("ADMIN", "SUPERVISOR", "CAJA")

                        .requestMatchers(HttpMethod.PUT, "/api/v1/inventario/**")
                        .hasRole("ADMIN")

                        // El personal confirma pedidos; clientes registran, consultan y cancelan los suyos
                        // Un cocinero marca platos como LISTO, pero no cobra ni confirma.
                        .requestMatchers(HttpMethod.POST, "/api/v1/pedidos/*/detalles/*/listo")
                        .hasAnyRole("ADMIN", "SUPERVISOR", "COCINERO")

                        .requestMatchers(HttpMethod.POST,"/api/v1/pedidos/*/detalles/*/entregado")
                        .hasRole("MOZO")

                        // Sólo Mozo modifica observaciones o anula platos individuales.
                        .requestMatchers(HttpMethod.PATCH,"/api/v1/pedidos/*/detalles/*/observacion")
                        .hasRole("MOZO")
                        .requestMatchers(HttpMethod.DELETE,"/api/v1/pedidos/*/detalles/*")
                        .hasRole("MOZO")

                        .requestMatchers(HttpMethod.POST,"/api/v1/pedidos/*/items")
                        .hasAnyRole("ADMIN","SUPERVISOR","MOZO","OPERADOR","CAJA")

                        .requestMatchers(HttpMethod.POST, "/api/v1/pedidos/envio")
                        .hasAnyRole("ADMIN","OPERADOR","MOZO","SUPERVISOR","CAJA")

                        .requestMatchers(HttpMethod.POST, "/api/v1/pedidos/*/confirmacion")
                        .hasAnyRole("ADMIN", "OPERADOR", "MOZO", "SUPERVISOR", "CAJA")

                        .requestMatchers(HttpMethod.POST, "/api/v1/pedidos/**")
                        .hasAnyRole("ADMIN", "OPERADOR", "MOZO", "SUPERVISOR", "CAJA", "CLIENTE")

                        .requestMatchers(HttpMethod.GET, "/api/v1/pedidos/**")
                        .hasAnyRole("ADMIN", "OPERADOR", "MOZO", "SUPERVISOR", "CAJA", "COCINERO", "CLIENTE")

                        // S06 ventas y comprobante interno
                        .requestMatchers("/api/v1/ventas/**")
                        .hasAnyRole("ADMIN","SUPERVISOR","CAJA")

                        // Gestión interna de usuarios y configuración del local
                        .requestMatchers("/api/v1/usuarios/**")
                        .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/configuracion/**").permitAll()
                        .requestMatchers(HttpMethod.PUT, "/api/v1/configuracion/**").hasRole("ADMIN")

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
