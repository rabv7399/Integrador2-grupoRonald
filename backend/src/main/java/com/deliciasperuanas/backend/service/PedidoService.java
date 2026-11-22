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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.time.LocalDateTime;

@Service
public class PedidoService {
    private final PedidoRepository pedidos;
    private final ProductoRepository productos;
    private final UsuarioRepository usuarios;
    private final MesaRepository mesas;
    private final InventarioService inventario;
    private final AdicionPedidoRepository adiciones;

    public PedidoService(PedidoRepository pedidos, ProductoRepository productos,
                         UsuarioRepository usuarios, MesaRepository mesas,
                         InventarioService inventario,AdicionPedidoRepository adiciones) {
        this.pedidos = pedidos;
        this.productos = productos;
        this.usuarios = usuarios;
        this.mesas = mesas;
        this.inventario = inventario;
        this.adiciones = adiciones;
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
        List<LineaPedidoSolicitud> lineas=normalizarConObservaciones(solicitud.items());
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

        String firma = firmaConObservaciones(
            firma(solicitante.getId(), cantidades, tipo, mesaId),lineas);
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
        for (LineaPedidoSolicitud linea : lineas) {
            Producto producto = productos.findById(linea.productoId()).orElseThrow(() ->
                error(HttpStatus.NOT_FOUND,"PRODUCTO_NO_ENCONTRADO","El producto no existe"));
            if (!Boolean.TRUE.equals(producto.getDisponible())) {
                throw error(HttpStatus.UNPROCESSABLE_CONTENT,"PRODUCTO_NO_DISPONIBLE",
                    "El plato no está disponible en la carta");
            }
            if (producto.getPrecio() == null || producto.getPrecio().signum() <= 0) {
                throw error(HttpStatus.UNPROCESSABLE_CONTENT,"PRECIO_INVALIDO",
                    "El producto tiene un precio no válido");
            }
            inventario.reservar(producto.getId(),linea.cantidad());

            DetallePedido detalle = new DetallePedido();
            detalle.setObservacion(normalizarObservacion(linea.observacion()));
            detalle.setProducto(producto);
            detalle.setCantidad(linea.cantidad());
            detalle.setEstadoPreparacion("PENDIENTE");
            detalle.setPrecioUnitario(producto.getPrecio());
            detalle.setSubtotal(producto.getPrecio().multiply(BigDecimal.valueOf(linea.cantidad())));
            pedido.agregarDetalle(detalle);
            total=total.add(detalle.getSubtotal());
        }
        pedido.setTotal(total);
        pedidos.saveAndFlush(pedido);
        return new ResultadoRegistro(respuesta(pedido),true);
    }

    // Un solo paso de negocio para el Mozo: registrar + reservar + enviar a Cocina.
    // Si confirmar falla, la transacción completa revierte la creación y la ocupación.
    // Reintentos con la misma Idempotency-Key no duplican el pedido.
    @Transactional
    public ResultadoRegistro enviarACocina(CrearPedidoSolicitud solicitud,String clave,String correo) {
        Usuario usuario=buscarUsuario(correo);
        verificarPersonal(usuario);
        ResultadoRegistro registro=registrar(solicitud,clave,correo);
        if("PENDIENTE".equals(registro.respuesta().estado())) {
            return new ResultadoRegistro(confirmar(registro.respuesta().pedidoId(),correo),registro.creado());
        }
        return registro;
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
            .filter(d->!"ANULADO".equals(d.getEstadoPreparacion()))
            .forEach(d->inventario.confirmar(d.getProducto().getId(),d.getCantidad()));
        pedido.setEstado("CONFIRMADO");
        return respuesta(pedido);
    }

