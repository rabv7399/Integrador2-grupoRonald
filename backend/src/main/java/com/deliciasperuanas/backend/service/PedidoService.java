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
import java.util.Locale;

@Service
public class PedidoService {
    private final PedidoRepository pedidos;
    private final ProductoRepository productos;
    private final UsuarioRepository usuarios;
    private final MesaRepository mesas;
    private final InventarioService inventario;

    public PedidoService(PedidoRepository pedidos, ProductoRepository productos,
                         UsuarioRepository usuarios, MesaRepository mesas, InventarioService inventario) {
        this.pedidos = pedidos;
        this.productos = productos;
        this.usuarios = usuarios;
        this.mesas = mesas;
        this.inventario = inventario;
    }

    public record ResultadoRegistro(PedidoRespuesta respuesta, boolean creado) {}

    @Transactional
    public ResultadoRegistro registrar(CrearPedidoSolicitud solicitud, String clave, String correo) {
        if (clave == null || !clave.matches("[A-Za-z0-9._:-]{8,100}")) {
            throw error(HttpStatus.BAD_REQUEST, "REFERENCIA_INVALIDA",
                "Indica una Idempotency-Key de 8 a 100 caracteres");
        }
        Usuario solicitante = buscarUsuario(correo);
        TreeMap<Long,Integer> cantidades = normalizar(solicitud);
        String tipo = solicitud.tipoAtencion() == null || solicitud.tipoAtencion().isBlank()
                ? "LLEVAR" : solicitud.tipoAtencion().trim().toUpperCase(Locale.ROOT);
        if (!List.of("MESA","LLEVAR").contains(tipo)) {
            throw error(HttpStatus.BAD_REQUEST,"TIPO_ATENCION_INVALIDO",
                "Selecciona MESA o LLEVAR");
        }
        Long mesaId = solicitud.mesaId();
        if ("MESA".equals(tipo) && (mesaId == null || mesaId <= 0)) {
            throw error(HttpStatus.BAD_REQUEST,"MESA_REQUERIDA","Un pedido de mesa requiere mesaId");
        }
        if ("LLEVAR".equals(tipo) && mesaId != null) {
            throw error(HttpStatus.BAD_REQUEST,"MESA_NO_APLICA",
                "Un pedido para llevar no debe ocupar una mesa");
        }
        if ("MESA".equals(tipo) && "CLIENTE".equals(solicitante.getRol())) {
            throw error(HttpStatus.FORBIDDEN,"PEDIDO_MESA_PERSONAL",
                "Un mozo o cajero registra los pedidos de mesa");
        }

        String firma = firma(solicitante.getId(), cantidades, tipo, mesaId);
        Pedido anterior = pedidos.findByReferenciaOperacion(clave).orElse(null);
        if (anterior != null) {
            if (!anterior.getCliente().getId().equals(solicitante.getId())
                  || !anterior.getFirmaSolicitud().equals(firma)) {
                throw error(HttpStatus.CONFLICT,"REFERENCIA_REUTILIZADA",
                    "La referencia corresponde a otro pedido o contiene datos distintos");
            }
            return new ResultadoRegistro(respuesta(anterior),false);
        }

        // Bloqueo pesimista: dos mozos no pueden asignar la misma mesa a la vez.
        if ("MESA".equals(tipo)) {
            Mesa mesa = mesas.bloquearMesa(mesaId).orElseThrow(() ->
                error(HttpStatus.NOT_FOUND,"MESA_NO_ENCONTRADA","La mesa no existe"));
            if (!"DISPONIBLE".equals(mesa.getEstado())) {
                throw error(HttpStatus.CONFLICT,"MESA_OCUPADA",
                    "La mesa ya tiene atención activa");
            }
            mesa.setEstado("OCUPADA");
        }

        Pedido pedido = new Pedido();
        pedido.setReferenciaOperacion(clave);
        pedido.setFirmaSolicitud(firma);
        pedido.setCliente(solicitante);
        pedido.setTipoAtencion(tipo);
        pedido.setMesaId(mesaId);
        pedido.setEstado("PENDIENTE");
        pedido.setTotal(BigDecimal.ZERO);
        // La referencia única evita duplicados incluso bajo concurrencia.
        pedidos.saveAndFlush(pedido);

        BigDecimal total = BigDecimal.ZERO;
        // Orden de bloqueo fijo por producto: reduce interbloqueos de inventario.
        for (Map.Entry<Long,Integer> linea : cantidades.entrySet()) {
            Producto producto = productos.findById(linea.getKey()).orElseThrow(() ->
                error(HttpStatus.NOT_FOUND,"PRODUCTO_NO_ENCONTRADO","El producto no existe"));
            if (!Boolean.TRUE.equals(producto.getDisponible())) {
                throw error(HttpStatus.UNPROCESSABLE_CONTENT,"PRODUCTO_NO_DISPONIBLE",
                    "El plato no está disponible en la carta");
            }
            if (producto.getPrecio() == null || producto.getPrecio().signum() <= 0) {
                throw error(HttpStatus.UNPROCESSABLE_CONTENT,"PRECIO_INVALIDO",
                    "El producto tiene un precio no válido");
            }
            inventario.reservar(producto.getId(),linea.getValue());

            DetallePedido detalle = new DetallePedido();
            detalle.setProducto(producto);
            detalle.setCantidad(linea.getValue());
            detalle.setEstadoPreparacion("PENDIENTE");
            detalle.setPrecioUnitario(producto.getPrecio());
            detalle.setSubtotal(producto.getPrecio().multiply(BigDecimal.valueOf(linea.getValue())));
            pedido.agregarDetalle(detalle);
            total=total.add(detalle.getSubtotal());
        }
        pedido.setTotal(total);
        pedidos.saveAndFlush(pedido);
        return new ResultadoRegistro(respuesta(pedido),true);
    }

