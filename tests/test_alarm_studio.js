/** Alarm Studio: comprobar integración real, seguridad y módulos, sin Android. */
const fs=require('node:fs'),vm=require('node:vm'),assert=require('node:assert/strict');
const html=fs.readFileSync('index.html','utf8');
const js=fs.readFileSync('modules/alarm_studio.js','utf8');
const css=fs.readFileSync('modules/alarm_studio.css','utf8');
const receiver=fs.readFileSync('android/app/src/main/java/com/neurosync/familia/AlarmReceiver.java','utf8');
const ring=fs.readFileSync('android/app/src/main/java/com/neurosync/familia/AlarmRingActivity.java','utf8');
const options=fs.readFileSync('android/app/src/main/java/com/neurosync/familia/AlarmOptionsStore.java','utf8');
const main=fs.readFileSync('android/app/src/main/java/com/neurosync/familia/MainActivity.java','utf8');
const workflow=fs.readFileSync('.github/workflows/android-apk.yml','utf8');
assert(main.includes('"/modules/alarm_studio.js"')&&main.includes('"/modules/alarm_studio.css"'),
 'Los módulos deben cargarse desde los assets internos protegidos de Android');
assert(workflow.includes('cp modules/alarm_studio.js modules/alarm_studio.css android/app/src/main/assets/modules/'),
 'La APK debe empaquetar los módulos');
new vm.Script(js,{filename:'alarm_studio.js'});
assert(html.includes('src="./modules/alarm_studio.js"'));
assert(html.includes('href="./modules/alarm_studio.css"'));
for(const name of ['home','editor','missions','settings','report'])
 assert(js.includes("function "+name+"("),'Falta pantalla '+name);
for(const mission of ['math','math_hard','typing','memory','shake','taps','qr','routine_five'])
 assert(js.includes("key:'"+mission+"'"),'Falta misión '+mission);
assert(ring.includes('setShowWhenLocked(true)'));
assert(ring.includes('Detener sin desafío'),'La salida segura es obligatoria');
assert(ring.includes('routine_five')&&ring.includes('AlarmOptionsStore.minutes'));
assert(receiver.includes('AlarmOptionsStore.max'));
assert(receiver.includes('"weekdays".equals(repeat)')&&receiver.includes('"weekends".equals(repeat)'));
assert(options.includes('getSharedPreferences'));
const audio=fs.readFileSync('android/app/src/main/java/com/neurosync/familia/AlarmAudioService.java','utf8');
assert(audio.includes('beginVolumeRamp()')&&audio.includes('setVolume(volume,volume)'));
assert(main.includes('setAlarmFadeSeconds(int seconds)')&&main.includes('getAlarmFadeSeconds()'));
assert(js.includes('id="ns-as-fade"')&&js.includes('fadeSeconds:+'));

assert(!js.includes('localStorage.setItem('),'No crear una base de alarmas paralela');
assert(!js.includes('fetch('),'Interfaz local sin peticiones externas');
assert(css.includes('prefers-reduced-motion')&&css.includes(':focus-visible'));
const from=html.indexOf('window.NeuroAlarmAPI={');
const to=html.indexOf('\n})();\n</script>',from);
assert(from>0&&to>from,'Falta conexión a los datos originales');
const window={dispatchEvent:()=>{}};
const account={username:'propietario',role:'user'};
const model={tasks:[],packs:[],alarms:[],pet:{xp:0}};
const nat={setAlarmChallenge:()=>{},setAlarmOptions:()=>{},cancelAlarm:()=>{}};
let writes=0,schedules=0;
const context={window,session:account,model,nat,Date,Number,String,Object,Array,
 JSON,Map,Set,Event:class Event{constructor(type){this.type=type}},
 uid:()=>String(Date.now())+'_new',day:()=> '2026-11-18',stamp:()=>Date.now(),
 active:xs=>xs.filter(x=>!x.deleted),owned:a=>a.owner===account.username,
 save:()=>{writes++;return true},render:()=>{},
 mark:a=>{a.updatedAt=Date.now()},scheduleAlarm:()=>{schedules++;return true},
 nsConfirm:()=>Promise.resolve(true),openModal:()=>{},by:()=>null,
 nsSleepRecords:()=>[],checkAlarmPermissions:()=>{}};
vm.createContext(context);
vm.runInContext(html.slice(from,to),context,{filename:'studio_bridge.js'});
const api=context.window.NeuroAlarmAPI;
assert(api.ready());
const tomorrow=new Date(Date.now()+86400000);
const d=[tomorrow.getFullYear(),String(tomorrow.getMonth()+1).padStart(2,'0'),String(tomorrow.getDate()).padStart(2,'0')].join('-');
const created=api.createOrUpdate({title:'Trabajo',date:d,time:'07:00',repeat:'weekdays',challenge:'routine_five',snoozeMinutes:10,snoozeLimit:2});
assert(created.ok,'Debe guardar una alarma válida');
assert.equal(model.alarms.length,1);
assert.equal(model.alarms[0].challenge,'routine_five');
assert.equal(model.alarms[0].repeat,'weekdays');
assert.equal(writes,1);assert.equal(schedules,1);
assert(api.toggle(created.id,false),'Debe poder desactivar');
assert.equal(model.alarms[0].enabled,false);
assert(api.toggle(created.id,true),'Debe poder reactivar');
assert.equal(schedules,2);
const foreign={id:'foreign',title:'Privada',date:d,time:'09:00',owner:'hermano',enabled:true};
model.alarms.push(foreign);
assert.equal(api.list().length,1,'Solo mostrar propias');
assert(!api.toggle('foreign',false),'No modificar cuentas ajenas');
assert(!api.createOrUpdate({title:'No',date:d,time:'08:00'},'foreign').ok);
const invalid=api.createOrUpdate({title:'Imposible',date:'2026-02-30',time:'08:00',repeat:'daily'});
assert(!invalid.ok,'Rechazar fechas imposibles');
console.log('Alarm Studio: módulos, permisos, repeticiones, guardado y seguridad OK');
