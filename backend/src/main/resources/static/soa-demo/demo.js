'use strict';
/* Demostrador REST SOA; el JWT solo se mantiene en memoria (nunca en localStorage). */
let token = '';
let rol = '';
let correoSesion = '';
let listaProductos = [];
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
  api:{titulo:'Laboratorio REST',servicio:'JSON',descripcion:'Inspecciona las respuestas de las API.'}
};
const PERFILES = {
  CLIENTE:{titulo:'Cliente',saludo:'¡Qué gusto tenerte aquí!',
    descripcion:'Explora nuestra carta, realiza pedidos y reserva una mesa.',
    permisos:'Puedes consultar la carta, registrar y cancelar tus pedidos pendientes y crear reservas. No puedes configurar inventario ni confirmar pedidos como personal.'},
  MOZO:{titulo:'Mozo',saludo:'Tu área de atención',
    descripcion:'Gestiona las solicitudes de los clientes y confirma sus pedidos.',
    permisos:'Puedes gestionar pedidos y reservas y consultar porciones. No puedes configurar inventario.'},
  SUPERVISOR:{titulo:'Supervisor',saludo:'Control de operaciones',
    descripcion:'Revisa el inventario y el flujo de pedidos del restaurante.',
    permisos:'Puedes consultar inventario, gestionar pedidos y reservas. La configuración de porciones corresponde al ADMIN.'},
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
  window.scrollTo({top:0,behavior:'instant'});
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
  return true;
}
function cerrarSesion(){
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
  for(const id of ['orderProduct','stockProduct']) {
    const element=$(id); element.replaceChildren();
    for(const p of listaProductos){
      const option=document.createElement('option');option.value=String(p.id);
      option.textContent=p.nombre+' · '+formatPrice(p.precio);
      element.append(option);
    }
  }
  $('productsCount').textContent=String(listaProductos.length);
  $('dashboardProductsCount').textContent=String(listaProductos.length);
  const div=$('productList');div.replaceChildren();
  if(!listaProductos.length){div.textContent='No hay platos registrados';return;}
  for(const p of listaProductos){
    const row=document.createElement('div');row.className='product-line';
    const name=document.createElement('strong');name.textContent=p.nombre;
    const price=document.createElement('span');price.textContent=formatPrice(p.precio);
    row.append(name,price);div.append(row);
  }
}
async function consultarCatalogo(){
  const [productos,categorias]=await Promise.all([api('/api/productos'),api('/api/categorias')]);
  mostrar('GET /api/productos',productos);
  if(productos.ok&&Array.isArray(productos.data)){listaProductos=productos.data;listaMenu();}
  if(categorias.ok&&Array.isArray(categorias.data))$('categoriesCount').textContent=String(categorias.data.length);
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
$('getStock').addEventListener('click',async()=>{
  if(!pedirSesion())return;
  const id=$('stockProduct').value;
  if(!id)return mostrarLocal('Primero consulta los productos.');
  mostrar('GET /api/v1/inventario/'+id,await api('/api/v1/inventario/'+id));
});
$('setStock').addEventListener('click',async()=>{
  if(!pedirSesion())return;
  if(rol!=='ADMIN')return mostrarLocal('Solo el ADMIN puede modificar el total de porciones. Usa la sesión de administrador.');
  const id=$('stockProduct').value;
  if(!id)return mostrarLocal('Primero consulta los productos.');
  const value=Number($('prepared').value);
  if(!Number.isInteger(value)||value<0)return mostrarLocal('El total preparado debe ser un entero no negativo.');
  mostrar('PUT /api/v1/inventario/'+id,await api('/api/v1/inventario/'+id,'PUT',{totalPreparado:value}));
});
$('newKey').addEventListener('click',nuevaClave);
let ultimaSolicitud = null;
$('createOrder').addEventListener('click',async()=>{
  if(!pedirSesion())return;
  const id=Number($('orderProduct').value),cantidad=Number($('orderQty').value);
  if(!Number.isInteger(id)||!Number.isInteger(cantidad)||cantidad<1||cantidad>20)return mostrarLocal('Selecciona un producto y una cantidad entre 1 y 20.');
  ultimaSolicitud={items:[{productoId:id,cantidad}]};
  const result=await api('/api/v1/pedidos','POST',ultimaSolicitud,{'Idempotency-Key':$('orderKey').value});
  mostrar('POST /api/v1/pedidos',result);
  if(result.ok&&result.data.pedidoId)$('orderId').value=result.data.pedidoId;
});
$('repeatOrder').addEventListener('click',async()=>{
  if(!pedirSesion())return;
  if(!ultimaSolicitud)return mostrarLocal('Primero registra un pedido.');
  const result=await api('/api/v1/pedidos','POST',ultimaSolicitud,{'Idempotency-Key':$('orderKey').value});
  mostrar('POST /api/v1/pedidos (reintento)',result);
});
for(const [button,path] of [['confirmOrder','confirmacion'],['cancelOrder','cancelacion']]){
  $(button).addEventListener('click',async()=>{
    if(!pedirSesion())return;
    const id=Number($('orderId').value);
    if(!Number.isInteger(id)||id<1)return mostrarLocal('Indica un ID de pedido válido.');
    mostrar('POST /api/v1/pedidos/'+id+'/'+path,await api('/api/v1/pedidos/'+id+'/'+path,'POST'));
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
  mostrar('POST /api/reservas',await api('/api/reservas','POST',data));
});
$('clearResponse').addEventListener('click',()=>{ $('httpStatus').textContent='Sin solicitudes';$('httpStatus').className='response-status';$('responseJson').textContent='Respuesta limpiada';});
$('reserveDate').value=new Date(Date.now()+86400000).toISOString().slice(0,10);
nuevaClave();
$('correo').focus();
