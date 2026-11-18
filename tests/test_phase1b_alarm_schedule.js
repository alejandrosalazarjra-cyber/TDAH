/** Phase 1b: verifica que los horarios repetidos no se desplacen por sumar milisegundos. */
const fs=require('node:fs');
const vm=require('node:vm');
const assert=require('node:assert/strict');
const html=fs.readFileSync('index.html','utf8');
const start=html.indexOf('function scheduleAlarm(a){');
const end=html.indexOf('function checkAlarmPermissions(){',start);
assert(start>0&&end>start,'No se encuentra el programador de alarmas');
const logic=html.slice(start,end);
assert(!/when\s*\+=\s*\d+\s*\*\s*86400000/.test(logic),
  'No sumar intervalos fijos a los horarios repetidos');
let scheduled=null,warning=[];
const nat={
  scheduleAlarmRepeat:(id,title,when,repeat)=>{scheduled={id,title,when,repeat};return true;},
  setAlarmChallenge:()=>{}
};
const sandbox={nat,Date,Number,JSON,say:t=>warning.push(t)};
vm.createContext(sandbox);
vm.runInContext(logic,sandbox);
let previous='2025-03-15',reference=new Date(previous+'T06:45:00').getTime();
assert.equal(sandbox.scheduleAlarm({id:'daily1',title:'Despertar',date:previous,time:'06:45',repeat:'daily'}),true);
assert.equal(scheduled.when,reference,'Pasar la hora original al Calendar nativo');
assert.equal(scheduled.repeat,'daily');
assert.equal(sandbox.scheduleAlarm({id:'once1',title:'Ya pasó',date:previous,time:'06:45',repeat:'none'}),false,
 'Una alarma única vencida debe rechazarse antes de entrar a Android');
assert.equal(sandbox.scheduleAlarm({id:'invalid',date:'invalid',time:'xx:xx',repeat:'daily'}),false);
assert.equal(sandbox.scheduleAlarm({id:'wrongDay',date:'2026-02-30',time:'08:00',repeat:'daily'}),false);
assert.equal(sandbox.scheduleAlarm({id:'badClock',date:'2026-12-01',time:'25:00',repeat:'daily'}),false);
assert.equal(sandbox.scheduleAlarm({id:'badRepeat',date:'2026-12-01',time:'09:00',repeat:'invalid'}),false);
assert(warning.some(x=>x.includes('inválida')));
const receiver=fs.readFileSync('android/app/src/main/java/com/neurosync/familia/AlarmReceiver.java','utf8');
assert(receiver.includes('calendar.add(unit,amount)'),'Las repeticiones deben usar Calendar');
assert(receiver.includes('public static JSONObject diagnostics(Context ctx)'));
assert(receiver.includes('"every2".equals(repeat)'),'La repetición personalizada debe validarse');
const scheduleSection=receiver.slice(receiver.indexOf('public static boolean scheduleRepeat('),receiver.indexOf('private static boolean program('));
const successSection=scheduleSection.slice(scheduleSection.indexOf('if(!program(ctx,id,when))'));
assert(successSection.includes('manager(ctx).cancel(pendingSnooze(ctx,id))'),
 'Al editar, cancelar posposición anterior solamente después de programar correctamente');
assert(successSection.includes('remove(SNOOZE_PREFIX+id)'),
 'Al editar, eliminar el registro persistente de la posposición anterior');
assert(successSection.includes('AlarmOptionsStore.reset(ctx,id)'),
 'La nueva programación debe reiniciar el contador de posposiciones');
const main=fs.readFileSync('android/app/src/main/java/com/neurosync/familia/MainActivity.java','utf8');
assert(main.includes('alarmScheduleDiagnostics()'),'Falta diagnóstico Android en JS bridge');
console.log('Fase 1b: horarios locales, validación y diagnóstico Android OK');