    @Transactional(readOnly=true)
    public List<PedidoRespuesta> listar(String correo) {
        Usuario usuario=buscarUsuario(correo);
        return pedidos.findAll().stream()
            .filter(p -> !"CLIENTE".equals(usuario.getRol()) || p.getCliente().getId().equals(usuario.getId()))
            .sorted((a,b) -> b.getId().compareTo(a.getId()))
            .map(this::respuesta).toList();
    }

    @Transactional(readOnly=true)
    public PedidoRespuesta consultar(Long pedidoId,String correo) {
        Usuario usuario=buscarUsuario(correo);
        Pedido pedido=pedidos.findById(pedidoId).orElseThrow(() ->
            error(HttpStatus.NOT_FOUND,"PEDIDO_NO_ENCONTRADO","No se encontró el pedido"));
        verificarAcceso(usuario,pedido);
        return respuesta(pedido);
    }

    @Transactional
    public PedidoRespuesta confirmar(Long pedidoId,String correo) {
        Usuario personal=buscarUsuario(correo);
        verificarPersonal(personal);
        Pedido pedido=bloquear(pedidoId);
        if (!"PENDIENTE".equals(pedido.getEstado())) {
            throw error(HttpStatus.CONFLICT,"ESTADO_PEDIDO_INVALIDO",
                "Solo se confirma un pedido pendiente");
        }
        pedido.getDetalles().stream()
            .sorted((a,b)->a.getProducto().getId().compareTo(b.getProducto().getId()))
            .forEach(d->inventario.confirmar(d.getProducto().getId(),d.getCantidad()));
        pedido.setEstado("CONFIRMADO");
        return respuesta(pedido);
    }

    @Transactional
    public PedidoRespuesta marcarListo(Long pedidoId,Long detalleId,String correo) {
        Usuario usuario=buscarUsuario(correo);
        if (!List.of("COCINERO","SUPERVISOR","ADMIN").contains(usuario.getRol())) {
            throw error(HttpStatus.FORBIDDEN,"SOLO_COCINA",
                "La preparación solo la actualiza Cocina o un responsable");
        }
        Pedido pedido=bloquear(pedidoId);
        if (!"CONFIRMADO".equals(pedido.getEstado())) {
            throw error(HttpStatus.CONFLICT,"PEDIDO_NO_CONFIRMADO",
                "El pedido debe confirmarse antes de preparar sus platos");
        }
        DetallePedido detalle=pedido.getDetalles().stream()
            .filter(d->d.getId().equals(detalleId)).findFirst().orElseThrow(() ->
                error(HttpStatus.NOT_FOUND,"DETALLE_NO_ENCONTRADO",
                    "El plato no pertenece a ese pedido"));
        // Un segundo toque es seguro e idempotente.
        detalle.setEstadoPreparacion("LISTO");
        return respuesta(pedido);
    }

    @Transactional
    public PedidoRespuesta cancelar(Long pedidoId,String correo) {
        Usuario usuario=buscarUsuario(correo);
        Pedido pedido=bloquear(pedidoId);
        verificarAcceso(usuario,pedido);
        if (!"PENDIENTE".equals(pedido.getEstado())) {
            throw error(HttpStatus.CONFLICT,"ESTADO_PEDIDO_INVALIDO",
                "Solo puede cancelarse un pedido pendiente. Una venta pagada no se revierte aquí.");
        }
        pedido.getDetalles().stream()
            .sorted((a,b)->a.getProducto().getId().compareTo(b.getProducto().getId()))
            .forEach(d->inventario.liberar(d.getProducto().getId(),d.getCantidad()));
        pedido.setEstado("CANCELADO");
        liberarMesa(pedido);
        return respuesta(pedido);
    }

