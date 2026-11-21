package com.deliciasperuanas.backend.service;

import com.deliciasperuanas.backend.dto.*;
import com.deliciasperuanas.backend.entity.*;
import com.deliciasperuanas.backend.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

@Service
public class VentaService {
    private final VentaRepository ventas;
    private final PedidoRepository pedidos;
    private final MesaRepository mesas;
    private final ConfiguracionLocalRepository configuracion;
    private final CierreCajaRepository cierres;

    public VentaService(VentaRepository ventas,PedidoRepository pedidos,MesaRepository mesas,
                        ConfiguracionLocalRepository configuracion,CierreCajaRepository cierres){
        this.ventas=ventas;this.pedidos=pedidos;this.mesas=mesas;
        this.configuracion=configuracion;this.cierres=cierres;
    }

    @Transactional
    public VentaRespuesta cobrar(Long pedidoId,BigDecimal recibido){
        return cobrar(pedidoId,new CobroSolicitud(recibido),"sistema");
    }

    @Transactional
    public VentaRespuesta cobrar(Long pedidoId,CobroSolicitud solicitud,String cajero){
        if(solicitud==null)
            throw error(HttpStatus.BAD_REQUEST,"PAGO_INVALIDO","Indica el medio de pago");
        String metodo=solicitud.medioPago()==null?"EFECTIVO":
            solicitud.medioPago().trim().toUpperCase(Locale.ROOT);
        if(!List.of("EFECTIVO","YAPE","POS").contains(metodo))
            throw error(HttpStatus.BAD_REQUEST,"MEDIO_PAGO_INVALIDO","Elige EFECTIVO, YAPE o POS");
        String referencia=solicitud.referenciaPago()==null?"":solicitud.referenciaPago().trim();
        if(!"EFECTIVO".equals(metodo) && (referencia.length()<4||referencia.length()>80))
            throw error(HttpStatus.BAD_REQUEST,"REFERENCIA_PAGO_REQUERIDA",
                "Para YAPE/POS indica el código de operación o voucher (4 a 80 caracteres)");
        if("EFECTIVO".equals(metodo) && !referencia.isEmpty())
            throw error(HttpStatus.BAD_REQUEST,"REFERENCIA_NO_APLICA",
                "El efectivo no utiliza referencia de Yape o POS");

        Pedido pedido=pedidos.bloquearPedido(pedidoId).orElseThrow(() ->
            error(HttpStatus.NOT_FOUND,"PEDIDO_NO_ENCONTRADO","El pedido no existe"));
        if(ventas.existsByPedidoId(pedidoId)||"PAGADO".equals(pedido.getEstado()))
            throw error(HttpStatus.CONFLICT,"PEDIDO_YA_PAGADO",
                "Esta orden ya tiene un cobro registrado");
        if(!"CONFIRMADO".equals(pedido.getEstado()))
            throw error(HttpStatus.CONFLICT,"PEDIDO_NO_CONFIRMADO",
                "Confirma el pedido antes de cobrar");
        if(pedido.getDetalles().stream().anyMatch(d->
              !List.of("LISTO","ENTREGADO").contains(d.getEstadoPreparacion())))
            throw error(HttpStatus.CONFLICT,"PLATOS_PENDIENTES",
                "Cocina todavía tiene platos sin marcar como LISTO");

        BigDecimal total=pedido.getTotal();
        BigDecimal recibido;
        BigDecimal cambio;
        if("EFECTIVO".equals(metodo)){
            recibido=solicitud.montoRecibido();
            if(recibido==null||recibido.signum()<=0||recibido.scale()>2)
                throw error(HttpStatus.BAD_REQUEST,"MONTO_INVALIDO",
                    "El efectivo recibido debe ser positivo, con máximo dos decimales");
            if(recibido.compareTo(total)<0)
                throw error(HttpStatus.UNPROCESSABLE_CONTENT,"PAGO_INSUFICIENTE",
                    "El importe recibido no cubre el total del pedido");
            cambio=recibido.subtract(total);
        }else{
            // YAPE/POS: ingreso digital exacto, sin cambio ni efectivo en gaveta.
            recibido=total;cambio=BigDecimal.ZERO;
        }

        Venta venta=new Venta();
        venta.setPedido(pedido);
        venta.setTotal(total);
        venta.setMontoRecibido(recibido);
        venta.setCambio(cambio);
        venta.setMedioPago(metodo);
        venta.setReferenciaPago("EFECTIVO".equals(metodo)?null:referencia);
        venta.setCajeroCorreo(cajero);
        venta.setNumeroComprobante("INT-"+LocalDate.now().toString().replace("-","")+"-"
            +UUID.randomUUID().toString().substring(0,10).toUpperCase(Locale.ROOT));
        ventas.saveAndFlush(venta);
        pedido.setEstado("PAGADO");
        if("MESA".equals(pedido.getTipoAtencion())&&pedido.getMesaId()!=null)
            mesas.bloquearMesa(pedido.getMesaId()).ifPresent(m->m.setEstado("DISPONIBLE"));
        return respuesta(venta);
    }

