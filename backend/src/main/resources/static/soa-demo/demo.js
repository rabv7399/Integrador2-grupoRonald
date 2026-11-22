'use strict';
/* Demostrador REST SOA; el JWT solo se mantiene en memoria (nunca en localStorage). */
let token = '';
let rol = '';
let correoSesion = '';
let listaProductos = [];
let listaCategorias = [];
let usuariosAdmin = [];
let configuracionLocal={nombre:'Delicias Peruanas',telefono:'',logoRuta:null};
const $ = id => document.getElementById(id);
const formatPrice = value => new Intl.NumberFormat('es-PE',{style:'currency',currency:'PEN'}).format(Number(value));
function nuevaClave() {
  $('orderKey').value='soa-demo-'+(crypto.randomUUID ? crypto.randomUUID() : Date.now().toString(36)+'-'+Math.random().toString(36).slice(2));
}
function mostrar(operacion, resultado) {
  $('httpStatus').textContent = operacion+' · HTTP '+resultado.status;
  $('httpStatus').className = 'response-status '+(resultado.ok?'ok':'fail');
  $('responseJson').textContent = JSON.stringify(resultado.data,null,2);
  $('responseHeadline').textContent = operacion+' · HTTP '+resultado.status+
    (resultado.ok?' · Operación completada':' · Revisa el error');
  $('responseBar').classList.toggle('failure',!resultado.ok);
}
function mostrarLocal(mensaje) {
  $('httpStatus').textContent = 'Sin solicitud HTTP';
  $('httpStatus').className = 'response-status fail';
  $('responseJson').textContent = mensaje;
  $('responseHeadline').textContent = mensaje;
  $('responseBar').classList.add('failure');
}
async function api(url, method='GET', body=null, headers={}) {
  const settings={method,headers:{Accept:'application/json',...headers}};
  if(token)settings.headers.Authorization='Bearer '+token;
  if(body!==null){settings.headers['Content-Type']='application/json';settings.body=JSON.stringify(body);}
  try{
    const response=await fetch(url,settings);
    const content=await response.text();
    let data;
    try{data=content?JSON.parse(content):{mensaje:'Operación completada sin contenido'};}
    catch{data={mensaje:content.slice(0,400)};}
    return {ok:response.ok,status:response.status,data};
  }catch(error){return {ok:false,status:'sin conexión',data:{codigo:'ERROR_RED',mensaje:'No se pudo contactar con Spring Boot',detalle:String(error)}};}
}
function pedirSesion(){
  if(token)return true;
  mostrarLocal('Inicia sesión con un usuario de demostración para consumir este servicio protegido.');
  return false;
}
const MODULOS = {
  inicio:{titulo:'Resumen',servicio:'Panel',descripcion:'Tu espacio de trabajo y las funciones autorizadas.'},
  menu:{titulo:'Carta y productos',servicio:'S02',descripcion:'Consulta platos y categorías del catálogo real.'},
  orders:{titulo:'Pedidos',servicio:'S03',descripcion:'Registra y consulta pedidos, con control de porciones.'},
  reservations:{titulo:'Reservas de mesas',servicio:'S04',descripcion:'Gestiona solicitudes de reserva para mesas.'},
  stock:{titulo:'Inventario',servicio:'S05',descripcion:'Verifica las porciones preparadas, reservadas y vendidas.'},
  kitchen:{titulo:'Cocina',servicio:'S03',descripcion:'Comandas por plato: pendiente o listo.'},
  sales:{titulo:'Caja y cobros',servicio:'S06',descripcion:'Registra pagos y emite tickets internos.'},
  salesDaily:{titulo:'Ventas del día',servicio:'S06',descripcion:'Detalle por producto, unidades y total cobrado por fecha.'},
  productsAdmin:{titulo:'Productos · CRUD',servicio:'S02',descripcion:'Agregar, modificar o desactivar platos y bebidas.'},
  categoriesAdmin:{titulo:'Categorías · CRUD',servicio:'S02',descripcion:'Mantener almuerzos, cenas y bebidas.'},
  usersAdmin:{titulo:'Usuarios · CRUD',servicio:'S01',descripcion:'Alta, permisos y baja lógica de usuarios.'},
  settings:{titulo:'Configuración del local',servicio:'ADM',descripcion:'Nombre, teléfono y logo.'},
  api:{titulo:'Laboratorio REST',servicio:'JSON',descripcion:'Inspecciona las respuestas de las API.'}
};
const PERFILES = {
  CLIENTE:{titulo:'Cliente',saludo:'¡Qué gusto tenerte aquí!',
    descripcion:'Consulta la carta virtual. Puedes solicitar una reserva; un mozo asignará la mesa y registrará el pedido.',
    permisos:'Puedes consultar productos y solicitar reservas. Un mozo registra los pedidos a mesa. No puedes acceder a inventario, caja ni personal.'},
  MOZO:{titulo:'Mozo',saludo:'Tu área de atención',
    descripcion:'Gestiona las solicitudes de los clientes y confirma sus pedidos.',
    permisos:'Puedes gestionar pedidos y reservas y consultar porciones. No puedes configurar inventario.'},
  CAJA:{titulo:'Caja',saludo:'Caja y control de ventas',
    descripcion:'Registra pedidos de mesa o para llevar, cobra e incrementa las porciones preparadas.',
    permisos:'Puedes registrar y cobrar pedidos, reponer porciones y ver el dashboard. No puedes crear productos ni gestionar usuarios.'},
  COCINERO:{titulo:'Cocinero',saludo:'Comandas de cocina',
    descripcion:'Consulta los platos pedidos y marca cada preparación como lista.',
    permisos:'Solo puedes consultar comandas y marcar LISTO en las líneas pendientes. No puedes cobrar ni cambiar el inventario.'},
  SUPERVISOR:{titulo:'Supervisor',saludo:'Control de operaciones',
    descripcion:'Revisa el inventario y el flujo de pedidos del restaurante.',
    permisos:'Puedes gestionar pedidos, caja y reservas y reponer porciones; la edición de la carta y usuarios corresponde al ADMIN.'},
  ADMIN:{titulo:'Administrador',saludo:'Gestión de Delicias Peruanas',
    descripcion:'Administra las operaciones y la disponibilidad de platos.',
    permisos:'Puedes usar todos los módulos, configurar porciones, confirmar pedidos y gestionar reservas.'},
  OPERADOR:{titulo:'Operador (compatibilidad)',saludo:'Gestión operativa',
    descripcion:'Accede a la operación del restaurante con el rol heredado.',
    permisos:'Puedes gestionar pedidos y reservas, consultar inventario y operar con los permisos heredados del sistema.'}
};
function tienePermiso(nodo) {
  const roles=(nodo.dataset.roles || '').split(',').map(x=>x.trim());
  return !nodo.dataset.roles || roles.includes(rol);
}
function abrirModulo(modulo) {
  if(!token || !Object.hasOwn(MODULOS,modulo))return;
  const nav=document.querySelector('#sideNav [data-module="'+modulo+'"]');
  if(!nav || nav.hidden || !tienePermiso(nav))return;
  document.querySelectorAll('.view-module').forEach(seccion=>{
    seccion.hidden=seccion.id!=='module-'+modulo;
  });
  document.querySelectorAll('#sideNav [data-module]').forEach(b=>{
    const active=b.dataset.module===modulo;
    b.classList.toggle('active',active);
    if(active)b.setAttribute('aria-current','page');
    else b.removeAttribute('aria-current');
  });
  $('workspaceTitle').textContent=MODULOS[modulo].titulo;
  if(window.refrescarOperaciones)setTimeout(window.refrescarOperaciones,0);
  window.scrollTo({top:0,behavior:'instant'});
  if(modulo==='stock' && $('stockProduct').value)consultarInventarioUI();
}
function iniciarPanel(){
  const perfil=PERFILES[rol];
  if(!perfil)return false;
  $('loginView').hidden=true;
  $('appView').hidden=false;
  $('userName').textContent=perfil.titulo;
  $('userRole').textContent=rol;
  $('userMail').textContent=correoSesion;
  $('dashboardRole').textContent=rol;
  $('welcomeTitle').textContent=perfil.saludo;
  $('welcomeDescription').textContent=perfil.descripcion;
  $('permissionsText').textContent=perfil.permisos;
  document.querySelectorAll('[data-roles]').forEach(nodo=>{nodo.hidden=!tienePermiso(nodo);});
  const quick=$('quickActions');quick.replaceChildren();
  document.querySelectorAll('#sideNav [data-module]').forEach(button=>{
    if(button.hidden||button.dataset.module==='inicio'||button.dataset.module==='api')return;
    const modulo=button.dataset.module;
    const card=document.createElement('button');
    card.type='button';card.className='quick-card';
    const service=document.createElement('span');service.textContent=MODULOS[modulo].servicio;
    const title=document.createElement('strong');title.textContent=MODULOS[modulo].titulo;
    const hint=document.createElement('small');hint.textContent=MODULOS[modulo].descripcion;
    const arrow=document.createElement('span');arrow.className='arrow';arrow.textContent='Abrir →';
    card.append(service,title,hint,arrow);card.addEventListener('click',()=>abrirModulo(modulo));
    quick.append(card);
  });
  abrirModulo('inicio');
  $('responseHeadline').textContent='Autenticación correcta · JWT verificado por Spring Security';
  $('responseBar').classList.remove('failure');
  history.replaceState(null,'',location.pathname+'#panel');
  consultarCatalogo();
  cargarConfiguracion();
  if(['ADMIN','CAJA','SUPERVISOR'].includes(rol)) refrescarResumen();
  if(['ADMIN','CAJA','MOZO','SUPERVISOR','OPERADOR'].includes(rol)) cargarMesas();
  if(rol==='ADMIN') { cargarUsuarios(); cargarCategorias(); }
  if(window.iniciarOperaciones)window.iniciarOperaciones();
  return true;
}
function cerrarSesion(){
  if(window.detenerOperaciones)window.detenerOperaciones();
  if(window.limpiarVistaPedidos)window.limpiarVistaPedidos();
  if(window.limpiarSeleccionCaja)window.limpiarSeleccionCaja('Selecciona un pedido listo para cobrar.');
  if($('cashReceiptId'))$('cashReceiptId').value='';
  token='';rol='';correoSesion='';
  $('appView').hidden=true;
  $('loginView').hidden=false;
  $('loginMessage').textContent='Sesión finalizada. Ingresa nuevamente para continuar.';
  $('loginMessage').classList.remove('error');
  $('correo').value='';
  $('contrasena').value='';
  $('orderId').value='';
  $('responseJson').textContent='No hay respuesta.';
  $('httpStatus').textContent='Sin solicitudes';
  history.replaceState(null,'',location.pathname);
  $('correo').focus();
}
function listaMenu(){
  for(const id of ['orderProduct','stockProduct','productManageSelect']) {
    const element=$(id);element.replaceChildren();
    if(id==='productManageSelect')element.add(new Option('Nuevo producto',''));
    for(const p of listaProductos){
      if(id==='orderProduct' && (!p.disponible || Number(p.precio)<=0))continue;
      const etiqueta=Number(p.precio)>0?formatPrice(p.precio):'Sin precio confirmado';
      element.add(new Option(p.nombre+' · '+etiqueta+(p.disponible?'':' · no disponible'),String(p.id)));
    }
  }
  if($('orderProductSearch').value.trim())filtrarProductosParaOrden();
  if(rol!=='CLIENTE' && $('orderProduct').value) consultarDisponibilidad();
  if(!$('module-stock').hidden && $('stockProduct').value)consultarInventarioUI();
  pintarAdminProductos();
  $('productsCount').textContent=String(listaProductos.length);
  $('dashboardProductsCount').textContent=String(listaProductos.length);
  const div=$('productList');div.replaceChildren();
  if(!listaProductos.length){div.textContent='No hay platos registrados';return;}
  for(const p of listaProductos){
    const row=document.createElement('div');row.className='product-line';
    const name=document.createElement('strong');name.textContent=p.nombre;
    const price=document.createElement('span');price.textContent=Number(p.precio)>0?formatPrice(p.precio):'Sin precio confirmado';
    if(!p.disponible)price.textContent+=' · no disponible';
    row.append(name,price);div.append(row);
  }
}
function filtrarProductosParaOrden(){
  const buscado=$('orderProductSearch').value.trim().toLocaleLowerCase('es');
  const select=$('orderProduct');
  const elegido=select.value;
  select.replaceChildren();
  for(const p of listaProductos){
    if(!p.disponible || Number(p.precio)<=0)continue;
    if(buscado && !p.nombre.toLocaleLowerCase('es').includes(buscado))continue;
    select.add(new Option(p.nombre+' · '+formatPrice(p.precio),String(p.id)));
  }
  if([...select.options].some(o=>o.value===elegido))select.value=elegido;
  if(select.value)consultarDisponibilidad();
  else $('orderAvailability').textContent='No se encontraron platos con ese nombre.';
}
$('orderProductSearch').addEventListener('input',filtrarProductosParaOrden);
async function consultarCatalogo(){
  const [productos,categorias]=await Promise.all([api('/api/productos'),api('/api/categorias')]);
  mostrar('GET /api/productos',productos);
  if(productos.ok&&Array.isArray(productos.data)){listaProductos=productos.data;listaMenu();}
  if(categorias.ok&&Array.isArray(categorias.data)){
    listaCategorias=categorias.data;
    $('categoriesCount').textContent=String(listaCategorias.length);
    pintarCategorias();
  }
}
$('loginForm').addEventListener('submit',async event=>{
  event.preventDefault();
  const boton=$('loginForm').querySelector('button[type="submit"]');
  boton.disabled=true;
  $('loginMessage').textContent='Validando credenciales con el servicio S01...';
  $('loginMessage').classList.remove('error');
  try{
    const resultado=await api('/api/auth/login','POST',{
      correo:$('correo').value.trim().toLowerCase(),password:$('contrasena').value
    });
    $('contrasena').value='';
    if(resultado.ok && typeof resultado.data.token==='string' && PERFILES[resultado.data.rol]){
      token=resultado.data.token;
      rol=resultado.data.rol;
      correoSesion=resultado.data.correo;
      if(!iniciarPanel())throw new Error('Rol no autorizado para este panel');
      mostrar('POST /api/auth/login',{ok:true,status:resultado.status,data:{
        mensaje:'Inicio de sesión correcto; el JWT se mantiene solo en memoria',rol,correo:correoSesion
      }});
    }else{
      token='';rol='';correoSesion='';
      const mensaje=resultado.ok?'No se reconoce el perfil de esta cuenta.':
        (resultado.data?.message||resultado.data?.mensaje||'Credenciales incorrectas o error de conexión');
      $('loginMessage').textContent=mensaje;
      $('loginMessage').classList.add('error');
    }
  }catch(error){
    token='';rol='';correoSesion='';
    $('loginMessage').textContent='No se pudo iniciar sesión. Comprueba el servidor e inténtalo nuevamente.';
    $('loginMessage').classList.add('error');
  }finally{boton.disabled=false;}
});
$('logout').addEventListener('click',cerrarSesion);
document.querySelectorAll('#sideNav [data-module]').forEach(button=>
  button.addEventListener('click',()=>abrirModulo(button.dataset.module)));
