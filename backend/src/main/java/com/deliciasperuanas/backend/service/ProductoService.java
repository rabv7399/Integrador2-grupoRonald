package com.deliciasperuanas.backend.service;

import com.deliciasperuanas.backend.entity.Categoria;
import com.deliciasperuanas.backend.entity.Producto;
import com.deliciasperuanas.backend.repository.CategoriaRepository;
import com.deliciasperuanas.backend.repository.ProductoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;

    public ProductoService(
            ProductoRepository productoRepository,
            CategoriaRepository categoriaRepository
    ) {
        this.productoRepository = productoRepository;
        this.categoriaRepository = categoriaRepository;
    }

    public List<Producto> listar() {
        return productoRepository.findAll();
    }

    public Producto buscarPorId(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Producto no encontrado"
                        )
                );
    }

    public Producto guardar(Producto producto) {

        if (producto.getCategoria() == null ||
                producto.getCategoria().getId() == null) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Debe indicar una categoría"
            );
        }

        Categoria categoria = categoriaRepository
                .findById(producto.getCategoria().getId())
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Categoría no encontrada"
                        )
                );

        producto.setCategoria(categoria);

        return productoRepository.save(producto);
    }

    public Producto actualizar(Long id, Producto datos) {

        Producto producto = buscarPorId(id);

        producto.setCodigo(datos.getCodigo());
        producto.setNombre(datos.getNombre());
        producto.setDescripcion(datos.getDescripcion());
        producto.setPrecio(datos.getPrecio());
        producto.setDisponible(datos.getDisponible());

        if (datos.getCategoria() != null &&
                datos.getCategoria().getId() != null) {

            Categoria categoria = categoriaRepository
                    .findById(datos.getCategoria().getId())
                    .orElseThrow(() ->
                            new ResponseStatusException(
                                    HttpStatus.NOT_FOUND,
                                    "Categoría no encontrada"
                            )
                    );

            producto.setCategoria(categoria);
        }

        return productoRepository.save(producto);
    }

    public void eliminar(Long id) {
        Producto producto = buscarPorId(id);
        productoRepository.delete(producto);
    }
}