    @Transactional(readOnly=true)
    public VentaRespuesta consultarPorPedido(Long pedidoId){
        return respuesta(ventas.findByPedidoId(pedidoId).orElseThrow(() ->
            error(HttpStatus.NOT_FOUND,"COBRO_NO_ENCONTRADO",
                "No hay un pago para este pedido")));
    }

    @Transactional(readOnly=true)
    public Map<String,Object> resumen(){return resumen(LocalDate.now());}

    @Transactional(readOnly=true)
    public Map<String,Object> resumen(LocalDate fecha) {
        final LocalDate diaConsultado=fecha==null?LocalDate.now():fecha;
        YearMonth mes=YearMonth.from(diaConsultado);
        List<Venta> registros=ventas.findAll();
        List<Venta> dia=registros.stream()
            .filter(v->v.getFechaPago().toLocalDate().equals(diaConsultado)).toList();
        List<Venta> mesVentas=registros.stream()
            .filter(v->YearMonth.from(v.getFechaPago()).equals(mes)).toList();
        Map<String,BigDecimal> totalMetodoDia=totalesMetodo(dia);
        Map<String,BigDecimal> totalMetodoMes=totalesMetodo(mesVentas);
        Map<String,BigDecimal> porDia=new TreeMap<>();
        Map<String,BigDecimal> porMes=new TreeMap<>();
        for(Venta v:registros){
            porDia.merge(v.getFechaPago().toLocalDate().toString(),v.getTotal(),BigDecimal::add);
            porMes.merge(YearMonth.from(v.getFechaPago()).toString(),v.getTotal(),BigDecimal::add);
        }
        List<Map<String,Object>> operaciones=dia.stream()
            .sorted(Comparator.comparing(Venta::getFechaPago).reversed())
            .map(this::registro).toList();
        Map<String,Object> datos=new LinkedHashMap<>();
        datos.put("fecha",diaConsultado.toString());datos.put("mes",mes.toString());
        datos.put("ventasRegistradas",registros.size());
        datos.put("ventasDia",dia.size());datos.put("ventasMes",mesVentas.size());
        datos.put("totalVentas",sumar(registros));
        datos.put("ventasHoy",sumar(dia)); // compatibilidad de dashboard anterior
        datos.put("ventasFecha",sumar(dia));
        datos.put("ventasMensuales",sumar(mesVentas));
        datos.put("porMedioDia",totalMetodoDia);
        datos.put("porMedioMes",totalMetodoMes);
        datos.put("efectivoEsperado",totalMetodoDia.get("EFECTIVO"));
        datos.put("ventasPorDia",porDia);datos.put("ventasPorMes",porMes);
        datos.put("operacionesDia",operaciones);
        datos.put("cuadreRegistrado",cierres.findByFecha(diaConsultado).map(this::dtoCierre).orElse(null));
        datos.put("moneda","PEN");
        datos.put("fuente","Ventas efectivamente cobradas en la base de datos activa");
        return datos;
    }

