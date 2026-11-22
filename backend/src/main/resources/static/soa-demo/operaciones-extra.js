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
    if(!stock.length){vacio(cuerpo,7,'No hay inventario configurado.');return;}
    for(const item of stock){
      const plato=porId.get(Number(item.productoId));
      cuerpo.append(fila([
        item.producto,String(item.totalPreparado),String(item.reservadas),
        String(item.vendidas),String(item.salidasManuales??0),
        String(item.disponibles),estado(item,plato||{disponible:true,precio:1})
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
    if(!['ADMIN','SUPERVISOR','CAJA'].includes(rol))return;
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

  // Foco operativo: un Mozo abre una mesa ocupada igual que una comanda de Botanero,
  // sin necesitar escribir un ID de pedido. El mismo visor sirve para llevar.
  let mesasEnPantalla=[];
  let pedidosEnPantalla=[];
  let mesaElegida=null;
  let pedidoElegido=null;
  let editorEstado='nuevo'; // nuevo, consulta, agregar

  function activa(p){return p.estado==='PENDIENTE'||p.estado==='CONFIRMADO';}
  function pedidoDeMesa(mesaId){
    return pedidosEnPantalla.find(p=>activa(p)&&p.tipoAtencion==='MESA'&&Number(p.mesaId)===Number(mesaId));
  }
  function pedidoFoco(){
    if(e('orderType').value==='MESA')return pedidoDeMesa(mesaElegida);
    return pedidosEnPantalla.find(p=>activa(p)&&p.tipoAtencion==='LLEVAR'&&
       Number(p.pedidoId)===Number(pedidoElegido));
  }
  function etiquetaMesa(mesaId){
    const mesa=mesasEnPantalla.find(m=>Number(m.id)===Number(mesaId));
    return mesa?'Mesa #'+mesa.numero:'Mesa #'+mesaId;
  }
  function seleccionarPedidoOperativo(p,desplazar=true){
    pedidoElegido=Number(p.pedidoId);
    editorEstado='consulta';
    if(p.tipoAtencion==='MESA'){
      e('orderType').value='MESA';
      mesaElegida=Number(p.mesaId);
    }else{
      e('orderType').value='LLEVAR';
    }
    modoMesa();
    if(p.tipoAtencion==='MESA')e('orderTable').value=String(p.mesaId);
    e('orderId').value=p.pedidoId;
    visualizarOrdenSeleccionada();
    if(desplazar)e('orderWorkspaceTitle').scrollIntoView({behavior:'smooth',block:'start'});
  }
  function seleccionarMesaOperativa(mesa,desplazar=true){
    e('orderType').value='MESA';
    modoMesa();
    mesaElegida=Number(mesa.id);
    pedidoElegido=null;
    editorEstado='nuevo';
    e('orderTable').value=String(mesa.id);
    visualizarOrdenSeleccionada();
    document.querySelectorAll('.mesa-button').forEach(b=>b.classList.toggle('seleccionada',
      b.dataset.mesaId===String(mesaElegida)));
    if(desplazar)e('orderWorkspaceTitle').scrollIntoView({behavior:'smooth',block:'start'});
  }
  function detallePlato(item,p){
    const linea=elemento('div',undefined,'ops-focused-line');
    const nombre=elemento('div',undefined,'ops-focused-description');
    nombre.append(elemento('strong',item.cantidad+' × '+item.nombre),
      elemento('small','Precio: '+moneda(item.precioUnitario)+' · Subtotal: '+moneda(item.subtotal)));
    const estadoPlato=badgeEstado(item.estadoPreparacion);
    const tiempo=elemento('small','Tiempo: —','ops-age');
    tiempo.dataset.inicioPlato=item.fechaInicio||p.fechaCreacion;
    if(item.fechaListo)tiempo.dataset.finPlato=item.fechaListo;
    linea.append(nombre,estadoPlato,tiempo);
    if(rol==='MOZO' &&
        p.estado==='CONFIRMADO'&&item.estadoPreparacion==='LISTO'){
      linea.append(boton('✓ Marcar ENTREGADO',async()=>{
        const url='/api/v1/pedidos/'+p.pedidoId+'/detalles/'+item.detalleId+'/entregado';
        const resultado=await api(url,'POST');
        mostrar('POST '+url,resultado);
        if(resultado.ok){pedidosEnPantalla=pedidosEnPantalla.map(x=>
          x.pedidoId===p.pedidoId?resultado.data:x);visualizarOrdenSeleccionada();
          await refrescarOperaciones();}
      },'primary'));
    }
    return linea;
  }
  function visualizarOrdenSeleccionada(){
    if(!token||!e('selectedOrderDetails'))return;
    const modo=e('orderType').value;
    const p=pedidoFoco();
    const mesa=mesasEnPantalla.find(m=>Number(m.id)===Number(mesaElegida));
    const mesaOcupada=modo==='MESA'&&mesa?.estado==='OCUPADA';
    const visor=e('selectedOrderDetails');
    visor.replaceChildren();
    e('orderWorkspaceTitle').textContent=modo==='MESA'
      ? etiquetaMesa(mesaElegida||Number(e('orderTable').value))+(mesaOcupada?' · comanda abierta':' · tomar orden')
      : (p?'Pedido para llevar #'+p.pedidoId:'Nuevo pedido para llevar');
    e('orderWorkspaceHint').textContent=p
      ? 'Consulta el estado y total de la orden sin cobrar desde Mozo'
      : modo==='MESA'?'Selecciona platos y envíalos directamente a Cocina':'No se asignará ninguna mesa';
    const puedeCrear=!p && !mesaOcupada;
    const agregar=!!p && editorEstado==='agregar';
    e('orderEntry').hidden=!(puedeCrear||agregar);
    e('newOrderActions').hidden=!puedeCrear;
    e('addToExistingActions').hidden=!agregar;
    e('orderFlowHelp').textContent=agregar
      ? 'Estos productos se sumarán al pedido #'+p.pedidoId+'. El total se recalcula y Cocina los verá pendientes.'
      : 'Selecciona los productos y pulsa «Enviar pedido a Cocina». Registrar y confirmar son una sola acción.';

    if(!p){
      if(mesaOcupada){
        visor.hidden=false;
        visor.append(elemento('p','Mesa ocupada; cargando su comanda. Si permanece vacía, comprueba el último pedido en el servidor.','ops-empty'));
      }else visor.hidden=true;
      return;
    }
    visor.hidden=false;
    e('orderId').value=p.pedidoId;
    const cabeza=elemento('div',undefined,'ops-focus-head');
    cabeza.append(elemento('div',undefined,'ops-focus-title'),badgeEstado(p.estado));
    const titulo=cabeza.firstChild;
    titulo.append(elemento('h3',modo==='MESA'?etiquetaMesa(p.mesaId):'Pedido #'+p.pedidoId+' · PARA LLEVAR'),
      elemento('small','Comanda #'+p.pedidoId+' · '+fechaCorta(p.fechaCreacion)+' · '+(p.clienteCorreo||'Personal')));
    visor.append(cabeza);
    const filas=elemento('div',undefined,'ops-focused-lines');
    for(const item of p.items||[])filas.append(detallePlato(item,p));
    visor.append(filas);
    const pie=elemento('div',undefined,'ops-focus-total');
    pie.append(elemento('span','TOTAL DE LA ORDEN'),
      elemento('strong',moneda(p.total)));
    visor.append(pie);
    const acciones=elemento('div',undefined,'buttons ops-focus-actions');
    if(['MOZO','CAJA','SUPERVISOR','ADMIN','OPERADOR'].includes(rol)){
      acciones.append(boton(editorEstado==='agregar'?'Editando platos':' + Agregar platos / bebidas',()=>{
        editorEstado='agregar';visualizarOrdenSeleccionada();
        e('orderProduct').scrollIntoView({behavior:'smooth',block:'center'});
      },'primary'));
      if(p.estado==='PENDIENTE')acciones.append(boton('Enviar pendiente a Cocina',async()=>{
        const url='/api/v1/pedidos/'+p.pedidoId+'/confirmacion';
        const res=await api(url,'POST');mostrar('POST '+url,res);
        if(res.ok){pedidosEnPantalla=pedidosEnPantalla.map(x=>x.pedidoId===p.pedidoId?res.data:x);
          visualizarOrdenSeleccionada();await refrescarOperaciones();}
      }));
      if((p.items||[]).every(x=>x.estadoPreparacion==='PENDIENTE')){
        acciones.append(boton('Cancelar orden',async()=>{
          if(!confirm('¿Cancelar la orden #'+p.pedidoId+'? Se liberará la mesa y el stock comprometido.'))return;
          const url='/api/v1/pedidos/'+p.pedidoId+'/cancelacion';
          const res=await api(url,'POST');mostrar('POST '+url,res);
          if(res.ok){
            pedidosEnPantalla=pedidosEnPantalla.map(x=>x.pedidoId===p.pedidoId?res.data:x);
            pedidoElegido=null;editorEstado='nuevo';
            visualizarOrdenSeleccionada();await refrescarOperaciones();
          }
        },'danger'));
      }
    }
    visor.append(acciones);
    const note=elemento('p','Cocina marca LISTO; el Mozo registra ENTREGADO. El cobro corresponde a Caja.','ops-focus-note');
    visor.append(note);
    relojActivos();
  }
  e('stopAddingItems').addEventListener('click',()=>{
    editorEstado='consulta';visualizarOrdenSeleccionada();
  });
  window.seleccionarMesaOperativa=seleccionarMesaOperativa;
  window.mesaOperativaSeleccionada=()=>mesaElegida;
  window.mesasSincronizadas=mesas=>{
    mesasEnPantalla=mesas;
    if(mesaElegida===null && e('orderType').value==='MESA')
      mesaElegida=Number(e('orderTable').value)||Number(mesas[0]?.id)||null;
    if(moduloActual()==='orders'){
      visualizarOrdenSeleccionada();
      dibujarAgendaEnMesas();
    }
  };
  window.modalidadPedidoCambiada=tipo=>{
    editorEstado='nuevo';pedidoElegido=null;
    if(tipo==='MESA'){
      mesaElegida=Number(e('orderTable').value)||mesaElegida||null;
    }
    visualizarOrdenSeleccionada();
  };
  window.pedidoEnviadoOperativo=p=>{
    pedidosEnPantalla=pedidosEnPantalla.filter(x=>x.pedidoId!==p.pedidoId);
    pedidosEnPantalla.unshift(p);
    pedidoElegido=Number(p.pedidoId);
    if(p.tipoAtencion==='MESA')mesaElegida=Number(p.mesaId);
    editorEstado='consulta';
    visualizarOrdenSeleccionada();
  };
  window.limpiarVistaPedidos=()=>{
    mesaElegida=null;pedidoElegido=null;editorEstado='nuevo';
    pedidosEnPantalla=[];mesasEnPantalla=[];
    if(e('selectedOrderDetails'))e('selectedOrderDetails').replaceChildren();
  };

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
      if(rol==='MOZO'
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
    acciones.append(boton('Ver comanda / total',()=>seleccionarPedidoOperativo(p), 'outline'));
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
    visualizarOrdenSeleccionada();
    relojActivos();
  }
  async function actualizarPedidos(){
    const r=await api('/api/v1/pedidos');
    if(!r.ok||!Array.isArray(r.data)||moduloActual()!=='orders'||!token)return;
    pedidosEnPantalla=r.data;
    pintarPedidos(r.data);
    if(['ADMIN','CAJA','MOZO','SUPERVISOR','OPERADOR'].includes(rol)){
      await Promise.all([cargarMesas(),consultarDisponibilidad()]);
      await actualizarAgendaMesas();
    }
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
        pedidosEnPantalla=pedidosEnPantalla.map(x=>x.pedidoId===id?r.data:x);
        editorEstado='consulta';pedidoElegido=id;
        visualizarOrdenSeleccionada();
        e('ordersSyncStatus').textContent='Se agregaron platos al pedido #'+id;
        await refrescarOperaciones();
      }
    }finally{b.disabled=false;}
  });

  function dibujarCocina(pedidos){
    const vista=e('kitchenOrders');vista.replaceChildren();
    const activos=pedidos.filter(p=>p.estado==='CONFIRMADO' &&
      (p.items||[]).some(i=>i.estadoPreparacion==='PENDIENTE'));
    if(!activos.length){vista.append(elemento('p','No hay platos pendientes de preparación. Las comandas LISTAS o ENTREGADAS salen de esta cola; el cobro sigue en Caja.'));return;}
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
  function pedidoListoParaCobro(p){
    return p.estado==='CONFIRMADO' && Array.isArray(p.items) && p.items.length>0 &&
      p.items.every(item=>['LISTO','ENTREGADO'].includes(item.estadoPreparacion));
  }
  function limpiarSeleccionCaja(mensaje='Selecciona un pedido listo para cobrar.'){
    e('cashPedidoId').value='';
    e('cashRecibido').value='';
    e('cashReference').value='';
    e('cashPosConsent').checked=false;
    e('cashCharge').disabled=true;
    ultimaCotizacion=null;
    e('cashQuote').textContent=mensaje;
  }
  window.limpiarSeleccionCaja=limpiarSeleccionCaja;
  window.registrarCobroEnCaja=(id)=>{
    e('cashReceiptId').value=String(id);
    limpiarSeleccionCaja('Cobro registrado. El pedido pasó al historial. Selecciona una nueva orden lista.');
    actualizarReporte();
  };
  function actualizarCajaPedidos(pedidos){
    const lista=e('cashOrders');lista.replaceChildren();
    const activos=pedidos.filter(p=>p.estado==='PENDIENTE'||p.estado==='CONFIRMADO');
    const seleccionado=Number(e('cashPedidoId').value);
    const vigente=activos.find(p=>p.pedidoId===seleccionado);
    if(seleccionado>0 && (!vigente || !pedidoListoParaCobro(vigente))){
      limpiarSeleccionCaja(!vigente
        ? 'El último pedido ya no está pendiente de cobro (pagado o cancelado). Selecciona otro.'
        : 'La orden volvió a preparación; espera a que Cocina termine todos los platos.');
    }
    if(!activos.length){
      lista.append(elemento('p','No hay pedidos por cobrar. Los ya pagados figuran en «Ventas del día».'));
      return;
    }
    for(const p of activos){
      const cobrable=pedidoListoParaCobro(p);
      const row=elemento('div',undefined,'cash-row');
      row.append(elemento('span','#'+p.pedidoId+' · '+(p.tipoAtencion==='MESA'?'Mesa '+p.mesaId:'Llevar')
          +' · '+moneda(p.total)+' · '+(cobrable?'LISTO PARA COBRAR':'ESPERANDO COCINA')));
      const seleccionar=boton(cobrable?'Seleccionar':'No listo',()=>{
        if(!cobrable)return;
        e('cashPedidoId').value=String(p.pedidoId);
        e('cashRecibido').value=n(p.total).toFixed(2);
        e('cashReference').value='';
        e('cashPosConsent').checked=false;
        e('cashCharge').disabled=true;
        refrescarCotizacionCaja();
      });
      seleccionar.disabled=!cobrable;
      row.append(seleccionar);
      lista.append(row);
    }
  }
  window.pintarPedidosCaja=actualizarCajaPedidos;
  async function actualizarCajaPedidosAsync(){
    const r=await api('/api/v1/pedidos');
    if(r.ok&&Array.isArray(r.data)&&moduloActual()==='sales')actualizarCajaPedidos(r.data);
  }

  let reservasEnPantalla=[];
  async function cancelarReservaOperativa(r) {
    if(!confirm('¿Cancelar la reserva #'+r.id+' de '+r.cliente+'?'))return;
    const url='/api/reservas/'+r.id+'/cancelar';
    const res=await api(url,'PATCH');mostrar('PATCH '+url,res);
    if(res.ok){
      reservasEnPantalla=reservasEnPantalla.map(x=>
        x.id===r.id?{...x,estado:'CANCELADA'}:x);
      mostrarReservas(reservasEnPantalla);
      await refrescarOperaciones();
    }
  }
  function tarjetaReserva(r){
    const card=elemento('article',undefined,'ops-reservation-card');
    const cabeza=elemento('div',undefined,'ops-reservation-header');
    const datos=elemento('div');
    datos.append(elemento('small','RESERVA #'+r.id,'ops-reservation-id'),
      elemento('h3','Mesa #'+(r.mesa?.numero||r.mesa?.id||'?')));
    cabeza.append(datos,badgeEstado(r.estado||'PENDIENTE'));card.append(cabeza);
    const resumen=elemento('div',undefined,'ops-reservation-data');
    resumen.append(
      elemento('span','👤 Cliente: '+r.cliente),
      elemento('span','📅 Fecha: '+r.fecha),
      elemento('span','🕒 Horario: '+String(r.horaInicio).slice(0,5)+' a '+String(r.horaFin).slice(0,5)),
      elemento('span','👥 Personas: '+r.cantidadPersonas)
    );
    card.append(resumen);
    card.append(elemento('p',r.estado==='PENDIENTE'
      ? 'Solicitud pendiente; aún no es una reserva confirmada.'
      : 'Reserva registrada para este horario.','ops-reservation-note'));
    if(['MOZO','SUPERVISOR','ADMIN','OPERADOR'].includes(rol) && r.estado!=='CANCELADA')
      card.append(boton('Cancelar solicitud',()=>cancelarReservaOperativa(r),'outline'));
    return card;
  }
  function mostrarReservas(reservas){
    const cuerpo=e('reservationsTable');
    const tarjetas=e('reservedCards');
    cuerpo.replaceChildren();tarjetas.replaceChildren();
    const orden=[...reservas].sort((a,b)=>(a.fecha||'').localeCompare(b.fecha||'')||
       (a.horaInicio||'').localeCompare(b.horaInicio||''));
    const vigentes=orden.filter(r=>r.estado!=='CANCELADA'&&String(r.fecha)>=fechaLocal());
    e('reservationSyncStatus').textContent='Actualizado '+new Date().toLocaleTimeString('es-PE')+
      ' · '+vigentes.length+' solicitud(es) futura(s)';
    if(!vigentes.length){
      tarjetas.append(elemento('p','No hay solicitudes de reserva futuras. Puedes registrar una con el formulario superior.','ops-empty'));
    }else{
      for(const r of vigentes)tarjetas.append(tarjetaReserva(r));
    }
    if(!orden.length){vacio(cuerpo,8,'No hay reservas registradas.');return;}
    for(const r of orden){
      const tr=fila([
        '#'+r.id,
        'Mesa #'+(r.mesa?.numero||r.mesa?.id||'?'),
        r.cliente,r.fecha,String(r.horaInicio).slice(0,5)+' – '+String(r.horaFin).slice(0,5),
        r.cantidadPersonas,r.estado,''
      ]);
      if(r.estado!=='CANCELADA'&&['MOZO','SUPERVISOR','ADMIN','OPERADOR'].includes(rol))
        tr.lastChild.append(boton('Cancelar',()=>cancelarReservaOperativa(r),'outline'));
      cuerpo.append(tr);
    }
  }
  function dibujarAgendaEnMesas(){
    if(!mesasEnPantalla.length)return;
    document.querySelectorAll('.mesa-button[data-mesa-id]').forEach(b=>{
      b.querySelectorAll('.mesa-reserva-detalle').forEach(n=>n.remove());
      const id=Number(b.dataset.mesaId);
      const reservasHoy=reservasEnPantalla.filter(r=>
        Number(r.mesa?.id)===id && r.estado!=='CANCELADA' && r.fecha===fechaLocal());
      if(reservasHoy.length){
        const primera=[...reservasHoy].sort((a,b)=>a.horaInicio.localeCompare(b.horaInicio))[0];
        const aviso=elemento('small','RESERVA #'+primera.id+' · '+
          String(primera.horaInicio).slice(0,5)+'–'+String(primera.horaFin).slice(0,5),
          'mesa-reserva-detalle');
        b.append(aviso);
      }
    });
  }
  async function actualizarAgendaMesas(){
    if(['CLIENTE','COCINERO'].includes(rol))return;
    const r=await api('/api/reservas');
    if(r.ok&&Array.isArray(r.data)){
      reservasEnPantalla=r.data;
      dibujarAgendaEnMesas();
    }
  }
  async function actualizarReservas(){
    if(rol==='CLIENTE'||rol==='COCINERO')return;
    const res=await api('/api/reservas');
    if(res.ok&&Array.isArray(res.data)&&moduloActual()==='reservations'){
      reservasEnPantalla=res.data;
      mostrarReservas(res.data);
    }
  }

  let ultimaCotizacion=null;
  async function refrescarCotizacionCaja(){
    const id=Number(e('cashPedidoId').value);
    const metodo=e('cashMethod').value;
    const caja=e('cashQuote');
    if(!Number.isInteger(id)||id<1){
      e('cashCharge').disabled=true;
      caja.textContent='Selecciona una orden lista para conocer el total a cobrar.';
      return;
    }
    e('cashCharge').disabled=true;
    const r=await api('/api/v1/ventas/pedidos/'+id+'/cotizacion?medioPago='+metodo);
    if(e('cashMethod').value!==metodo||Number(e('cashPedidoId').value)!==id)return;
    if(!r.ok||!r.data){
      if(r.data?.codigo==='PEDIDO_YA_PAGADO'){
        limpiarSeleccionCaja('Este pedido ya fue COBRADO. Busca el comprobante en «Ventas del día».');
      }else{
        caja.textContent='No es posible cobrar este pedido: '+(r.data?.mensaje||('HTTP '+r.status));
        e('cashCharge').disabled=true;
      }
      return;
    }
    if(r.data.estado!=='CONFIRMADO'){
      limpiarSeleccionCaja('Solo se cobran pedidos confirmados y completamente preparados.');
      return;
    }
    ultimaCotizacion=r.data;
    e('cashCharge').disabled=false;
    const dat=r.data;
    caja.replaceChildren();
    const h=elemento('div',undefined,'ops-payment-row');
    h.append(elemento('span','Subtotal de platos'),elemento('strong',moneda(dat.subtotalBase)));
    caja.append(h);
    if(metodo==='POS'){
      const extra=elemento('div',undefined,'ops-payment-row');
      extra.append(elemento('span','Recargo POS 4% + redondeo hacia arriba a S/ 0.50'),
        elemento('strong',moneda(dat.recargoPos)));
      caja.append(extra);
    }
    const total=elemento('div',undefined,'ops-payment-row ops-payment-total');
    total.append(elemento('span','Total que debe pagar el cliente'),
      elemento('strong',moneda(dat.totalCobro)));
    caja.append(total);
    if(metodo==='POS')caja.append(elemento('small',
      'Informar este importe antes de cobrar. El recargo se registra separado del precio de los platos.'));
    if(metodo==='EFECTIVO' && (!e('cashRecibido').value ||
       n(e('cashRecibido').value)<n(dat.totalCobro))){
      e('cashRecibido').value=n(dat.totalCobro).toFixed(2);
    }
  }
  function seleccionarMetodo(){
    const medio=e('cashMethod').value;
    e('cashRecibidoWrap').hidden=medio!=='EFECTIVO';
    e('cashReferenceWrap').hidden=medio==='EFECTIVO';
    e('cashPosDisclosure').hidden=medio!=='POS';
    e('cashPosConsent').checked=false;
    if(medio==='EFECTIVO')e('cashReference').value='';
    refrescarCotizacionCaja();
  }
  e('cashMethod').addEventListener('change',seleccionarMetodo);
  e('cashPedidoId').addEventListener('change',refrescarCotizacionCaja);
  e('cashPedidoId').addEventListener('input',refrescarCotizacionCaja);
  window.refrescarCotizacionCaja=refrescarCotizacionCaja;
  seleccionarMetodo();

  function pintarReporte(datos){
    const metodos=datos.porMedioDia||{};
    e('reportDailyTotal').textContent=moneda(datos.ventasFecha);
    e('reportMonthlyTotal').textContent=moneda(datos.ventasMensuales);
    e('reportCash').textContent=moneda(metodos.EFECTIVO);
    e('reportYape').textContent=moneda(metodos.YAPE);
    e('reportPos').textContent=moneda(metodos.POS);
    e('reportTransfer').textContent=moneda(metodos.TRANSFERENCIA);
    e('reportPosFee').textContent=moneda(datos.recargosPosDia);
    const actual=Number(e('cashCounted').value);
    const esperado=n(datos.efectivoEsperado);
    e('cashDifference').textContent=(!e('cashCounted').value?'Efectivo esperado: '+moneda(esperado):
      'Diferencia provisional = '+moneda(actual-esperado))+
      ' · Yape/POS/Transferencia no se cuentan físicamente.';
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

  function mostrarDetalleDia(datos){
    const tbody=e('dailySalesTable');tbody.replaceChildren();
    const lineas=Array.isArray(datos.lineas)?datos.lineas:[];
    if(!lineas.length){
      vacio(tbody,4,'Sin productos vendidos y cobrados en la fecha seleccionada.');
    }
    for(const l of lineas){
      tbody.append(fila([
        l.producto,
        String(l.cantidad),
        moneda(l.precioUnitario),
        moneda(l.subtotal)
      ]));
    }
    e('dailySalesTickets').textContent=String(datos.ventasCobradas||0);
    e('dailySalesUnits').textContent=String(datos.unidadesVendidas||0);
    e('dailySalesBase').textContent=moneda(datos.subtotalPlatos);
    e('dailySalesPosFee').textContent=moneda(datos.recargosPos);
    e('dailySalesTotal').textContent=moneda(datos.totalCobrado);
    e('dailySalesFooterBase').textContent=moneda(datos.subtotalPlatos);
    e('dailySalesFooterFee').textContent=moneda(datos.recargosPos);
    e('dailySalesFooterTotal').textContent=moneda(datos.totalCobrado);
    const pagos=datos.porMedioPago||{};
    e('dailySalesCash').textContent=moneda(pagos.EFECTIVO);
    e('dailySalesYape').textContent=moneda(pagos.YAPE);
    e('dailySalesPos').textContent=moneda(pagos.POS);
    e('dailySalesTransfer').textContent=moneda(pagos.TRANSFERENCIA);
    e('dailySalesCheck').textContent=datos.cuadreDetalle
      ? 'Verificado: suma de productos + recargos POS = total cobrado. Las salidas manuales no constituyen ventas.'
      : 'ADVERTENCIA: el detalle de productos no cuadra con los cobros. Verifica las operaciones.';
    e('dailySalesUpdated').textContent='Fecha: '+datos.fecha+
      ' · Actualizado '+new Date().toLocaleTimeString('es-PE');
  }
  async function actualizarVentasDiarias(){
    if(!['ADMIN','SUPERVISOR','CAJA'].includes(rol))return;
    const fecha=e('dailySalesDate').value||fechaLocal();
    const respuesta=await api('/api/v1/ventas/detalle-diario?fecha='+encodeURIComponent(fecha));
    if(!token || e('dailySalesDate').value!==fecha)return;
    if(respuesta.ok)mostrarDetalleDia(respuesta.data);
    else e('dailySalesUpdated').textContent=
      'No fue posible consultar la fecha (HTTP '+respuesta.status+').';
  }
  e('dailySalesDate').value=fechaLocal();
  e('dailySalesRefresh').addEventListener('click',actualizarVentasDiarias);
  e('dailySalesDate').addEventListener('change',actualizarVentasDiarias);

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
        await refrescarCotizacionCaja();
      }
      else if(mod==='salesDaily')await actualizarVentasDiarias();
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