    @Transactional
    public PedidoRespuesta marcarListo(Long pedidoId,Long detalleId,String correo) {
        Usuario usuario=buscarUsuario(correo);
        if (!"COCINERO".equals(usuario.getRol())) {
            throw error(HttpStatus.FORBIDDEN,"SOLO_COCINA",
                "Solo el Cocinero puede marcar platos LISTOS");
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
        if("ANULADO".equals(detalle.getEstadoPreparacion()))
            throw error(HttpStatus.CONFLICT,"PLATO_ANULADO",
                "No se prepara un plato anulado");
        // No deshacer una entrega al volver a pulsar LISTO.
        if("PENDIENTE".equals(detalle.getEstadoPreparacion())){
            detalle.setEstadoPreparacion("LISTO");
            detalle.setFechaListo(LocalDateTime.now());
        }
        return respuesta(pedido);
    }

    @Transactional
    public PedidoRespuesta marcarEntregado(Long pedidoId,Long detalleId,String correo) {
        Usuario usuario=buscarUsuario(correo);
        if(!"MOZO".equals(usuario.getRol()))
            throw error(HttpStatus.FORBIDDEN,"SOLO_MOZO",
                "La entrega solo la registra Mozo");
        Pedido pedido=bloquear(pedidoId);
        if(!"CONFIRMADO".equals(pedido.getEstado()))
            throw error(HttpStatus.CONFLICT,"PEDIDO_NO_CONFIRMADO",
                "No se puede entregar un pedido no confirmado o ya cobrado");
        DetallePedido detalle=pedido.getDetalles().stream()
            .filter(d->d.getId().equals(detalleId)).findFirst().orElseThrow(() ->
                error(HttpStatus.NOT_FOUND,"DETALLE_NO_ENCONTRADO",
                    "El plato no pertenece a este pedido"));
        if("ANULADO".equals(detalle.getEstadoPreparacion()))
            throw error(HttpStatus.CONFLICT,"PLATO_ANULADO",
                "Un plato anulado no puede entregarse");
        if("PENDIENTE".equals(detalle.getEstadoPreparacion()))
            throw error(HttpStatus.CONFLICT,"PLATO_NO_LISTO",
                "Cocina debe marcar LISTO antes de que el Mozo entregue el plato");
        if("LISTO".equals(detalle.getEstadoPreparacion())){
            detalle.setEstadoPreparacion("ENTREGADO");
            detalle.setFechaEntrega(LocalDateTime.now());
        }
        return respuesta(pedido);
    }

    @Transactional
    public ResultadoRegistro adicionar(Long pedidoId,AdicionPedidoSolicitud solicitud,
                                        String clave,String correo) {
        if(clave==null||!clave.matches("[A-Za-z0-9._:-]{8,100}"))
            throw error(HttpStatus.BAD_REQUEST,"REFERENCIA_INVALIDA",
                "La Idempotency-Key debe tener de 8 a 100 caracteres");
        Usuario usuario=buscarUsuario(correo);
        verificarPersonal(usuario);
        if(solicitud==null)
            throw error(HttpStatus.BAD_REQUEST,"PEDIDO_INVALIDO","Indica uno o más platos");
        TreeMap<Long,Integer> cantidades=normalizar(new CrearPedidoSolicitud(solicitud.items()));
        List<LineaPedidoSolicitud> lineas=normalizarConObservaciones(solicitud.items());
        String hash=firmaConObservaciones(firma(usuario.getId(),cantidades,"ADICION",pedidoId),lineas);
        Pedido pedido=bloquear(pedidoId);

        AdicionPedido registrada=adiciones.findByClave(clave).orElse(null);
        if(registrada!=null){
            if(!registrada.getPedido().getId().equals(pedidoId)||!registrada.getFirma().equals(hash))
                throw error(HttpStatus.CONFLICT,"REFERENCIA_REUTILIZADA",
                    "Esta referencia de adición fue utilizada con otros datos");
            return new ResultadoRegistro(respuesta(pedido),false);
        }
        if(!List.of("PENDIENTE","CONFIRMADO").contains(pedido.getEstado()))
            throw error(HttpStatus.CONFLICT,"PEDIDO_CERRADO",
                "No se pueden agregar platos después de cancelar o cobrar");
        if(pedido.getDetalles().size()+lineas.size()>30)
            throw error(HttpStatus.BAD_REQUEST,"DEMASIADAS_LINEAS",
                "Se permiten hasta 30 líneas por pedido");

        BigDecimal nuevoTotal=pedido.getTotal();
        // Bloqueo de stock por ID ascendente y bloqueo de pedido para sincronizar con Caja.
        for(LineaPedidoSolicitud item:lineas){
            Producto producto=productos.findById(item.productoId()).orElseThrow(() ->
                error(HttpStatus.NOT_FOUND,"PRODUCTO_NO_ENCONTRADO","No existe el plato"));
            if(!Boolean.TRUE.equals(producto.getDisponible())||producto.getPrecio()==null||
                    producto.getPrecio().signum()<=0)
                throw error(HttpStatus.UNPROCESSABLE_CONTENT,"PRODUCTO_NO_DISPONIBLE",
                    "Este plato no está habilitado para pedidos");
            inventario.reservar(producto.getId(),item.cantidad());
            if("CONFIRMADO".equals(pedido.getEstado()))
                inventario.confirmar(producto.getId(),item.cantidad());
            DetallePedido d=new DetallePedido();
            d.setProducto(producto);
            d.setObservacion(normalizarObservacion(item.observacion()));
            d.setCantidad(item.cantidad());
            d.setEstadoPreparacion("PENDIENTE");
            d.setFechaInicio(LocalDateTime.now());
            d.setPrecioUnitario(producto.getPrecio());
            d.setSubtotal(producto.getPrecio().multiply(BigDecimal.valueOf(item.cantidad())));
            pedido.agregarDetalle(d);
            nuevoTotal=nuevoTotal.add(d.getSubtotal());
        }
        pedido.setTotal(nuevoTotal);
        pedidos.saveAndFlush(pedido);

        AdicionPedido adicion=new AdicionPedido();
        adicion.setClave(clave);adicion.setFirma(hash);adicion.setPedido(pedido);
        adiciones.saveAndFlush(adicion);
        return new ResultadoRegistro(respuesta(pedido),true);
    }

    // Observación por línea, visible en Cocina y en la comanda. Nunca se altera una
    // instrucción de preparación cuando Cocina ya marcó LISTO o se entregó.
    @Transactional
    public PedidoRespuesta cambiarObservacion(Long pedidoId,Long detalleId,
        ObservacionPlatoSolicitud solicitud,String correo) {
        exigirMozo(correo);
        if(solicitud==null)
            throw error(HttpStatus.BAD_REQUEST,"OBSERVACION_INVALIDA",
                "Indica el texto de la observación");
        Pedido pedido=bloquear(pedidoId);
        verificarOrdenEditable(pedido);
        DetallePedido detalle=buscarDetalle(pedido,detalleId);
        exigirPlatoPendiente(detalle);
        detalle.setObservacion(normalizarObservacion(solicitud.observacion()));
        return respuesta(pedido);
    }

    // Cantidad por línea editable mientras esté pendiente: recalcula subtotal y
    // disponibilidad; si aumenta aplica reserva/verificación; si baja libera stock.
    @Transactional
    public PedidoRespuesta cambiarCantidad(Long pedidoId,Long detalleId,
        CantidadPlatoSolicitud solicitud,String correo){
        exigirMozo(correo);
        if(solicitud==null||solicitud.cantidad()==null||
           solicitud.cantidad()<1||solicitud.cantidad()>20)
            throw error(HttpStatus.BAD_REQUEST,"CANTIDAD_INVALIDA",
                "La cantidad debe ser entre 1 y 20");
        Pedido pedido=bloquear(pedidoId);
        verificarOrdenEditable(pedido);
        DetallePedido detalle=buscarDetalle(pedido,detalleId);
        exigirPlatoPendiente(detalle);
        int nueva=solicitud.cantidad(),anterior=detalle.getCantidad();
        int delta=nueva-anterior;
        if(delta==0)return respuesta(pedido);
        if(delta>0){
            // También se controla el límite de 20 porciones por producto.
            int suma=pedido.getDetalles().stream()
                .filter(d->d.getProducto().getId().equals(detalle.getProducto().getId())&&
                    !"ANULADO".equals(d.getEstadoPreparacion()))
                .mapToInt(DetallePedido::getCantidad).sum();
            if(suma+delta>20)
                throw error(HttpStatus.BAD_REQUEST,"CANTIDAD_INVALIDA",
                    "Máximo 20 porciones del mismo producto por pedido");
            inventario.reservar(detalle.getProducto().getId(),delta);
            if("CONFIRMADO".equals(pedido.getEstado()))
                inventario.confirmar(detalle.getProducto().getId(),delta);
        }else{
            int retiradas=-delta;
            if("CONFIRMADO".equals(pedido.getEstado()))
                inventario.revertirConfirmado(detalle.getProducto().getId(),retiradas);
            else inventario.liberar(detalle.getProducto().getId(),retiradas);
        }
        BigDecimal nuevoSubtotal=detalle.getPrecioUnitario().multiply(BigDecimal.valueOf(nueva));
        pedido.setTotal(pedido.getTotal().subtract(detalle.getSubtotal()).add(nuevoSubtotal));
        detalle.setCantidad(nueva);
        detalle.setSubtotal(nuevoSubtotal);
        return respuesta(pedido);
    }

    // Baja lógica para conservar el rastro: plato, precio original, motivo y usuario.
    // La anulación parcial libera porciones bajo bloqueo transaccional y resta sólo
    // el importe de esa línea. No se permite tocar platos que Cocina ya terminó.
    @Transactional
    public PedidoRespuesta anularPlato(Long pedidoId,Long detalleId,
        AnulacionPlatoSolicitud solicitud,String correo) {
        Usuario mozo=exigirMozo(correo);
        if(solicitud==null || solicitud.motivo()==null ||
           solicitud.motivo().trim().length()<5 ||
           solicitud.motivo().trim().length()>300)
            throw error(HttpStatus.BAD_REQUEST,"MOTIVO_ANULACION_REQUERIDO",
                "Explica la anulación del plato (5 a 300 caracteres)");
        Pedido pedido=bloquear(pedidoId);
        verificarOrdenEditable(pedido);
        DetallePedido detalle=buscarDetalle(pedido,detalleId);
        exigirPlatoPendiente(detalle);
        long restantes=pedido.getDetalles().stream()
            .filter(d->!"ANULADO".equals(d.getEstadoPreparacion())).count();
        if(restantes==1){
            // Último plato: se cancela la orden y se desocupa la mesa, pero
            // se conserva el motivo de anulación en el detalle auditado.
            cancelar(pedidoId,correo);
            detalle.setEstadoPreparacion("ANULADO");
            detalle.setMotivoAnulacion(solicitud.motivo().trim());
            detalle.setAnuladoPor(mozo.getCorreo());
            detalle.setFechaAnulacion(LocalDateTime.now());
            pedido.setTotal(BigDecimal.ZERO);
            return respuesta(pedido);
        }
        if("CONFIRMADO".equals(pedido.getEstado()))
            inventario.revertirConfirmado(detalle.getProducto().getId(),detalle.getCantidad());
        else inventario.liberar(detalle.getProducto().getId(),detalle.getCantidad());
        detalle.setEstadoPreparacion("ANULADO");
        detalle.setMotivoAnulacion(solicitud.motivo().trim());
        detalle.setAnuladoPor(mozo.getCorreo());
        detalle.setFechaAnulacion(LocalDateTime.now());
        pedido.setTotal(pedido.getTotal().subtract(detalle.getSubtotal()));
        return respuesta(pedido);
    }

    private Usuario exigirMozo(String correo){
        Usuario usuario=buscarUsuario(correo);
        if(!"MOZO".equals(usuario.getRol()))
            throw error(HttpStatus.FORBIDDEN,"SOLO_MOZO",
                "Solo el Mozo puede editar observaciones o anular un plato");
        return usuario;
    }
    private void verificarOrdenEditable(Pedido pedido){
        if(!List.of("PENDIENTE","CONFIRMADO").contains(pedido.getEstado()))
            throw error(HttpStatus.CONFLICT,"PEDIDO_CERRADO",
                "No se puede editar un pedido cancelado o ya cobrado");
    }
    private DetallePedido buscarDetalle(Pedido pedido,Long id){
        return pedido.getDetalles().stream()
            .filter(d->d.getId().equals(id)).findFirst().orElseThrow(()->
                error(HttpStatus.NOT_FOUND,"DETALLE_NO_ENCONTRADO",
                    "El plato no pertenece a esta orden"));
    }
    private void exigirPlatoPendiente(DetallePedido detalle){
        if(!"PENDIENTE".equals(detalle.getEstadoPreparacion()))
            throw error(HttpStatus.CONFLICT,"PLATO_EN_PREPARACION_O_ANULADO",
                "Sólo se modifican platos PENDIENTES: Cocina debe no haberlos marcado LISTO");
    }
    private String normalizarObservacion(String value){
        if(value==null||value.isBlank())return null;
        String nota=value.strip();
        if(nota.length()>240)
            throw error(HttpStatus.BAD_REQUEST,"OBSERVACION_LARGA",
                "Máximo 240 caracteres por observación");
        return nota;
    }

    // Mantener por separado dos porciones del mismo producto con distinta preparación:
    // p. ej., un Lomo Saltado normal y otro bajo en sal. Combinar sólo notas iguales.
    private List<LineaPedidoSolicitud> normalizarConObservaciones(
        List<LineaPedidoSolicitud> originales){
        TreeMap<String,LineaPedidoSolicitud> porNota=new TreeMap<>();
        for(LineaPedidoSolicitud linea:originales){
            String nota=normalizarObservacion(linea.observacion());
            String clave=String.format("%020d",linea.productoId())+"|"+(nota==null?"":nota);
            LineaPedidoSolicitud anterior=porNota.get(clave);
            int cantidad=anterior==null?linea.cantidad():anterior.cantidad()+linea.cantidad();
            if(cantidad>20)
                throw error(HttpStatus.BAD_REQUEST,"CANTIDAD_INVALIDA",
                    "Máximo 20 porciones por línea");
            porNota.put(clave,new LineaPedidoSolicitud(linea.productoId(),cantidad,nota));
        }
        return new ArrayList<>(porNota.values());
    }
    private String firmaConObservaciones(String base,List<LineaPedidoSolicitud> lineas){
        boolean algunaNota=lineas.stream().anyMatch(l->l.observacion()!=null);
        if(!algunaNota)return base;
        StringBuilder datos=new StringBuilder(base);
        for(LineaPedidoSolicitud l:lineas){
            datos.append("|").append(l.productoId()).append(":").append(l.cantidad())
                .append(":").append(l.observacion()==null?"":l.observacion());
        }
        try{
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(datos.toString().getBytes(StandardCharsets.UTF_8)));
        }catch(NoSuchAlgorithmException ex){
            throw new IllegalStateException("SHA-256 no disponible",ex);
        }
    }

