/* NeuroSync · Task Studio
 * Hábitos y tareas usan NeuroTasksAPI: no hay segunda base de datos.
 */
(function(){
'use strict';
const api=window.NeuroTasksAPI;
const tasksHost=document.getElementById('view-tasks'),progressHost=document.getElementById('view-habits');
if(!api||!tasksHost||!progressHost)return;
const CATALOG=[
 ['Salud',[['💧','Beber agua','daily'],['🪥','Cepillarme los dientes','daily'],['🚿','Ducharme','daily'],['🌙','Acostarme temprano','daily'],['☀','Levantarme temprano','daily'],['💊','Tomar medicamentos','daily'],['☕','Tomar un descanso','daily'],['🍎','Comer frutas','daily']]],
 ['Vida diaria',[['📚','Estudiar','daily'],['🍳','Cocinar','daily'],['🧹','Limpiar la casa','weekly'],['🧴','Cuidado de la piel','daily'],['💰','Registrar gastos','daily'],['🛒','Hacer compras','weekly'],['🛏','Tender la cama','daily'],['🐾','Alimentar mascotas','daily'],['📖','Leer','daily'],['🌐','Aprender un idioma','daily'],['🎸','Practicar un instrumento','weekly'],['💬','Contactar a mi familia','weekly']]],
 ['Movimiento',[['🏃','Hacer ejercicio','weekly'],['🚶','Caminar','daily'],['🧘','Estirar','daily'],['🏊','Nadar','weekly'],['🧘','Practicar yoga','weekly'],['🚴','Montar bicicleta','weekly']]],
 ['Mente',[['🌿','Meditación','daily'],['✍','Escribir un diario','daily'],['🧠','Revisar mis objetivos','daily'],['💗','Practicar gratitud','daily'],['😊','Sonreír y cuidar mi ánimo','daily']]],
 ['Reducir',[['🍬','Reducir el azúcar','daily'],['📵','Reducir tiempo de pantalla','daily'],['🎮','Limitar videojuegos','daily']]]
];
const PRESETS=[
 {title:'Lista de compras',category:'Personal',steps:['Frutas','Verduras','Leche','Pan','Arroz','Huevos']},
 {title:'Preparación para viajar',category:'Viajes',steps:['Revisar documentos','Confirmar transporte','Confirmar alojamiento','Organizar equipaje','Preparar medicamentos']},
 {title:'Preparar el trabajo',category:'Trabajo',steps:['Revisar agenda','Preparar documentos','Organizar materiales','Comprobar llaves']},
 {title:'Rutina de estudio',category:'Estudio',steps:['Definir objetivo','Preparar material','Concentrarme 25 minutos','Repasar','Anotar dudas']},
 {title:'Antes de dormir',category:'Salud',steps:['Dejar listo el día siguiente','Poner alarma','Preparar ropa','Apartar el teléfono']}
];
const escape=x=>String(x??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const day=d=>[d.getFullYear(),String(d.getMonth()+1).padStart(2,'0'),String(d.getDate()).padStart(2,'0')].join('-');
const app=document.createElement('section');app.className='ns-task-studio';
const stats=document.createElement('section');stats.className='ns-task-stats';
tasksHost.insertBefore(app,tasksHost.firstChild);progressHost.insertBefore(stats,progressHost.firstChild);
let tab='all',category='Todas',sheet=false,selectedPreset=null,creating=false,sendTaskId=null;
function items(){return api.list()}
function isDone(t,date){
 return Array.isArray(t.doneDates)&&t.doneDates.includes(t.repeat==='none'?t.date:date);
}
function summary(){
 const t=items(),today=api.today();
 const completed=t.filter(x=>isDone(x,today)).length;
 return {completed,pending:t.filter(x=>!isDone(x,today)).length,total:t.length};
}
function chips(){
 const categories=['Todas','Personal','Trabajo','Estudio','Salud','Movimiento','Vida diaria','Mente','Reducir'];
 return '<div class="ns-ts-chips" role="group" aria-label="Filtrar tareas">'+categories.map(c=>
  '<button type="button" data-category="'+escape(c)+'"'+(category===c?' aria-pressed="true"':'')+'>'+escape(c)+'</button>').join('')+'</div>';
}
function taskRows(){
 const today=api.today();
 const rows=items().filter(t=>category==='Todas'||(t.category||'Personal')===category)
 .filter(t=>tab==='all'||(tab==='pending'?!isDone(t,today):isDone(t,today)));
 return rows.length?rows.slice(0,12).map(t=>
  '<div class="ns-ts-row"><button type="button" class="ns-ts-check" aria-label="Cambiar estado de '+escape(t.title)+'" data-check="'+escape(t.id)+'">'+
  (isDone(t,today)?'✓':'')+'</button><div><strong>'+escape(t.title)+'</strong><small>'+escape(t.category||'Personal')+' · '+
  escape(t.repeat==='daily'?'Diario':t.repeat==='weekly'?'Semanal':'Una vez')+
  (t.steps?.length?' · '+t.steps.length+' pasos':'')+
  (t.owner==='todos'?' · Compartida con la familia':'')+
  (t.sentBy?' · Enviada por '+escape(t.sentBy):'')+'</small></div>'+
  '<button type="button" class="ns-ts-share" data-send="'+escape(t.id)+'" aria-label="Enviar '+escape(t.title)+' a otra persona">Enviar</button>'+'</div>').join(''):
  '<div class="ns-ts-blank"><div class="ns-ts-icon">☑</div><p>Sin tareas en esta categoría.</p><small>Usa «+ Añadir» para comenzar.</small></div>';
}
function renderTasks(){
 if(!api.ready()){app.replaceChildren();return}
 const summaryData=summary();
 app.innerHTML='<div class="ns-ts-head"><div><span class="ns-ts-eyebrow">MI ORGANIZADOR</span><h2>Tareas y hábitos</h2></div>'+
  '<button type="button" class="ns-ts-add" data-action="quick" aria-label="Añadir tarea">+</button></div>'+
  '<div class="ns-ts-metrics"><div><strong>'+summaryData.completed+'</strong><span>Completadas</span></div>'+
  '<div><strong>'+summaryData.pending+'</strong><span>Pendientes</span></div></div>'+
  chips()+
  '<div class="ns-ts-tabs"><button data-tab="all" '+(tab==='all'?'aria-pressed="true"':'')+'>Todas</button>'+
  '<button data-tab="pending" '+(tab==='pending'?'aria-pressed="true"':'')+'>Pendientes</button>'+
  '<button data-tab="done" '+(tab==='done'?'aria-pressed="true"':'')+'>Completadas</button></div>'+
  '<div class="ns-ts-items">'+taskRows()+'</div>'+
  '<div class="ns-ts-toolbar"><button data-action="habits">+ Catálogo de hábitos</button>'+
  '<button data-action="templates">Plantillas de tareas</button></div>'+
  '<div class="ns-ts-pane" id="ns-ts-pane"></div>';
 if(sheet)renderPane();
}
function recipientOptions(includeSelf){
 const contacts=api.contacts();
 return (includeSelf?'<option value="">Solo para mí</option>':'<option value="">Selecciona a alguien</option>')+
  contacts.map(u=>'<option value="'+escape(u)+'">'+escape(u)+'</option>').join('');
}
function renderPane(){
 const pane=app.querySelector('#ns-ts-pane');if(!pane)return;
 let markup='';
 if(sheet==='habits'){
  markup='<div class="ns-ts-pane-head"><h3>Lista de hábitos</h3><button type="button" data-action="close" aria-label="Cerrar">×</button></div>'+
   '<p>Elige un hábito y agrégalo a tus tareas. Podrás editarlo después.</p>'+
   CATALOG.map(([group,list])=>'<section class="ns-ts-group"><h4>'+escape(group)+'</h4>'+
    list.map(([icon,title,repeat])=>
    '<button type="button" class="ns-ts-catalog-row" data-habit="'+escape(title)+'" data-repeat="'+repeat+'" data-group="'+escape(group)+'">'+
    '<span>'+icon+'</span><strong>'+escape(title)+'</strong><span>+</span></button>').join('')+'</section>').join('');
 }else if(sheet==='templates'){
  const templates=api.templates(),all=PRESETS.map((x,i)=>Object.assign({id:'preset_'+i,preset:true},x))
   .concat(templates);
  markup='<div class="ns-ts-pane-head"><h3>Plantillas de tareas</h3><button type="button" data-action="close" aria-label="Cerrar">×</button></div>'+
   '<p>Utiliza una lista predefinida o guarda tu propia plantilla.</p>'+
   all.map(t=>'<article class="ns-ts-template"><div><strong>'+escape(t.title)+'</strong>'+
    '<small>'+t.steps.length+' subtareas · '+escape(t.category)+'</small>'+
    '<div>'+t.steps.slice(0,3).map((step,i)=>'<span>'+escape(step)+(i<Math.min(t.steps.length,3)-1?' · ':'')+'</span>').join('')+'</div></div>'+
    '<button type="button" data-use="'+escape(t.id)+'">Usar</button></article>').join('')+
   '<form id="ns-ts-template-form"><h4>Nueva plantilla</h4>'+
   '<input required name="title" maxlength="100" placeholder="Nombre de la plantilla">'+
   '<textarea required name="steps" placeholder="Un paso por línea (máximo 20)"></textarea>'+
   '<button type="submit">Guardar plantilla</button></form>';
 }else if(sheet==='quick'){
  markup='<div class="ns-ts-pane-head"><h3>Nueva tarea</h3><button type="button" data-action="close" aria-label="Cerrar">×</button></div>'+
   '<form id="ns-ts-quick-form"><input name="title" required maxlength="100" autocomplete="off" placeholder="¿Qué necesitas hacer?">'+
   '<div class="ns-ts-inline"><label>Categoría<select name="category"><option>Personal</option><option>Trabajo</option><option>Estudio</option><option>Salud</option><option>Movimiento</option></select></label>'+
   '<label>Repetición<select name="repeat"><option value="none">Una vez</option><option value="daily">Diaria</option><option value="weekly">Semanal</option></select></label></div>'+
   '<label>Enviar a<select name="recipient">'+recipientOptions(true)+'</select></label>'+
   '<button type="submit">Guardar o enviar</button><button type="button" data-action="full">Opciones avanzadas</button></form>';
 }else if(sheet==='send'){
  const existing=items().find(t=>t.id===sendTaskId);
  markup='<div class="ns-ts-pane-head"><h3>Enviar tarea</h3><button type="button" data-action="close" aria-label="Cerrar">×</button></div>'+
   (existing?'<p>'+escape(existing.title)+'</p><form id="ns-ts-send-form"><label>Destinatario<select name="recipient" required>'+recipientOptions(false)+'</select></label><button type="submit">Enviar tarea</button></form>':
   '<p>No se encontró la tarea.</p>');
 }
 pane.innerHTML=markup;
 if(sheet==='quick')pane.querySelector('[name=title]')?.focus();
}
function renderStats(){
 if(!api.ready()){stats.replaceChildren();return}
 const tasks=items(),now=new Date(),calendar=[];
 for(let i=83;i>=0;i--){
  const d=new Date(now.getFullYear(),now.getMonth(),now.getDate()-i),key=day(d);
  const completed=tasks.filter(t=>Array.isArray(t.doneDates)&&t.doneDates.includes(key)).length;
  calendar.push({date:key,count:completed});
 }
 const year=now.getFullYear(),totalDone=tasks.reduce((n,t)=>n+(t.doneDates||[]).filter(x=>x.startsWith(year+'-')).length,0);
 const week=calendar.slice(-7),weekTotal=week.reduce((n,x)=>n+x.count,0),best=Math.max(1,...week.map(x=>x.count));
 const focus=api.focus(),since=day(new Date(now.getFullYear(),now.getMonth(),now.getDate()-6));
 const focusPerDay=week.map(x=>Math.round(focus.filter(f=>f.day===x.date).reduce((n,f)=>n+Math.max(0,Number(f.ms)||0),0)/60000));
 const focusMax=Math.max(1,...focusPerDay);
 stats.innerHTML='<div class="ns-ts-stat-head"><span class="ns-ts-eyebrow">ESTADÍSTICAS PERSONALES</span><h2>Mi progreso</h2></div>'+
 '<div class="ns-ts-metrics"><div><strong>'+totalDone+'</strong><span>Tareas completadas en '+year+'</span></div>'+
 '<div><strong>'+weekTotal+'</strong><span>Últimos 7 días</span></div></div>'+
 '<div class="ns-ts-report"><h3>Mapa de actividad · 12 semanas</h3><div class="ns-ts-heatmap">'+
 calendar.map(x=>'<span class="ns-ts-cell" data-level="'+Math.min(4,x.count)+'" title="'+escape(x.date)+': '+x.count+' tarea(s)"></span>').join('')+
 '</div><p>Más intensidad significa más tareas completadas. Los días sin registro aparecen claros.</p></div>'+
 '<div class="ns-ts-report"><h3>Tareas completadas · 7 días</h3><div class="ns-ts-bars">'+
 week.map(x=>'<div><b>'+x.count+'</b><span><i style="height:'+Math.round(x.count/best*100)+'%"></i></span><small>'+
 escape(new Date(x.date+'T12:00:00').toLocaleDateString('es-CO',{weekday:'short'}))+'</small></div>').join('')+
 '</div></div>'+
 '<div class="ns-ts-report"><h3>Enfoque · 7 días</h3><div class="ns-ts-bars">'+
 week.map((x,i)=>'<div><b>'+focusPerDay[i]+'</b><span><i style="height:'+Math.round(focusPerDay[i]/focusMax*100)+'%"></i></span>'+
 '<small>'+escape(new Date(x.date+'T12:00:00').toLocaleDateString('es-CO',{weekday:'short'}))+'</small></div>').join('')+
 '</div><p>Minutos registrados en Pomodoro, sin estimaciones.</p></div>';
}
app.addEventListener('click',event=>{
 const el=event.target.closest('button');if(!el)return;
 if(el.dataset.category){category=el.dataset.category;renderTasks();return}
 if(el.dataset.tab){tab=el.dataset.tab;renderTasks();return}
 if(el.dataset.check){if(api.toggleToday(el.dataset.check))renderTasks();return}
 if(el.dataset.send){
  sendTaskId=el.dataset.send;sheet='send';renderTasks();return;
 }
 if(el.dataset.habit){
  const repeat=el.dataset.repeat,group=el.dataset.group;
  if(api.create({title:el.dataset.habit,category:group,repeat})){renderTasks()}
  return;
 }
 if(el.dataset.use){
  const preset=PRESETS.find((_,i)=>el.dataset.use==='preset_'+i);
  if(preset){api.create({title:preset.title,category:preset.category,steps:preset.steps});}
  else api.useTemplate(el.dataset.use);
  sheet=false;renderTasks();return;
 }
 const action=el.dataset.action;
 if(['habits','templates','quick'].includes(action)){sheet=action;renderTasks()}
 else if(action==='close'){sheet=false;renderTasks()}
 else if(action==='full'){api.openExistingEditor()}
});
app.addEventListener('submit',event=>{
 const form=event.target;
 if(form.id==='ns-ts-quick-form'){
  event.preventDefault();const v=Object.fromEntries(new FormData(form).entries());
  if(v.recipient){
   // Misma clave de envío en los reintentos del formulario abierto.
   const requestId=form.dataset.requestId||(form.dataset.requestId='send_'+Date.now()+'_'+Math.random().toString(36).slice(2));
   const result=api.sendTo(v.recipient,{id:requestId,title:v.title,category:v.category,repeat:v.repeat});
   if(result.ok){sheet=false;renderTasks();}
   else {let note=form.querySelector('[data-send-error]');if(!note){note=document.createElement('p');note.setAttribute('data-send-error','');note.setAttribute('role','alert');form.append(note);}note.textContent=result.error||'No se pudo enviar';}
  }else if(api.create(v)){sheet=false;renderTasks()}
 }
 if(form.id==='ns-ts-send-form'){
  event.preventDefault();
  const existing=items().find(t=>t.id===sendTaskId);
  if(!existing)return;
  const recipient=form.elements.recipient.value;
  const result=api.sendTo(recipient,existing);
  if(result.ok){sheet=false;sendTaskId=null;renderTasks();}
  else {let note=form.querySelector('[data-send-error]');if(!note){note=document.createElement('p');note.setAttribute('role','alert');note.setAttribute('data-send-error','');form.append(note);}note.textContent=result.error||'No se pudo enviar';}
 }
 if(form.id==='ns-ts-template-form'){
  event.preventDefault();const v=Object.fromEntries(new FormData(form).entries());
  if(api.saveTemplate({title:v.title,steps:String(v.steps).split(/\r?\n/),category:'Personal'})){
   sheet='templates';renderTasks();
  }
 }
});
window.NeuroTasksStudio={render:function(view){
 if(view==='tasks')renderTasks();else if(view==='habits')renderStats();
}};
renderTasks();renderStats();
})();
