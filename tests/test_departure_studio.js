/** Regresión de listas de salida, guardado, horarios, cuentas y pantalla nativa. */
const fs=require('node:fs'),assert=require('node:assert/strict'),vm=require('node:vm');
const html=fs.readFileSync('index.html','utf8');
const js=fs.readFileSync('modules/departure_studio.js','utf8');
const css=fs.readFileSync('modules/departure_studio.css','utf8');
const receiver=fs.readFileSync('android/app/src/main/java/com/neurosync/familia/DepartureReminderReceiver.java','utf8');
const activity=fs.readFileSync('android/app/src/main/java/com/neurosync/familia/DepartureChecklistActivity.java','utf8');
const bridge=fs.readFileSync('android/app/src/main/java/com/neurosync/familia/MainActivity.java','utf8');
const manifest=fs.readFileSync('android/app/src/main/AndroidManifest.xml','utf8');
const workflow=fs.readFileSync('.github/workflows/android-apk.yml','utf8');
new vm.Script(js);
assert(js.includes('data-new-item')&&js.includes('data-object'),'Editor sin separar objetos por punto y coma');
assert(js.includes('data-enable-time')&&js.includes('data-schedule'),'Fecha/hora opcionales');
assert(js.includes('data-preset'),'Plantillas por destino');
assert(js.includes('data-mark'),'Checks independientes');
assert(css.includes('ns-departure-overlay')&&css.includes(':focus-visible'));
assert(receiver.includes('ACTION_BOOT_COMPLETED')&&receiver.includes('setFullScreenIntent(fullscreen,true)'));
assert(activity.includes('setShowWhenLocked(true)')&&activity.includes('DepartureReminderReceiver.mark(this,id,position,active)'));
assert(activity.includes('if(allDone())'),'No declarar terminada la revisión sin todos los checks');
assert(bridge.includes('scheduleDepartureList(')&&bridge.includes('departureListState('));
assert(bridge.includes('departureKey(String username,String id)')&&bridge.includes('canAccessSnapshot(username)'));
assert(html.includes('nat.departureListState(session.username,p.id)'));
assert(html.includes("if(kind==='bag'&&window.NeuroDepartureStudio)"));
assert(bridge.includes('"/modules/departure_studio.js"'));
assert(manifest.includes('android:name=".DepartureChecklistActivity"')&&manifest.includes('android:name=".DepartureReminderReceiver"'));
assert(workflow.includes('cp modules/departure_studio.js modules/departure_studio.css'));
const from=html.indexOf('window.NeuroDepartureAPI={'),to=html.indexOf('\n};',from)+3;
assert(from>0&&to>from);
const packs=[],native=new Map(),user={username:'owner',role:'user'},model={packs,tasks:[],alarms:[],pet:{xp:0}};
let saves=0,serial=0;const now=new Date(Date.now()+86400000),date=[now.getFullYear(),String(now.getMonth()+1).padStart(2,'0'),String(now.getDate()).padStart(2,'0')].join('-');
const nat={
 scheduleDepartureList:(user,id,title,itemsJson,when,repeat)=>{if(user!=='owner')return false;native.set(id,{id,title,items:JSON.parse(itemsJson),when,repeat,checked:{}});return true},
 cancelDepartureList:(user,id)=>{if(user==='owner')native.delete(id)},
 departureListState:(user,id)=>JSON.stringify(user==='owner'?native.get(id)||{}:{}),
 markDepartureItem:(user,id,i,checked)=>{const p=user==='owner'?native.get(id):null;if(!p)return false;p.checked[i]=checked;return true}
};
const args={session:user,model,nat,window:{},Date,Number,JSON,Array,Object,Set,Map,
 uid:()=> 'list_'+(++serial),stamp:()=>Date.now(),day:()=>date,
 active:x=>x.filter(p=>!p.deleted),mark:x=>x.updatedAt=Date.now(),save:()=>{saves++;return true},
 render:()=>{},nsConfirm:()=>Promise.resolve(true)};
const api=new Function(...Object.keys(args),html.slice(from,to)+'\nreturn window.NeuroDepartureAPI;')(...Object.values(args));
let result=api.save({place:'Gimnasio',items:['Agua','Toalla','Candado'],date,time:'19:00',repeat:'daily'});
assert(result.ok);assert.equal(packs.length,1);assert.equal(native.size,1);
assert.equal(packs[0].items.length,3);
assert(api.check(result.id,0,true));assert.equal(packs[0].checked[0],true);
assert.equal(native.get(result.id).checked[0],true);
// Los checks marcados en la pantalla nativa se recuperan al volver a la WebView.
nat.markDepartureItem('owner',result.id,1,true);
assert.equal(api.list()[0].checked[1],true);
packs.push({id:'another',owner:'brother',place:'Privada',items:['Clave'],checked:{}});
assert(!api.check('another',0,true),'Prohibir cambios en lista de otra cuenta');
assert.equal(api.list().length,1,'No revelar listas de otras cuentas');
assert(!api.save({place:'Sin objetos',items:[]}).ok);
result=api.save({place:'Manual',items:['Llaves','Billetera'],date,time:'',repeat:'none'});
assert(result.ok);
assert.equal(native.size,1,'Una lista sin horario no necesita alarma nativa');
assert(saves>=3);
console.log('Listas de salida: editor, checklist, horario, sincronización, seguridad y assets OK');
