package com.deliciasperuanas.backend.service;

import com.deliciasperuanas.backend.entity.Categoria;
import com.deliciasperuanas.backend.entity.Producto;
import com.deliciasperuanas.backend.repository.CategoriaRepository;
import com.deliciasperuanas.backend.repository.ProductoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.http.CacheControl;
import com.deliciasperuanas.backend.entity.ProductoImagen;
import com.deliciasperuanas.backend.repository.ProductoImagenRepository;
import jakarta.transaction.Transactional;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;

import java.util.List;

@Service
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final ProductoImagenRepository imagenes;

    public ProductoService(
            ProductoRepository productoRepository,
            CategoriaRepository categoriaRepository,ProductoImagenRepository imagenes
    ) {
        this.productoRepository = productoRepository;
        this.categoriaRepository = categoriaRepository;
        this.imagenes=imagenes;
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

    @Transactional
    public Producto cargarImagen(Long productoId,MultipartFile archivo){
        Producto producto=buscarPorId(productoId);
        if(archivo==null||archivo.isEmpty()||archivo.getSize()>1_500_000L)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Imagen obligatoria de hasta 1.5 MB (JPEG o PNG)");
        try{
            byte[] datos=archivo.getBytes();
            String formato;
            int ancho,alto;
            try(ImageInputStream stream=ImageIO.createImageInputStream(new ByteArrayInputStream(datos))){
                if(stream==null)throw new IOException("Archivo vacío o no legible");
                Iterator<ImageReader> lectores=ImageIO.getImageReaders(stream);
                if(!lectores.hasNext())throw new IOException("Formato de imagen no reconocido");
                ImageReader lector=lectores.next();
                try{
                    lector.setInput(stream,true,true);
                    formato=lector.getFormatName().toLowerCase(java.util.Locale.ROOT);
                    ancho=lector.getWidth(0);alto=lector.getHeight(0);
                }finally{lector.dispose();}
            }
            String tipo= switch(formato){
                case "jpeg","jpg" -> "image/jpeg";
                case "png" -> "image/png";
                default -> throw new IOException("Solo se admiten JPEG o PNG");
            };
            if(ancho<64||alto<64||ancho>5000||alto>5000)
                throw new IOException("La foto debe tener de 64 a 5000 píxeles por lado");
            ProductoImagen imagen=imagenes.findById(productoId).orElseGet(ProductoImagen::new);
            imagen.setProducto(producto);
            imagen.setContenido(datos);
            imagen.setTipoMime(tipo);
            imagenes.saveAndFlush(imagen);
            producto.setImagenUrl("/api/productos/"+productoId+"/imagen?v="+System.currentTimeMillis());
            return productoRepository.saveAndFlush(producto);
        }catch(IOException ex){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Imagen inválida: "+ex.getMessage(),ex);
        }
    }

    @org.springframework.transaction.annotation.Transactional(readOnly=true)
    public ResponseEntity<byte[]> obtenerImagen(Long id){
        ProductoImagen imagen=imagenes.findById(id).orElseThrow(()->
            new ResponseStatusException(HttpStatus.NOT_FOUND,"Este plato no tiene fotografía"));
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(imagen.getTipoMime()))
            .cacheControl(CacheControl.noCache()).body(imagen.getContenido());
    }

    // DELETE administrativo = baja lógica para conservar detalles de pedidos y ventas.
    public void eliminar(Long id) {
        Producto producto = buscarPorId(id);
        producto.setDisponible(false);
        productoRepository.save(producto);
    }
}