$('viewJson').addEventListener('click',()=>abrirModulo('api'));
$('getCatalog').addEventListener('click',consultarCatalogo);
function pintarInventario(datos) {
  const producto=listaProductos.find(p=>p.id===Number(datos.productoId));
  const disponible=Math.max(0,Number(datos.disponibles));
  const preparado=Number(datos.totalPreparado);
  const habilitado=producto ? producto.disponible===true : true;
  const estado=!habilitado?'No disponible en carta':disponible>0?'Disponible':'Agotado';
  $('inventoryName').textContent=datos.producto||producto?.nombre||'Plato seleccionado';
  $('inventoryPrepared').textContent=String(datos.totalPreparado);
  $('inventoryReserved').textContent=String(datos.reservadas);
  $('inventorySold').textContent=String(datos.vendidas);
  $('inventoryAvailable').textContent=String(datos.disponibles);
  $('inventoryBadge').textContent=estado;
  $('inventoryBadge').className='inventory-badge '+(habilitado&&disponible>0?'is-available':'is-unavailable');
  $('inventoryMeterFill').style.width=(preparado>0?Math.min(100,disponible/preparado*100):0)+'%';
  $('inventoryMeterLabel').setAttribute('aria-label',disponible+' de '+preparado+' porciones disponibles');
  $('inventorySummaryNote').textContent=habilitado
    ? (disponible>0?'Hay '+disponible+' porciones para nuevos pedidos.':'El plato está agotado; Caja puede reponer cuando Cocina entregue más porciones.')
    : 'El producto está desactivado en la carta. No se puede pedir aunque haya porciones registradas.';
  $('inventorySummaryError').hidden=true;
  $('inventorySummary').hidden=false;
}
async function consultarInventarioUI(){
  if(!pedirSesion())return;
  const id=$('stockProduct').value;
  if(!id){
    $('inventorySummary').hidden=true;
    return mostrarLocal('Primero consulta los productos.');
  }
  const consultado=id;
  const url='/api/v1/inventario/'+id;
  const resultado=await api(url);
  // Evita mostrar el inventario anterior tras cambiar de plato.
  if($('stockProduct').value!==consultado)return;
  mostrar('GET '+url,resultado);
  if(resultado.ok&&resultado.data&&Number.isFinite(Number(resultado.data.disponibles))){
    pintarInventario(resultado.data);
  }else{
    $('inventorySummary').hidden=true;
    $('inventorySummaryError').hidden=false;
    $('inventorySummaryError').textContent=resultado.status===401
      ? 'La sesión caducó. Pulsa Salir y vuelve a ingresar para consultar las porciones.'
      : (resultado.data?.mensaje||resultado.data?.message||'No se pudo consultar la disponibilidad.');
  }
}
$('getStock').addEventListener('click',consultarInventarioUI);
$('stockProduct').addEventListener('change',consultarInventarioUI);
$('setStock').addEventListener('click',async()=>{
  if(!pedirSesion())return;
  if(rol!=='ADMIN')return mostrarLocal('Solo el ADMIN puede modificar el total de porciones. Usa la sesión de administrador.');
  const id=$('stockProduct').value;
  if(!id)return mostrarLocal('Primero consulta los productos.');
  const value=Number($('prepared').value);
  if(!Number.isInteger(value)||value<0)return mostrarLocal('El total preparado debe ser un entero no negativo.');
  const resultado=await api('/api/v1/inventario/'+id,'PUT',{totalPreparado:value});
  mostrar('PUT /api/v1/inventario/'+id,resultado);
  if(resultado.ok)pintarInventario(resultado.data);
});
$('newKey').addEventListener('click',nuevaClave);
let ultimaSolicitud = null;
let ultimaClavePedido = null;
let carritoPlatos = [];
$('createOrder').addEventListener('click',async()=>{
  if(!pedirSesion())return;
  const id=Number($('orderProduct').value),cantidad=Number($('orderQty').value);
  if(!carritoPlatos.length && (!id||!Number.isInteger(cantidad)||cantidad<1||cantidad>20))
    return mostrarLocal('Selecciona un producto y una cantidad entre 1 y 20.');
  const tipoAtencion=$('orderType').value;
  const mesaId=tipoAtencion==='MESA'?Number($('orderTable').value):null;
  if(tipoAtencion==='MESA' && (!Number.isInteger(mesaId)||mesaId<1))return mostrarLocal('Elige una mesa disponible.');
  const items=carritoPlatos.length?carritoPlatos.map(i=>({...i})):[{productoId:id,cantidad}];
  ultimaSolicitud={items,tipoAtencion,mesaId};
  const clave=$('orderKey').value;
  const result=await api('/api/v1/pedidos/envio','POST',ultimaSolicitud,{'Idempotency-Key':clave});
  mostrar('POST /api/v1/pedidos/envio',result);
  if(result.ok&&result.data.pedidoId){
    $('orderId').value=result.data.pedidoId;
    $('cashPedidoId').value=result.data.pedidoId;
    ultimaClavePedido=clave;
    carritoPlatos=[];pintarCarrito();
    nuevaClave();
    if(window.pedidoEnviadoOperativo)window.pedidoEnviadoOperativo(result.data);
    cargarMesas();consultarDisponibilidad();
    if(window.refrescarOperaciones)window.refrescarOperaciones();
  }
});
$('repeatOrder').addEventListener('click',async()=>{
  if(!pedirSesion())return;
  if(!ultimaSolicitud)return mostrarLocal('Primero registra un pedido.');
  const result=await api('/api/v1/pedidos/envio','POST',ultimaSolicitud,{'Idempotency-Key':ultimaClavePedido||$('orderKey').value});
  mostrar('POST /api/v1/pedidos/envio (reintento)',result);
});
for(const [button,path] of [['confirmOrder','confirmacion'],['cancelOrder','cancelacion']]){
  $(button).addEventListener('click',async()=>{
    if(!pedirSesion())return;
    const id=Number($('orderId').value);
    if(!Number.isInteger(id)||id<1)return mostrarLocal('Indica un ID de pedido válido.');
    const result=await api('/api/v1/pedidos/'+id+'/'+path,'POST');
    mostrar('POST /api/v1/pedidos/'+id+'/'+path,result);
    if(result.ok){cargarMesas();consultarDisponibilidad();}

  });
}
$('reserveForm').addEventListener('submit',async event=>{
  event.preventDefault();
  if(!pedirSesion())return;
  const data={
    cliente:$('reserveName').value.trim(),
    fecha:$('reserveDate').value,
    horaInicio:$('reserveStart').value,
    horaFin:$('reserveEnd').value,
    cantidadPersonas:Number($('reserveCount').value),
    mesa:{id:Number($('mesaId').value)}
  };
  const respuesta=await api('/api/reservas','POST',data);
  mostrar('POST /api/reservas',respuesta);
  if(respuesta.ok&&window.refrescarOperaciones)window.refrescarOperaciones();
});
$('clearResponse').addEventListener('click',()=>{ $('httpStatus').textContent='Sin solicitudes';$('httpStatus').className='response-status';$('responseJson').textContent='Respuesta limpiada';});
$('reserveDate').value=new Date(Date.now()+86400000).toISOString().slice(0,10);
nuevaClave();
$('correo').focus();


