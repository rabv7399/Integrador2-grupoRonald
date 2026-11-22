package com.deliciasperuanas.backend.service;

import com.deliciasperuanas.backend.dto.InventarioRespuesta;
import com.deliciasperuanas.backend.dto.MovimientoInventarioRespuesta;
import com.deliciasperuanas.backend.entity.MovimientoInventario;
import com.deliciasperuanas.backend.repository.MovimientoInventarioRepository;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Set;
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
    private final MovimientoInventarioRepository movimientos;

    public InventarioService(InventarioPlatoRepository inventarios,ProductoRepository productos,
                             MovimientoInventarioRepository movimientos) {
        this.inventarios=inventarios;this.productos=productos;this.movimientos=movimientos;
    }

    @Transactional(readOnly=true)
    public List<InventarioRespuesta> listar(){
        Map<Long,Integer> salidas=salidasAcumuladas();
        return inventarios.findAll().stream()
            .map(i->respuesta(i,salidas.getOrDefault(i.getProducto().getId(),0)))
            .sorted((a,b)->a.producto().compareToIgnoreCase(b.producto())).toList();
    }

    @Transactional(readOnly=true)
    public List<MovimientoInventarioRespuesta> historial(){
        return historial(null,null);
    }

    /**
     * Permite consultar días antiguos aunque haya más de 200 movimientos recientes.
     * El caso sin filtro mantiene el límite anterior; una búsqueda explícita por
     * fecha o tipo recorre el historial completo de esta demostración académica.
     */
    @Transactional(readOnly=true)
    public List<MovimientoInventarioRespuesta> historial(LocalDate fecha,String tipo){
        String filtro=tipo==null||tipo.isBlank()?"TODOS":tipo.trim().toUpperCase(Locale.ROOT);
        if(!Set.of("TODOS","SALIDA","ENTRADA","AJUSTE").contains(filtro))
            throw new OperacionNegocioException(HttpStatus.BAD_REQUEST,
                "TIPO_MOVIMIENTO_INVALIDO","Elige TODOS, SALIDA, ENTRADA o AJUSTE");
        var registro=movimientos.findAllByOrderByIdDesc().stream()
            .filter(m->fecha==null||m.getFecha().toLocalDate().equals(fecha))
            .filter(m->"TODOS".equals(filtro)||filtro.equals(m.getTipo()));
        if(fecha==null&&"TODOS".equals(filtro))
            registro=registro.limit(200);
        return registro
            .map(m->new MovimientoInventarioRespuesta(m.getId(),m.getProducto().getId(),
                m.getProducto().getNombre(),m.getTipo(),m.getCantidad(),
                m.getComentario(),m.getUsuarioCorreo(),m.getFecha())).toList();
    }

    private void auditar(Producto producto,String tipo,int cantidad,String comentario,String correo){
        MovimientoInventario m=new MovimientoInventario();
        m.setProducto(producto);m.setTipo(tipo);m.setCantidad(cantidad);
        m.setComentario(comentario);m.setUsuarioCorreo(correo);
        movimientos.save(m);
    }

    @Transactional(readOnly = true)
    public InventarioRespuesta consultar(Long productoId) {
        InventarioPlato stock = inventarios.findByProductoId(productoId).orElseThrow(() ->
            new OperacionNegocioException(HttpStatus.NOT_FOUND, "INVENTARIO_NO_CONFIGURADO",
                "El plato no tiene cantidades configuradas"));
        return respuesta(stock);
    }

    @Transactional
    public InventarioRespuesta configurar(Long productoId,int totalPreparado) {
        return configurar(productoId,totalPreparado,"sistema");
    }

    @Transactional
    public InventarioRespuesta configurar(Long productoId,int totalPreparado,String correo) {
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
        int anterior=stock.getTotalPreparado();
        stock.setTotalPreparado(totalPreparado);
        InventarioRespuesta salida=respuesta(inventarios.saveAndFlush(stock));
        if(anterior!=totalPreparado)
            auditar(producto,"AJUSTE",Math.abs(totalPreparado-anterior),
                   "Configuración administrativa: de "+anterior+" a "+totalPreparado,correo);
        return salida;
    }

    @Transactional
    public InventarioRespuesta reponer(Long productoId,int cantidad){
        return reponer(productoId,cantidad,"sistema");
    }

    @Transactional
    public InventarioRespuesta reponer(Long productoId,int cantidad,String correo) {
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
        auditar(stock.getProducto(),"ENTRADA",cantidad,"Nueva producción de Cocina",correo);
        return respuesta(stock);
    }

    @Transactional
    public InventarioRespuesta darSalida(Long productoId,int cantidad,String comentario,String correo) {
        if(cantidad<1||cantidad>10000){
            throw new OperacionNegocioException(HttpStatus.BAD_REQUEST,"CANTIDAD_INVALIDA",
                "La salida debe tener entre 1 y 10000 porciones");
        }
        if(comentario==null||comentario.trim().length()<5||comentario.length()>500){
            throw new OperacionNegocioException(HttpStatus.BAD_REQUEST,"MOTIVO_REQUERIDO",
                "Explica el motivo de la salida (5 a 500 caracteres)");
        }
        InventarioPlato stock=inventarios.bloquearProducto(productoId).orElseThrow(() ->
            new OperacionNegocioException(HttpStatus.NOT_FOUND,"INVENTARIO_NO_CONFIGURADO",
                "No se encontró el inventario del plato"));
        if(stock.disponibles()<cantidad){
            throw new OperacionNegocioException(HttpStatus.CONFLICT,"SALIDA_SUPERA_DISPONIBLE",
                "No puedes retirar porciones reservadas o vendidas; quedan "
                +stock.disponibles()+" porciones disponibles");
        }
        stock.setTotalPreparado(stock.getTotalPreparado()-cantidad);
        auditar(stock.getProducto(),"SALIDA",cantidad,comentario.trim(),correo);
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

    // Anulación previa a preparación: regresa porciones confirmadas a disponibles.
    @Transactional
    public void revertirConfirmado(Long productoId,int cantidad) {
        InventarioPlato stock=bloquearComprometido(productoId);
        if(cantidad<=0 || stock.getVendidas()<cantidad) {
            throw new OperacionNegocioException(HttpStatus.CONFLICT,"VENTA_STOCK_INCONSISTENTE",
                "No es posible revertir las porciones confirmadas de este pedido");
        }
        stock.setVendidas(stock.getVendidas()-cantidad);
    }

    private InventarioPlato bloquearComprometido(Long productoId) {
        return inventarios.bloquearProducto(productoId).orElseThrow(() ->
            new OperacionNegocioException(HttpStatus.CONFLICT, "RESERVA_STOCK_NO_ENCONTRADA",
                "No se encontro el inventario del pedido"));
    }

    private Map<Long,Integer> salidasAcumuladas(){
        Map<Long,Integer> sumas=new HashMap<>();
        for(MovimientoInventario m:movimientos.findAll()){
            if("SALIDA".equals(m.getTipo())){
                sumas.merge(m.getProducto().getId(),m.getCantidad(),Integer::sum);
            }
        }
        return sumas;
    }

    private InventarioRespuesta respuesta(InventarioPlato stock){
        return respuesta(stock,salidasAcumuladas().getOrDefault(stock.getProducto().getId(),0));
    }

    private InventarioRespuesta respuesta(InventarioPlato stock,int salida){
        return new InventarioRespuesta(stock.getProducto().getId(),stock.getProducto().getNombre(),
            stock.getTotalPreparado(),stock.getReservadas(),stock.getVendidas(),stock.disponibles(),salida);
    }
}
