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
let activeMode = 'completa';
const legacyCatalog = (window.DELICIAS_MENU_ORIGINAL || []).map(p => ({...p, __legacy:true, __grupo:p.grupo, __image:p.imagen, categoria:{nombre:p.grupo === 'almuerzos'?'Almuerzos':p.grupo==='cenas'?'Cenas':'Bebidas'}}));
const menuSearch = document.querySelector('#menuSearch');
const modeButtons = [...document.querySelectorAll('[data-mode]')];
const normalize = (v) => String(v == null ? '' : v).normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLowerCase().trim();
function categoryName(product){
  const c = product && product.categoria;
  return String((c && (c.nombre || c.descripcion)) || 'Sin categoría');
}
function menuGroup(product){
  if(product.__grupo)return product.__grupo;
  const name = normalize(product.nombre);
  const legacyMatch = legacyCatalog.find(p=>normalize(p.nombre)===name);
  if(legacyMatch)return legacyMatch.__grupo;
  const category = normalize(categoryName(product));
  const tipo=normalize(product.categoria && product.categoria.tipo);
  if(tipo==='bebida'||/bebida|refresco|jugos?|gaseosa|infusion|chicha|coctel/.test(category))return 'bebidas';
  if(/cena|noche|parrilla/.test(category))return 'cenas';
  if(/almuerzo|menu|principal|comida|plato|entrada|fondo/.test(category))return 'almuerzos';
  return 'otros';
}
function imageFor(product){
  const name = normalize(product.nombre);
  const match = legacyCatalog.find(p => normalize(p.nombre)===name);
  let filename = product.__image || (match && match.__image);
  if(!filename){
    const hints = [
      [/lomo saltado/,'lomo.jpg'],[/rocoto/,'rocoto.jpg'],[/ceviche/,'ceviche.jpg'],
      [/anticucho/,'anticucho.jpg'],[/chaufa/,'chaufa.jpg'],[/chicha morada/,'chicha.jpg'],
      [/huancaina/,'papa.jpg'],[/juane/,'juane.jpg'],[/caldo/,'caldo.jpg']
    ];
    filename=(hints.find(([regex])=>regex.test(name))||[])[1];
  }
  return {src:filename?'./images/'+filename:null, icon: menuGroup(product)==='bebidas'?'🥤':'🍽️'};
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
  const reference=Boolean(p.__legacy);
  const card=document.createElement('article');card.className='food-card'+(reference?' food-card--reference':'');
  const photo=textNode('div','food-photo','');
  const asset=imageFor(p);
  const fallback=textNode('span','food-fallback',asset.icon);
  fallback.setAttribute('aria-hidden','true');photo.append(fallback);
  if(asset.src){
    const img=document.createElement('img');img.src=asset.src;img.alt=String(p.nombre||'Plato tradicional');
    img.loading='lazy';img.decoding='async';
    img.addEventListener('load',()=>fallback.remove(),{once:true});
    img.addEventListener('error',()=>img.remove(),{once:true});photo.append(img);
  }
  const available=p.disponible===true;
  const badge=textNode('span','food-badge'+(reference?' reference':(!available?' unavailable':'')),reference?'Carta original':(available?'En sistema':'No disponible'));
  photo.append(badge);
  const body=textNode('div','food-body','');
  const cat=reference?(p.__grupo==='almuerzos'?'Almuerzo':p.__grupo==='cenas'?'Cena':'Bebida'):categoryName(p);
  body.append(textNode('span','food-category',cat));
  body.append(textNode('h3','',p.nombre||'Producto'));
  body.append(textNode('p','',p.descripcion||'Conoce este producto en nuestra carta.'));
  const footer=textNode('div','food-bottom','');
  const raw=p.precio==null?'':String(p.precio);
  const price=raw===''?NaN:Number(raw);
  footer.append(textNode('strong','',Number.isFinite(price)&&price>0?money.format(price):'Precio por confirmar'));
  const tag=reference?'Precio histórico · Referencial':(available?'Precio publicado por la API':'Producto no disponible');
  footer.append(textNode('small','',tag));
  body.append(footer);card.append(photo,body);
  return card;
}
function showMenu(){
  catalogGrid.replaceChildren();
  const names=new Set(products.map(p=>normalize(p.nombre)).filter(Boolean));
  const combined=activeMode==='sistema'?products:[...products,...legacyCatalog.filter(p=>!names.has(normalize(p.nombre)))];
  const query=normalize(menuSearch.value);
  const shown=combined.filter(p=>
    (activeGroup==='todos'||menuGroup(p)===activeGroup)
    &&(!activeCategory||normalize(categoryName(p))===activeCategory)
    &&(!query||normalize([p.nombre,p.descripcion,categoryName(p)].join(' ')).includes(query))
  );
  if(!shown.length){
    const empty=textNode('div','empty-state','');
    empty.append(textNode('strong','','No encontramos productos con esos filtros.'));
    empty.append(textNode('p','','Prueba otra categoría o selecciona “Carta completa”.'));
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
  setStatus('Consultando catálogo REST…');
  catalogGrid.replaceChildren(textNode('div','loading-state','Cargando productos desde Spring Boot…'));
  try{
    const [ps,cs]=await Promise.all([getJSON(API.productos),getJSON(API.categorias)]);
    if(!Array.isArray(ps)||!Array.isArray(cs))throw new Error('Formato JSON inesperado');
    products=ps.filter(p=>p && typeof p==='object');
    categories=cs.filter(c=>c && typeof c==='object');
    categoryChips();showMenu();
    setStatus('API conectada · '+products.length+' productos registrados');
  }catch(err){
    products=[];categories=[];categoryChips();showMenu();
    setStatus('API desconectada · Solo referencias históricas',true);
    console.error('No se pudo consultar el catálogo REST:',err);
  }
}
modeButtons.forEach(button=>button.addEventListener('click',()=>{
  activeMode=button.dataset.mode;
  modeButtons.forEach(b=>{const selected=b===button;b.classList.toggle('active',selected);b.setAttribute('aria-pressed',String(selected));});
  activeCategory='';categoryChips();showMenu();
}));
menuSearch.addEventListener('input',showMenu);

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
async function cargarDatosLocal(){
  try {
    const config=await getJSON('/api/v1/configuracion');
    if(!config || !config.nombre)return;
    document.querySelectorAll('.brand').forEach(e=>{
      const a=e.querySelector('strong');if(a)a.textContent=config.nombre;
    });
    const telefono=document.querySelector('#contactPhone');
    if(telefono && config.telefono)telefono.textContent='Teléfono de contacto: '+config.telefono;
    if(config.logoRuta && /^\/portal\/images\/[a-zA-Z0-9_-]+\.(png|jpg|jpeg|webp)$/.test(config.logoRuta)){
      document.querySelectorAll('.brand-mark').forEach(marca=>{
        const imagen=document.createElement('img');
        imagen.src=config.logoRuta;imagen.alt='';imagen.loading='lazy';
        imagen.style.cssText='width:100%;height:100%;object-fit:cover;border-radius:50%';
        imagen.addEventListener('error',()=>{imagen.remove();marca.textContent='DP';},{once:true});
        marca.textContent='';marca.append(imagen);
      });
    }
  }catch(err){console.warn('No se pudo obtener la configuración pública del restaurante',err);}
}
cargarDatosLocal();