package com.deliciasperuanas.backend.service;

import com.deliciasperuanas.backend.dto.*;
import com.deliciasperuanas.backend.entity.*;
import com.deliciasperuanas.backend.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;
import java.util.List;

@Service
public class PedidoService {
    private final PedidoRepository pedidos;
    private final ProductoRepository productos;
    private final UsuarioRepository usuarios;
    private final InventarioService inventario;

    public PedidoService(PedidoRepository pedidos, ProductoRepository productos,
                         UsuarioRepository usuarios, InventarioService inventario) {
        this.pedidos = pedidos;
        this.productos = productos;
        this.usuarios = usuarios;
        this.inventario = inventario;
    }

    public record ResultadoRegistro(PedidoRespuesta respuesta, boolean creado) {}

    @Transactional
    public ResultadoRegistro registrar(CrearPedidoSolicitud solicitud, String clave, String correo) {
        if (clave == null || !clave.matches("[A-Za-z0-9._:-]{8,100}")) {
            throw error(HttpStatus.BAD_REQUEST, "REFERENCIA_INVALIDA",
                "Indica una Idempotency-Key de 8 a 100 caracteres");
        }
        Usuario cliente = buscarUsuario(correo);
        TreeMap<Long, Integer> cantidades = normalizar(solicitud);
        String firma = firma(cliente.getId(), cantidades);
        Pedido existente = pedidos.findByReferenciaOperacion(clave).orElse(null);
        if (existente != null) {
            if (!existente.getCliente().getId().equals(cliente.getId())
                    || !existente.getFirmaSolicitud().equals(firma)) {
                throw error(HttpStatus.CONFLICT, "REFERENCIA_REUTILIZADA",
                    "La referencia ya pertenece a otro pedido o contiene datos diferentes");
            }
            return new ResultadoRegistro(respuesta(existente), false);
        }

        Pedido pedido = new Pedido();
        pedido.setReferenciaOperacion(clave);
        pedido.setFirmaSolicitud(firma);
        pedido.setCliente(cliente);
        pedido.setEstado("PENDIENTE");
        pedido.setTotal(BigDecimal.ZERO);
        // El indice unico impide dos pedidos con la misma referencia concurrente.
        pedidos.saveAndFlush(pedido);

        BigDecimal total = BigDecimal.ZERO;
        // TreeMap ordena los bloqueos por producto para reducir los interbloqueos.
        for (Map.Entry<Long, Integer> linea : cantidades.entrySet()) {
            Producto producto = productos.findById(linea.getKey()).orElseThrow(() ->
                error(HttpStatus.NOT_FOUND, "PRODUCTO_NO_ENCONTRADO", "El producto no existe"));
            if (!Boolean.TRUE.equals(producto.getDisponible())) {
                throw error(HttpStatus.UNPROCESSABLE_CONTENT, "PRODUCTO_NO_DISPONIBLE",
                    "Este producto no esta disponible en la carta");
            }
            if (producto.getPrecio() == null || producto.getPrecio().signum() <= 0) {
                throw error(HttpStatus.UNPROCESSABLE_CONTENT, "PRECIO_INVALIDO",
                    "El precio registrado para el producto es invalido");
            }
            inventario.reservar(producto.getId(), linea.getValue());

            DetallePedido detalle = new DetallePedido();
            detalle.setProducto(producto);
            detalle.setCantidad(linea.getValue());
            detalle.setPrecioUnitario(producto.getPrecio());
            detalle.setSubtotal(producto.getPrecio().multiply(BigDecimal.valueOf(linea.getValue())));
            pedido.agregarDetalle(detalle);
            total = total.add(detalle.getSubtotal());
        }
        pedido.setTotal(total);
        pedidos.flush();
        return new ResultadoRegistro(respuesta(pedido), true);
    }

    @Transactional(readOnly = true)
    public PedidoRespuesta consultar(Long pedidoId, String correo) {
        Usuario solicitante = buscarUsuario(correo);
        Pedido pedido = pedidos.findById(pedidoId).orElseThrow(() ->
            error(HttpStatus.NOT_FOUND, "PEDIDO_NO_ENCONTRADO", "No se encontro el pedido"));
        verificarAcceso(solicitante, pedido);
        return respuesta(pedido);
    }

