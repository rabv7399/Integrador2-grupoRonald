package com.deliciasperuanas.backend.service;

import com.deliciasperuanas.backend.dto.*;
import com.deliciasperuanas.backend.entity.*;
import com.deliciasperuanas.backend.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class VentaService {
    private final VentaRepository ventas;
    private final PedidoRepository pedidos;
    private final MesaRepository mesas;
    private final ConfiguracionLocalRepository configuracion;

    public VentaService(VentaRepository ventas, PedidoRepository pedidos,
                        MesaRepository mesas, ConfiguracionLocalRepository configuracion) {
        this.ventas=ventas; this.pedidos=pedidos;
        this.mesas=mesas; this.configuracion=configuracion;
    }

    @Transactional
    public VentaRespuesta cobrar(Long pedidoId,BigDecimal recibido) {
        if (recibido==null || recibido.signum()<=0 || recibido.scale()>2) {
            throw error(HttpStatus.BAD_REQUEST,"MONTO_INVALIDO",
                "El monto debe ser positivo y tener máximo dos decimales");
        }
        Pedido pedido=pedidos.bloquearPedido(pedidoId).orElseThrow(() ->
            error(HttpStatus.NOT_FOUND,"PEDIDO_NO_ENCONTRADO","El pedido no existe"));
        if (ventas.existsByPedidoId(pedidoId) || "PAGADO".equals(pedido.getEstado())) {
            throw error(HttpStatus.CONFLICT,"PEDIDO_YA_PAGADO",
                "Esta orden ya tiene un cobro registrado");
        }
        if (!"CONFIRMADO".equals(pedido.getEstado())) {
            throw error(HttpStatus.CONFLICT,"PEDIDO_NO_CONFIRMADO",
                "Confirma el pedido antes de cobrar");
        }
        if (pedido.getDetalles().stream().anyMatch(d->!"LISTO".equals(d.getEstadoPreparacion()))) {
            throw error(HttpStatus.CONFLICT,"PLATOS_PENDIENTES",
                "Cocina todavía tiene platos sin marcar como LISTO");
        }
        if (recibido.compareTo(pedido.getTotal())<0) {
            throw error(HttpStatus.UNPROCESSABLE_CONTENT,"PAGO_INSUFICIENTE",
                "El importe recibido no cubre el total del pedido");
        }

        Venta venta=new Venta();
        venta.setPedido(pedido);
        venta.setTotal(pedido.getTotal());
        venta.setMontoRecibido(recibido);
        venta.setCambio(recibido.subtract(pedido.getTotal()));
        venta.setMedioPago("EFECTIVO");
        venta.setNumeroComprobante("INT-"+LocalDate.now().toString().replace("-","")+"-"+UUID.randomUUID().toString().substring(0,10).toUpperCase(Locale.ROOT));
        ventas.saveAndFlush(venta);
        pedido.setEstado("PAGADO");
        if ("MESA".equals(pedido.getTipoAtencion()) && pedido.getMesaId()!=null) {
            mesas.bloquearMesa(pedido.getMesaId()).ifPresent(m->m.setEstado("DISPONIBLE"));
        }
        return respuesta(venta);
    }

    @Transactional(readOnly=true)
    public VentaRespuesta consultarPorPedido(Long pedidoId) {
        return respuesta(ventas.findByPedidoId(pedidoId).orElseThrow(() ->
            error(HttpStatus.NOT_FOUND,"COBRO_NO_ENCONTRADO",
                "No hay un pago para este pedido")));
    }

    @Transactional(readOnly=true)
    public Map<String,Object> resumen() {
        List<Venta> registros=ventas.findAll();
        BigDecimal total=registros.stream().map(Venta::getTotal)
            .reduce(BigDecimal.ZERO,BigDecimal::add);
        LocalDate hoy=LocalDate.now();
        BigDecimal hoyTotal=registros.stream()
            .filter(v->v.getFechaPago().toLocalDate().equals(hoy))
            .map(Venta::getTotal).reduce(BigDecimal.ZERO,BigDecimal::add);
        Map<String,BigDecimal> porDia=new TreeMap<>();
        for(Venta v:registros)porDia.merge(v.getFechaPago().toLocalDate().toString(),
            v.getTotal(),BigDecimal::add);
        Map<String,Object> datos=new LinkedHashMap<>();
        datos.put("ventasRegistradas",registros.size());
        datos.put("totalVentas",total);
        datos.put("ventasHoy",hoyTotal);
        datos.put("ventasPorDia",porDia);
        datos.put("moneda","PEN");
        datos.put("fuente","SOA DEMO / datos reales de la base activa");
        return datos;
    }

    private VentaRespuesta respuesta(Venta venta) {
        Pedido pedido=venta.getPedido();
        ConfiguracionLocal local=configuracion.findById(1L).orElseGet(ConfiguracionLocal::new);
        List<PedidoRespuesta.Item> items=pedido.getDetalles().stream()
            .map(d->new PedidoRespuesta.Item(d.getId(),d.getProducto().getId(),
                d.getProducto().getNombre(),d.getCantidad(),d.getPrecioUnitario(),
                d.getSubtotal(),d.getEstadoPreparacion())).toList();
        return new VentaRespuesta(venta.getId(),venta.getNumeroComprobante(),
            pedido.getId(),pedido.getTipoAtencion(),pedido.getMesaId(),
            local.getNombre(),local.getTelefono(),venta.getTotal(),
            venta.getMontoRecibido(),venta.getCambio(),venta.getFechaPago(),
            venta.getMedioPago(),items,
            "COMPROBANTE INTERNO ACADÉMICO — NO ES BOLETA NI FACTURA TRIBUTARIA");
    }

    private OperacionNegocioException error(HttpStatus s,String c,String m) {
        return new OperacionNegocioException(s,c,m);
    }
}
