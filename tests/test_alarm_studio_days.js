/** Regresiones de recurrencia personalizada y posposición segura.
 * Ejecutar desde la raíz con node tests/test_alarm_studio_days.js
 */
const fs=require('node:fs'),assert=require('node:assert/strict'),vm=require('node:vm');
const html=fs.readFileSync('index.html','utf8');
const studio=fs.readFileSync('modules/alarm_studio.js','utf8');
const receiver=fs.readFileSync('android/app/src/main/java/com/neurosync/familia/AlarmReceiver.java','utf8');
const ring=fs.readFileSync('android/app/src/main/java/com/neurosync/familia/AlarmRingActivity.java','utf8');
new vm.Script(studio);for(const m of html.matchAll(/<script(?:[^>]*)>([\s\S]*?)<\/script>/g))
 if(m[1].trim())new vm.Script(m[1]);
assert(studio.includes("['custom','Elegir días']"),'El editor debe permitir elegir días');
assert(studio.includes('data-weekday='),'Cada día debe tener un control');
assert(studio.includes("repeat.charAt(5+w)==='1'"),'La cuenta regresiva debe respetar la máscara');
assert(receiver.includes('repeat.matches("days:[01]{7}")'),'Android debe validar máscaras de días');
assert(receiver.includes("bits.charAt(dow)=='1'"),'Android debe calcular solo días seleccionados');
assert(html.includes("repeat.slice(5).includes('1')"),'No permitir una máscara sin ningún día');
assert(receiver.includes('public static boolean snooze('),'Posponer debe informar si falla');
const scheduled=receiver.indexOf('alarm.setExactAndAllowWhileIdle',receiver.indexOf('public static boolean snooze'));
const consumed=receiver.indexOf('AlarmOptionsStore.increment(ctx,id)',receiver.indexOf('public static boolean snooze'));
assert(scheduled>0&&consumed>scheduled,'No gastar posposición antes de programarla');
assert(receiver.includes('if(!ctx.getSharedPreferences("ns_alarm_snoozes",0).edit().putLong'));
assert(ring.includes('if(AlarmReceiver.snooze(this,id,snoozeMinutes))finish()'),
 'No cerrar pantalla sin posponer correctamente');
const from=html.indexOf('window.NeuroAlarmAPI={'),to=html.indexOf('\n};',from)+3;
assert(from>0&&to>from,'API de alarma no disponible');
const apiCode=html.slice(from,to),u={username:'a',role:'user'};
const model={tasks:[],packs:[],alarms:[],pet:{xp:0,food:0}};
let alarm=null,store=null;const bridge={
 window:{dispatchEvent:()=>{}},session:u,model,Date,Number,Array,JSON,Set,Map,Object,
 Event:class Event{constructor(type){this.type=type}},
 uid:()=>'1234',stamp:()=>Date.now(),day:()=> '2026-11-18',
 active:l=>l.filter(x=>!x.deleted),owned:a=>a.owner===u.username,
 save:()=>true,render:()=>{},mark:()=>{},scheduleAlarm:a=>{alarm=a;return true},
 nat:{setAlarmOptions:()=>{},cancelAlarm:()=>{}},nsConfirm:async()=>true,by:()=>null,
 nsSleepRecords:()=>[],nsFocusRead:()=>({sessions:[]}),checkAlarmPermissions:()=>{}
};
const api=new Function(...Object.keys(bridge),apiCode+'\nreturn window.NeuroAlarmAPI;')(...Object.values(bridge));
const d=new Date(Date.now()+86400000),date=[d.getFullYear(),String(d.getMonth()+1).padStart(2,'0'),String(d.getDate()).padStart(2,'0')].join('-');
assert(api.createOrUpdate({date,time:'08:00',title:'Alarma',repeat:'days:0101010',challenge:'math'}).ok);
assert.equal(alarm.repeat,'days:0101010');
assert(!api.createOrUpdate({date,time:'08:00',repeat:'days:0000000'}).ok||model.alarms.length===1,
 'No debe crear una máscara sin días');
console.log('Días personalizados y posposición segura: verificaciones OK');
