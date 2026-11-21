package com.deliciasperuanas.backend.service;

import com.deliciasperuanas.backend.dto.InventarioRespuesta;
import com.deliciasperuanas.backend.entity.InventarioPlato;
import com.deliciasperuanas.backend.entity.Producto;
import com.deliciasperuanas.backend.repository.InventarioPlatoRepository;
import com.deliciasperuanas.backend.repository.ProductoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventarioService {
    private final InventarioPlatoRepository inventarios;
    private final ProductoRepository productos;

    public InventarioService(InventarioPlatoRepository inventarios, ProductoRepository productos) {
        this.inventarios = inventarios;
        this.productos = productos;
    }

    @Transactional(readOnly = true)
    public InventarioRespuesta consultar(Long productoId) {
        InventarioPlato stock = inventarios.findByProductoId(productoId).orElseThrow(() ->
            new OperacionNegocioException(HttpStatus.NOT_FOUND, "INVENTARIO_NO_CONFIGURADO",
                "El plato no tiene cantidades configuradas"));
        return respuesta(stock);
    }

    @Transactional
    public InventarioRespuesta configurar(Long productoId, int totalPreparado) {
        if (totalPreparado < 0) {
            throw new OperacionNegocioException(HttpStatus.BAD_REQUEST, "CANTIDAD_INVALIDA",
                "El total preparado no puede ser negativo");
        }
        Producto producto = productos.findById(productoId).orElseThrow(() ->
            new OperacionNegocioException(HttpStatus.NOT_FOUND, "PRODUCTO_NO_ENCONTRADO",
                "El producto solicitado no existe"));
        InventarioPlato stock = inventarios.bloquearProducto(productoId).orElseGet(() -> {
            InventarioPlato nuevo = new InventarioPlato();
            nuevo.setProducto(producto);
            return nuevo;
        });
        if (totalPreparado < stock.getReservadas() + stock.getVendidas()) {
            throw new OperacionNegocioException(HttpStatus.CONFLICT, "TOTAL_MENOR_AL_COMPROMETIDO",
                "No se puede configurar un total inferior a las porciones reservadas y vendidas");
        }
        stock.setTotalPreparado(totalPreparado);
        return respuesta(inventarios.saveAndFlush(stock));
    }

    @Transactional
    public InventarioRespuesta reponer(Long productoId,int cantidad) {
        if (cantidad<=0 || cantidad>10000) {
            throw new OperacionNegocioException(HttpStatus.BAD_REQUEST,"CANTIDAD_INVALIDA",
                "La reposición debe sumar entre 1 y 10000 porciones");
        }
        InventarioPlato stock=inventarios.bloquearProducto(productoId).orElseThrow(() ->
            new OperacionNegocioException(HttpStatus.NOT_FOUND,"INVENTARIO_NO_CONFIGURADO",
                "El Administrador debe crear primero el inventario del plato"));
        if ((long)stock.getTotalPreparado()+cantidad>Integer.MAX_VALUE) {
            throw new OperacionNegocioException(HttpStatus.CONFLICT,"TOTAL_FUERA_RANGO",
                "La reposición supera el máximo de porciones permitido");
        }
        stock.setTotalPreparado(stock.getTotalPreparado()+cantidad);
        return respuesta(stock);
    }

    // Se invoca dentro de la misma transaccion que registra el pedido.
    // El bloqueo pesimista impide descontar dos veces las mismas porciones.
    @Transactional
    public void reservar(Long productoId, int cantidad) {
        InventarioPlato stock = inventarios.bloquearProducto(productoId).orElseThrow(() ->
            new OperacionNegocioException(HttpStatus.UNPROCESSABLE_CONTENT, "INVENTARIO_NO_CONFIGURADO",
                "Todavia no se ha configurado la disponibilidad de este plato"));
        if (stock.disponibles() < cantidad) {
            throw new OperacionNegocioException(HttpStatus.UNPROCESSABLE_CONTENT, "STOCK_INSUFICIENTE",
                "No hay suficientes porciones disponibles para este plato");
        }
        stock.setReservadas(stock.getReservadas() + cantidad);
    }

    @Transactional
    public void confirmar(Long productoId, int cantidad) {
        InventarioPlato stock = bloquearComprometido(productoId);
        if (stock.getReservadas() < cantidad) {
            throw new OperacionNegocioException(HttpStatus.CONFLICT, "RESERVA_STOCK_INCONSISTENTE",
                "La reserva de porciones necesita conciliacion");
        }
        stock.setReservadas(stock.getReservadas() - cantidad);
        stock.setVendidas(stock.getVendidas() + cantidad);
    }

    @Transactional
    public void liberar(Long productoId, int cantidad) {
        InventarioPlato stock = bloquearComprometido(productoId);
        if (stock.getReservadas() < cantidad) {
            throw new OperacionNegocioException(HttpStatus.CONFLICT, "RESERVA_STOCK_INCONSISTENTE",
                "La reserva de porciones necesita conciliacion");
        }
        stock.setReservadas(stock.getReservadas() - cantidad);
    }

    private InventarioPlato bloquearComprometido(Long productoId) {
        return inventarios.bloquearProducto(productoId).orElseThrow(() ->
            new OperacionNegocioException(HttpStatus.CONFLICT, "RESERVA_STOCK_NO_ENCONTRADA",
                "No se encontro el inventario del pedido"));
    }

    private InventarioRespuesta respuesta(InventarioPlato stock) {
        return new InventarioRespuesta(stock.getProducto().getId(), stock.getProducto().getNombre(),
            stock.getTotalPreparado(), stock.getReservadas(), stock.getVendidas(), stock.disponibles());
    }
}