/* Módulos operativos Botanero → SOA APF3, conectados a REST real. */
function modoMesa(){
  const esMesa=$('orderType').value==='MESA';
  $('orderTableWrap').hidden=!esMesa;
  $('mesaCards').hidden=!esMesa;
}
$('orderType').addEventListener('change',()=>{
  modoMesa();
  if(window.modalidadPedidoCambiada)window.modalidadPedidoCambiada($('orderType').value);
});
$('orderTable').addEventListener('change',()=>{
  if(window.seleccionarMesaOperativa){
    const id=Number($('orderTable').value);
    window.seleccionarMesaOperativa({id,numero:id,estado:'CONSULTANDO'},false);
  }
});
modoMesa();

async function cargarMesas(){
  if(!token || !['MOZO','CAJA','SUPERVISOR','ADMIN','OPERADOR'].includes(rol))return;
  const resultado=await api('/api/mesas');
  if(!resultado.ok || !Array.isArray(resultado.data))return;
  const seleccionado=window.mesaOperativaSeleccionada
    ? window.mesaOperativaSeleccionada() : $('orderTable').value;
  $('orderTable').replaceChildren();
  $('mesaCards').replaceChildren();
  const mesas=[...resultado.data].sort((a,b)=>a.numero-b.numero);
  for(const m of mesas){
    const option=new Option('Mesa #'+m.numero+' · '+m.estado,String(m.id));
    // Las mesas OCUPADAS siguen siendo seleccionables PARA VER SU COMANDA;
    // el servidor sigue impidiendo crear otra orden en esa misma mesa.
    $('orderTable').add(option);
    const boton=document.createElement('button');
    boton.type='button';boton.className='mesa-button '+(m.estado==='OCUPADA'?'ocupada':'libre');
    boton.dataset.mesaId=String(m.id);
    const elegido=String(m.id)===String(seleccionado)&&$('orderType').value==='MESA';
    if(elegido)boton.classList.add('seleccionada');
    const nombre=document.createElement('strong');nombre.textContent='Mesa #'+m.numero;
    const estado=document.createElement('small');
    estado.textContent=m.estado==='OCUPADA'?'OCUPADA · Ver comanda':'DISPONIBLE · Tomar orden';
    boton.append(nombre,estado);
    boton.addEventListener('click',()=>{
      $('orderType').value='MESA';
      modoMesa();
      $('orderTable').value=String(m.id);
      if(window.seleccionarMesaOperativa)window.seleccionarMesaOperativa(m);
    });
    $('mesaCards').append(boton);
  }
  if(mesas.some(m=>String(m.id)===String(seleccionado)))$('orderTable').value=String(seleccionado);
  if(window.mesasSincronizadas)window.mesasSincronizadas(mesas);
}

