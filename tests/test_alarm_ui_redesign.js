/** Regresión visual/funcional del rediseño de Alarmas. Sin dependencias externas.
 * node tests/test_alarm_ui_redesign.js
 */
const fs=require('node:fs');
const vm=require('node:vm');
const assert=require('node:assert/strict');
const html=fs.readFileSync('index.html','utf8');
const scripts=[...html.matchAll(/<script(?:\s[^>]*)?>([\s\S]*?)<\/script>/g)];
assert(scripts.length>0,'No se encuentra el script de NeuroSync');
for(const [,code] of scripts)new vm.Script(code,{filename:'index.html'});
for(const id of ['ns-next-hour','ns-next-description','ns-next-countdown',
 'alarms-list','ns-alarms-tab-report','ns-alarms-tab-settings','ns-wake-list',
 'ns-wake-fired','ns-wake-dismissed','ns-wake-snoozed','alarm-permissions-open',
 'ns-mission-select','ns-mission-options','alarm-local-sound']){
 assert(html.includes('id="'+id+'"') || html.includes('id=\\"'+id+'\\"'),
  'Falta control '+id);
}
const marker=html.indexOf('function nsNextAlarmDate(');
const end=html.indexOf('function nsNextAlarmSummary()',marker);
assert(marker>=0&&end>marker,'No existe el cálculo de próxima alarma');
const ctx={Date,Number,String,Object};
vm.createContext(ctx);vm.runInContext(html.slice(marker,end),ctx);
const now=new Date(2026,10,17,12,0,0).getTime();
const input=(repeat,enabled=true,date='2026-11-16',time='06:30')=>({
 date,time,repeat,enabled,deleted:false});
const daily=ctx.nsNextAlarmDate(input('daily'),now);
assert(daily&&daily.getDate()===18&&daily.getHours()===6&&daily.getMinutes()===30);
const one=ctx.nsNextAlarmDate(input('none'),now);
assert.equal(one,null,'Una alarma única vencida no debe presentarse como próxima');
assert.equal(ctx.nsNextAlarmDate(input('daily',false),now),null,'No anunciar alarmas pausadas');
const week=ctx.nsNextAlarmDate(input('weekly'),now);
assert(week&&week.getDate()===23,'Repeticiones semanales deben usar días calendario');
const custom=ctx.nsNextAlarmDate(input('days:1,3,5'),now);
assert(custom&&custom.getDate()===18&&custom.getDay()===3,
 'Los días personalizados deben avanzar hasta el próximo día seleccionado');
assert(html.includes('name="weekDay"'),'Deben existir casillas para seleccionar días');
assert(html.includes('function nsRepeatFromEditor('),'No basta la interfaz sin conversión al modelo');
const extra=html.slice(html.indexOf('function nsAlarmEdit('),html.indexOf('function renderCalendar(){'));
for(const token of ['nsAlarmToggle(','nsAlarmEdit(','nsAlarmDuplicate(','nat.pauseAlarm',
 'setAlarmOptions','nsAlarmCanEdit('])assert(extra.includes(token),'Falta '+token);
const native=fs.readFileSync('android/app/src/main/java/com/neurosync/familia/AlarmRingActivity.java','utf8');
const store=fs.readFileSync('android/app/src/main/java/com/neurosync/familia/AlarmOptionsStore.java','utf8');
const receiver=fs.readFileSync('android/app/src/main/java/com/neurosync/familia/AlarmReceiver.java','utf8');
const bridge=fs.readFileSync('android/app/src/main/java/com/neurosync/familia/MainActivity.java','utf8');
assert(native.includes('Detener sin desafío'),'Debe existir salida de seguridad');
assert(native.includes('setShowWhenLocked(true)'),'Mantener comportamiento Android bloqueado');
assert(native.includes('AlarmOptionsStore.max(this,id)'));
assert(store.includes('static boolean configure(')&&store.includes('static boolean canSnooze('));
assert(store.includes('static boolean setAudio(')&&store.includes('static int fadeSeconds('));
const audio=fs.readFileSync('android/app/src/main/java/com/neurosync/familia/AlarmAudioService.java','utf8');
assert(audio.includes('fadeVolume(')&&audio.includes('AlarmOptionsStore.volume(this,id)'),
 'El sonido gradual debe depender de las preferencias de cada alarma');
assert(html.includes('name="volumePercent"')&&html.includes('name="gentleSeconds"'));
assert(receiver.includes('AlarmOptionsStore.consumed(ctx,id)'));
assert(receiver.includes('public static void pause(Context ctx,String id)'));
assert(receiver.includes('static boolean isDaysRepeat('));
assert(receiver.includes('wanted.contains(isoDay(c))'));
assert(receiver.includes('Math.max(System.currentTimeMillis(),original)'),
 'Una alarma sonada temprano no puede reprogramarse en la misma ocurrencia');
assert(bridge.includes('setAlarmOptions(')&&bridge.includes('pauseAlarm('));
assert(bridge.includes('setAlarmSoundBehavior('),'Falta puente de volumen y despertar gradual');
assert(html.includes('id="alarm-local-sound"'),'Debe existir el selector de tonos locales');
assert(bridge.includes('chooseAlarmSound'),'Debe mantenerse el selector de tonos Android');
console.log('Alarmas UI OK: tabs, próxima hora, editor, misiones, posponer y seguridad nativa');
