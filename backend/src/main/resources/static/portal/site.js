'use strict';
/*
 * Delicias Peruanas · Portal público APF3
 * Consume únicamente endpoints REST existentes; no realiza pedidos ni modifica stock.
 */
const API = Object.freeze({productos:'/api/productos',categorias:'/api/categorias',registro:'/api/auth/registro'});
const catalogGrid = document.querySelector('#catalogGrid');
const catalogStatus = document.querySelector('#catalogStatus');
const categoryFilters = document.querySelector('#categoryFilters');
const filters = [...document.querySelectorAll('[data-filter]')];
const menuToggle = document.querySelector('#menuToggle');
const mainMenu = document.querySelector('#primaryMenu');
const money = new Intl.NumberFormat('es-PE',{style:'currency',currency:'PEN'});
let products = [];
let categories = [];
let activeGroup = 'todos';
let activeCategory = '';
const normalize = (v) => String(v == null ? '' : v).normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLowerCase().trim();
function categoryName(product){
  const c = product && product.categoria;
  return String((c && (c.nombre || c.descripcion)) || 'Sin categoría');
}
function menuGroup(product){
  const category = normalize(categoryName(product));
  if(/bebida|refresco|jugos?|gaseosa|infusion|chicha|coctel|cerveza|vino/.test(category))return 'bebidas';
  if(/cena|noche|parrilla/.test(category))return 'cenas';
  if(/almuerzo|menu|principal|comida|plato|entrada/.test(category))return 'almuerzos';
  return 'otros';
}
function imageFor(product){
  const name = normalize(product.nombre);
  if(name.includes('lomo saltado'))return {src:'./images/lomo.jpg',icon:'🍛'};
  if(name.includes('rocoto'))return {src:'./images/rocoto.jpg',icon:'🌶️'};
  if(name.includes('ceviche'))return {src:'./images/ceviche.jpg',icon:'🐟'};
  if(name.includes('anticucho'))return {src:'./images/anticucho.jpg',icon:'🍢'};
  if(name.includes('chaufa'))return {src:'./images/chaufa.jpg',icon:'🍚'};
  if(name.includes('chicha morada'))return {src:'./images/chicha.jpg',icon:'🥤'};
  return {src:null,icon:menuGroup(product)==='bebidas'?'🥤':'🍽️'};
}
function setStatus(message,failed=false){
  catalogStatus.textContent=message;
  catalogStatus.classList.toggle('offline',failed);
}
function textNode(tag,className,value){
  const el=document.createElement(tag);
  if(className)el.className=className;
  el.textContent=String(value ?? '');
  return el;
}
function cardFor(p){
  const card=document.createElement('article');card.className='food-card';
  const photo=textNode('div','food-photo','');
  const fallback=textNode('span','food-fallback',imageFor(p).icon);
  fallback.setAttribute('aria-hidden','true');
  photo.append(fallback);
  const asset=imageFor(p);
  if(asset.src){
    const image=document.createElement('img');
    image.src=asset.src;image.alt=String(p.nombre||'Plato');
    image.loading='lazy';image.decoding='async';
    image.addEventListener('load',()=>fallback.remove(),{once:true});
    image.addEventListener('error',()=>image.remove(),{once:true});
    photo.append(image);
  }
  const available=p.disponible===true;
  const badge=textNode('span','food-badge'+(available?'':' unavailable'),available?'Disponible':'No disponible');
  photo.append(badge);
  const body=textNode('div','food-body','');
  body.append(textNode('span','food-category',categoryName(p)));
  body.append(textNode('h3','',p.nombre||'Producto'));
  body.append(textNode('p','',p.descripcion||'Conoce este producto en nuestra carta.'));
  const footer=textNode('div','food-bottom','');
  const price=Number(p.precio);
  footer.append(textNode('strong','',Number.isFinite(price)?money.format(price):'Precio a consultar'));
  footer.append(textNode('small','',available?'En carta':'Agotado en carta'));
  body.append(footer);card.append(photo,body);
  return card;
}
function showMenu(){
  catalogGrid.replaceChildren();
  const shown=products.filter(p=>(activeGroup==='todos'||menuGroup(p)===activeGroup)&&(!activeCategory||normalize(categoryName(p))===activeCategory));
  if(!shown.length){
    const empty=textNode('div','empty-state','');
    empty.append(textNode('strong','','No hay productos para esta selección.'));
    empty.append(textNode('p','','Selecciona “Todos” o comprueba las categorías publicadas en la base de datos.'));
    catalogGrid.append(empty);return;
  }
  shown.forEach(p=>catalogGrid.append(cardFor(p)));
}
function categoryChips(){
  categoryFilters.replaceChildren();
  const availableNames=[...new Set(categories.map(c=>String(c.nombre||'')).filter(Boolean))];
  for(const name of availableNames){
    const chip=textNode('button','category-filter',name);
    chip.type='button';
    chip.classList.toggle('active',normalize(name)===activeCategory);
    chip.setAttribute('aria-pressed',String(normalize(name)===activeCategory));
    chip.addEventListener('click',()=>{
      activeCategory=activeCategory===normalize(name)?'':normalize(name);
      document.querySelectorAll('.category-filter').forEach(b=>{
        const selected=normalize(b.textContent)===activeCategory;
        b.classList.toggle('active',selected);
        b.setAttribute('aria-pressed',String(selected));
      });showMenu();
    });
    categoryFilters.append(chip);
  }
}
async function getJSON(url){
  const controller=new AbortController();
  const timeout=setTimeout(()=>controller.abort(),12000);
  try{
    const response=await fetch(url,{headers:{Accept:'application/json'},cache:'no-store',signal:controller.signal});
    if(!response.ok)throw new Error('HTTP '+response.status);
    return await response.json();
  }finally{clearTimeout(timeout);}
}
async function loadCatalog(){
  setStatus('Consultando API…');
  catalogGrid.replaceChildren(textNode('div','loading-state','Cargando productos desde el backend…'));
  try{
    // Mostrar solamente productos reales del sistema: sin precios o platos inventados.
    const [ps,cs]=await Promise.all([getJSON(API.productos),getJSON(API.categorias)]);
    if(!Array.isArray(ps)||!Array.isArray(cs))throw new Error('Formato JSON inesperado');
    products=ps.filter(p=>p && typeof p==='object');
    categories=cs.filter(c=>c && typeof c==='object');
    categoryChips();showMenu();
    setStatus('Conectado · '+products.length+' productos');
  }catch(err){
    products=[];categories=[];categoryFilters.replaceChildren();
    catalogGrid.replaceChildren();
    const error=textNode('div','empty-state','No se pudo cargar el catálogo. Revisa que Spring Boot esté disponible y vuelve a intentar.');
    catalogGrid.append(error);
    setStatus('Sin conexión al catálogo',true);
    console.error('Error al consultar el catálogo:',err);
  }
}
filters.forEach(button=>button.addEventListener('click',()=>{
  activeGroup=button.dataset.filter;
  activeCategory='';
  filters.forEach(b=>{const active=b===button;b.classList.toggle('active',active);b.setAttribute('aria-pressed',String(active));});
  categoryChips();showMenu();
}));
document.querySelector('#reloadCatalog').addEventListener('click',loadCatalog);
menuToggle.addEventListener('click',()=>{
  const open=mainMenu.classList.toggle('open');
  menuToggle.setAttribute('aria-expanded',String(open));
});
mainMenu.querySelectorAll('a').forEach(link=>link.addEventListener('click',()=>{
  mainMenu.classList.remove('open');menuToggle.setAttribute('aria-expanded','false');
}));
const registerForm=document.querySelector('#registerForm');
const registerMessage=document.querySelector('#registerMessage');
const submitRegister=document.querySelector('#submitRegister');
registerForm.addEventListener('submit',async(event)=>{
  event.preventDefault();
  registerMessage.textContent='';registerMessage.classList.remove('error');
  if(!registerForm.reportValidity())return;
  const values=Object.fromEntries(new FormData(registerForm).entries());
  const payload={nombre:values.nombre.trim(),correo:values.correo.trim().toLowerCase(),telefono:values.telefono.trim(),password:values.password};
  if(payload.nombre.length<2){registerMessage.textContent='Ingresa tu nombre completo.';registerMessage.classList.add('error');return;}
  submitRegister.disabled=true;submitRegister.textContent='Registrando…';
  try{
    const controller=new AbortController();
    const timeout=setTimeout(()=>controller.abort(),12000);
    let response;
    try{
      response=await fetch(API.registro,{method:'POST',headers:{'Content-Type':'application/json',Accept:'application/json'},body:JSON.stringify(payload),signal:controller.signal});
    }finally{clearTimeout(timeout);}
    if(response.status===201){
      const registered=await response.json();
      registerForm.reset();
      registerMessage.textContent='Cuenta de '+(registered.nombre||'cliente')+' creada correctamente. Ya puedes iniciar sesión.';
    }else if(response.status===409){
      registerMessage.textContent='El correo ya se encuentra registrado. Usa otro o inicia sesión.';registerMessage.classList.add('error');
    }else if(response.status===400){
      registerMessage.textContent='Revisa el nombre, correo, teléfono y contraseña. La contraseña debe tener entre 8 y 72 caracteres.';registerMessage.classList.add('error');
    }else{
      registerMessage.textContent='No se pudo completar el registro (HTTP '+response.status+').';registerMessage.classList.add('error');
    }
  }catch(err){
    registerMessage.textContent='No se pudo conectar con el servicio de registro. Inténtalo nuevamente.';registerMessage.classList.add('error');
    console.error('Fallo de registro:',err);
  }finally{submitRegister.disabled=false;submitRegister.textContent='Registrar cliente';}
});
loadCatalog();