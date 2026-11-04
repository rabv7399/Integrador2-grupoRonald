package com.deliciasperuanas.backend.integration;

import com.deliciasperuanas.backend.entity.Categoria;
import com.deliciasperuanas.backend.entity.Producto;
import com.deliciasperuanas.backend.repository.ProductoRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:productos_test;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "jwt.secret=MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE="
})
@Transactional
class ProductoConsultaIntegrationTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private ProductoRepository productoRepository;

    @Test
    void listadoCargaCategoriasConUnaSolaConsulta() {
        Categoria entradas = crearCategoria("Entradas");
        Categoria bebidas = crearCategoria("Bebidas");
        crearProducto("P-001", entradas);
        crearProducto("P-002", bebidas);
        crearProducto("P-003", entradas);
        entityManager.flush();
        entityManager.clear();

        Statistics statistics = entityManagerFactory
                .unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        List<Producto> productos = productoRepository.findAll();

        assertEquals(3, productos.size());
        for (Producto producto : productos) {
            assertNotNull(producto.getCategoria());
            String esperado = producto.getCodigo().equals("P-002") ? "Bebidas" : "Entradas";
            assertEquals(esperado, producto.getCategoria().getNombre());
        }
        assertEquals(1L, statistics.getPrepareStatementCount(),
                "El listado y sus categorias deben resolverse en una consulta");
    }

    private Categoria crearCategoria(String nombre) {
        Categoria categoria = new Categoria();
        categoria.setNombre(nombre);
        categoria.setTipo("PRUEBA");
        entityManager.persist(categoria);
        return categoria;
    }

    private void crearProducto(String codigo, Categoria categoria) {
        Producto producto = new Producto();
        producto.setCodigo(codigo);
        producto.setNombre("Producto " + codigo);
        producto.setPrecio(new BigDecimal("10.00"));
        producto.setCategoria(categoria);
        entityManager.persist(producto);
    }
}
