/* NeuroSync Pomodoro Studio. Usa la única sesión existente.
 * La UI nunca inicia o recompensa sesiones por su cuenta; delega en NeuroFocusAPI.
 */
(function(){
'use strict';
const api=window.NeuroFocusAPI;if(!api)return;
const themes={
 calma:{name:'Calma',subtitle:'Respira y vuelve a lo importante',icon:'☀'},
 bosque:{name:'Bosque',subtitle:'Encuentra tu ritmo natural',icon:'♧'},
 noche:{name:'Noche',subtitle:'Un espacio tranquilo para concentrarte',icon:'☾'}
};
let screen=null,video=null,canvas=null,scene='calma',audio=null,audioOn=false,ready=false,visible=false,mediaLoaded=false;
const $=q=>screen?.querySelector(q);
const esc=s=>String(s??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
function init(el){
 if(ready)return;screen=el;
 screen.innerHTML='<div class="ns-pf-backdrop" aria-hidden="true"><div class="ns-pf-sky"></div>'+
  '<div class="ns-pf-orb"></div><div class="ns-pf-layer ns-pf-far"></div><div class="ns-pf-layer ns-pf-near"></div>'+
  '<div class="ns-pf-fog"></div><div class="ns-pf-sparkles"></div>'+
  '<video class="ns-pf-video" muted playsinline loop preload="none" aria-hidden="true"></video></div>'+
  '<div class="ns-pf-interface"><div class="ns-pf-top"><span>NEUROSYNC · ENFOQUE</span>'+
  '<button type="button" id="ns-pf-exit" aria-label="Volver al modo minimalista">✕</button></div>'+
  '<div class="ns-pf-heading"><h2 id="ns-pf-theme-title">Calma</h2>'+
  '<p id="ns-pf-theme-subtitle">Respira y vuelve a lo importante</p></div>'+
  '<div class="ns-pomodoro-content"><div class="ns-pomodoro-caption">UNA COSA A LA VEZ</div>'+
  '<div id="ns-pomodoro-count" role="timer" aria-live="off">25:00</div>'+
  '<div id="ns-pomodoro-status">Elige tu ambiente y comienza</div></div>'+
  '<div class="ns-pf-controls"><div class="ns-pf-scenes" role="group" aria-label="Ambientes">'+
   Object.keys(themes).map(k=>'<button type="button" data-pf-scene="'+k+'">'+themes[k].icon+' '+themes[k].name+'</button>').join('')+
  '</div><div class="ns-pf-duration" role="group" aria-label="Duración de la sesión">'+
   [5,15,25,45].map(n=>'<button type="button" data-pf-minutes="'+n+'">'+n+' min</button>').join('')+'</div>'+
  '<label class="ns-pf-task">¿En qué te concentrarás?<select id="ns-pf-task"><option value="">Una tarea a la vez</option></select></label>'+
  '<div class="ns-pf-actions"><button type="button" id="ns-pf-start">Iniciar enfoque</button>'+
  '<button type="button" id="ns-pomodoro-finish">Terminar sesión</button></div>'+
  '<button type="button" id="ns-pf-audio" aria-pressed="false">♩ Activar sonido ambiental</button>'+
  '<p class="ns-pf-note">El sonido es opcional. Tu progreso se guarda aunque cierres esta pantalla.</p></div></div>';
 video=$('.ns-pf-video');
 video.addEventListener('canplay',()=>{mediaLoaded=true;screen.classList.add('ns-pf-has-video');});
 video.addEventListener('error',()=>{mediaLoaded=false;screen.classList.remove('ns-pf-has-video');});
 $('#ns-pf-exit').onclick=()=>{stopAudio(false);api.close()};
 $('#ns-pf-start').onclick=()=>api.toggle();
 $('#ns-pomodoro-finish').onclick=()=>{if(api.current())api.stop();else api.close()};
 $('#ns-pf-audio').onclick=()=>toggleAudio();
 screen.addEventListener('click',e=>{
  const b=e.target.closest('[data-pf-scene],[data-pf-minutes]');if(!b)return;
  if(b.dataset.pfScene)api.setScene(b.dataset.pfScene);
  if(b.dataset.pfMinutes)api.setDuration(Number(b.dataset.pfMinutes));
 });
 $('#ns-pf-task').onchange=e=>api.setTask(e.target.value);
 document.addEventListener('visibilitychange',()=>{if(document.hidden)pauseMedia();else if(visible)resumeMedia();});
 ready=true;
}
function tasks(){
 const select=$('#ns-pf-task');if(!select)return;
 const current=api.task(),list=api.tasks();
 const names=new Map(list.map(t=>[String(t.id),t.title]));
 if(select.options.length===list.length+1&&list.every(t=>Array.from(select.options).some(o=>o.value===String(t.id)&&o.text===t.title)))return;
 select.replaceChildren(new Option('Una tarea a la vez',''));
 for(const task of list)select.add(new Option(String(task.title).slice(0,90),String(task.id)));
 select.value=names.has(current)?current:'';
}
function sceneChange(name){
 if(!themes[name])name='calma';
 if(!ready||!screen){scene=name;return}
 if(scene===name&&screen.dataset.scene===name)return;
 scene=name;screen.dataset.scene=name;
 $('#ns-pf-theme-title').textContent=themes[name].name;
 $('#ns-pf-theme-subtitle').textContent=themes[name].subtitle;
 screen.querySelectorAll('[data-pf-scene]').forEach(b=>b.setAttribute('aria-pressed',String(b.dataset.pfScene===name)));
 mediaLoaded=false;screen.classList.remove('ns-pf-has-video');
 video.pause();video.removeAttribute('src');video.load();
 // Siempre empieza por el recurso local en la APK. Si no existe, la escena CSS sigue animada.
 video.src='./assets/focus/'+name+'.mp4';
 if(visible){video.load();resumeMedia()}
 if(audioOn)refreshAudio();
}
function pauseMedia(){if(video)video.pause();suspendAudio()}
function resumeMedia(){
 if(!video||!visible)return;
 video.play().catch(()=>{screen.classList.remove('ns-pf-has-video')});
 if(audioOn&&audio?.ctx?.state==='suspended')audio.ctx.resume().catch(()=>{});
}
function makeAudio(){
 const AC=window.AudioContext||window.webkitAudioContext;
 if(!AC)return false;
 const ctx=new AC(),buffer=ctx.createBuffer(1,ctx.sampleRate*3,ctx.sampleRate);
 const data=buffer.getChannelData(0);
 let brown=0;
 for(let i=0;i<data.length;i++){brown=(brown+(Math.random()*2-1)*0.03)*0.994;data[i]=Math.max(-1,Math.min(1,brown))*0.65}
 const source=ctx.createBufferSource();source.buffer=buffer;source.loop=true;
 const filter=ctx.createBiquadFilter();filter.type='lowpass';
 const gain=ctx.createGain();gain.gain.value=0.035;
 source.connect(filter);filter.connect(gain);gain.connect(ctx.destination);source.start();
 audio={ctx,source,filter,gain};return true;
}
function refreshAudio(){
 if(!audio)return;
 const freq={calma:310,bosque:720,noche:190}[scene];
 audio.filter.frequency.setTargetAtTime(freq,audio.ctx.currentTime,0.6);
 audio.gain.gain.setTargetAtTime(scene==='noche'?.018:.035,audio.ctx.currentTime,0.6);
}
function suspendAudio(){if(audio?.ctx?.state==='running')audio.ctx.suspend().catch(()=>{})}
function stopAudio(reset){
 if(audio){try{audio.source.stop();audio.ctx.close()}catch(e){}audio=null}
 if(reset){audioOn=false;const button=$('#ns-pf-audio');if(button){button.setAttribute('aria-pressed','false');button.textContent='♩ Activar sonido ambiental'}}
}
function toggleAudio(){
 if(audioOn){stopAudio(true);return}
 if(!audio&&!makeAudio())return;
 audioOn=true;refreshAudio();
 $('#ns-pf-audio').setAttribute('aria-pressed','true');
 $('#ns-pf-audio').textContent='♫ Silenciar ambiente';
 if(audio.ctx.state==='suspended')audio.ctx.resume().catch(()=>{});
}
function render(el,state){
 init(el);
 visible=state.visible;
 if(!visible){pauseMedia();return}
 if(scene!==state.scene||screen.dataset.scene!==state.scene)sceneChange(state.scene);
 const start=$('#ns-pf-start'),finish=$('#ns-pomodoro-finish');
 start.textContent=!state.started?'Iniciar enfoque':state.running?'Pausar':'Continuar';
 finish.textContent=state.started?'Terminar sesión':'Volver';
 screen.querySelectorAll('[data-pf-minutes]').forEach(b=>{
  b.disabled=state.started;
  b.setAttribute('aria-pressed',String(+b.dataset.pfMinutes===state.minutes));
 });
 if(!state.started)tasks();
 const select=$('#ns-pf-task');select.disabled=state.started;
 resumeMedia();
}
window.NSPomodoroStudio={render,scene:sceneChange};
})();