    @Transactional
    public void liberarMesa(Pedido pedido) {
        if ("MESA".equals(pedido.getTipoAtencion()) && pedido.getMesaId()!=null) {
            mesas.bloquearMesa(pedido.getMesaId()).ifPresent(m->m.setEstado("DISPONIBLE"));
        }
    }

    private Pedido bloquear(Long id) {
        return pedidos.bloquearPedido(id).orElseThrow(() ->
            error(HttpStatus.NOT_FOUND,"PEDIDO_NO_ENCONTRADO","No se encontró el pedido"));
    }
    private Usuario buscarUsuario(String correo) {
        return usuarios.findByCorreo(correo).filter(Usuario::getActivo).orElseThrow(() ->
            error(HttpStatus.UNAUTHORIZED,"USUARIO_NO_AUTORIZADO","Cuenta no activa"));
    }
    private boolean personal(Usuario usuario) {
        return List.of("ADMIN","OPERADOR","MOZO","SUPERVISOR","CAJA","COCINERO").contains(usuario.getRol());
    }
    private void verificarPersonal(Usuario usuario) {
        if (!List.of("ADMIN","OPERADOR","MOZO","SUPERVISOR","CAJA").contains(usuario.getRol())) {
            throw error(HttpStatus.FORBIDDEN,"OPERACION_RESTRINGIDA",
                "Solo Mozo, Caja, Supervisor o Administrador confirma pedidos");
        }
    }
    private void verificarAcceso(Usuario usuario,Pedido pedido) {
        if (!usuario.getId().equals(pedido.getCliente().getId()) && !personal(usuario)) {
            throw error(HttpStatus.FORBIDDEN,"PEDIDO_AJENO","No puedes consultar este pedido");
        }
    }

    private TreeMap<Long,Integer> normalizar(CrearPedidoSolicitud solicitud) {
        if (solicitud == null || solicitud.items()==null
              || solicitud.items().isEmpty() || solicitud.items().size()>20) {
            throw error(HttpStatus.BAD_REQUEST,"PEDIDO_INVALIDO",
                "Indica entre 1 y 20 líneas");
        }
        TreeMap<Long,Integer> items=new TreeMap<>();
        for (LineaPedidoSolicitud linea:solicitud.items()) {
            if (linea==null || linea.productoId()==null || linea.productoId()<=0
                    || linea.cantidad()==null || linea.cantidad()<1 || linea.cantidad()>20) {
                throw error(HttpStatus.BAD_REQUEST,"CANTIDAD_INVALIDA",
                    "Cada línea requiere un producto y de 1 a 20 porciones");
            }
            items.merge(linea.productoId(),linea.cantidad(),Integer::sum);
            if (items.get(linea.productoId())>20) {
                throw error(HttpStatus.BAD_REQUEST,"CANTIDAD_INVALIDA",
                    "Máximo 20 porciones por plato");
            }
        }
        return items;
    }

    private String firma(Long usuarioId,TreeMap<Long,Integer> lineas,String tipo,Long mesaId) {
        StringBuilder data=new StringBuilder(usuarioId.toString());
        data.append('|').append(tipo).append('|').append(mesaId==null?"SIN_MESA":mesaId);
        lineas.forEach((id,cantidad)->data.append('|').append(id).append(':').append(cantidad));
        try{
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(data.toString().getBytes(StandardCharsets.UTF_8)));
        }catch(NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no disponible",ex);
        }
    }

    private PedidoRespuesta respuesta(Pedido pedido) {
        List<PedidoRespuesta.Item> items=pedido.getDetalles().stream()
            .map(d->new PedidoRespuesta.Item(d.getId(),d.getProducto().getId(),
                d.getProducto().getNombre(),d.getCantidad(),d.getPrecioUnitario(),
                d.getSubtotal(),d.getEstadoPreparacion())).toList();
        return new PedidoRespuesta(pedido.getId(),pedido.getReferenciaOperacion(),
            pedido.getEstado(),pedido.getCliente().getCorreo(),pedido.getTotal(),
            pedido.getFechaCreacion(),items,pedido.getTipoAtencion(),pedido.getMesaId());
    }
    private OperacionNegocioException error(HttpStatus estado,String codigo,String mensaje) {
        return new OperacionNegocioException(estado,codigo,mensaje);
    }
}
