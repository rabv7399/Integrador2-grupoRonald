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
        MesaRepository mesas, ConfiguracionLocalRepository locales, PasswordEncoder encoder,
        @Value("${soa.demo.admin-password}") String adminPassword,
        @Value("${soa.demo.client-password}") String clientPassword,
        @Value("${soa.demo.mozo-password}") String mozoPassword,
        @Value("${soa.demo.supervisor-password}") String supervisorPassword,
        @Value("${soa.demo.caja-password}") String cajaPassword,
        @Value("${soa.demo.cocinero-password}") String cocineroPassword
    ) {
        return args -> {
            for(String clave:new String[]{adminPassword,clientPassword,mozoPassword,
                                          supervisorPassword,cajaPassword,cocineroPassword}) {
                if(clave==null || clave.length()<8) {
                    throw new IllegalStateException("Configura la contraseña temporal de cada rol (8 caracteres mínimo)");
                }
            }
            crearUsuario(usuarios,encoder,"Administrador Demo","admin.demo@delicias.local","ADMIN",adminPassword);
            crearUsuario(usuarios,encoder,"Cliente Demo","cliente.demo@delicias.local","CLIENTE",clientPassword);
            crearUsuario(usuarios,encoder,"Mozo Demo","mozo.demo@delicias.local","MOZO",mozoPassword);
            crearUsuario(usuarios,encoder,"Supervisor Demo","supervisor.demo@delicias.local","SUPERVISOR",supervisorPassword);
            crearUsuario(usuarios,encoder,"Caja Demo","caja.demo@delicias.local","CAJA",cajaPassword);
            crearUsuario(usuarios,encoder,"Cocinero Demo","cocinero.demo@delicias.local","COCINERO",cocineroPassword);

            Categoria almuerzos=categoria(categorias,"ALMUERZO","COMIDA");
            Categoria cenas=categoria(categorias,"CENA","COMIDA");
            Categoria bebidas=categoria(categorias,"BEBIDAS","BEBIDA");
            // Precios del sistema actual tienen prioridad sobre los históricos (p.ej. Lomo S/25).
            producto(productos,inventarios,almuerzos,"DP-001","Lomo Saltado","Salteado peruano", "25.00",30);
            producto(productos,inventarios,almuerzos,"DP-002","Ají de Gallina","Crema peruana de ají amarillo", "22.00",20);
            producto(productos,inventarios,bebidas,"DP-003","Chicha Morada","Bebida de maíz morado", "8.00",25);
            producto(productos,inventarios,cenas,"DP-004","Anticucho","Brochetas a la parrilla", "18.00",12);

            // Carta histórica de Botanero Ventas: PRECIOS SOLO REFERENCIALES PARA DEMO.
            producto(productos,inventarios,almuerzos,"DP-005","Rocoto Relleno","Especialidad arequipeña de rocoto con carne y queso","33.00",18);
            producto(productos,inventarios,almuerzos,"DP-006","Papa a la Huancaína","Papas con salsa cremosa de ají amarillo","35.00",12);
            producto(productos,inventarios,almuerzos,"DP-007","Ceviche","Pescado marinado con limón, cebolla y ají","32.00",15);
            producto(productos,inventarios,almuerzos,"DP-008","Tacacho con Cecina","Plátano verde y cecina amazónica","35.00",15);
            producto(productos,inventarios,almuerzos,"DP-009","Juane","Arroz con pollo y especias envuelto en bijao","30.00",16);
            producto(productos,inventarios,cenas,"DP-010","Parrillada de Chancho/Pollo/Res","Carnes a la parrilla","30.00",12);
            producto(productos,inventarios,cenas,"DP-011","Arroz Chaufa","Arroz salteado estilo chifa","23.00",18);
            producto(productos,inventarios,cenas,"DP-012","Caldo de Gallina","Sopa peruana tradicional","25.00",12);
            // Sin precio original confirmado: precio 0, disponible false. No se pueden ordenar.
            producto(productos,inventarios,bebidas,"DP-013","Maracuyá","Refresco de maracuyá","0.00",0);
            producto(productos,inventarios,bebidas,"DP-014","Chicha de Jora","Bebida tradicional de maíz","0.00",0);
            producto(productos,inventarios,bebidas,"DP-015","Infusiones de té y mate","Infusiones calientes","0.00",0);
            producto(productos,inventarios,bebidas,"DP-016","Gaseosas Coca-Cola e Inca Kola","Refrescos gaseosos","0.00",0);
            producto(productos,inventarios,bebidas,"DP-017","Escocesa","Bebida de la carta original","0.00",0);
            producto(productos,inventarios,bebidas,"DP-018","Pisco Sour","Cóctel peruano","20.00",8);
            producto(productos,inventarios,bebidas,"DP-019","Chilcano","Cóctel tradicional de pisco","20.00",8);
            producto(productos,inventarios,bebidas,"DP-020","Sangría","Bebida de vino y frutas","20.00",8);
            producto(productos,inventarios,bebidas,"DP-021","Cusqueña","Cerveza de la carta original","15.00",8);
            producto(productos,inventarios,bebidas,"DP-022","Vino Parras y Reyes","Vino de la carta original","23.00",8);

            for(int numero=1;numero<=15;numero++){
                Mesa mesa=new Mesa();
                mesa.setNumero(numero);mesa.setCapacidad(4);mesa.setEstado("DISPONIBLE");
                mesas.save(mesa);
            }
            ConfiguracionLocal local=new ConfiguracionLocal();
            locales.save(local);
        };
    }

    private void crearUsuario(UsuarioRepository repo,PasswordEncoder encoder,
          String nombre,String correo,String rol,String password) {
        Usuario u=new Usuario();
        u.setNombre(nombre);u.setCorreo(correo);
        u.setPassword(encoder.encode(password));u.setRol(rol);u.setActivo(true);
        repo.save(u);
    }
    private Categoria categoria(CategoriaRepository repo,String nombre,String tipo) {
        Categoria c=new Categoria();
        c.setNombre(nombre);c.setTipo(tipo);c.setDescripcion("Carta de demostración APF3");
        return repo.save(c);
    }
    private void producto(ProductoRepository repo,InventarioPlatoRepository inv,
            Categoria cat,String codigo,String nombre,String descripcion,
            String precio,int total) {
        Producto p=new Producto();
        p.setCodigo(codigo);p.setNombre(nombre);p.setDescripcion(descripcion);
        p.setPrecio(new BigDecimal(precio));p.setDisponible(total>0);
        p.setCategoria(cat);p=repo.save(p);
        InventarioPlato stock=new InventarioPlato();
        stock.setProducto(p);stock.setTotalPreparado(total);inv.save(stock);
    }
}
