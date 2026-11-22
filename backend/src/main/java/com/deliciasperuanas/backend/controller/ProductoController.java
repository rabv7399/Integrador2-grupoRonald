package com.deliciasperuanas.backend.controller;

import com.deliciasperuanas.backend.entity.Producto;
import com.deliciasperuanas.backend.service.ProductoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;
import java.util.Map;

import java.util.List;

@RestController
@RequestMapping("/api/productos")
public class ProductoController {

    private final ProductoService productoService;

    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    @GetMapping
    public List<Producto> listar() {
        return productoService.listar();
    }

    @GetMapping("/{id}")
    public Producto buscarPorId(@PathVariable Long id) {
        return productoService.buscarPorId(id);
    }

    @PostMapping
    public ResponseEntity<Producto> crear(@RequestBody Producto producto) {
        Producto nuevo = productoService.guardar(producto);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevo);
    }

    @PutMapping("/{id}")
    public Producto actualizar(
            @PathVariable Long id,
            @RequestBody Producto producto) {

        return productoService.actualizar(id, producto);
    }

    @PostMapping(value="/{id}/imagen",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public Producto cargarImagen(@PathVariable Long id,@RequestParam("archivo") MultipartFile archivo){
        return productoService.cargarImagen(id,archivo);
    }

    @GetMapping("/{id}/imagen")
    public ResponseEntity<byte[]> imagen(@PathVariable Long id){
        return productoService.obtenerImagen(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        productoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}