async function consultarDisponibilidad(){
  const id=$('orderProduct').value;
  if(!id || !token || rol==='CLIENTE')return;
  const resultado=await api('/api/v1/inventario/'+id);
  if(resultado.ok && Number.isFinite(Number(resultado.data.disponibles))){
    const disponible=Number(resultado.data.disponibles);
    $('orderAvailability').textContent='Porciones disponibles: '+disponible+
      ' · Reservadas: '+resultado.data.reservadas+
      ' · Vendidas: '+resultado.data.vendidas;
    $('createOrder').disabled=disponible<1 && carritoPlatos.length===0;
  }else{
    $('orderAvailability').textContent='Sin datos de inventario o acceso no autorizado.';
    $('createOrder').disabled=true;
  }
}
$('orderProduct').addEventListener('change',consultarDisponibilidad);

$('replenishStock').addEventListener('click',async()=>{
  if(!pedirSesion())return;
  const id=$('stockProduct').value,cantidad=Number($('stockReplenishQty').value);
  if(!id || !Number.isInteger(cantidad)||cantidad<1)return mostrarLocal('Indica un plato y un número positivo de porciones.');
  const result=await api('/api/v1/inventario/'+id+'/reponer','PATCH',{cantidad});
  mostrar('PATCH /api/v1/inventario/'+id+'/reponer',result);
  if(result.ok){
    pintarInventario(result.data);
    consultarDisponibilidad();
    if(window.refrescarOperaciones)window.refrescarOperaciones();
  }
});

