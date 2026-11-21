'use strict';
/* Vista operativa APF3: sincronización REST cada 5 segundos entre navegadores.
 * No se guardan JWT en storage, ni se simula stock o datos financieros.
 * El backend sigue siendo responsable de permisos, validación y transacciones. */
(() => {
  const e = id => document.getElementById(id);
  const n = x => Number(x) || 0;
  const moneda = x => formatPrice(n(x));
  let sincronizacion = null;
  let reloj = null;
  let ocupado = false;
  let ultimaOrdenElegida = null;

  function moduloActual() {
    const nodo=document.querySelector('.view-module:not([hidden])');
    return nodo?nodo.id.substring('module-'.length):'inicio';
  }
  function elemento(tag,texto,clase) {
    const item=document.createElement(tag);
    if(texto!==undefined)item.textContent=String(texto);
    if(clase)item.className=clase;
    return item;
  }
  function celda(texto,clase) {
    return elemento('td',texto,clase);
  }
  function fila(celdas) {
    const tr=document.createElement('tr');
    for(const dato of celdas)tr.append(celda(dato));
    return tr;
  }
  function vacio(tbody,columnas,mensaje) {
    tbody.replaceChildren();
    const tr=document.createElement('tr'),td=celda(mensaje,'ops-empty');
    td.colSpan=columnas;tr.append(td);tbody.append(tr);
  }
  function boton(texto,accion,clase) {
    const b=elemento('button',texto,clase||'outline');
    b.type='button';b.addEventListener('click',accion);return b;
  }
  function estado(stock,plato) {
    if(!plato?.disponible || n(plato.precio)<=0)return 'No disponible';
    return n(stock?.disponibles)>0?'DISPONIBLE':'AGOTADO';
  }
  function badgeEstado(etiqueta) {
    return elemento('span',etiqueta,'ops-badge '+(
      /DISPONIBLE|LISTO|ENTREGADO|PAGADO|CONFIRMADO/.test(etiqueta)?'good':
      /AGOTADO|CANCELADO|NO DISPONIBLE/.test(etiqueta)?'bad':'waiting'));
  }
  function fechaLocal(){
    const ahora=new Date();
    return new Date(ahora.getTime()-ahora.getTimezoneOffset()*60000).toISOString().slice(0,10);
  }
  function fechaCorta(valor) {
    if(!valor)return '—';
    return String(valor).replace('T',' ').slice(0,16);
  }
  function minutosDesde(inicio,hasta) {
    if(!inicio)return '—';
    // Spring Boot devuelve LocalDateTime sin zona. Navegador local coincide con el servidor demo.
    const a=new Date(inicio),b=hasta?new Date(hasta):new Date();
    if(!Number.isFinite(a.getTime())||!Number.isFinite(b.getTime()))return '—';
    const s=Math.max(0,Math.floor((b-a)/1000));
    return s<60?s+' s':Math.floor(s/60)+' min '+String(s%60).padStart(2,'0')+' s';
  }
  function relojActivos(){
    if(!token||e('appView').hidden)return;
    document.querySelectorAll('[data-inicio-plato]').forEach(node=>{
      const hasta=node.dataset.finPlato||null;
      node.textContent='Tiempo: '+minutosDesde(node.dataset.inicioPlato,hasta);
    });
  }
  function referenciaNueva(){
    return 'adicion-'+(crypto.randomUUID?crypto.randomUUID():
      Date.now().toString(36)+'-'+Math.random().toString(36).slice(2));
  }
  function datosCarta(){
    return listaProductos.filter(x=>x&&x.id);
  }
  async function obtenerStock(){
    const r=await api('/api/v1/inventario');
    return r.ok&&Array.isArray(r.data)?r.data:null;
  }

  function tablaCarta(productos,stock,permitido){
    const tabla=e('menuStockTable');
    if(!tabla)return;
    tabla.replaceChildren();
    if(!productos.length){vacio(tabla,4,'No hay productos registrados.');return;}
    const porId=new Map((stock||[]).map(x=>[Number(x.productoId),x]));
    for(const p of [...productos].sort((a,b)=>a.nombre.localeCompare(b.nombre,'es'))){
      const registro=porId.get(Number(p.id));
      const row=fila([
        p.nombre,
        n(p.precio)>0?moneda(p.precio):'Por confirmar',
        permitido?(registro?String(registro.disponibles):'Sin stock configurado'):'Acceso interno',
        permitido&&registro?estado(registro,p):(p.disponible?'En carta':'No disponible')
      ]);
      tabla.append(row);
    }
    e('menuRefreshInfo').textContent='Actualizado: '+new Date().toLocaleTimeString('es-PE');
  }
  async function actualizarCarta(){
    const producto=await api('/api/productos');
    if(!producto.ok||!Array.isArray(producto.data))return;
    const personal=rol!=='CLIENTE';
    const stock=personal?await obtenerStock():null;
    if(!token || moduloActual()!=='menu')return;
    tablaCarta(producto.data,stock,personal);
  }

  function tablaInventario(stock){
    const cuerpo=e('inventoryTable');
    if(!cuerpo)return;
    cuerpo.replaceChildren();
    const porId=new Map(datosCarta().map(x=>[Number(x.id),x]));
    if(!stock.length){vacio(cuerpo,6,'No hay inventario configurado.');return;}
    for(const item of stock){
      const plato=porId.get(Number(item.productoId));
      cuerpo.append(fila([
        item.producto,String(item.totalPreparado),String(item.reservadas),
        String(item.vendidas),String(item.disponibles),estado(item,plato||{disponible:true,precio:1})
      ]));
    }
    // Si había un producto seleccionado, actualizar sus números con el mismo resultado.
    const actual=stock.find(x=>String(x.productoId)===e('stockProduct').value);
    if(actual && typeof pintarInventario==='function')pintarInventario(actual);
  }
  function tablaMovimientos(items){
    const cuerpo=e('inventoryMovementsTable');
    if(!cuerpo)return;
    cuerpo.replaceChildren();
    if(!items.length){vacio(cuerpo,6,'Todavía no se registraron entradas ni salidas manuales.');return;}
    for(const m of items.slice(0,25)){
      cuerpo.append(fila([
        fechaCorta(m.fecha),m.producto,m.tipo,String(m.cantidad),
        m.comentario,m.usuarioCorreo
      ]));
    }
  }
  async function actualizarInventario(){
    const stock=await obtenerStock();
    if(!stock||!token||moduloActual()!=='stock')return;
    tablaInventario(stock);
    if(['ADMIN','CAJA','SUPERVISOR'].includes(rol)){
      const r=await api('/api/v1/inventario/movimientos');
      if(r.ok&&Array.isArray(r.data))tablaMovimientos(r.data);
    }
  }
  e('stockOutSubmit').addEventListener('click',async()=>{
    if(rol!=='ADMIN')return;
    const id=Number(e('stockProduct').value);
    const cantidad=Number(e('stockOutQuantity').value);
    const comentario=e('stockOutReason').value.trim();
    if(!Number.isInteger(cantidad)||cantidad<1||comentario.length<5)
      return mostrarLocal('La salida requiere cantidad positiva y comentario de 5 caracteres como mínimo.');
    if(!confirm('¿Registrar salida de '+cantidad+' porciones? El movimiento quedará auditado.'))return;
    const url='/api/v1/inventario/'+id+'/salida';
    const r=await api(url,'PATCH',{cantidad,comentario});
    mostrar('PATCH '+url,r);
    if(r.ok){
      e('stockOutReason').value='';
      await actualizarInventario();
    }
  });

  function tarjetaPedido(p){
    const card=elemento('article',undefined,'ops-order-card');
    const cabeza=elemento('div',undefined,'ops-order-head');
    const titulo=elemento('strong','Pedido #'+p.pedidoId+' · '+(p.tipoAtencion==='MESA'?'Mesa '+p.mesaId:'PARA LLEVAR'));
    cabeza.append(titulo,badgeEstado(p.estado));card.append(cabeza);
    const sub=elemento('p',fechaCorta(p.fechaCreacion)+' · TOTAL: '+moneda(p.total),'ops-order-sub');
    card.append(sub);
    const lista=elemento('div',undefined,'ops-order-lines');
    for(const item of p.items||[]){
      const linea=elemento('div',undefined,'ops-order-line');
      const texto=elemento('span',item.cantidad+' × '+item.nombre+' · '+moneda(item.subtotal));
      const estadoLinea=badgeEstado(item.estadoPreparacion);
      const tiempo=elemento('small','Tiempo: —','ops-age');
      tiempo.dataset.inicioPlato=item.fechaInicio||p.fechaCreacion;
      if(item.fechaListo)tiempo.dataset.finPlato=item.fechaListo;
      linea.append(texto,estadoLinea,tiempo);
      if(['MOZO','SUPERVISOR','ADMIN','OPERADOR'].includes(rol)
          && p.estado==='CONFIRMADO' && item.estadoPreparacion==='LISTO'){
        linea.append(boton('Marcar ENTREGADO',async()=>{
          const url='/api/v1/pedidos/'+p.pedidoId+'/detalles/'+item.detalleId+'/entregado';
          const r=await api(url,'POST');
          mostrar('POST '+url,r);
          if(r.ok)refrescarOperaciones();
        },'primary'));
      }
      lista.append(linea);
    }
    card.append(lista);
    const acciones=elemento('div',undefined,'buttons ops-order-actions');
    acciones.append(boton('Seleccionar',()=>{
      e('orderId').value=p.pedidoId;
      ultimaOrdenElegida=p.pedidoId;
      e('ordersSyncStatus').textContent='Seleccionado: pedido #'+p.pedidoId;
      e('orderLines').scrollIntoView({behavior:'smooth',block:'nearest'});
    }));
    if(p.estado==='PENDIENTE'){
      acciones.append(boton('Confirmar',async()=>{
        const url='/api/v1/pedidos/'+p.pedidoId+'/confirmacion';
        const r=await api(url,'POST');
        mostrar('POST '+url,r);if(r.ok)refrescarOperaciones();
      },'primary'));
      acciones.append(boton('Cancelar',async()=>{
        if(!confirm('¿Cancelar este pedido PENDIENTE y liberar sus porciones?'))return;
        const url='/api/v1/pedidos/'+p.pedidoId+'/cancelacion';
        const r=await api(url,'POST');
        mostrar('POST '+url,r);if(r.ok)refrescarOperaciones();
      },'danger'));
    }
    card.append(acciones);
    return card;
  }
  function pintarPedidos(pedidos){
    const lista=e('activeOrders');
    if(!lista)return;
    lista.replaceChildren();
    const activos=pedidos.filter(p=>p.estado==='PENDIENTE'||p.estado==='CONFIRMADO');
    e('ordersSyncStatus').textContent='Actualizado a las '+new Date().toLocaleTimeString('es-PE');
    if(!activos.length){
      lista.append(elemento('p','No hay pedidos activos. Las mesas disponibles pueden recibir órdenes.','ops-empty'));
      return;
    }
    for(const p of activos)lista.append(tarjetaPedido(p));
    relojActivos();
  }
  async function actualizarPedidos(){
    const r=await api('/api/v1/pedidos');
    if(!r.ok||!Array.isArray(r.data)||moduloActual()!=='orders'||!token)return;
    pintarPedidos(r.data);
    if(['ADMIN','CAJA','MOZO','SUPERVISOR','OPERADOR'].includes(rol))
      await Promise.all([cargarMesas(),consultarDisponibilidad()]);
  }
  e('appendOrderItems').addEventListener('click',async()=>{
    const id=Number(e('orderId').value);
    if(!Number.isInteger(id)||id<=0)return mostrarLocal('Selecciona primero un pedido activo.');
    const producto=Number(e('orderProduct').value),cantidad=Number(e('orderQty').value);
    const items=carritoPlatos.length?carritoPlatos.map(x=>({...x})):[{productoId:producto,cantidad}];
    if(items.some(x=>!Number.isInteger(x.productoId)||!Number.isInteger(x.cantidad)||x.cantidad<1))
      return mostrarLocal('Selecciona platos y cantidades válidas.');
    const b=e('appendOrderItems');b.disabled=true;
    try{
      const url='/api/v1/pedidos/'+id+'/items';
      const r=await api(url,'POST',{items},{'Idempotency-Key':referenciaNueva()});
      mostrar('POST '+url,r);
      if(r.ok){
        carritoPlatos=[];pintarCarrito();
        e('ordersSyncStatus').textContent='Se agregaron platos al pedido #'+id;
        await refrescarOperaciones();
      }
    }finally{b.disabled=false;}
  });

  function dibujarCocina(pedidos){
    const vista=e('kitchenOrders');vista.replaceChildren();
    const activos=pedidos.filter(p=>p.estado==='PENDIENTE'||p.estado==='CONFIRMADO');
    if(!activos.length){vista.append(elemento('p','No hay comandas pendientes. Los pedidos PAGADOS están en el historial de Caja.'));return;}
    for(const p of activos){
      const card=elemento('article',undefined,'command-card');
      const cab=elemento('div',undefined,'command-heading');
      cab.append(elemento('strong','Pedido #'+p.pedidoId+' · '+(p.tipoAtencion==='MESA'?'Mesa '+p.mesaId:'Llevar')),
                 badgeEstado(p.estado));
      card.append(cab);
      const ul=elemento('ul',undefined,'command-items');
      for(const item of p.items||[]){
        const li=elemento('li',undefined,'ops-cocina-line');
        const descripcion=elemento('span',item.cantidad+' × '+item.nombre+' · '+item.estadoPreparacion);
        const edad=elemento('small','Tiempo: —','ops-age');
        edad.dataset.inicioPlato=item.fechaInicio||p.fechaCreacion;
        if(item.fechaListo)edad.dataset.finPlato=item.fechaListo;
        li.append(descripcion,edad);
        if(p.estado==='CONFIRMADO'&&item.estadoPreparacion==='PENDIENTE'){
          li.append(boton('Marcar LISTO',async()=>{
            const url='/api/v1/pedidos/'+p.pedidoId+'/detalles/'+item.detalleId+'/listo';
            const res=await api(url,'POST');
            mostrar('POST '+url,res);
            if(res.ok)await refrescarOperaciones();
          },'primary'));
        }
        ul.append(li);
      }
      card.append(ul);vista.append(card);
    }
    relojActivos();
  }
  async function actualizarCocina(){
    const r=await api('/api/v1/pedidos');
    if(r.ok&&Array.isArray(r.data)&&moduloActual()==='kitchen')dibujarCocina(r.data);
  }
  function actualizarCajaPedidos(pedidos){
    const lista=e('cashOrders');lista.replaceChildren();
    const activos=pedidos.filter(p=>p.estado==='PENDIENTE'||p.estado==='CONFIRMADO');
    if(!activos.length){lista.append(elemento('p','No hay pedidos activos para cobrar.'));return;}
    for(const p of activos){
      const row=elemento('div',undefined,'cash-row');
      row.append(elemento('span','#'+p.pedidoId+' · '+(p.tipoAtencion==='MESA'?'Mesa '+p.mesaId:'Llevar')
          +' · '+moneda(p.total)+' · '+p.estado));
      row.append(boton('Seleccionar',()=>{
        e('cashPedidoId').value=p.pedidoId;
        e('cashRecibido').value=n(p.total).toFixed(2);
      }));
      lista.append(row);
    }
  }
  async function actualizarCajaPedidosAsync(){
    const r=await api('/api/v1/pedidos');
    if(r.ok&&Array.isArray(r.data)&&moduloActual()==='sales')actualizarCajaPedidos(r.data);
  }

  function mostrarReservas(reservas){
    const cuerpo=e('reservationsTable');cuerpo.replaceChildren();
    const orden=[...reservas].sort((a,b)=>(a.fecha||'').localeCompare(b.fecha||'')||
       (a.horaInicio||'').localeCompare(b.horaInicio||''));
    if(!orden.length){vacio(cuerpo,7,'No hay reservas registradas.');return;}
    for(const r of orden){
      const tr=fila([
        'Mesa #'+(r.mesa?.numero||r.mesa?.id||'?'),
        r.cliente,r.fecha,r.horaInicio+' – '+r.horaFin,
        r.cantidadPersonas,r.estado,''
      ]);
      if(r.estado!=='CANCELADA'&&['MOZO','SUPERVISOR','ADMIN','OPERADOR'].includes(rol)){
        tr.lastChild.append(boton('Cancelar reserva',async()=>{
          if(!confirm('¿Cancelar solicitud de reserva de '+r.cliente+'?'))return;
          const url='/api/reservas/'+r.id+'/cancelar';
          const res=await api(url,'PATCH');mostrar('PATCH '+url,res);
          if(res.ok)await refrescarOperaciones();
        },'outline'));
      }
      cuerpo.append(tr);
    }
  }
  async function actualizarReservas(){
    if(rol==='CLIENTE'||rol==='COCINERO')return;
    const res=await api('/api/reservas');
    if(res.ok&&Array.isArray(res.data)&&moduloActual()==='reservations')mostrarReservas(res.data);
  }

  function seleccionarMetodo(){
    const efectivo=e('cashMethod').value==='EFECTIVO';
    e('cashRecibidoWrap').hidden=!efectivo;
    e('cashReferenceWrap').hidden=efectivo;
    if(efectivo)e('cashReference').value='';
  }
  e('cashMethod').addEventListener('change',seleccionarMetodo);
  seleccionarMetodo();

  function pintarReporte(datos){
    const metodos=datos.porMedioDia||{};
    e('reportDailyTotal').textContent=moneda(datos.ventasFecha);
    e('reportMonthlyTotal').textContent=moneda(datos.ventasMensuales);
    e('reportCash').textContent=moneda(metodos.EFECTIVO);
    e('reportYape').textContent=moneda(metodos.YAPE);
    e('reportPos').textContent=moneda(metodos.POS);
    const actual=Number(e('cashCounted').value);
    const esperado=n(datos.efectivoEsperado);
    e('cashDifference').textContent=(!e('cashCounted').value?'Efectivo esperado: '+moneda(esperado):
      'Diferencia provisional = '+moneda(actual-esperado))+
      ' · Yape/POS no se cuentan físicamente.';
    e('cashCloseStatus').textContent=datos.cuadreRegistrado
      ? 'Cuadre registrado el '+fechaCorta(datos.cuadreRegistrado.fechaRegistro)+
        ' · Contado '+moneda(datos.cuadreRegistrado.efectivoContado)+
        ' · Diferencia '+moneda(datos.cuadreRegistrado.diferencia)
      : 'Esta fecha aún no tiene un cuadre guardado.';
    const cuerpo=e('salesHistoryTable');cuerpo.replaceChildren();
    const operaciones=datos.operacionesDia||[];
    if(!operaciones.length){vacio(cuerpo,5,'No se registraron cobros en esta fecha.');return;}
    for(const registro of operaciones){
      const row=fila([
        fechaCorta(registro.fechaPago).slice(11),
        registro.comprobante,
        '#'+registro.pedidoId,
        registro.medioPago,
        moneda(registro.total)
      ]);
      cuerpo.append(row);
    }
  }
  async function actualizarReporte(){
    if(!['ADMIN','SUPERVISOR','CAJA'].includes(rol))return;
    const fecha=e('cashReportDate').value||fechaLocal();
    const resultado=await api('/api/v1/ventas/resumen?fecha='+encodeURIComponent(fecha));
    if(resultado.ok)pintarReporte(resultado.data);
  }
  e('cashReportDate').value=fechaLocal();
  e('cashReportRefresh').addEventListener('click',actualizarReporte);
  e('cashCounted').addEventListener('input',actualizarReporte);
  e('cashCloseSave').addEventListener('click',async()=>{
    const fecha=e('cashReportDate').value;
    const contado=Number(e('cashCounted').value);
    if(!fecha||e('cashCounted').value===''||!Number.isFinite(contado)||contado<0)
      return mostrarLocal('Indica fecha y cantidad de efectivo contado.');
    if(!confirm('¿Guardar cuadre diario? Se conservará y no podrá sobrescribirse.'))return;
    const r=await api('/api/v1/ventas/cuadres','POST',{fecha,efectivoContado:contado});
    mostrar('POST /api/v1/ventas/cuadres',r);
    if(r.ok)await actualizarReporte();
  });
  function pintarResumenDashboard(data) {
    e('salesCount').textContent=String(data.ventasRegistradas);
    e('salesToday').textContent=moneda(data.ventasHoy);
    e('salesTotal').textContent=moneda(data.totalVentas);
    const chart=e('salesChart');chart.replaceChildren();
    const valores=Object.entries(data.ventasPorDia||{}).slice(-7);
    if(!valores.length){chart.textContent='Todavía no hay cobros registrados.';return;}
    const max=Math.max(1,...valores.map(([,v])=>n(v)));
    for(const [fecha,total] of valores){
      const grupo=elemento('div',undefined,'bar-group');
      const bar=elemento('div',undefined,'bar');
      bar.style.height=Math.max(5,n(total)/max*145)+'px';
      grupo.append(elemento('b',moneda(total)),bar,elemento('small',fecha.slice(5)));
      chart.append(grupo);
    }
  }
  async function actualizarResumen(){
    if(!['ADMIN','CAJA','SUPERVISOR'].includes(rol))return;
    const r=await api('/api/v1/ventas/resumen');
    if(r.ok)pintarResumenDashboard(r.data);
  }

  async function refrescarOperaciones(){
    if(ocupado||!token||e('appView').hidden||document.hidden)return;
    ocupado=true;
    const t=token,mod=moduloActual();
    try{
      if(mod==='orders')await actualizarPedidos();
      else if(mod==='kitchen')await actualizarCocina();
      else if(mod==='sales'){
        await Promise.all([actualizarCajaPedidosAsync(),actualizarReporte()]);
      }
      else if(mod==='reservations')await actualizarReservas();
      else if(mod==='stock')await actualizarInventario();
      else if(mod==='menu')await actualizarCarta();
      else if(mod==='inicio')await actualizarResumen();
      if(t!==token)return;
    }catch(err){console.warn('Sincronización REST no disponible:',err);}
    finally{ocupado=false;}
  }
  function detener(){
    if(sincronizacion){clearInterval(sincronizacion);sincronizacion=null;}
    if(reloj){clearInterval(reloj);reloj=null;}
    ocupado=false;
  }
  function iniciar(){
    detener();
    refrescarOperaciones();
    sincronizacion=setInterval(refrescarOperaciones,5000);
    reloj=setInterval(relojActivos,1000);
  }
  document.addEventListener('visibilitychange',()=>{
    if(!document.hidden && token)refrescarOperaciones();
  });
  window.iniciarOperaciones=iniciar;
  window.detenerOperaciones=detener;
  window.refrescarOperaciones=refrescarOperaciones;
})();
