package com.deliciasperuanas.backend.service;

import com.deliciasperuanas.backend.dto.*;
import com.deliciasperuanas.backend.entity.*;
import com.deliciasperuanas.backend.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

    // Regla comercial del prototipo: POS = subtotal * 1.04 y techo al siguiente S/ 0.50.
    // No se altera el precio del plato; el recargo se registra por separado.
    public static BigDecimal totalPos(BigDecimal subtotal){
        if(subtotal==null||subtotal.signum()<0)throw new IllegalArgumentException("Subtotal inválido");
        BigDecimal bruto=subtotal.multiply(new BigDecimal("1.04"));
        return bruto.multiply(new BigDecimal("2")).setScale(0,RoundingMode.CEILING)
            .divide(new BigDecimal("2"),2,RoundingMode.UNNECESSARY);
    }

    @Transactional(readOnly=true)
    public Map<String,Object> cotizar(Long pedidoId,String medioPago){
        Pedido p=pedidos.findById(pedidoId).orElseThrow(()->
            error(HttpStatus.NOT_FOUND,"PEDIDO_NO_ENCONTRADO","El pedido no existe"));
        String metodo=medioPago==null?"EFECTIVO":medioPago.trim().toUpperCase(Locale.ROOT);
        if(!List.of("EFECTIVO","YAPE","POS","TRANSFERENCIA").contains(metodo))
            throw error(HttpStatus.BAD_REQUEST,"MEDIO_PAGO_INVALIDO","Medio de pago no reconocido");
        if("PAGADO".equals(p.getEstado()))
            throw error(HttpStatus.CONFLICT,"PEDIDO_YA_PAGADO",
                "Este pedido ya está cobrado. Consulta su ticket en el historial.");
        if(!"CONFIRMADO".equals(p.getEstado()))
            throw error(HttpStatus.CONFLICT,"PEDIDO_NO_CONFIRMADO",
                "Solo es posible cotizar pedidos confirmados pendientes de cobro.");
        BigDecimal total="POS".equals(metodo)?totalPos(p.getTotal()):p.getTotal();
        Map<String,Object> detalle=new LinkedHashMap<>();
        detalle.put("pedidoId",p.getId());detalle.put("estado",p.getEstado());
        detalle.put("medioPago",metodo);detalle.put("subtotalBase",p.getTotal());
        detalle.put("recargoPos",total.subtract(p.getTotal()));detalle.put("totalCobro",total);
        detalle.put("tasaPos","0.04");detalle.put("reglaRedondeoPos","TECHO_A_0_50");
        detalle.put("aviso","POS incluye recargo 4% y redondeo hacia arriba a S/ 0.50; informar antes de cobrar.");
        return detalle;
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
        if(!List.of("EFECTIVO","YAPE","POS","TRANSFERENCIA").contains(metodo))
            throw error(HttpStatus.BAD_REQUEST,"MEDIO_PAGO_INVALIDO","Elige EFECTIVO, YAPE, POS o TRANSFERENCIA");
        String referencia=solicitud.referenciaPago()==null?"":solicitud.referenciaPago().trim();
        if(!"EFECTIVO".equals(metodo) && (referencia.length()<4||referencia.length()>80))
            throw error(HttpStatus.BAD_REQUEST,"REFERENCIA_PAGO_REQUERIDA",
                "Para YAPE, POS o TRANSFERENCIA indica el código de operación (4 a 80 caracteres)");
        if(!"EFECTIVO".equals(metodo) &&
             ventas.existsByMedioPagoAndReferenciaPago(metodo,referencia))
            throw error(HttpStatus.CONFLICT,"REFERENCIA_PAGO_REUTILIZADA",
                "Esta referencia ya figura en otro cobro del mismo medio de pago");
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
              !List.of("LISTO","ENTREGADO","ANULADO").contains(d.getEstadoPreparacion())))
            throw error(HttpStatus.CONFLICT,"PLATOS_PENDIENTES",
                "Cocina todavía tiene platos sin marcar como LISTO");

        if("POS".equals(metodo)&&!Boolean.TRUE.equals(solicitud.recargoInformadoAceptado()))
            throw error(HttpStatus.BAD_REQUEST,"RECARGO_POS_NO_ACEPTADO",
                "El recargo POS del 4% y su redondeo deben informarse y aceptarse antes del cobro");
        BigDecimal subtotalBase=pedido.getTotal();
        BigDecimal total="POS".equals(metodo)?totalPos(subtotalBase):subtotalBase;
        BigDecimal recargoPos=total.subtract(subtotalBase);
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
        venta.setSubtotalBase(subtotalBase);
        venta.setRecargoPos(recargoPos);
        venta.setRecargoInformadoAceptado("POS".equals(metodo));
        venta.setMontoRecibido(recibido);
        venta.setCambio(cambio);
        venta.setMedioPago(metodo);
        venta.setReferenciaPago("EFECTIVO".equals(metodo)?null:referencia);
        venta.setCajeroCorreo(cajero);
        venta.setNumeroComprobante("INT-"+LocalDate.now(java.time.ZoneId.of("America/Lima")).toString().replace("-","")+"-"
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
    public Map<String,Object> resumen(){return resumen(LocalDate.now(java.time.ZoneId.of("America/Lima")));}

    @Transactional(readOnly=true)
    public Map<String,Object> resumen(LocalDate fecha) {
        final LocalDate diaConsultado=fecha==null?LocalDate.now(java.time.ZoneId.of("America/Lima")):fecha;
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
        datos.put("subtotalPlatosDia",sumarBase(dia));
        datos.put("recargosPosDia",sumarRecargos(dia));
        datos.put("subtotalPlatosMes",sumarBase(mesVentas));
        datos.put("recargosPosMes",sumarRecargos(mesVentas));
        datos.put("mediosCuadrados",sumar(dia).compareTo(
            totalMetodoDia.values().stream().reduce(BigDecimal.ZERO,BigDecimal::add))==0);
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

    /**
     * Ventas del día: sólo cobros efectivamente registrados, NO pedidos confirmados sin pago.
     * Agrupa por producto y precio guardado al crear el pedido, para no reescribir ventas
     * anteriores cuando el administrador cambie el precio de la carta.
     */
    @Transactional(readOnly=true)
    public Map<String,Object> detalleDiario(LocalDate fecha){
        LocalDate dia=fecha==null?LocalDate.now(java.time.ZoneId.of("America/Lima")):fecha;
        List<Venta> pagadas=ventas.findAll().stream()
            .filter(v->v.getFechaPago().toLocalDate().equals(dia)).toList();
        Map<String,Map<String,Object>> agrupado=new LinkedHashMap<>();
        int unidades=0;
        for(Venta venta:pagadas){
            for(DetallePedido detalle:venta.getPedido().getDetalles()){
                if("ANULADO".equals(detalle.getEstadoPreparacion()))continue;
                Long id=detalle.getProducto().getId();
                BigDecimal precio=detalle.getPrecioUnitario();
                String clave=id+"|"+precio.stripTrailingZeros().toPlainString();
                Map<String,Object> linea=agrupado.computeIfAbsent(clave,k->{
                    Map<String,Object> v=new LinkedHashMap<>();
                    v.put("productoId",id);
                    v.put("producto",detalle.getProducto().getNombre());
                    v.put("precioUnitario",precio);
                    v.put("cantidad",0);
                    v.put("subtotal",BigDecimal.ZERO);
                    return v;
                });
                linea.put("cantidad",(Integer)linea.get("cantidad")+detalle.getCantidad());
                linea.put("subtotal",((BigDecimal)linea.get("subtotal")).add(detalle.getSubtotal()));
                unidades+=detalle.getCantidad();
            }
        }
        List<Map<String,Object>> lineas=new ArrayList<>(agrupado.values());
        lineas.sort(Comparator.comparing(v->((String)v.get("producto")).toLowerCase(Locale.ROOT)));
        BigDecimal subtotal=sumarBase(pagadas);
        BigDecimal recargo=sumarRecargos(pagadas);
        BigDecimal ingresos=sumar(pagadas);
        BigDecimal sumaDetalle=lineas.stream().map(x->(BigDecimal)x.get("subtotal"))
            .reduce(BigDecimal.ZERO,BigDecimal::add);
        Map<String,Object> respuesta=new LinkedHashMap<>();
        respuesta.put("fecha",dia.toString());
        respuesta.put("ventasCobradas",pagadas.size());
        respuesta.put("productosDiferentes",lineas.size());
        respuesta.put("unidadesVendidas",unidades);
        respuesta.put("lineas",lineas);
        respuesta.put("subtotalPlatos",subtotal);
        respuesta.put("recargosPos",recargo);
        respuesta.put("totalCobrado",ingresos);
        respuesta.put("porMedioPago",totalesMetodo(pagadas));
        respuesta.put("cuadreDetalle",sumaDetalle.compareTo(subtotal)==0
            && subtotal.add(recargo).compareTo(ingresos)==0);
        respuesta.put("nota","Se incluyen únicamente pedidos PAGADOS. El subtotal usa precios guardados al crear cada pedido; el recargo POS se muestra aparte.");
        return respuesta;
    }

    @Transactional
    public CierreCajaRespuesta registrarCuadre(CierreCajaSolicitud req,String correo) {
        if(req==null||req.fecha()==null||req.efectivoContado()==null)
            throw error(HttpStatus.BAD_REQUEST,"CUADRE_INVALIDO","Indica fecha y efectivo contado");
        if(req.fecha().isAfter(LocalDate.now(java.time.ZoneId.of("America/Lima"))))
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
        for(String medio:List.of("EFECTIVO","YAPE","POS","TRANSFERENCIA"))res.put(medio,BigDecimal.ZERO);
        for(Venta v:data)res.merge(v.getMedioPago(),v.getTotal(),BigDecimal::add);
        return res;
    }

    private BigDecimal sumarBase(List<Venta> lista){
        return lista.stream().map(Venta::getSubtotalBase).reduce(BigDecimal.ZERO,BigDecimal::add);
    }
    private BigDecimal sumarRecargos(List<Venta> lista){
        return lista.stream().map(Venta::getRecargoPos).reduce(BigDecimal.ZERO,BigDecimal::add);
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
        r.put("subtotalBase",v.getSubtotalBase());r.put("recargoPos",v.getRecargoPos());
        r.put("referenciaPago",v.getReferenciaPago());
        r.put("cajeroCorreo",v.getCajeroCorreo());
        return r;
    }

    private VentaRespuesta respuesta(Venta v){
        Pedido p=v.getPedido();
        ConfiguracionLocal local=configuracion.findById(1L).orElseGet(ConfiguracionLocal::new);
        List<PedidoRespuesta.Item> items=p.getDetalles().stream()
            .filter(d->!"ANULADO".equals(d.getEstadoPreparacion()))
            .map(d->new PedidoRespuesta.Item(d.getId(),d.getProducto().getId(),
                d.getProducto().getNombre(),d.getCantidad(),d.getPrecioUnitario(),
                d.getSubtotal(),d.getEstadoPreparacion(),d.getFechaInicio(),d.getFechaListo(),
                d.getFechaEntrega(),d.getObservacion(),d.getMotivoAnulacion(),
                d.getAnuladoPor(),d.getFechaAnulacion())).toList();
        return new VentaRespuesta(v.getId(),v.getNumeroComprobante(),p.getId(),
            p.getTipoAtencion(),p.getMesaId(),local.getNombre(),local.getTelefono(),
            v.getTotal(),v.getMontoRecibido(),v.getCambio(),v.getFechaPago(),
            v.getMedioPago(),items,
            "COMPROBANTE INTERNO ACADÉMICO — NO ES BOLETA NI FACTURA TRIBUTARIA",
            v.getReferenciaPago(),v.getCajeroCorreo(),v.getSubtotalBase(),
            v.getRecargoPos(),v.isRecargoInformadoAceptado());
    }

    private OperacionNegocioException error(HttpStatus s,String c,String m){
        return new OperacionNegocioException(s,c,m);
    }
}
