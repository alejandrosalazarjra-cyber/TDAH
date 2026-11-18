/* NeuroSync Alarm Studio — UI accesible y totalmente local.
 * La lógica de programación, permisos y retos la proporciona NeuroAlarmAPI.
 * Sin duplicar datos ni invocar endpoints no oficiales.
 */
(function(){
 'use strict';
 const api=window.NeuroAlarmAPI;
 const host=document.getElementById('view-alarms');
 if(!api||!host)return;
 const legacy=document.createElement('div');
 legacy.className='ns-alarm-legacy';
 legacy.setAttribute('aria-hidden','true');
 while(host.firstChild)legacy.appendChild(host.firstChild);
 host.appendChild(legacy);
 const studio=document.createElement('section');
 studio.className='ns-alarm-studio';
 studio.id='ns-alarm-studio';
 studio.setAttribute('aria-label','Centro de alarmas de NeuroSync');
 host.insertBefore(studio,legacy);
 const state={screen:'home',draft:null,editing:null,focus:null,missionReturn:'editor'};
 const MISSIONS=[
  {key:'math',icon:'＋',title:'Matemáticas',detail:'Resuelve tres sumas',group:'Despierta tu mente'},
  {key:'math_hard',icon:'×',title:'Matemáticas avanzadas',detail:'Operaciones más difíciles',group:'Despierta tu mente'},
  {key:'memory',icon:'▦',title:'Memoria',detail:'Recuerda un código de cuatro dígitos',group:'Despierta tu mente'},
  {key:'typing',icon:'⌨',title:'Escritura',detail:'Copia la frase que se muestra',group:'Despierta tu mente'},
  {key:'taps',icon:'◎',title:'Toques',detail:'Pulsa doce veces para despertarte',group:'Despierta tu cuerpo'},
  {key:'shake',icon:'↝',title:'Movimiento suave',detail:'Mueve el teléfono doce veces',group:'Despierta tu cuerpo'},
  {key:'qr',icon:'▣',title:'Código QR físico',detail:'Escanea un QR registrado',group:'Despierta tu cuerpo'},
  {key:'barcode',icon:'▤',title:'Código de barras',detail:'Escanea el código físico del objeto registrado',group:'Despierta tu cuerpo'},
  {key:'walk',icon:'♧',title:'Caminar 20 pasos',detail:'Usa el detector de pasos del teléfono',group:'Despierta tu cuerpo'},
  {key:'photo',icon:'▧',title:'Tomar una foto',detail:'Abre la cámara y captura una imagen (sin verificar lugar)',group:'Despierta tu cuerpo'},
  {key:'routine_focus',icon:'◷',title:'Rutina mental',detail:'Matemáticas y escritura',group:'Rutinas de NeuroSync'},
  {key:'routine_active',icon:'✦',title:'Rutina activa',detail:'Memoria, movimiento y matemáticas',group:'Rutinas de NeuroSync'},
  {key:'routine_five',icon:'★',title:'Cinco desafíos',detail:'Matemáticas, memoria, escritura, toques y movimiento',group:'Rutinas de NeuroSync'},
  {key:'routine_custom',icon:'☷',title:'Mi secuencia',detail:'Organiza entre dos y cinco desafíos',group:'Rutinas de NeuroSync'},
  {key:'none',icon:'—',title:'Sin desafío',detail:'Detener con un botón',group:'Otras opciones'}
 ];
 const planned=[
  'Escanear objetos mediante cámara inteligente',
  'Verificar una fotografía de un lugar específico',
  'Contar sentadillas con un sensor validado',
  'Completar cinco misiones configurables'
 ];
 const escape=s=>String(s??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
 const $=q=>studio.querySelector(q);
 const fmt=(d,o)=>d.toLocaleString('es-CO',o);
 const note=message=>{const status=$('[data-status]');if(status){status.textContent=message;status.hidden=false;}}
 const isActive=a=>a.enabled!==false;
 const pad=n=>String(n).padStart(2,'0');
 const dateKey=d=>[d.getFullYear(),pad(d.getMonth()+1),pad(d.getDate())].join('-');
 function nextOccurrence(a){
  if(!isActive(a))return null;
  const parts=String(a.date||'').split('-').map(Number),clock=String(a.time||'').split(':').map(Number);
  if(parts.length!==3||clock.length<2)return null;
  const initial=new Date(parts[0],parts[1]-1,parts[2],clock[0],clock[1]);
  if(!Number.isFinite(initial.getTime()))return null;
  const now=Date.now();
  const repeat=a.repeat||(a.daily?'daily':'none');
  const allowedDay=d=>{
   const w=d.getDay(),weekend=w===0||w===6;
   return repeat==='weekdays'?!weekend:repeat==='weekends'?weekend:true;
  };
  if(initial.getTime()>now&&allowedDay(initial))return initial;
  if(repeat==='none')return null;
  const d=new Date(initial);
  for(let n=0;n<3000&&(d.getTime()<=now||!allowedDay(d));n++){
   if(repeat==='weekly')d.setDate(d.getDate()+7);
   else d.setDate(d.getDate()+(repeat==='every2'?2:repeat==='every3'?3:1));
  }
  if(!allowedDay(d))return null;
  return d.getTime()>now?d:null;
 }
 function until(date){
  if(!date)return 'Sin próxima hora';
  const minutes=Math.max(0,Math.ceil((date-Date.now())/60000));
  return minutes>=60?'En '+Math.floor(minutes/60)+' h '+minutes%60+' min':'En '+minutes+' min';
 }
 function action(label,key,extra=''){
  return '<button type="button" class="ns-as-action '+extra+'" data-action="'+key+'">'+label+'</button>';
 }
 function nav(){
  return '<nav class="ns-as-nav" aria-label="Secciones de alarmas">'+
   [['home','Alarmas'],['editor','Crear'],['missions','Misiones'],['report','Informe'],['settings','Ajustes']]
   .map(([name,label])=>'<button type="button" data-go="'+name+'"'+
    (state.screen===name?' aria-current="page"':'')+'>'+label+'</button>').join('')+'</nav>';
 }
 function shell(title,subtitle,body){
  studio.innerHTML='<header class="ns-as-header"><div class="ns-as-header-top"><span class="ns-as-brand">NEUROSYNC / ALARMAS</span>'+
   '<span class="ns-as-dot" aria-hidden="true"></span></div><h2>'+escape(title)+'</h2>'+
   (subtitle?'<p>'+escape(subtitle)+'</p>':'')+'</header>'+nav()+
   '<div class="ns-as-content">'+body+'</div><p class="ns-as-status" data-status hidden role="status"></p>';
  studio.classList.toggle('ns-as-dark',true);
 }
 function home(){
  const items=api.list().slice().sort((a,b)=>(nextOccurrence(a)?.getTime()||Infinity)-(nextOccurrence(b)?.getTime()||Infinity));
  const nearest=items.map(a=>({a,date:nextOccurrence(a)})).filter(x=>x.date).sort((a,b)=>a.date-b.date)[0];
  let top='<section class="ns-as-next">'+
   '<span class="ns-as-eyebrow">PRÓXIMA ALARMA</span>'+
   (nearest?'<div class="ns-as-bigtime">'+escape(nearest.a.time)+'</div>'+
    '<h3>'+escape(nearest.a.title||'Despertar')+'</h3><p>'+escape(until(nearest.date))+'</p>':
    '<div class="ns-as-bigtime ns-as-emptytime">--:--</div><p>No hay alarmas próximas</p>')+
   '</section>';
  const list=items.length?items.map(a=>{
   const date=nextOccurrence(a), mission=MISSIONS.find(m=>m.key===a.challenge);
   return '<article class="ns-as-alarm-row">'+
    '<button type="button" class="ns-as-row-main" data-edit="'+escape(a.id)+'" aria-label="Editar '+escape(a.title)+'">'+
    '<strong>'+escape(a.time||'--:--')+'</strong><span><b>'+escape(a.title||'Alarma')+'</b>'+
    '<small>'+escape(a.repeat==='daily'?'Todos los días':a.repeat==='weekly'?'Semanal':a.repeat==='every2'?'Cada 2 días':a.repeat==='every3'?'Cada 3 días':a.repeat==='weekdays'?'Lun a vie':a.repeat==='weekends'?'Fines de semana':'Una vez')+
    ' · '+escape(mission?mission.title:'Sin desafío')+'</small>'+
    '<small>'+escape(date?until(date):'Desactivada o vencida')+'</small></span></button>'+
    '<label class="ns-as-switch"><input type="checkbox" data-toggle="'+escape(a.id)+'" '+
    (isActive(a)?'checked ':'')+'aria-label="Activar alarma '+escape(a.title)+'"><span></span></label></article>';
  }).join(''):'<div class="ns-as-empty"><span>◷</span><h3>Aún no tienes alarmas</h3><p>Configura tu primera alarma.</p></div>';
  shell('Mis alarmas','Despierta a tu ritmo',top+
   '<div class="ns-as-between"><h3>Programadas</h3>'+action('+ Nueva alarma','new','ns-as-primary')+'</div>'+
   '<div class="ns-as-list">'+list+'</div>'+
   '<div class="ns-as-footer-links">'+action('Probar alarma','test')+action('Ajustes y permisos','settings')+'</div>');
 }
 function defaultDraft(){
  const d=new Date(Date.now()+3600000);d.setSeconds(0,0);
  const pref=api.preferences();
  return {title:'Despertar',date:dateKey(d),time:pad(d.getHours())+':'+pad(d.getMinutes()),
   repeat:'none',challenge:'none',snoozeMinutes:pref.snoozeMinutes||5,snoozeLimit:pref.snoozeLimit??3,
   volumePercent:100,vibration:true,gentleWake:false};
 }
 function startEditor(id){
  state.editing=id||null;
  state.draft=id?Object.assign(defaultDraft(),api.list().find(a=>a.id===id)||{}):defaultDraft();
  state.screen='editor';render();
 }
 function captureDraft(){
  if(state.screen!=='editor'||!state.draft)return;
  for(const key of ['title','date','time','repeat','snoozeMinutes','snoozeLimit']){
   const el=$('[name="'+key+'"]');
   if(el)state.draft[key]=el.value;
  }
  if(state.draft.challenge==='routine_custom'){
   state.draft.sequence=[...studio.querySelectorAll('[data-sequence-step]')]
     .map(el=>el.value).filter(Boolean);
  }
  const volume=$('[name="volumePercent"]');
  if(volume)state.draft.volumePercent=+volume.value;
  const vibration=$('[name="vibration"]');
  if(vibration)state.draft.vibration=vibration.checked;
  const gentle=$('[name="gentleWake"]');
  if(gentle)state.draft.gentleWake=gentle.checked;
 }
 function editor(){
  const d=state.draft||defaultDraft();
  const mission=MISSIONS.find(m=>m.key===d.challenge)||MISSIONS.find(m=>m.key==='none');
  const edit=!!state.editing;
  shell(edit?'Editar alarma':'Nueva alarma','Configura solo lo necesario',
   '<form id="ns-as-form" class="ns-as-form">'+
   '<label>Nombre<input name="title" maxlength="90" required value="'+escape(d.title)+'"></label>'+
   '<div class="ns-as-time-field"><label>Hora de despertar<input type="time" name="time" required value="'+escape(d.time)+'"></label>'+
   '<p id="ns-as-next-preview">Programa tu hora</p></div>'+
   '<label>Primera fecha<input type="date" name="date" required value="'+escape(d.date)+'"></label>'+
   '<label>Repetición<select name="repeat">'+
   [['none','Una vez'],['daily','Todos los días'],['weekly','Una vez por semana'],
    ['every2','Cada 2 días'],['every3','Cada 3 días'],['weekdays','Lunes a viernes'],['weekends','Sábados y domingos']]
    .map(([v,t])=>'<option value="'+v+'"'+(d.repeat===v?' selected':'')+'>'+t+'</option>').join('')+
   '</select></label>'+
   '<div class="ns-as-section-title">Desafío</div>'+
   '<button type="button" class="ns-as-pick" data-action="missions"><span>'+escape(mission.icon)+'</span>'+
   '<b>'+escape(mission.title)+'</b><small>Elegir ›</small></button>'+
   (d.challenge==='routine_custom'?'<div class="ns-as-section-title">Orden de tus misiones</div>'+
     '<p class="ns-as-hint">Las dos primeras son obligatorias. Las siguientes son opcionales.</p>'+
     Array.from({length:5},(_,i)=>{
       const chosen=(d.sequence||['math','typing'])[i]||'';
       const options=[['','Sin misión']].concat(MISSIONS.filter(m=>['math','math_hard','memory','typing','taps','shake','walk','photo'].includes(m.key)).map(m=>[m.key,m.title]));
       return '<label>Desafío '+(i+1)+'<select data-sequence-step="'+i+'">'+
          options.filter(o=>i>=2||o[0]).map(([v,t])=>'<option value="'+v+'"'+(v===chosen?' selected':'')+'>'+t+'</option>').join('')+
          '</select></label>';
     }).join(''):'')+
   '<div class="ns-as-section-title">Posponer</div><div class="ns-as-field-grid">'+
   '<label>Minutos<select name="snoozeMinutes">'+[1,3,5,10,15].map(n=>'<option value="'+n+'"'+(+d.snoozeMinutes===n?' selected':'')+'>'+n+' min</option>').join('')+'</select></label>'+
   '<label>Veces<select name="snoozeLimit">'+[0,1,2,3,5,10].map(n=>'<option value="'+n+'"'+(+d.snoozeLimit===n?' selected':'')+'>'+n+'</option>').join('')+'</select></label></div>'+
   ((d.challenge==='qr'||d.challenge==='barcode')?
    '<p class="ns-as-hint">'+(d.qrReady?'Código capturado para esta alarma':'Escanea el código físico de referencia. No se envía a internet.')+'</p>'+
    action(d.challenge==='barcode'?'Escanear código de barras':'Escanear QR de referencia',
      d.challenge==='barcode'?'barcode':'qr'):'')+
   '<div class="ns-as-section-title">Sonido</div>'+
   '<label>Volumen del sonido ('+(d.volumePercent??100)+'%)<input type="range" name="volumePercent" min="0" max="100" step="5" value="'+(d.volumePercent??100)+'"></label>'+
   '<label class="ns-as-check"><input name="vibration" type="checkbox" '+(d.vibration!==false?'checked':'')+'> Vibrar cuando suene</label>'+
   '<label class="ns-as-check"><input name="gentleWake" type="checkbox" '+(d.gentleWake?'checked':'')+'> Subir el volumen gradualmente (30 s)</label>'+
   '<p class="ns-as-hint">'+(edit&&api.hasSound(state.editing)?'Tono personalizado seleccionado':'Tono local predeterminado')+'</p>'+
   (edit?action('Elegir tono para esta alarma','sound-alarm')+action('Usar tono predeterminado','clear-sound'):'<p class="ns-as-hint">Guarda la alarma para asignarle un tono individual.</p>')+
   '<div class="ns-as-sticky"><button type="submit" class="ns-as-save">'+(edit?'Guardar cambios':'Guardar alarma')+'</button>'+
   (edit?action('Eliminar','delete','ns-as-danger'):'')+'</div></form>');
  const form=$('#ns-as-form');
  function preview(){
   const d=state.draft;captureDraft();
   const date=nextOccurrence(Object.assign({},d,{enabled:true}));
   $('#ns-as-next-preview').textContent=date?until(date):'Elige una fecha futura';
  }
  form.addEventListener('input',e=>{
    if(e.target&&e.target.name==='volumePercent'){
      const label=e.target.closest('label');if(label)label.childNodes[0].textContent='Volumen del sonido ('+e.target.value+'%)';
    }
    preview();
  });
  form.addEventListener('change',preview);
  form.onsubmit=e=>{
   e.preventDefault();captureDraft();
   const result=api.createOrUpdate(state.draft,state.editing);
   if(result.ok){state.screen='home';state.draft=null;state.editing=null;render()}
   else note(result.error||'No se pudo guardar la alarma');
  };
  preview();
 }
 function missions(){
  const selected=state.draft?.challenge||'none';
  const groups=[...new Set(MISSIONS.map(m=>m.group))];
  const sections=groups.map(group=>'<section class="ns-as-mission-group"><h3>'+escape(group)+'</h3>'+
    MISSIONS.filter(m=>m.group===group).map(m=>
     '<button type="button" class="ns-as-mission '+(selected===m.key?'chosen':'')+'" data-mission="'+m.key+'">'+
     '<span class="ns-as-mission-icon">'+escape(m.icon)+'</span>'+
     '<span><strong>'+escape(m.title)+'</strong><small>'+escape(m.detail)+'</small></span>'+
     '<span aria-hidden="true">'+(selected===m.key?'✓':'›')+'</span></button>').join('')+
    '</section>').join('');
  shell('Elige tu misión','Desafíos disponibles sin internet',
    '<div class="ns-as-missions">'+sections+'</div>'+
    '<details class="ns-as-soon"><summary>Misiones en desarrollo</summary><p>'+
    planned.map(x=>escape(x)).join(' · ')+
    '</p><small>Estas opciones todavía no pueden configurarse ni cuentan como pruebas realizadas.</small></details>'+
    action('Volver al editor','back','ns-as-primary'));
 }
 function settings(){
  const p=api.permissions(),prefs=api.preferences();
  const features=p?[
   ['Notificaciones',p.notifications],['Alarmas exactas',p.exact],['Pantalla completa',p.fullScreen],
   ['Prioridad del canal',p.channelHigh!==false]
  ]:[['Permisos de alarma',false]];
  shell('Ajustes de alarmas','Configuración de Android y NeuroSync',
   '<div class="ns-as-settings-group"><h3>Optimización de alarma</h3>'+
   features.map(([name,ok])=>'<div class="ns-as-settings-line"><span>'+escape(name)+'</span>'+
    '<b class="'+(ok?'ok':'missing')+'">'+(ok?'Listo':'Revisar')+'</b></div>').join('')+
   action('Revisar permisos en Android','permissions','ns-as-primary')+
   '<p class="ns-as-hint">Android controla la ejecución exacta y la apertura en pantalla bloqueada.</p></div>'+
   '<div class="ns-as-settings-group"><h3>Sonido y música</h3>'+
    action('Elegir tono local','sound')+
    action('Abrir Reloj de Google','clock')+
    action('Alarma con YouTube Music en Reloj','music')+
    '<p class="ns-as-hint">Las canciones de YouTube Music se eligen en Reloj de Google; no se reproducen desde los retos de NeuroSync.</p></div>'+
   '<div class="ns-as-settings-group"><h3>Preferencias para nuevas alarmas</h3>'+
    '<label>Posponer por defecto<select id="ns-as-default-snooze">'+[1,3,5,10,15].map(n=>'<option value="'+n+'"'+(+prefs.snoozeMinutes===n?' selected':'')+'>'+n+' min</option>').join('')+'</select></label>'+
    '<label>Posposiciones por defecto<select id="ns-as-default-limit">'+[0,1,2,3,5,10].map(n=>'<option value="'+n+'"'+(+prefs.snoozeLimit===n?' selected':'')+'>'+n+'</option>').join('')+'</select></label>'+
    action('Guardar preferencias','save-settings','ns-as-primary')+'</div>'+
   '<div class="ns-as-settings-group"><h3>Seguridad</h3><p class="ns-as-hint">Siempre existe una opción de detener la alarma sin desafío por seguridad. No activaremos bloqueos de desinstalación ni intentaremos eludir las protecciones de Android.</p></div>');
 }
 function report(){
  const now=Date.now(),weekStart=now-7*86400000;
  const events=api.events().filter(e=>+e.at>=weekStart&&+e.at<=now);
  const counts=t=>events.filter(e=>e.action===t).length;
  const stats=[['Sonaron',counts('ring')],['Detenidas',counts('dismiss')],['Pospuestas',counts('snooze')]];
  const productivity=api.productivity();
  const bars=Array.from({length:7},(_,i)=>{
   const d=new Date();d.setHours(0,0,0,0);d.setDate(d.getDate()-(6-i));
   return {date:d,day:d.toLocaleDateString('es-CO',{weekday:'short'}),
     count:events.filter(e=>e.action==='ring'&&+e.at>=d.getTime()&&+e.at<d.getTime()+86400000).length};
  });
  const max=Math.max(1,...bars.map(x=>x.count));
  let sleep=api.sleep();
  if(!Array.isArray(sleep))sleep=[];
  const sleeps=sleep.filter(x=>x&&Date.parse(x.day||x.date||'')>=weekStart);
  shell('Informe de despertar','Tus últimos siete días',
   '<div class="ns-as-report-stats">'+stats.map(([l,v])=>'<div><strong>'+v+'</strong><small>'+l+'</small></div>').join('')+'</div>'+
   '<section class="ns-as-chart"><h3>Alarmas que sonaron</h3><div class="ns-as-bars">'+bars.map(x=>
    '<div><span>'+x.count+'</span><div class="ns-as-bar-track"><i style="height:'+(x.count/max*100)+'%"></i></div><small>'+escape(x.day)+'</small></div>').join('')+'</div></section>'+
   '<section class="ns-as-settings-group"><h3>Productividad y Azul</h3>'+
    '<div class="ns-as-report-stats"><div><strong>'+productivity.focusMinutes+'</strong><small>Min de enfoque</small></div>'+
    '<div><strong>'+productivity.tasksCompleted+'</strong><small>Tareas de 7 días</small></div>'+
    '<div><strong>'+productivity.dragonXp+'</strong><small>XP de Azul</small></div></div></section>'+
   '<section class="ns-as-settings-group"><h3>Diario del sueño</h3><p>'+
    (sleeps.length?sleeps.length+' registro(s) recientes':'Sin registros de descanso esta semana')+
    '</p><p class="ns-as-hint">El sueño se registra voluntariamente desde Progreso; no se estima a partir de la alarma.</p>'+
    action('Abrir mi progreso','progress','ns-as-primary')+'</section>'+
   '<section class="ns-as-settings-group"><h3>Eventos recientes</h3>'+
    (events.length?events.slice(-10).reverse().map(x=>'<div class="ns-as-settings-line"><span>'+
      escape({ring:'Sonó',dismiss:'Detenida',snooze:'Pospuesta'}[x.action]||'Alarma')+
      '</span><small>'+escape(fmt(new Date(+x.at),{day:'numeric',month:'short',hour:'2-digit',minute:'2-digit'}))+'</small></div>').join(''):
     '<p class="ns-as-hint">Aún no hay eventos registrados.</p>')+'</section>');
 }
 function render(){
  if(!api.ready()){shell('Alarmas','Inicia sesión para ver tus alarmas','');return}
  if(state.screen==='home')home();
  else if(state.screen==='editor')editor();
  else if(state.screen==='missions')missions();
  else if(state.screen==='settings')settings();
  else report();
 }
 function navigate(screen){
  if(screen==='editor'){startEditor(null);return}
  if(screen==='missions'&&!state.draft){startEditor(null);state.screen='missions'}
  else{if(state.screen==='editor')captureDraft();state.screen=screen}
  render();studio.scrollIntoView({block:'start',behavior:'auto'});
 }
 studio.addEventListener('click',async e=>{
  const target=e.target.closest('button');if(!target)return;
  if(target.dataset.go){navigate(target.dataset.go);return}
  if(target.dataset.edit){startEditor(target.dataset.edit);return}
  if(target.dataset.mission){if(!state.draft)state.draft=defaultDraft();
    state.draft.challenge=target.dataset.mission;
    if(target.dataset.mission==='routine_custom'&&(!Array.isArray(state.draft.sequence)||state.draft.sequence.length<2))
      state.draft.sequence=['math','typing'];
    state.screen='editor';render();return;}
  const type=target.dataset.action;
  if(!type)return;
  if(type==='new')startEditor(null);
  else if(type==='missions'){captureDraft();state.screen='missions';render()}
  else if(type==='settings'){state.screen='settings';render()}
  else if(type==='back'){state.screen='editor';render()}
  else if(type==='qr'){
   if(!api.scanQr())note('El lector QR necesita Android.');
  }
  else if(type==='barcode'){
   if(!api.scanBarcode())note('El lector de barras necesita Android.');
  }
  else if(type==='sound-alarm'&&state.editing){
   if(!api.chooseSound(state.editing))note('El selector necesita Android.');
  }
  else if(type==='clear-sound'&&state.editing){
   api.clearSound(state.editing);render();note('Se usará el tono predeterminado.');
  }
  else if(type==='delete'&&state.editing){const ok=await api.remove(state.editing);if(ok){state.editing=null;state.draft=null;state.screen='home';render()}}
  else if(type==='permissions'){api.requestPermissions();api.checkPermissions();setTimeout(render,350)}
  else if(type==='save-settings'){
   const ok=api.setPreferences({snoozeMinutes:+$('#ns-as-default-snooze').value,
    snoozeLimit:+$('#ns-as-default-limit').value});
   note(ok?'Preferencias guardadas':'No se pudieron guardar las preferencias');
  }
  else if(['sound','clock','music','test'].includes(type)){
   if(!api.native(type))note('Esta opción requiere la APK Android.');
  }else if(type==='progress'){const view=document.querySelector('[data-view="habits"]');if(view)view.click()}
 });
 studio.addEventListener('change',e=>{
  const input=e.target.closest('[data-toggle]');
  if(!input)return;
  const ok=api.toggle(input.dataset.toggle,input.checked);
  if(!ok){input.checked=!input.checked;note('No se pudo actualizar esta alarma. Revisa permisos.')}
 });
 let scheduled=false;
 function onChanged(){if(scheduled)return;scheduled=true;setTimeout(()=>{scheduled=false;if(state.screen==='home'||state.screen==='report')render()},0)}
 window.addEventListener('neuro-alarm-changed',onChanged);
 window.addEventListener('neuro-alarm-qr-ready',e=>{
  if(state.draft&&(state.draft.challenge==='qr'||state.draft.challenge==='barcode')&&typeof e.detail==='string'){
   state.draft.qrReady=e.detail;
   if(state.screen==='editor')render();
   note('QR de referencia capturado; guarda la alarma para confirmar.');
  }
 });
 const priorSoundCallback=window.neuroAlarmSoundSelected;
 window.neuroAlarmSoundSelected=function(id){
  if(typeof priorSoundCallback==='function')priorSoundCallback(id);
  if(state.screen==='editor'&&state.editing===id){render();note('Tono personal de esta alarma guardado.')}
 };
 document.querySelectorAll('[data-view="alarms"]').forEach(b=>b.addEventListener('click',()=>{state.screen='home';render()}));
 window.NeuroAlarmStudio={open:navigate,render};
 render();
})();
