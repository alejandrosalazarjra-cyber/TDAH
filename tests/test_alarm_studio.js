/** QA de vistas y programador de Alarm Studio. Ejecutar: node tests/test_alarm_studio.js */
const fs=require('node:fs'),vm=require('node:vm'),assert=require('node:assert/strict');
const html=fs.readFileSync('index.html','utf8');
const scripts=[...html.matchAll(/<script(?:\s[^>]*)?>([\s\S]*?)<\/script>/g)].map(m=>m[1]);
assert(scripts.length>0);scripts.forEach(s=>new vm.Script(s));
for(const id of ['ns-alarm-studio','ns-studio-alarm-form','ns-select-mission',
 'ns-studio-mission-list','ns-report-ring','ns-alarm-settings-destination',
 'ns-alarm-list-destination','ns-weekdays','ns-next-alarm-hour']){
 assert(html.includes(id),'Falta el módulo '+id);
}
const start=html.indexOf('function nsAlarmNextDate('),end=html.indexOf('function nsAlarmStudioNext(',start);
assert(start>=0&&end>start,'Falta el cálculo de próxima alarma');
const ctx={Date,Number};vm.createContext(ctx);
vm.runInContext(html.slice(start,end),ctx);
function next(repeat,now='2026-11-17T21:00:00'){
 return ctx.nsAlarmNextDate({date:'2026-11-17',time:'07:00',repeat},new Date(now).getTime());
}
const daily=next('daily');
const tue=next('days:2');
const monday=next('days:1');
assert.equal(daily.getDate(),18,'Próxima repetición diaria');
assert.equal(tue.getDay(),2,'Máscara de martes');
assert.equal(tue.getDate(),24,'Martes siguiente');
assert.equal(monday.getDay(),1,'Máscara de lunes');
assert.equal(monday.getDate(),23,'Lunes siguiente');
assert.equal(next('days:0'),null,'Rechazar máscara vacía');
assert.equal(next('days:128'),null,'Rechazar máscara superior a 127');
const java=fs.readFileSync('android/app/src/main/java/com/neurosync/familia/AlarmReceiver.java','utf8');
const ring=fs.readFileSync('android/app/src/main/java/com/neurosync/familia/AlarmRingActivity.java','utf8');
const main=fs.readFileSync('android/app/src/main/java/com/neurosync/familia/MainActivity.java','utf8');
assert(java.includes('static int daysMask(String value)'), 'Falta programación por días');
assert(java.includes('candidate.add(java.util.Calendar.DAY_OF_YEAR,1)'),'Falta cálculo calendárico');
assert(java.includes('setSnoozeMinutes(Context ctx,String id,int minutes)'),'Falta posponer individual');
assert(ring.includes('AlarmReceiver.snoozeMinutes(this,id)'),'La pantalla debe usar la preferencia');
assert(ring.includes('Detener sin desafío'),'Se requiere salida accesible de seguridad');
assert(main.includes('setAlarmSnoozeMinutes(String id,int minutes)'));
const challenge=fs.readFileSync('android/app/src/main/java/com/neurosync/familia/AlarmChallengeStore.java','utf8');
assert(challenge.includes('validSequence(String type)'),'Misiones múltiples deben validarse');
assert(ring.includes('AlarmChallengeStore.validSequence(configured)'),'Alarma sonando debe ejecutar secuencia');
assert(html.includes('nsAlarmMissionSequence.length<5'),'Límite de 5 misiones');
assert(html.includes('ns-alarm-mission-done'),'Aplicación explícita de misiones');
assert(html.includes('ns-report-productivity'),'Informe debe enlazar con progreso');
assert(html.includes('if(!scheduleAlarm(item))return'),'No guardar como éxito si Android rechaza la alarma');
assert(html.includes('model.alarms=model.alarms.filter(function(a){return a.id!==item.id})'),'Revertir alarma nueva si falla el guardado');
assert(html.includes("var baseRender=renderAlarms"),'La vista debe compartir el render anterior');
console.log('Alarm Studio QA OK: navegación, misiones combinadas, repeticiones, posponer, informes y seguridad');