    @Transactional
    public PedidoRespuesta cancelar(Long pedidoId,String correo) {
        Usuario usuario=buscarUsuario(correo);
        Pedido pedido=bloquear(pedidoId);
        verificarAcceso(usuario,pedido);
        if (!List.of("PENDIENTE","CONFIRMADO").contains(pedido.getEstado())) {
            throw error(HttpStatus.CONFLICT,"ESTADO_PEDIDO_INVALIDO",
                "No se puede cancelar una orden ya pagada o previamente cancelada.");
        }
        if("CONFIRMADO".equals(pedido.getEstado())) {
            // Las cancelaciones de clientes siguen siendo solo PENDIENTE (compatibilidad APF2/APF3).
            if(!personal(usuario) || "COCINERO".equals(usuario.getRol())){
                throw error(HttpStatus.CONFLICT,"ESTADO_PEDIDO_INVALIDO",
                    "Un cliente no puede anular un pedido confirmado; consulta al Mozo.");
            }
            // No anular entregas o producción ya comenzada sin un proceso de devolución.
            verificarPersonal(usuario);
            if(pedido.getDetalles().stream().anyMatch(d ->
                 !List.of("PENDIENTE","ANULADO").contains(d.getEstadoPreparacion()))) {
                throw error(HttpStatus.CONFLICT,"ORDEN_EN_PREPARACION",
                    "Cocina ya preparó platos. Solicita al supervisor una devolución controlada.");
            }
        }
        boolean confirmado="CONFIRMADO".equals(pedido.getEstado());
        pedido.getDetalles().stream()
            .filter(d->!"ANULADO".equals(d.getEstadoPreparacion()))
            .sorted((a,b)->a.getProducto().getId().compareTo(b.getProducto().getId()))
            .forEach(d->{
                if(confirmado)inventario.revertirConfirmado(d.getProducto().getId(),d.getCantidad());
                else inventario.liberar(d.getProducto().getId(),d.getCantidad());
            });
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
                d.getSubtotal(),d.getEstadoPreparacion(),d.getFechaInicio(),d.getFechaListo(),
                d.getFechaEntrega(),d.getObservacion(),d.getMotivoAnulacion(),
                d.getAnuladoPor(),d.getFechaAnulacion())).toList();
        return new PedidoRespuesta(pedido.getId(),pedido.getReferenciaOperacion(),
            pedido.getEstado(),pedido.getCliente().getCorreo(),pedido.getTotal(),
            pedido.getFechaCreacion(),items,pedido.getTipoAtencion(),pedido.getMesaId());
    }
    private OperacionNegocioException error(HttpStatus estado,String codigo,String mensaje) {
        return new OperacionNegocioException(estado,codigo,mensaje);
    }
}
