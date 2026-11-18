const assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm');
const html=fs.readFileSync('index.html','utf8');
const moduleJS=fs.readFileSync('modules/wellbeing_studio.js','utf8');
const css=fs.readFileSync('modules/wellbeing_studio.css','utf8');
const android=fs.readFileSync('android/app/src/main/java/com/neurosync/familia/MainActivity.java','utf8');
const workflow=fs.readFileSync('.github/workflows/android-apk.yml','utf8');
new vm.Script(moduleJS);
for(const x of html.matchAll(/<script(?:[^>]*)>([\s\S]*?)<\/script>/g))if(x[1].trim())new vm.Script(x[1]);
assert(moduleJS.includes('checkins')&&moduleJS.includes('urges'));
assert(moduleJS.includes('data-wb="support"')&&moduleJS.includes('data-wb="erase"'));
assert(moduleJS.includes('setInterval('),'La pausa consciente debe tener temporizador independiente, solo cuando se use');
assert(moduleJS.includes('clearInterval(urgeTimer)'),'El temporizador de pausa debe detenerse al cerrar');
assert(moduleJS.includes('ns-wb-heatmap')&&moduleJS.includes('objective'));
assert(moduleJS.includes('URL.createObjectURL(blob)'),'Exportación personal');
assert(!moduleJS.includes('fetch(')&&!moduleJS.includes('XMLHttpRequest'),'Nada de envío de datos a servidores');
assert(!moduleJS.includes('model.pet')&&!moduleJS.includes('nsDragon'),'No condicionar mascota a conducta íntima');
assert(css.includes('prefers-reduced-motion')&&css.includes(':focus-visible'));
assert(android.includes('"/modules/wellbeing_studio.js"')&&android.includes('"/modules/wellbeing_studio.css"'));
assert(workflow.includes('cp modules/wellbeing_studio.js modules/wellbeing_studio.css'));
assert(html.includes('window.NeuroWellbeingAPI={')&&html.includes('renderStreak();nsHealthRender();if(window.NeuroWellbeingStudio)'));
const start=html.indexOf('window.NeuroWellbeingAPI={'),end=html.indexOf('\n};',start)+3;
assert(start>=0&&end>start);
const store=new Map(),session={username:'account_A'},now='2026-11-18',key=()=> 'ns_private_progress_'+session.username;
const localStorage={
 getItem:x=>store.has(x)?store.get(x):null,
 setItem:(x,v)=>store.set(x,String(v)),
 removeItem:x=>store.delete(x)
};
const deps={session,localStorage,day:()=>now,streakKey:key,renderStreak:()=>{}};
const api=new Function(...Object.keys(deps),html.slice(start,end)+'\nreturn window.NeuroWellbeingAPI;')(...Object.values(deps));
assert.deepEqual(api.read().history,[]);
const original={started:'2026-11-01',history:[{from:'2026-10-01',to:'2026-10-10'}]};
assert(api.write(original));assert.equal(api.read().started,'2026-11-01');
assert.equal(api.read().history.length,1,'No perder historial antiguo');
session.username='account_B';assert.equal(api.read().started,now,'Datos de otra cuenta separados');
assert(api.write({started:now,history:[],checkins:[{kind:'checkin',date:now,mood:3}]}));
session.username='account_A';assert.equal(api.read().started,'2026-11-01');
assert(api.erase());assert.equal(api.read().started,now);
session.username='account_B';assert.equal(api.read().checkins.length,1,'El borrado no debe afectar a la otra cuenta');
console.log('NoFap/Bienestar: compatibilidad, aislamiento, borrado, sin telemetría y UI: OK');