function crearBoton(texto,handler,clase='outline'){
  const b=document.createElement('button');b.type='button';b.className=clase;
  b.textContent=texto;b.addEventListener('click',handler);return b;
}
function articuloPedido(p){
  const container=document.createElement('article');container.className='command-card';
  const h=document.createElement('div');h.className='command-heading';
  const titulo=document.createElement('strong');titulo.textContent='Pedido #'+p.pedidoId+' · '+(p.tipoAtencion==='MESA'?'Mesa '+p.mesaId:'Para llevar');
  const estado=document.createElement('span');estado.textContent=p.estado;estado.className='order-status';
  h.append(titulo,estado);container.append(h);
  const ul=document.createElement('ul');ul.className='command-items';
  for(const i of p.items||[]){
    const li=document.createElement('li');
    const descripcion=document.createElement('span');descripcion.textContent=i.cantidad+' × '+i.nombre+' · '+i.estadoPreparacion;
    li.append(descripcion);
    if(['ADMIN','COCINERO','SUPERVISOR'].includes(rol) &&
      p.estado==='CONFIRMADO' && i.estadoPreparacion==='PENDIENTE'){
      li.append(crearBoton('Marcar LISTO',async()=>{
        const x=await api('/api/v1/pedidos/'+p.pedidoId+'/detalles/'+i.detalleId+'/listo','POST');
        mostrar('POST cocina LISTO',x);if(x.ok)cargarComandas();
      },'primary'));
    }
    ul.append(li);
  }
  container.append(ul);return container;
}

async function cargarComandas(){
  if(!pedirSesion())return;
  const resultado=await api('/api/v1/pedidos');
  mostrar('GET /api/v1/pedidos',resultado);
  if(!resultado.ok || !Array.isArray(resultado.data))return;
  const activos=resultado.data.filter(p=>p.estado==='CONFIRMADO' &&
    (p.items||[]).some(i=>i.estadoPreparacion==='PENDIENTE'));
  $('kitchenOrders').replaceChildren();
  if(!activos.length)$('kitchenOrders').textContent='No hay comandas activas.';
  for(const pedido of activos)$('kitchenOrders').append(articuloPedido(pedido));
}
$('refreshKitchen').addEventListener('click',cargarComandas);

async function cargarPedidosCaja(){
  if(!pedirSesion())return;
  const resultado=await api('/api/v1/pedidos');
  mostrar('GET /api/v1/pedidos',resultado);
  if(resultado.ok && Array.isArray(resultado.data)){
    if(window.pintarPedidosCaja)window.pintarPedidosCaja(resultado.data);
  }
}
$('refreshSalesOrders').addEventListener('click',cargarPedidosCaja);

