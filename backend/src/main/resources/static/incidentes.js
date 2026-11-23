(() => {
 const base = location.origin; // Misma API que publica esta interfaz
 let token = "";
 const $ = id => document.getElementById(id);
 const mensaje = (id, valor, tipo = "") => {
  const e=$(id);e.textContent=valor;e.className="mensaje "+tipo;
 };
 const json = async r => {
  const t=await r.text();
  if(!t)return null;
  try{return JSON.parse(t)}catch{return {message:t}}
 };
 const headers = () => ({Authorization:"Bearer "+token,"Content-Type":"application/json"});
 function renderTickets(datos){
  const cuerpo=$("tickets");cuerpo.replaceChildren();
  if(!Array.isArray(datos)||!datos.length){
   const tr=document.createElement("tr"),td=document.createElement("td");
   td.colSpan=7;td.textContent="No se registran incidentes.";tr.append(td);cuerpo.append(tr);return;
  }
  datos.forEach(t=>{
   const tr=document.createElement("tr");
   const fecha=t.fechaRegistro ? new Date(t.fechaRegistro).toLocaleString("es-PE") : "—";
   [t.codigo,fecha,t.titulo,t.moduloAfectado,t.prioridad,t.estado,t.usuarioReporta].forEach(v=>{
    const td=document.createElement("td");td.textContent=v??"—";tr.append(td);
   });
   cuerpo.append(tr);
  });
 }
 async function cargar(){
  mensaje("mensajeListado","Consultando tickets...");
  try {
   const r=await fetch(base+"/api/incidentes",{headers:headers()});
   const d=await json(r);
   if(!r.ok)throw new Error(d?.message||"Error HTTP "+r.status);
   renderTickets(d);mensaje("mensajeListado","Listado actualizado.","exito");
  }catch(e){mensaje("mensajeListado",e.message,"error")}
 }
 $("login").addEventListener("submit",async e=>{
  e.preventDefault();mensaje("mensajeAcceso","Autenticando...");
  try{
   const r=await fetch(base+"/api/auth/login",{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify({correo:$("correo").value.trim(),password:$("clave").value})});
   const d=await json(r);
   if(!r.ok||!d?.token)throw new Error(d?.message||"No fue posible iniciar sesión");
   if(!["ADMIN","OPERADOR"].includes(d.rol))throw new Error("Solo ADMIN u OPERADOR pueden gestionar incidentes.");
   token=d.token;$("sesion").textContent=(d.correo||"Operador")+" · "+d.rol;
   $("acceso").hidden=true;$("operacion").hidden=false;$("clave").value="";
   await cargar();
  }catch(err){token="";mensaje("mensajeAcceso",err.message,"error")}
 });
 $("formTicket").addEventListener("submit",async e=>{
  e.preventDefault();const boton=e.submitter;boton.disabled=true;
  mensaje("mensajeTicket","Registrando incidente...");
  const payload={titulo:$("titulo").value.trim(),moduloAfectado:$("modulo").value,
   descripcion:$("descripcion").value.trim(),prioridad:$("prioridad").value};
  try{
   const r=await fetch(base+"/api/incidentes",{method:"POST",headers:headers(),body:JSON.stringify(payload)});
   const d=await json(r);
   if(!r.ok)throw new Error(d?.message||"Error HTTP "+r.status);
   mensaje("mensajeTicket","Ticket "+d.codigo+" registrado. Estado: "+d.estado+".","exito");
   $("formTicket").reset();await cargar();
  }catch(err){mensaje("mensajeTicket",err.message,"error")}
  finally{boton.disabled=false}
 });
 $("actualizar").addEventListener("click",cargar);
 $("salir").addEventListener("click",()=>{
  token="";$("operacion").hidden=true;$("acceso").hidden=false;
  $("login").reset();mensaje("mensajeAcceso","Sesión cerrada.");
 });
})();