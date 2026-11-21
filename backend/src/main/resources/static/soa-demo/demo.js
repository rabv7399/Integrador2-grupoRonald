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
}
function mostrarLocal(mensaje) {
  $('httpStatus').textContent = 'Sin solicitud HTTP';
  $('httpStatus').className = 'response-status fail';
  $('responseJson').textContent = mensaje;
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
function actualizarSesion(){
  $('session').textContent=token?'Sesión: '+correoSesion+' · Rol: '+rol+' · JWT activo':'Sin sesión · las operaciones protegidas requieren JWT';
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
  // Solo mostrar en pantalla rol/correo; nunca el JWT devuelto.
  const resultado=await api('/api/auth/login','POST',{
    correo:$('correo').value.trim().toLowerCase(),password:$('contrasena').value
  });
  $('contrasena').value='';
  if(resultado.ok&&resultado.data.token){
    token=resultado.data.token;rol=resultado.data.rol;correoSesion=resultado.data.correo;
    actualizarSesion();
    mostrar('POST /api/auth/login', {ok:true,status:resultado.status,data:{mensaje:'Autenticación correcta; token JWT recibido y conservado solo en memoria',rol,correo:correoSesion}});
  }else{
    token='';rol='';correoSesion='';actualizarSesion();mostrar('POST /api/auth/login',resultado);
  }
});
$('logout').addEventListener('click',()=>{token='';rol='';correoSesion='';actualizarSesion();mostrarLocal('Sesión finalizada localmente.');});
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
nuevaClave();consultarCatalogo();