    @Transactional
    public CierreCajaRespuesta registrarCuadre(CierreCajaSolicitud req,String correo) {
        if(req==null||req.fecha()==null||req.efectivoContado()==null)
            throw error(HttpStatus.BAD_REQUEST,"CUADRE_INVALIDO","Indica fecha y efectivo contado");
        if(req.fecha().isAfter(LocalDate.now()))
            throw error(HttpStatus.BAD_REQUEST,"FECHA_FUTURA","No puedes cuadrar una fecha futura");
        if(req.efectivoContado().signum()<0||req.efectivoContado().scale()>2)
            throw error(HttpStatus.BAD_REQUEST,"EFECTIVO_INVALIDO",
                "El efectivo contado debe ser no negativo y tener dos decimales como máximo");
        if(cierres.findByFecha(req.fecha()).isPresent())
            throw error(HttpStatus.CONFLICT,"CUADRE_EXISTENTE",
                "El cuadre ya fue registrado. No se sobrescribe el historial");
        // Solo refleja lo cobrado en efectivo; Yape y POS no se cuentan en caja física.
        List<Venta> delDia=ventas.findAll().stream()
            .filter(v->v.getFechaPago().toLocalDate().equals(req.fecha())).toList();
        BigDecimal esperado=totalesMetodo(delDia).get("EFECTIVO");
        CierreCaja cierre=new CierreCaja();
        cierre.setFecha(req.fecha());cierre.setEfectivoEsperado(esperado);
        cierre.setEfectivoContado(req.efectivoContado());
        cierre.setDiferencia(req.efectivoContado().subtract(esperado));
        cierre.setRegistradoPor(correo);
        return dtoCierre(cierres.saveAndFlush(cierre));
    }

    @Transactional(readOnly=true)
    public List<CierreCajaRespuesta> listarCuadres(){
        return cierres.findAllByOrderByFechaDesc().stream().limit(100)
            .map(this::dtoCierre).toList();
    }

    private Map<String,BigDecimal> totalesMetodo(List<Venta> data){
        Map<String,BigDecimal> res=new LinkedHashMap<>();
        for(String medio:List.of("EFECTIVO","YAPE","POS"))res.put(medio,BigDecimal.ZERO);
        for(Venta v:data)res.merge(v.getMedioPago(),v.getTotal(),BigDecimal::add);
        return res;
    }

    private BigDecimal sumar(List<Venta> lista){
        return lista.stream().map(Venta::getTotal).reduce(BigDecimal.ZERO,BigDecimal::add);
    }

    private CierreCajaRespuesta dtoCierre(CierreCaja c){
        return new CierreCajaRespuesta(c.getId(),c.getFecha(),c.getEfectivoEsperado(),
            c.getEfectivoContado(),c.getDiferencia(),c.getRegistradoPor(),c.getFechaRegistro());
    }

    private Map<String,Object> registro(Venta v){
        Map<String,Object> r=new LinkedHashMap<>();
        r.put("ventaId",v.getId());r.put("pedidoId",v.getPedido().getId());
        r.put("comprobante",v.getNumeroComprobante());
        r.put("fechaPago",v.getFechaPago());
        r.put("tipoAtencion",v.getPedido().getTipoAtencion());
        r.put("mesaId",v.getPedido().getMesaId());
        r.put("medioPago",v.getMedioPago());r.put("total",v.getTotal());
        r.put("referenciaPago",v.getReferenciaPago());
        r.put("cajeroCorreo",v.getCajeroCorreo());
        return r;
    }

    private VentaRespuesta respuesta(Venta v){
        Pedido p=v.getPedido();
        ConfiguracionLocal local=configuracion.findById(1L).orElseGet(ConfiguracionLocal::new);
        List<PedidoRespuesta.Item> items=p.getDetalles().stream()
            .map(d->new PedidoRespuesta.Item(d.getId(),d.getProducto().getId(),
                d.getProducto().getNombre(),d.getCantidad(),d.getPrecioUnitario(),
                d.getSubtotal(),d.getEstadoPreparacion(),d.getFechaInicio(),d.getFechaListo(),
                d.getFechaEntrega())).toList();
        return new VentaRespuesta(v.getId(),v.getNumeroComprobante(),p.getId(),
            p.getTipoAtencion(),p.getMesaId(),local.getNombre(),local.getTelefono(),
            v.getTotal(),v.getMontoRecibido(),v.getCambio(),v.getFechaPago(),
            v.getMedioPago(),items,
            "COMPROBANTE INTERNO ACADÉMICO — NO ES BOLETA NI FACTURA TRIBUTARIA",
            v.getReferenciaPago(),v.getCajeroCorreo());
    }

    private OperacionNegocioException error(HttpStatus s,String c,String m){
        return new OperacionNegocioException(s,c,m);
    }
}