    @Transactional
    public PedidoRespuesta confirmar(Long pedidoId, String correo) {
        Usuario solicitante = buscarUsuario(correo);
        verificarPersonal(solicitante);
        Pedido pedido = bloquear(pedidoId);
        if (!"PENDIENTE".equals(pedido.getEstado())) {
            throw error(HttpStatus.CONFLICT, "ESTADO_PEDIDO_INVALIDO",
                "Solo puede confirmarse un pedido pendiente");
        }
        pedido.getDetalles().stream()
            .sorted((a,b) -> a.getProducto().getId().compareTo(b.getProducto().getId()))
            .forEach(d -> inventario.confirmar(d.getProducto().getId(), d.getCantidad()));
        pedido.setEstado("CONFIRMADO");
        return respuesta(pedido);
    }

    @Transactional
    public PedidoRespuesta cancelar(Long pedidoId, String correo) {
        Usuario solicitante = buscarUsuario(correo);
        Pedido pedido = bloquear(pedidoId);
        verificarAcceso(solicitante, pedido);
        if (!"PENDIENTE".equals(pedido.getEstado())) {
            throw error(HttpStatus.CONFLICT, "ESTADO_PEDIDO_INVALIDO",
                "Solo puede cancelarse un pedido pendiente");
        }
        pedido.getDetalles().stream()
            .sorted((a,b) -> a.getProducto().getId().compareTo(b.getProducto().getId()))
            .forEach(d -> inventario.liberar(d.getProducto().getId(), d.getCantidad()));
        pedido.setEstado("CANCELADO");
        return respuesta(pedido);
    }

    private Pedido bloquear(Long id) {
        return pedidos.bloquearPedido(id).orElseThrow(() ->
            error(HttpStatus.NOT_FOUND, "PEDIDO_NO_ENCONTRADO", "No se encontro el pedido"));
    }

    private Usuario buscarUsuario(String correo) {
        return usuarios.findByCorreo(correo).filter(Usuario::getActivo).orElseThrow(() ->
            error(HttpStatus.UNAUTHORIZED, "USUARIO_NO_AUTORIZADO", "El usuario no esta activo"));
    }

    private void verificarAcceso(Usuario u, Pedido p) {
        if (!u.getId().equals(p.getCliente().getId()) && !esPersonal(u)) {
            throw error(HttpStatus.FORBIDDEN, "PEDIDO_AJENO", "No tienes acceso a este pedido");
        }
    }
    private void verificarPersonal(Usuario u) {
        if (!esPersonal(u)) throw error(HttpStatus.FORBIDDEN, "OPERACION_RESTRINGIDA",
            "Solo el personal autorizado puede confirmar pedidos");
    }
    private boolean esPersonal(Usuario u) {
        return List.of("ADMIN", "OPERADOR", "MOZO", "SUPERVISOR").contains(u.getRol());
    }

    private TreeMap<Long, Integer> normalizar(CrearPedidoSolicitud s) {
        if (s == null || s.items() == null || s.items().isEmpty() || s.items().size() > 20) {
            throw error(HttpStatus.BAD_REQUEST, "PEDIDO_INVALIDO", "Indica entre 1 y 20 lineas");
        }
        TreeMap<Long, Integer> cantidad = new TreeMap<>();
        for (LineaPedidoSolicitud linea : s.items()) {
            if (linea == null || linea.productoId() == null || linea.productoId() <= 0
                    || linea.cantidad() == null || linea.cantidad() < 1 || linea.cantidad() > 20) {
                throw error(HttpStatus.BAD_REQUEST, "CANTIDAD_INVALIDA", "Cada linea requiere un producto y una cantidad entre 1 y 20");
            }
            cantidad.merge(linea.productoId(), linea.cantidad(), Integer::sum);
            if (cantidad.get(linea.productoId()) > 20) {
                throw error(HttpStatus.BAD_REQUEST, "CANTIDAD_INVALIDA",
                    "Maximo 20 porciones por producto y pedido");
            }
        }
        return cantidad;
    }

    private String firma(Long clienteId, TreeMap<Long, Integer> lineas) {
        StringBuilder data = new StringBuilder(clienteId.toString());
        lineas.forEach((id,cantidad) -> data.append('|').append(id).append(':').append(cantidad));
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(data.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }

    private PedidoRespuesta respuesta(Pedido p) {
        List<PedidoRespuesta.Item> items = p.getDetalles().stream()
            .map(d -> new PedidoRespuesta.Item(d.getProducto().getId(), d.getProducto().getNombre(),
                d.getCantidad(), d.getPrecioUnitario(), d.getSubtotal())).toList();
        return new PedidoRespuesta(p.getId(), p.getReferenciaOperacion(), p.getEstado(),
            p.getCliente().getCorreo(), p.getTotal(), p.getFechaCreacion(), items);
    }

    private OperacionNegocioException error(HttpStatus estado, String codigo, String mensaje) {
        return new OperacionNegocioException(estado, codigo, mensaje);
    }
}
