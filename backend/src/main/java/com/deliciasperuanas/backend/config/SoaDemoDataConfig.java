package com.deliciasperuanas.backend.config;

import com.deliciasperuanas.backend.entity.*;
import com.deliciasperuanas.backend.repository.*;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;

@Configuration
@Profile("soa-demo")
public class SoaDemoDataConfig {
    @Bean
    ApplicationRunner datosDemo(
        UsuarioRepository usuarios, CategoriaRepository categorias,
        ProductoRepository productos, InventarioPlatoRepository inventarios,
        MesaRepository mesas, PasswordEncoder encoder,
        @Value("${soa.demo.admin-password}") String adminPassword,
        @Value("${soa.demo.client-password}") String clientPassword
    ) {
        return args -> {
            if (adminPassword == null || adminPassword.length() < 8
                    || clientPassword == null || clientPassword.length() < 8) {
                throw new IllegalStateException("Define SOA_DEMO_ADMIN_PASSWORD y SOA_DEMO_CLIENT_PASSWORD (8+ caracteres) solo en tu terminal");
            }
            crearUsuario(usuarios, encoder, "Administrador Demo", "admin.demo@delicias.local", "ADMIN", adminPassword);
            crearUsuario(usuarios, encoder, "Cliente Demo", "cliente.demo@delicias.local", "CLIENTE", clientPassword);
            Categoria fondos = categoria(categorias, "Platos de Fondo", "COMIDA");
            Categoria cenas = categoria(categorias, "Cenas", "COMIDA");
            Categoria bebidas = categoria(categorias, "Bebidas", "BEBIDA");
            producto(productos, inventarios, fondos, "DP-001", "Lomo Saltado",
                "Carne y vegetales salteados, al estilo peruano", "25.00", 30);
            producto(productos, inventarios, fondos, "DP-002", "Aji de Gallina",
                "Plato peruano con crema de aji amarillo", "22.00", 20);
            producto(productos, inventarios, bebidas, "DP-003", "Chicha Morada",
                "Bebida tradicional de maiz morado", "8.00", 25);
            producto(productos, inventarios, cenas, "DP-004", "Anticuchos",
                "Brochetas a la parrilla con aji panca", "18.00", 12);

            Mesa mesa = new Mesa();
            mesa.setNumero(1);
            mesa.setCapacidad(4);
            mesa.setEstado("DISPONIBLE");
            mesas.save(mesa);
        };
    }

    private void crearUsuario(UsuarioRepository repo, PasswordEncoder encoder,
                              String nombre, String correo, String rol, String password) {
        Usuario u = new Usuario();
        u.setNombre(nombre);
        u.setCorreo(correo);
        u.setPassword(encoder.encode(password));
        u.setRol(rol);
        u.setActivo(true);
        repo.save(u);
    }

    private Categoria categoria(CategoriaRepository repo, String nombre, String tipo) {
        Categoria c = new Categoria();
        c.setNombre(nombre);
        c.setTipo(tipo);
        c.setDescripcion("Categoria de demostracion APF3");
        return repo.save(c);
    }

    private void producto(ProductoRepository repo, InventarioPlatoRepository stockRepo,
                          Categoria categoria, String codigo, String nombre,
                          String descripcion, String precio, int total) {
        Producto p = new Producto();
        p.setCodigo(codigo);
        p.setNombre(nombre);
        p.setDescripcion(descripcion);
        p.setPrecio(new BigDecimal(precio));
        p.setDisponible(true);
        p.setCategoria(categoria);
        p = repo.save(p);
        InventarioPlato stock = new InventarioPlato();
        stock.setProducto(p);
        stock.setTotalPreparado(total);
        stockRepo.save(stock);
    }
}