function escapeTicket(s){
  return String(s??'').replace(/[&<>"']/g,ch=>({
    '&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'
  })[ch]);
}
function pintarTicket(ventana,venta){
  if(!ventana)return;
  const lineas=(venta.items||[]).map(item=>
    '<tr><td>'+escapeTicket(item.nombre)+' × '+item.cantidad+'</td><td class="r">'+formatPrice(item.subtotal)+'</td></tr>'
  ).join('');
  const contenido=`<!doctype html><html lang="es"><head><meta charset="utf-8">
  <title>Ticket interno ${escapeTicket(venta.comprobante)}</title>
  <style>body{font:12px/1.5 Consolas,monospace;width:290px;margin:25px auto;color:#222}
  h1{text-align:center;font-size:18px}p{text-align:center}table{width:100%;border-collapse:collapse}
  td{padding:4px;border-bottom:1px dashed #ddd}.r{text-align:right}strong{font-size:15px}
  button{width:100%;margin:15px 0;padding:10px;background:#204c34;color:#fff;border:0}
  @media print{button{display:none}body{margin:0;width:75mm}}</style></head><body>
  <h1>${escapeTicket(venta.restaurante)}</h1>
  <p>Tel.: ${escapeTicket(venta.telefono)}</p>
  <p>COMPROBANTE INTERNO<br>${escapeTicket(venta.comprobante)}<br>${escapeTicket(venta.fechaPago)}</p>
  <p>${escapeTicket(venta.tipoAtencion)} ${venta.mesaId?'· Mesa '+venta.mesaId:''}<br>Pedido #${venta.pedidoId}</p>
  <hr><table>${lineas}</table><hr>
  <table><tr><td>SUBTOTAL PLATOS</td><td class="r">${formatPrice(venta.subtotalBase)}</td></tr>
  <tr><td>RECARGO POS</td><td class="r">${formatPrice(venta.recargoPos)}</td></tr>
  <tr><td><strong>TOTAL COBRADO</strong></td><td class="r"><strong>${formatPrice(venta.total)}</strong></td></tr>
  <tr><td>RECIBIDO</td><td class="r">${formatPrice(venta.montoRecibido)}</td></tr>
  <tr><td>CAMBIO</td><td class="r">${formatPrice(venta.cambio)}</td></tr>
  <tr><td>MEDIO DE PAGO</td><td class="r">${escapeTicket(venta.medioPago)}</td></tr></table>
  <p>${venta.referenciaPago?'Referencia: '+escapeTicket(venta.referenciaPago):''}</p>
  <p>Gracias por tu compra.<br><strong>Documento académico; no válido tributariamente</strong></p>
  <button onclick="window.print()">Imprimir o guardar como PDF</button></body></html>`;
  ventana.document.open();ventana.document.write(contenido);ventana.document.close();
}
async function cobrarPedido(reimpresion=false){
  if(!pedirSesion())return;
  const pedidoId=Number(reimpresion?$('cashReceiptId').value:$('cashPedidoId').value);
  if(!Number.isInteger(pedidoId)||pedidoId<1)
    return mostrarLocal(reimpresion?'Indica el ID de un pedido ya pagado para consultar su ticket.':
      'Primero selecciona un pedido LISTO para cobrar.');
  const medioPago=$('cashMethod').value;
  const referenciaPago=$('cashReference').value.trim();
  const recibido=medioPago==='EFECTIVO'?Number($('cashRecibido').value):null;
  if(!reimpresion && medioPago==='EFECTIVO' && (!Number.isFinite(recibido)||recibido<=0))
    return mostrarLocal('Indica cuánto dinero recibiste en efectivo.');
  if(!reimpresion && medioPago!=='EFECTIVO' && referenciaPago.length<4)
    return mostrarLocal('Indica un código de operación para Yape, POS o Transferencia de al menos 4 caracteres.');
  if(!reimpresion && medioPago==='POS' && !$('cashPosConsent').checked)
    return mostrarLocal('Debes informar al cliente y registrar su aceptación del total POS antes de cobrar.');
  if(!reimpresion && $('cashCharge').disabled)
    return mostrarLocal('El pedido no está habilitado para cobro. Actualiza la lista de pedidos listos.');
  // Apertura sincrónica para evitar bloqueo de ventanas emergentes tras await.
  const ventana=window.open('','_blank','width=410,height=680');
  const url='/api/v1/ventas/pedidos/'+pedidoId+(reimpresion?'':'/cobrar');
  const resultado=await api(url,reimpresion?'GET':'POST',
      reimpresion?null:{montoRecibido:recibido,medioPago,
        referenciaPago:medioPago==='EFECTIVO'?null:referenciaPago,
        recargoInformadoAceptado:medioPago==='POS'&&$('cashPosConsent').checked});
  mostrar((reimpresion?'GET ':'POST ')+url,resultado);
  if(resultado.ok){
    pintarTicket(ventana,resultado.data);
    if(!reimpresion){
      if(window.registrarCobroEnCaja)window.registrarCobroEnCaja(pedidoId);
      cargarMesas();refrescarResumen();cargarPedidosCaja();
      if(window.refrescarOperaciones)window.refrescarOperaciones();
    }
  }else if(ventana){ventana.close();}
}
$('cashCharge').addEventListener('click',()=>cobrarPedido(false));
$('cashReprint').addEventListener('click',()=>cobrarPedido(true));

async function refrescarResumen(){
  if(!['CAJA','SUPERVISOR','ADMIN'].includes(rol))return;
  const r=await api('/api/v1/ventas/resumen');
  if(!r.ok)return mostrar('GET /api/v1/ventas/resumen',r);
  $('salesCount').textContent=String(r.data.ventasRegistradas);
  $('salesTotal').textContent=formatPrice(r.data.totalVentas);
  $('salesToday').textContent=formatPrice(r.data.ventasHoy);
  const chart=$('salesChart');chart.replaceChildren();
  const valores=Object.entries(r.data.ventasPorDia||{}).slice(-7);
  if(!valores.length){chart.textContent='No hay ventas pagadas todavía.';return;}
  const max=Math.max(1,...valores.map(([,m])=>Number(m)));
  for(const [fecha,monto] of valores){
    const grupo=document.createElement('div');grupo.className='bar-group';
    const bar=document.createElement('div');bar.className='bar';
    bar.style.height=(Math.max(4,Number(monto)/max*145))+'px';
    const label=document.createElement('small');label.textContent=fecha.slice(5);
    const total=document.createElement('b');total.textContent=formatPrice(monto);
    grupo.append(total,bar,label);chart.append(grupo);
  }
}

