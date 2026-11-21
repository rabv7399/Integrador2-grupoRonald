package com.deliciasperuanas.backend.integration;

import com.deliciasperuanas.backend.entity.*;
import com.deliciasperuanas.backend.repository.*;
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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.Base64;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PedidoInventarioIntegrationTest {
    private static final String CLAVE;
    static {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        CLAVE = Base64.getEncoder().encodeToString(bytes);
    }

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", () -> "jdbc:h2:mem:soa_pedidos_test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE");
        r.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        r.add("spring.datasource.username", () -> "sa");
        r.add("spring.datasource.password", () -> "");
        r.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        r.add("spring.jpa.show-sql", () -> "false");
        r.add("jwt.secret", () -> CLAVE);
        r.add("jwt.expiration-ms", () -> "3600000");
    }

    @Autowired MockMvc mvc;
    @Autowired ProductoRepository productos;
    @Autowired CategoriaRepository categorias;
    @Autowired UsuarioRepository usuarios;
    @Autowired PedidoRepository pedidos;
    @Autowired JwtService jwt;
    @Autowired PasswordEncoder encoder;

    private Long productoId;
    private String tokenCliente;
    private String tokenAdmin;

    @BeforeEach
    void preparar() {
        Categoria categoria = new Categoria();
        categoria.setNombre("Comidas APF3");
        categoria.setTipo("COMIDA");
        categoria = categorias.saveAndFlush(categoria);

        Producto producto = new Producto();
        producto.setNombre("Lomo Saltado");
        producto.setCodigo("SOA-" + System.nanoTime());
        producto.setPrecio(new BigDecimal("25.00"));
        producto.setDisponible(true);
        producto.setCategoria(categoria);
        productoId = productos.saveAndFlush(producto).getId();

        Usuario cliente = usuario("cliente.apf3@ejemplo.test", "CLIENTE");
        Usuario admin = usuario("admin.apf3@ejemplo.test", "ADMIN");
        tokenCliente = jwt.generarToken(cliente);
        tokenAdmin = jwt.generarToken(admin);
    }

    private Usuario usuario(String correo, String rol) {
        Usuario u = new Usuario();
        u.setNombre("Persona de prueba");
        u.setCorreo(correo);
        u.setPassword(encoder.encode("Test12345678!"));
        u.setRol(rol);
        u.setActivo(true);
        return usuarios.saveAndFlush(u);
    }

    private void prepararInventario(int total) throws Exception {
        mvc.perform(put("/api/v1/inventario/{id}", productoId)
                .header("Authorization", "Bearer " + tokenAdmin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"totalPreparado\":" + total + "}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.disponibles").value(total));
    }

    private String solicitud(int cantidad) {
        return "{\"items\":[{\"productoId\":" + productoId + ",\"cantidad\":" + cantidad + "}]}";
    }

    @Test
    void pedidoReservaPorcionesYConfirmacionVendeSinDuplicar() throws Exception {
        prepararInventario(30);
        String clave = "apf3-prueba-0001";
        mvc.perform(post("/api/v1/pedidos").header("Authorization", "Bearer " + tokenCliente)
                .header("Idempotency-Key", clave).contentType(MediaType.APPLICATION_JSON)
                .content(solicitud(2)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.estado").value("PENDIENTE"))
            .andExpect(jsonPath("$.total").value(50.00));

        mvc.perform(get("/api/v1/inventario/{id}", productoId).header("Authorization", "Bearer " + tokenAdmin))
            .andExpect(status().isOk()).andExpect(jsonPath("$.reservadas").value(2))
            .andExpect(jsonPath("$.disponibles").value(28));

        mvc.perform(post("/api/v1/pedidos").header("Authorization", "Bearer " + tokenCliente)
                .header("Idempotency-Key", clave).contentType(MediaType.APPLICATION_JSON).content(solicitud(2)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.referenciaOperacion").value(clave));

        Long id = pedidos.findByReferenciaOperacion(clave).orElseThrow().getId();
        mvc.perform(post("/api/v1/pedidos/{id}/confirmacion", id)
                .header("Authorization", "Bearer " + tokenAdmin))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("CONFIRMADO"));

        mvc.perform(get("/api/v1/inventario/{id}", productoId).header("Authorization", "Bearer " + tokenAdmin))
            .andExpect(status().isOk()).andExpect(jsonPath("$.reservadas").value(0))
            .andExpect(jsonPath("$.vendidas").value(2))
            .andExpect(jsonPath("$.disponibles").value(28));

        mvc.perform(post("/api/v1/pedidos/{id}/cancelacion", id)
                .header("Authorization", "Bearer " + tokenCliente))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.codigo").value("ESTADO_PEDIDO_INVALIDO"));
    }

    @Test
    void sinPorcionesRetorna422YSinPedido() throws Exception {
        prepararInventario(1);
        mvc.perform(post("/api/v1/pedidos").header("Authorization", "Bearer " + tokenCliente)
                .header("Idempotency-Key", "apf3-sin-stock").contentType(MediaType.APPLICATION_JSON)
                .content(solicitud(2)))
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.codigo").value("STOCK_INSUFICIENTE"));
    }

    @Test
    void cancelarPedidoPendienteLiberaPorciones() throws Exception {
        prepararInventario(4);
        mvc.perform(post("/api/v1/pedidos").header("Authorization", "Bearer " + tokenCliente)
                .header("Idempotency-Key", "apf3-cancelacion").contentType(MediaType.APPLICATION_JSON)
                .content(solicitud(3)))
            .andExpect(status().isCreated());
        Long id = pedidos.findByReferenciaOperacion("apf3-cancelacion").orElseThrow().getId();
        mvc.perform(post("/api/v1/pedidos/{id}/cancelacion", id)
                .header("Authorization", "Bearer " + tokenCliente))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("CANCELADO"));
        mvc.perform(get("/api/v1/inventario/{id}", productoId).header("Authorization", "Bearer " + tokenAdmin))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.reservadas").value(0))
            .andExpect(jsonPath("$.disponibles").value(4));
    }

    @Test
    void mismaReferenciaConOtroContenidoRetorna409() throws Exception {
        prepararInventario(10);
        mvc.perform(post("/api/v1/pedidos").header("Authorization", "Bearer " + tokenCliente)
                .header("Idempotency-Key", "apf3-clave-unica").contentType(MediaType.APPLICATION_JSON)
                .content(solicitud(1))).andExpect(status().isCreated());
        mvc.perform(post("/api/v1/pedidos").header("Authorization", "Bearer " + tokenCliente)
                .header("Idempotency-Key", "apf3-clave-unica").contentType(MediaType.APPLICATION_JSON)
                .content(solicitud(2)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.codigo").value("REFERENCIA_REUTILIZADA"));
    }

    @Test
    void clienteNoPuedeAlterarStockYNadiePuedeAccederSinToken() throws Exception {
        mvc.perform(put("/api/v1/inventario/{id}", productoId)
                .header("Authorization", "Bearer " + tokenCliente)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"totalPreparado\":30}"))
            .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/pedidos")
                .header("Idempotency-Key", "apf3-sin-token")
                .contentType(MediaType.APPLICATION_JSON).content(solicitud(1)))
            .andExpect(status().isUnauthorized());
    }
}
