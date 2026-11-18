/* NeuroSync · Departure Studio
 * Editor por objetos; recordatorio nativo con pantalla completa al llegar la hora.
 */
(function(){
 'use strict';
 const api=window.NeuroDepartureAPI,host=document.getElementById('view-bags');
 if(!api||!host)return;
 const old=host.firstElementChild;if(old)old.classList.add('ns-departure-legacy');
 const studio=document.createElement('section');studio.className='ns-departure-studio';
 host.insertBefore(studio,old);
 const overlay=document.createElement('div');overlay.className='ns-departure-overlay';overlay.hidden=true;
 overlay.setAttribute('role','dialog');overlay.setAttribute('aria-modal','true');overlay.setAttribute('aria-label','Revisar lista antes de salir');
 document.body.appendChild(overlay);
 const e=x=>String(x??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
 const dateKey=d=>[d.getFullYear(),String(d.getMonth()+1).padStart(2,'0'),String(d.getDate()).padStart(2,'0')].join('-');
 const clock=d=>String(d.getHours()).padStart(2,'0')+':'+String(d.getMinutes()).padStart(2,'0');
 const $=q=>studio.querySelector(q);
 const presets={
  'Trabajo':['Llaves','Celular','Billetera','Documento','Carné','Agua','Cargador'],
  'Gimnasio':['Agua','Toalla','Candado','Ropa deportiva','Tenis','Audífonos'],
  'Estudio':['Cuaderno','Lapicero','Documento','Computador','Cargador','Agua'],
  'Salida diaria':['Llaves','Celular','Billetera','Documento']
 };
 let form=null,reviewId=null;
 function nowDraft(id){
  const original=id?api.list().find(p=>p.id===id):null;
  const next=new Date(Date.now()+3600000);next.setSeconds(0,0);
  return {id:id||null,place:original?.place||'',owner:api.owner?.()||'',
   items:original?.items?.slice()||[],date:original?.date||dateKey(next),
   time:original?.time||'',repeat:original?.repeat||'none'};
 }
 function notice(x){const n=$('[data-notice]');if(n){n.textContent=x;n.hidden=false}}
 function listCards(){
  const packs=api.list();
  if(!packs.length)return '<div class="ns-ds-empty">🎒<h3>Tu mochila empieza aquí</h3><p>Guarda lo que debes llevar y compruébalo antes de salir.</p></div>';
  return packs.map(p=>{
   const done=(p.items||[]).filter((_,i)=>!!(p.checked||{})[i]).length;
   return '<article class="ns-ds-card"><button type="button" class="ns-ds-card-main" data-review="'+e(p.id)+'">'+
    '<strong>🎒 '+e(p.place)+'</strong><span>'+done+' de '+(p.items||[]).length+' objetos listos'+
    (p.time?' · '+e(p.time):'')+'</span></button><div class="ns-ds-progress"><i style="width:'+
    ((p.items||[]).length?Math.round(done*100/p.items.length):0)+'%"></i></div><div class="ns-ds-row-actions">'+
    '<button type="button" data-review="'+e(p.id)+'">Revisar</button>'+
    '<button type="button" data-edit="'+e(p.id)+'">Editar</button>'+
    '<button type="button" data-delete="'+e(p.id)+'" aria-label="Eliminar '+e(p.place)+'">Eliminar</button>'+
    '</div></article>';
  }).join('');
 }
 function render(){
  if(!api.ready()){studio.replaceChildren();return}
  studio.innerHTML='<header class="ns-ds-heading"><span>NEUROSYNC / SALIDAS</span><h2>¿Llevas todo?</h2>'+
   '<p>Una comprobación sencilla antes de salir.</p></header>'+
   '<button class="ns-ds-create" data-create>+ Nueva lista de salida</button>'+
   '<div class="ns-ds-list">'+listCards()+'</div>'+
   '<p class="ns-ds-footnote">Programa la hora y NeuroSync intentará mostrar el recordatorio en pantalla completa. Android controla si permite abrirlo sobre la pantalla bloqueada.</p>'+
   '<div data-notice role="status" hidden></div>';
  if(reviewId&&!overlay.hidden)renderReview();
 }
 function editorRows(){
  const rows=form?.items||[];
  const target=$('[data-objects]');if(!target)return;
  target.innerHTML=rows.map((value,i)=>'<div class="ns-ds-object-row">'+
   '<span>'+(i+1)+'</span><input data-object="'+i+'" maxlength="100" aria-label="Objeto '+(i+1)+'" value="'+e(value)+'" placeholder="Ej. Llaves">'+
   '<button type="button" data-remove="'+i+'" aria-label="Quitar objeto '+(i+1)+'">×</button></div>').join('');
 }
 function showEditor(id){
  form=nowDraft(id);reviewId=null;
  studio.innerHTML='<header class="ns-ds-heading"><span>LISTA DE SALIDA</span><h2>'+ (id?'Editar lista':'Nueva lista')+
   '</h2><p>Añade cada objeto en su propia casilla.</p></header>'+
   '<form class="ns-ds-editor" id="ns-ds-form">'+
   '<label>¿A dónde vas?<input name="place" required maxlength="90" value="'+e(form.place)+'" placeholder="Trabajo, gimnasio, universidad…"></label>'+
   '<div class="ns-ds-presets"><span>Añadir sugerencias</span>'+
   Object.keys(presets).map(n=>'<button type="button" data-preset="'+e(n)+'">'+e(n)+'</button>').join('')+'</div>'+
   '<div class="ns-ds-section-title">Cosas que llevar <small>'+form.items.length+' objetos</small></div>'+
   '<div data-objects></div>'+
   '<div class="ns-ds-addrow"><input data-new-item maxlength="100" placeholder="Escribe otro objeto" aria-label="Nuevo objeto">'+
   '<button type="button" data-add aria-label="Agregar objeto">+ Agregar</button></div>'+
   '<div class="ns-ds-section-title">¿Cuándo debe aparecer?</div>'+
   '<label class="ns-ds-switch"><input type="checkbox" data-enable-time '+(form.time?'checked':'')+'>'+
   '<span>Mostrar recordatorio a una hora</span></label>'+
   '<div class="ns-ds-schedule" data-schedule '+(!form.time?'hidden':'')+'>'+
   '<label>Fecha inicial<input type="date" name="date" value="'+e(form.date)+'"></label>'+
   '<label>Hora<input type="time" name="time" value="'+e(form.time)+'"></label>'+
   '<label>Repetir<select name="repeat">'+
   [['none','Solo una vez'],['daily','Todos los días'],['weekdays','Lunes a viernes'],['weekends','Fines de semana'],['weekly','Cada semana']]
    .map(([v,t])=>'<option value="'+v+'"'+(v===form.repeat?' selected':'')+'>'+t+'</option>').join('')+'</select></label>'+
   '</div><div class="ns-ds-actions"><button class="ns-ds-primary" type="submit">Guardar lista</button>'+
   '<button type="button" data-cancel>Cancelar</button></div>'+
   '<div data-notice role="status" hidden></div></form>';
  editorRows();
  const input=$('[name=place]');if(!form.place&&input)input.focus();
 }
 function collect(){
  if(!form)return;
  const place=$('[name=place]');if(place)form.place=place.value;
  const enabled=$('[data-enable-time]')?.checked;
  form.time=enabled?($('[name=time]')?.value||''):'';
  form.date=$('[name=date]')?.value||form.date;
  form.repeat=$('[name=repeat]')?.value||'none';
  studio.querySelectorAll('[data-object]').forEach(x=>{form.items[+x.dataset.object]=x.value});
 }
 function addItem(){
  collect();
  const x=$('[data-new-item]'),v=x?.value.trim();
  if(!v){if(x)x.focus();return}
  if(form.items.length>=80){notice('Máximo 80 objetos por lista');return}
  form.items.push(v);editorRows();if(x){x.value='';x.focus();}
 }
 function renderReview(){
  const p=api.list().find(p=>p.id===reviewId);
  if(!p){overlay.hidden=true;reviewId=null;return}
  const count=(p.items||[]).filter((_,i)=>!!(p.checked||{})[i]).length;
  const complete=count===p.items.length;
  overlay.innerHTML='<div class="ns-ds-review-head"><span>NEUROSYNC / REVISIÓN</span>'+
    '<button type="button" data-close-review aria-label="Cerrar revisión">×</button></div>'+
    '<div class="ns-ds-review-main"><div class="ns-ds-bag-icon">🎒</div><h2>'+e(p.place)+'</h2>'+
    '<p>Comprueba cada cosa antes de salir</p><strong>'+count+' / '+p.items.length+'</strong>'+
    '<div class="ns-ds-review-items">'+p.items.map((item,i)=>
     '<label class="ns-ds-review-check"><input type="checkbox" data-mark="'+i+'" '+
     ((p.checked||{})[i]?'checked':'')+'><span>'+e(item)+'</span></label>').join('')+'</div>'+
    '<button class="ns-ds-finish" data-finish '+(!complete?'disabled':'')+'>'+
    (complete?'✓ Todo listo, salir':'Faltan '+(p.items.length-count)+' objetos')+'</button>'+
    '<button type="button" class="ns-ds-safe" data-close-review>Cerrar sin completar</button></div>';
 }
 function showReview(id){
  reviewId=id;
  const p=api.list().find(x=>x.id===id);if(!p)return;
  if(p.time&&api.openNative(id))return;
  overlay.hidden=false;renderReview();
 }
 studio.addEventListener('click',async ev=>{
  const b=ev.target.closest('button');if(!b)return;
  if(b.hasAttribute('data-create')){showEditor(null);return}
  if(b.dataset.edit){showEditor(b.dataset.edit);return}
  if(b.dataset.review){showReview(b.dataset.review);return}
  if(b.dataset.delete){if(await api.remove(b.dataset.delete))render();return}
  if(b.hasAttribute('data-cancel')){form=null;render();return}
  if(b.hasAttribute('data-add')){addItem();return}
  if(b.dataset.remove!==undefined){collect();form.items.splice(+b.dataset.remove,1);editorRows();return}
  if(b.dataset.preset){
   collect();
   for(const item of presets[b.dataset.preset]||[])
    if(form.items.length<80&&!form.items.some(x=>x.toLowerCase()===item.toLowerCase()))form.items.push(item);
   editorRows();
  }
 });
 studio.addEventListener('change',ev=>{
  if(ev.target.matches('[data-enable-time]')){
   const schedule=$('[data-schedule]');if(schedule)schedule.hidden=!ev.target.checked;
   if(ev.target.checked&&!$('[name=time]').value)$('[name=time]').value=clock(new Date(Date.now()+3600000));
  }
 });
 studio.addEventListener('keydown',ev=>{
  if(ev.key==='Enter'&&ev.target.matches('[data-new-item]')){ev.preventDefault();addItem()}
 });
 studio.addEventListener('submit',ev=>{
  if(ev.target.id!=='ns-ds-form')return;
  ev.preventDefault();collect();
  if($('[data-enable-time]')?.checked&&!form.time){notice('Elige la hora del aviso');return}
  const result=api.save(form,form.id);
  if(!result.ok){notice(result.error||'No se pudo guardar');return}
  form=null;render();
 });
 overlay.addEventListener('change',ev=>{
  const input=ev.target.closest('[data-mark]');if(!input||!reviewId)return;
  if(!api.check(reviewId,+input.dataset.mark,input.checked)){
   input.checked=!input.checked;
   return;
  }
  renderReview();
 });
 overlay.addEventListener('click',ev=>{
  const b=ev.target.closest('button');if(!b)return;
  if(b.hasAttribute('data-close-review')){overlay.hidden=true;reviewId=null;render()}
  if(b.hasAttribute('data-finish')){overlay.hidden=true;reviewId=null;render()}
 });
 window.NeuroDepartureStudio={render,create:function(){showEditor(null)}};
 render();
})();