function pintarAdminProductos(){
  const list=$('productAdminList');list.replaceChildren();
  for(const p of listaProductos){
    const row=document.createElement('div');row.className='cash-row';
    const txt=document.createElement('span');
    txt.textContent=p.codigo+' · '+p.nombre+' · '+(Number(p.precio)>0?formatPrice(p.precio):'Precio pendiente')+' · '+(p.disponible?'Activo':'No disponible');
    row.append(txt,crearBoton('Editar',()=>{$('productManageSelect').value=String(p.id);seleccionarProductoAdmin();}));
    list.append(row);
  }
}
function seleccionarProductoAdmin(){
  const p=listaProductos.find(p=>String(p.id)===$('productManageSelect').value);
  $('productCode').value=p?.codigo||'';
  $('productName').value=p?.nombre||'';
  $('productDescription').value=p?.descripcion||'';
  $('productPrice').value=p?.precio??'';
  $('productCategory').value=p?.categoria?.id??'';
  $('productAvailable').value=p?.disponible?'true':'false';
  $('productImageFile').value='';
  const preview=$('productImagePreview');preview.replaceChildren();
  if(p?.imagenUrl){
    const imagen=document.createElement('img');imagen.alt='Fotografía: '+p.nombre;
    imagen.src=p.imagenUrl;imagen.loading='lazy';
    imagen.addEventListener('error',()=>{preview.textContent='Imagen no disponible';});
    preview.append(imagen);
  }else preview.textContent='Selecciona y guarda una foto para mostrarla en la web pública.';
}
$('productManageSelect').addEventListener('change',seleccionarProductoAdmin);
$('productNew').addEventListener('click',()=>{$('productManageSelect').value='';seleccionarProductoAdmin();});
function productoFormulario(){
  return {codigo:$('productCode').value.trim(),nombre:$('productName').value.trim(),
    descripcion:$('productDescription').value.trim(),precio:Number($('productPrice').value),
    disponible:$('productAvailable').value==='true',
    categoria:{id:Number($('productCategory').value)}};
}
$('productSave').addEventListener('click',async()=>{
  const id=$('productManageSelect').value,data=productoFormulario();
  if(!data.codigo||!data.nombre||!data.categoria.id || !Number.isFinite(data.precio)||data.precio<0)
    return mostrarLocal('Código, nombre, categoría y precio deben ser válidos.');
  const path='/api/productos'+(id?'/'+id:'');
  const r=await api(path,id?'PUT':'POST',data);mostrar((id?'PUT ':'POST ')+path,r);
  if(r.ok){
    await consultarCatalogo();
    $('productManageSelect').value=String(r.data.id);
    seleccionarProductoAdmin();
  }
});
$('productImageUpload').addEventListener('click',async()=>{
  if(rol!=='ADMIN')return mostrarLocal('Solo Administración puede agregar fotografías.');
  const id=Number($('productManageSelect').value);
  const archivo=$('productImageFile').files?.[0];
  if(!id)return mostrarLocal('Guarda primero el producto para obtener su ID.');
  if(!archivo||!['image/jpeg','image/png'].includes(archivo.type)||
     archivo.size>1500000)return mostrarLocal('Selecciona foto JPG o PNG, máximo 1.5 MB.');
  const boton=$('productImageUpload');boton.disabled=true;
  try{
    const datos=new FormData();datos.append('archivo',archivo);
    const response=await fetch('/api/productos/'+id+'/imagen',{
      method:'POST',headers:{Authorization:'Bearer '+token},body:datos,cache:'no-store'
    });
    const body=await response.json().catch(()=>({message:'Error de carga'}));
    mostrar('POST /api/productos/'+id+'/imagen',
      {ok:response.ok,status:response.status,data:body});
    if(response.ok){
      await consultarCatalogo();
      $('productManageSelect').value=String(id);seleccionarProductoAdmin();
    }
  }catch(error){mostrarLocal('No se pudo cargar la imagen: '+String(error.message));}
  finally{boton.disabled=false;}
});
$('productDisable').addEventListener('click',async()=>{
  const id=$('productManageSelect').value;
  if(!id)return mostrarLocal('Selecciona un producto que ya exista.');
  const p=listaProductos.find(p=>String(p.id)===id);
  const r=await api('/api/productos/'+id,'PUT',{...p,disponible:false});
  mostrar('PUT /api/productos/'+id,r);if(r.ok)consultarCatalogo();
});
$('productDelete').addEventListener('click',async()=>{
  const id=$('productManageSelect').value;
  if(!id||!confirm('¿Eliminar este producto? Si tiene pedidos históricos, primero desactívalo.'))return;
  const r=await api('/api/productos/'+id,'DELETE');
  mostrar('DELETE /api/productos/'+id,r);
  if(r.ok)consultarCatalogo();
});

function pintarCategorias(){
  $('categorySelect').replaceChildren();$('productCategory').replaceChildren();
  $('categorySelect').add(new Option('Nueva categoría',''));
  for(const c of listaCategorias){
    $('categorySelect').add(new Option(c.nombre,String(c.id)));
    $('productCategory').add(new Option(c.nombre,String(c.id)));
  }
}
$('categorySelect').addEventListener('change',()=>{
  const c=listaCategorias.find(c=>String(c.id)===$('categorySelect').value);
  $('categoryName').value=c?.nombre||'';
  $('categoryType').value=c?.tipo||'COMIDA';
  $('categoryDescription').value=c?.descripcion||'';
});
$('categoryNew').addEventListener('click',()=>{$('categorySelect').value='';$('categoryName').value='';$('categoryDescription').value='';});
$('categorySave').addEventListener('click',async()=>{
  const id=$('categorySelect').value;
  const d={nombre:$('categoryName').value.trim(),tipo:$('categoryType').value,
           descripcion:$('categoryDescription').value.trim()};
  if(!d.nombre)return mostrarLocal('Indica un nombre de categoría.');
  const url='/api/categorias'+(id?'/'+id:'');
  const r=await api(url,id?'PUT':'POST',d);
  mostrar((id?'PUT ':'POST ')+url,r);if(r.ok)consultarCatalogo();
});
$('categoryDelete').addEventListener('click',async()=>{
  const id=$('categorySelect').value;
  if(!id||!confirm('¿Eliminar esta categoría? Solo es posible si no contiene productos.'))return;
  const r=await api('/api/categorias/'+id,'DELETE');
  mostrar('DELETE /api/categorias/'+id,r);if(r.ok)consultarCatalogo();
});
async function cargarCategorias(){
  const r=await api('/api/categorias');
  if(r.ok&&Array.isArray(r.data)){listaCategorias=r.data;pintarCategorias();}
}

