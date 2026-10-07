package com.deliciasperuanas.backend.integration;

import com.deliciasperuanas.backend.entity.Usuario;
import com.deliciasperuanas.backend.repository.UsuarioRepository;
import com.deliciasperuanas.backend.security.JwtService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.security.SecureRandom;
import java.util.Base64;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest {

    private static final String TEST_SECRET;

    static {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        TEST_SECRET = Base64.getEncoder().encodeToString(bytes);
    }

    @DynamicPropertySource
    static void propiedadesPrueba(DynamicPropertyRegistry registry) {

        registry.add(
                "spring.datasource.url",
                () -> "jdbc:h2:mem:delicias_test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE"
        );

        registry.add(
                "spring.datasource.driver-class-name",
                () -> "org.h2.Driver"
        );

        registry.add(
                "spring.datasource.username",
                () -> "sa"
        );

        registry.add(
                "spring.datasource.password",
                () -> ""
        );

        registry.add(
                "spring.jpa.hibernate.ddl-auto",
                () -> "create-drop"
        );

        registry.add(
                "spring.jpa.show-sql",
                () -> "false"
        );

        registry.add(
                "jwt.secret",
                () -> TEST_SECRET
        );

        registry.add(
                "jwt.expiration-ms",
                () -> "3600000"
        );

        registry.add(
                "cors.allowed-origins",
                () -> "http://localhost:5500"
        );
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private Usuario cliente;
    private String tokenCliente;

    @BeforeEach
    void prepararUsuario() {

        usuarioRepository.deleteAll();

        cliente = new Usuario();
        cliente.setNombre("Cliente Integracion");
        cliente.setCorreo("cliente.integration@deliciasperuanas.pe");
        cliente.setTelefono("987654321");
        cliente.setPassword(
                passwordEncoder.encode("TestPassword123!")
        );
        cliente.setRol("CLIENTE");
        cliente.setActivo(true);

        cliente = usuarioRepository.save(cliente);

        tokenCliente = jwtService.generarToken(cliente);
    }

    @Test
    void endpointProtegidoSinTokenDebeRetornar401()
            throws Exception {

        mockMvc.perform(
                        get("/api/categorias")
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void clienteConJwtValidoPuedeConsultarCategorias()
            throws Exception {

        mockMvc.perform(
                        get("/api/categorias")
                                .header(
                                        "Authorization",
                                        "Bearer " + tokenCliente
                                )
                )
                .andExpect(status().isOk());
    }

    @Test
    void clienteNoPuedeCrearCategoriaDebeRetornar403()
            throws Exception {

        String json = """
                {
                    "nombre": "Categoria Integracion",
                    "tipo": "PRUEBA",
                    "descripcion": "Prueba RBAC"
                }
                """;

        mockMvc.perform(
                        post("/api/categorias")
                                .header(
                                        "Authorization",
                                        "Bearer " + tokenCliente
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void loginConCredencialesValidasDebeRetornarToken()
            throws Exception {

        String json = """
                {
                    "correo": "cliente.integration@deliciasperuanas.pe",
                    "password": "TestPassword123!"
                }
                """;

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.rol").value("CLIENTE"));
    }
}