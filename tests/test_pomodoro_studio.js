/* NeuroSync · QA: Pomodoro inmersivo y gesto minimalista.
 * Ejecutar desde el repositorio: node tests/test_pomodoro_studio.js
 */
const assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm');
const html=fs.readFileSync('index.html','utf8');
const studio=fs.readFileSync('modules/pomodoro_studio.js','utf8');
const css=fs.readFileSync('modules/pomodoro_studio.css','utf8');
const launcher=fs.readFileSync('android/app/src/main/java/com/neurosync/familia/FocusLauncherActivity.java','utf8');
const android=fs.readFileSync('android/app/src/main/java/com/neurosync/familia/MainActivity.java','utf8');
const workflow=fs.readFileSync('.github/workflows/android-apk.yml','utf8');
new vm.Script(studio,{filename:'pomodoro_studio.js'});
for(const m of html.matchAll(/<script(?:[^>]*)>([\s\S]*?)<\/script>/g))
 if(m[1].trim())new vm.Script(m[1]);
assert(launcher.includes('public boolean dispatchTouchEvent(MotionEvent event)'));
assert(launcher.includes('dx<-dp(85)')&&launcher.includes('openNeuroSync("focus")'));
assert(launcher.includes('Math.abs(dx)>Math.abs(dy)*1.4f'));
assert(html.includes('var nsPomodoroExpanded=false'));
assert(html.includes('nsPomodoroExpanded=true;')&&html.includes('var visible=!!(nsPomodoroExpanded&&session)'));
assert(html.includes("else if(route==='focus')nsOpenFocusStudio()"));
assert(html.includes("window.NeuroFocusAPI={"));
assert(html.includes('src="./modules/pomodoro_studio.js"'));
assert(html.includes('href="./modules/pomodoro_studio.css"'));
assert(css.includes('#view-home #focus-studio{display:none!important}'));
assert(css.includes('ns-pf-backdrop')&&css.includes('data-scene=bosque')&&css.includes('data-scene=noche'));
assert(studio.includes('calma:')&&studio.includes('bosque:')&&studio.includes('noche:'));
assert(studio.includes("video.src='./assets/focus/'+name+'.mp4'"),'Se deben priorizar fondos de video locales');
assert(studio.includes('prefers-reduced-motion')===false,'La preferencia CSS controla el movimiento');
assert(css.includes('prefers-reduced-motion'));
assert(studio.includes("window.AudioContext||window.webkitAudioContext"));
assert(studio.includes('audioOn=false'),'El sonido empieza apagado');
assert(studio.includes('document.hidden')&&studio.includes('pauseMedia()'),'Suspender al ocultar app');
assert(studio.includes('api.toggle()')&&studio.includes('api.stop()'));
assert(!studio.includes('setInterval('),'No crear un segundo reloj que compita con Pomodoro');
assert(css.includes('body.ns-pomodoro-active>.ns-overlay'),'Diálogos por encima del fullscreen');
assert(android.includes('"/modules/pomodoro_studio.js"')&&android.includes('"/assets/focus/bosque.mp4"'));
assert(workflow.includes('cp modules/pomodoro_studio.js modules/pomodoro_studio.css'));
console.log('Pomodoro Studio OK: gesture, fullscreen idle, ambientes, audio opcional, reloj único, empaquetado');
