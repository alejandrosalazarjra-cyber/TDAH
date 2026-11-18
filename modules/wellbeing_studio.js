/* NeuroSync | Bienestar íntimo privado.
 * No clasifica la masturbación como enfermedad: seguimiento de un objetivo voluntario.
 * Misma clave existente ns_private_progress_<username>, cero red y sin datos familiares.
 */
(function(){
'use strict';
const api=window.NeuroWellbeingAPI,host=document.getElementById('ns-private-wellbeing');
if(!host||!api)return;
const card=host.closest('.card');
if(card)card.classList.add('ns-wb-card');
const esc=x=>String(x??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const isDay=x=>/^\d{4}-\d{2}-\d{2}$/.test(x)&&!Number.isNaN(Date.parse(x+'T12:00:00'));
const dayDiff=(a,b)=>Math.max(0,Math.round((new Date(b+'T12:00:00')-new Date(a+'T12:00:00'))/86400000));
const TAGS=['Aburrimiento','Estrés','Soledad','Cansancio','Ansiedad','Redes sociales','Otro'];
const STEPS=['Levántate y cambia de lugar','Bebe un vaso de agua','Respira lento durante un minuto','Da una caminata breve','Escribe o realiza una tarea pequeña','Contacta a alguien de confianza'];
let dialog=null,dialogMode='',urgeStep=0,urgeTimer=null,remaining=60;
function normalize(data){
 const today=api.today();
 const started=isDay(data.started)&&data.started<=today?data.started:today;
 return {
  started,history:Array.isArray(data.history)?data.history.filter(v=>v&&isDay(v.from)&&isDay(v.to)).slice(-500):[],
  checkins:Array.isArray(data.checkins)?data.checkins.filter(v=>v&&isDay(v.date)).slice(-365):[],
  urges:Array.isArray(data.urges)?data.urges.filter(v=>v&&isDay(v.date)).slice(-400):[],
  objective:String(data.objective||'').slice(0,180),
  plan:Array.isArray(data.plan)?data.plan.map(v=>String(v).slice(0,120)).slice(0,6):[]
 };
}
function persist(x){return api.write(normalize(x))}
function data(){return normalize(api.read()||{})}
function periodStats(d){
 const count=dayDiff(d.started,api.today()),best=Math.max(count,...d.history.map(h=>dayDiff(h.from,h.to)));
 const completed=d.checkins.filter(x=>x.kind==='checkin').length;
 const recent=d.checkins.filter(x=>dayDiff(x.date,api.today())<=6);
 const mood=recent.map(x=>Number(x.mood)).filter(x=>x>=1&&x<=5);
 return {count,best,checkins:completed,mood:mood.length?(mood.reduce((a,b)=>a+b,0)/mood.length).toFixed(1):'—'};
}
function heatmap(d){
 const byDate=new Map();
 d.checkins.forEach(x=>{if(x.kind==='checkin')byDate.set(x.date,(byDate.get(x.date)||0)+1)});
 const now=new Date(api.today()+'T12:00:00'),cells=[];
 for(let days=27;days>=0;days--){
  const dt=new Date(now);dt.setDate(dt.getDate()-days);
  const key=[dt.getFullYear(),String(dt.getMonth()+1).padStart(2,'0'),String(dt.getDate()).padStart(2,'0')].join('-');
  cells.push('<span title="'+key+'" class="'+(byDate.has(key)?'has-log':'')+'"></span>');
 }
 return cells.join('');
}
function render(){
 if(!api.ready()){host.replaceChildren();return}
 const d=data(),stats=periodStats(d),latest=d.checkins.slice(-5).reverse();
 host.innerHTML='<div class="ns-wb-head"><div><span>SEGUIMIENTO PERSONAL</span><h3>Mi bienestar, a mi ritmo</h3></div>'+
  '<button type="button" data-wb="privacy" aria-label="Información de privacidad">ⓘ</button></div>'+
  '<p class="ns-wb-description">Tu decisión personal, sin culpa. El progreso también incluye lo que aprendes sobre ti.</p>'+
  '<div class="ns-wb-metrics"><div><strong>'+stats.count+'</strong><small>Días actuales</small></div>'+
  '<div><strong>'+stats.best+'</strong><small>Mejor período</small></div>'+
  '<div><strong>'+stats.checkins+'</strong><small>Registros diarios</small></div></div>'+
  '<div class="ns-wb-action"><button type="button" data-wb="checkin">✎ Registrar cómo me siento</button>'+
  '<button type="button" data-wb="support">◷ Necesito una pausa</button></div>'+
  '<section class="ns-wb-panel"><h4>Mi intención</h4><p>'+(d.objective?esc(d.objective):'¿Qué te gustaría mejorar o comprender? Puedes establecer un objetivo sin exigencias.')+'</p>'+
  '<button type="button" data-wb="objective">Editar objetivo</button></section>'+
  '<section class="ns-wb-panel"><div class="ns-wb-inline"><h4>Actividad del último mes</h4><small>Registros, no éxitos ni fracasos</small></div>'+
  '<div class="ns-wb-heatmap">'+heatmap(d)+'</div>'+
  '<p class="ns-wb-low">Estado de ánimo reciente: '+stats.mood+'/5 · Sin calificaciones morales</p></section>'+
  '<section class="ns-wb-panel"><h4>Mis últimos registros</h4>'+
  (latest.length?latest.map(x=>'<div class="ns-wb-record"><span>'+esc(x.date)+'</span><strong>'+
   (x.kind==='reset'?'Nuevo comienzo':'Estado de ánimo '+Number(x.mood)+'/5')+'</strong>'+
   (x.note?'<p>'+esc(x.note)+'</p>':'')+'</div>').join(''):'<p>Aún no hay registros. Puedes empezar cuando quieras.</p>')+
  '</section><div class="ns-wb-footer"><button data-wb="restart">Registrar nuevo comienzo</button>'+
  '<button data-wb="export">Exportar datos</button><button data-wb="erase">Borrar datos privados</button></div>'+
  '<div class="ns-wb-note">El bienestar sexual es personal. No se considera que masturbarse sea perjudicial por sí mismo. Si te causa malestar persistente o sientes pérdida de control, puedes consultar con un profesional.</div>';
}
function showDialog(mode){
 dialogMode=mode;dialog=document.createElement('div');dialog.className='ns-wb-modal';
 dialog.setAttribute('role','dialog');dialog.setAttribute('aria-modal','true');dialog.setAttribute('aria-label','Bienestar personal');
 document.body.appendChild(dialog);
 const d=data();
 let contents='';
 if(mode==='checkin'){
  contents='<h3>¿Cómo te sientes hoy?</h3><p>Registrar un día no implica juzgar lo que hayas hecho.</p>'+
   '<label>Estado de ánimo<select id="ns-wb-mood"><option value="3">3 · Neutral</option><option value="1">1 · Muy bajo</option><option value="2">2 · Bajo</option><option value="4">4 · Bien</option><option value="5">5 · Muy bien</option></select></label>'+
   '<label>¿Qué influyó hoy?<select id="ns-wb-tag"><option value="">Prefiero no decirlo</option>'+
   TAGS.map(t=>'<option>'+esc(t)+'</option>').join('')+'</select></label>'+
   '<label>Nota opcional<textarea id="ns-wb-note" maxlength="240" placeholder="¿Qué observaste o qué te ayudó?"></textarea></label>'+
   '<button class="ns-wb-primary" data-wb-submit="checkin">Guardar registro</button>';
 }else if(mode==='objective'){
  contents='<h3>Mi intención</h3><p>Define un propósito que te sirva, sin presión.</p>'+
   '<label>Objetivo personal<textarea id="ns-wb-objective" maxlength="180">'+esc(d.objective)+'</textarea></label>'+
   '<button class="ns-wb-primary" data-wb-submit="objective">Guardar objetivo</button>';
 }else if(mode==='support'){
  urgeStep=0;remaining=60;
  contents='<h3>Una pausa de un minuto</h3><p>Un impulso es una sensación pasajera; puedes decidir qué hacer sin juzgarte.</p>'+
   '<div class="ns-wb-breath"><strong id="ns-wb-seconds">60</strong><span>segundos</span></div>'+
   '<p id="ns-wb-step">'+esc(STEPS[0])+'</p>'+
   '<div class="ns-wb-support"><button data-wb-submit="next">Otra idea</button>'+
   '<button data-wb-submit="logurge">Registrar desencadenante</button></div>';
 }else if(mode==='urge'){
  contents='<h3>¿Qué notaste?</h3><p>Este registro es opcional y privado.</p>'+
   '<label>Situación<select id="ns-wb-urge-tag">'+TAGS.map(t=>'<option>'+esc(t)+'</option>').join('')+'</select></label>'+
   '<label>Intensidad del impulso<select id="ns-wb-intensity">'+[1,2,3,4,5].map(i=>'<option value="'+i+'">'+i+' / 5</option>').join('')+'</select></label>'+
   '<button class="ns-wb-primary" data-wb-submit="urge">Guardar observación</button>';
 }else if(mode==='privacy'){
  contents='<h3>Privacidad</h3><p>Estos datos se guardan localmente en el almacenamiento WebView de este dispositivo y se separan por nombre de cuenta. No se envían a la sincronización familiar ni afectan el dragón.</p>'+
   '<p>El almacenamiento local no equivale a cifrado independiente: quien tenga acceso al dispositivo o sus copias podría leerlo. Exporta únicamente a un lugar privado.</p>';
 }else if(mode==='restart'){
  contents='<h3>Nuevo comienzo</h3><p>Se conserva tu historial. Un cambio de fecha no borra tus aprendizajes.</p>'+
   '<button class="ns-wb-primary" data-wb-submit="restart">Confirmar nuevo comienzo</button>';
 }else if(mode==='erase'){
  contents='<h3>Eliminar datos privados</h3><p>Se borrará el historial de esta cuenta en este dispositivo. No se puede deshacer.</p>'+
   '<button class="ns-wb-danger" data-wb-submit="erase">Confirmar eliminación</button>';
 }
 dialog.innerHTML='<div class="ns-wb-dialog"><button type="button" class="ns-wb-close" data-wb-close aria-label="Cerrar">×</button>'+contents+'</div>';
 dialog.addEventListener('click',onDialogClick);
 if(mode==='support')urgeTimer=setInterval(()=>{
  if(remaining<=0){clearInterval(urgeTimer);urgeTimer=null;return}
  remaining--;const label=dialog?.querySelector('#ns-wb-seconds');if(label)label.textContent=remaining;
 },1000);
}
function closeDialog(){
 if(urgeTimer){clearInterval(urgeTimer);urgeTimer=null}
 dialog?.remove();dialog=null;dialogMode='';
}
function saveCheckin(){
 const d=data(),date=api.today(),mood=Number(dialog.querySelector('#ns-wb-mood').value),tag=dialog.querySelector('#ns-wb-tag').value;
 const note=dialog.querySelector('#ns-wb-note').value.trim().slice(0,240);
 d.checkins=d.checkins.filter(x=>!(x.kind==='checkin'&&x.date===date));
 d.checkins.push({date,kind:'checkin',mood,tag,note});
 if(persist(d)){closeDialog();render()}else alert('No se pudo guardar el registro. Comprueba el almacenamiento.');
}
function exportData(){
 const payload={format:'neurosync-private-wellbeing-v1',exportedAt:new Date().toISOString(),data:data()};
 const blob=new Blob([JSON.stringify(payload,null,2)],{type:'application/json'});
 const link=document.createElement('a'),url=URL.createObjectURL(blob);
 link.href=url;link.download='neurosync-bienestar-privado.json';document.body.appendChild(link);link.click();link.remove();
 setTimeout(()=>URL.revokeObjectURL(url),3000);
}
function onDialogClick(e){
 const button=e.target.closest('button');if(!button)return;
 if(button.hasAttribute('data-wb-close')){closeDialog();return}
 const action=button.dataset.wbSubmit;if(!action)return;
 if(action==='checkin'){saveCheckin();return}
 if(action==='objective'){
  const d=data();d.objective=dialog.querySelector('#ns-wb-objective').value.trim().slice(0,180);
  if(persist(d)){closeDialog();render()}return;
 }
 if(action==='next'){urgeStep=(urgeStep+1)%STEPS.length;dialog.querySelector('#ns-wb-step').textContent=STEPS[urgeStep];return}
 if(action==='logurge'){closeDialog();showDialog('urge');return}
 if(action==='urge'){
  const d=data();d.urges.push({date:api.today(),tag:dialog.querySelector('#ns-wb-urge-tag').value,
   intensity:Number(dialog.querySelector('#ns-wb-intensity').value)});
  if(persist(d)){closeDialog();render()}return;
 }
 if(action==='restart'){
  const d=data(),today=api.today();
  if(d.started!==today)d.history.push({from:d.started,to:today});
  d.started=today;
  d.checkins.push({date:today,kind:'reset',note:''});
  if(persist(d)){closeDialog();render()}return;
 }
 if(action==='erase'){if(api.erase()){closeDialog();render()}}
}
host.addEventListener('click',e=>{
 const b=e.target.closest('button[data-wb]');if(!b)return;
 const action=b.dataset.wb;
 if(action==='export'){exportData();return}
 if(['checkin','support','objective','privacy','restart','erase'].includes(action))showDialog(action);
});
window.NeuroWellbeingStudio={render};
render();
})();