function pintarUsuarios(){
  $('userSelect').replaceChildren();$('userSelect').add(new Option('Nuevo usuario',''));
  $('usersList').replaceChildren();
  for(const u of usuariosAdmin){
    $('userSelect').add(new Option(u.nombre+' · '+u.rol+(u.activo?'':' · BAJA'),String(u.id)));
    const row=document.createElement('div');row.className='cash-row';
    const txt=document.createElement('span');
    txt.textContent=u.nombre+' · '+u.correo+' · '+u.rol+(u.activo?'':' · INACTIVO');
    row.append(txt,crearBoton('Editar',()=>{$('userSelect').value=String(u.id);seleccionarUsuario();}));
    $('usersList').append(row);
  }
}
async function cargarUsuarios(){
  if(rol!=='ADMIN')return;
  const r=await api('/api/v1/usuarios');
  if(r.ok&&Array.isArray(r.data)){usuariosAdmin=r.data;pintarUsuarios();}
  else mostrar('GET /api/v1/usuarios',r);
}
function seleccionarUsuario(){
  const u=usuariosAdmin.find(u=>String(u.id)===$('userSelect').value);
  $('userNameInput').value=u?.nombre||'';
  $('userEmailInput').value=u?.correo||'';
  $('userEmailInput').disabled=!!u;
  $('userPhoneInput').value=u?.telefono||'';
  $('userRoleSelect').value=u?.rol||'CLIENTE';
  $('userPasswordInput').value='';
  $('userPasswordInput').disabled=!!u;
}
$('userSelect').addEventListener('change',seleccionarUsuario);
$('userNew').addEventListener('click',()=>{$('userSelect').value='';seleccionarUsuario();});
$('usersRefresh').addEventListener('click',cargarUsuarios);
$('userSave').addEventListener('click',async()=>{
  const id=$('userSelect').value;
  const base={nombre:$('userNameInput').value.trim(),telefono:$('userPhoneInput').value.trim(),
    rol:$('userRoleSelect').value};
  if(!base.nombre)return mostrarLocal('Indica el nombre del usuario.');
  const url='/api/v1/usuarios'+(id?'/'+id:'');
  const data=id?{...base,activo:usuariosAdmin.find(u=>String(u.id)===id).activo}:
    {...base,correo:$('userEmailInput').value.trim(),password:$('userPasswordInput').value};
  if(!id && (!data.correo||data.password.length<8))return mostrarLocal('Un usuario nuevo requiere correo y contraseña de ocho caracteres.');
  const r=await api(url,id?'PUT':'POST',data);
  mostrar((id?'PUT ':'POST ')+url,r);
  if(r.ok){$('userPasswordInput').value='';cargarUsuarios();}
});
for(const [button,activo] of [['userDisable',false],['userEnable',true]]){
  $(button).addEventListener('click',async()=>{
    const id=$('userSelect').value;
    if(!id)return mostrarLocal('Selecciona un usuario existente.');
    const u=usuariosAdmin.find(u=>String(u.id)===id);
    if(!u)return;
    if(!activo&&!confirm('¿Dar de baja al usuario conservando su historial?'))return;
    const url='/api/v1/usuarios/'+id;
    const r=await api(url,activo?'PUT':'DELETE',
        activo?{nombre:u.nombre,telefono:u.telefono,rol:u.rol,activo:true}:null);
    mostrar((activo?'PUT ':'DELETE ')+url,r);
    if(r.ok)cargarUsuarios();
  });
}

async function cargarConfiguracion(){
  const r=await api('/api/v1/configuracion');
  if(!r.ok)return;
  configuracionLocal=r.data;
  $('settingName').value=r.data.nombre||'';
  $('settingPhone').value=r.data.telefono||'';
  $('settingLogo').value=r.data.logoRuta||'';
  $('settingPreview').textContent=(r.data.nombre||'Delicias Peruanas')+' · '+(r.data.telefono||'');
  document.querySelectorAll('.side-brand strong,.brand strong').forEach(x=>{x.textContent=r.data.nombre;});
}
$('settingsSave').addEventListener('click',async()=>{
  const data={nombre:$('settingName').value.trim(),telefono:$('settingPhone').value.trim(),
    logoRuta:$('settingLogo').value.trim()||null};
  const r=await api('/api/v1/configuracion','PUT',data);
  mostrar('PUT /api/v1/configuracion',r);if(r.ok)cargarConfiguracion();
});

/* Carrito temporal para pedido de mesa o para llevar con múltiples platos */
function pintarCarrito(){
  const lugar=$('orderLines');lugar.replaceChildren();
  if(!carritoPlatos.length){
    lugar.textContent='Agrega platos a la orden o registra directamente el producto seleccionado.';
    return;
  }
  let total=0;
  for(const [index,linea] of carritoPlatos.entries()){
    const producto=listaProductos.find(p=>p.id===linea.productoId);
    const row=document.createElement('div');row.className='cash-row';
    const texto=document.createElement('span');
    total+=Number(producto?.precio||0)*linea.cantidad;
    texto.textContent=linea.cantidad+' × '+(producto?.nombre||'Producto #'+linea.productoId)+' · '+formatPrice(Number(producto?.precio||0)*linea.cantidad);
    row.append(texto,crearBoton('Quitar',()=>{carritoPlatos.splice(index,1);pintarCarrito();},'danger'));
    lugar.append(row);
  }
  const cierre=document.createElement('strong');
  cierre.textContent='Total estimado: '+formatPrice(total)+' · '+carritoPlatos.length+' líneas';
  lugar.append(cierre);
}
$('addOrderLine').addEventListener('click',async()=>{
  const id=Number($('orderProduct').value),cantidad=Number($('orderQty').value);
  if(!Number.isInteger(id)||!Number.isInteger(cantidad)||cantidad<1||cantidad>20)
    return mostrarLocal('Producto o cantidad no válidos.');
  const producto=listaProductos.find(p=>p.id===id);
  if(!producto?.disponible || Number(producto.precio)<=0)
    return mostrarLocal('El plato seleccionado no se encuentra disponible.');
  const inventario=await api('/api/v1/inventario/'+id);
  const yaEnCarrito=carritoPlatos.filter(p=>p.productoId===id).reduce((n,p)=>n+p.cantidad,0);
  if(!inventario.ok || Number(inventario.data.disponibles)<cantidad+yaEnCarrito)
    return mostrarLocal('No hay suficientes porciones para añadir esa cantidad al pedido.');
  if(carritoPlatos.length>=20 || cantidad+yaEnCarrito>20)
    return mostrarLocal('Máximo 20 líneas y 20 porciones por producto.');
  carritoPlatos.push({productoId:id,cantidad});
  pintarCarrito();
  $('createOrder').disabled=false;
});
$('clearOrderLines').addEventListener('click',()=>{carritoPlatos=[];pintarCarrito();consultarDisponibilidad();});
pintarCarrito();
