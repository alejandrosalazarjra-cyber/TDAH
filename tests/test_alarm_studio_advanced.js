/** Fase avanzada de Alarm Studio: validación de funciones reales y seguridad.
 * Ejecutar: node tests/test_alarm_studio_advanced.js
 */
const fs=require('node:fs'),vm=require('node:vm'),assert=require('node:assert/strict');
const source=fs.readFileSync('index.html','utf8');
const ui=fs.readFileSync('modules/alarm_studio.js','utf8');
const challenge=fs.readFileSync('android/app/src/main/java/com/neurosync/familia/AlarmChallengeStore.java','utf8');
const ringing=fs.readFileSync('android/app/src/main/java/com/neurosync/familia/AlarmRingActivity.java','utf8');
const audio=fs.readFileSync('android/app/src/main/java/com/neurosync/familia/AlarmAudioService.java','utf8');
const main=fs.readFileSync('android/app/src/main/java/com/neurosync/familia/MainActivity.java','utf8');
const manifest=fs.readFileSync('android/app/src/main/AndroidManifest.xml','utf8');
new vm.Script(ui,{filename:'modules/alarm_studio.js'});
for(const code of ['walk','photo','barcode','routine_custom']){
 assert(ui.includes("key:'"+code+"'"),'Misión ausente: '+code);
 assert(challenge.includes('"'+code+'".equals(type)'),'Motor no acepta misión: '+code);
 assert(source.includes("'"+code+"'"),'Editor no acepta: '+code);
}
assert(ringing.includes('Sensor.TYPE_STEP_DETECTOR'),'Contador debe utilizar sensor de pasos real');
assert(manifest.includes('android.permission.ACTIVITY_RECOGNITION'),'Permiso de actividad explícito');
assert(ringing.includes('ACTION_IMAGE_CAPTURE'),'Foto debe abrir cámara del sistema');
assert(ringing.includes('instanceof android.graphics.Bitmap'),'Foto debe confirmar imagen real');
assert(ringing.includes('Detener sin desafío'),'Salida de seguridad no se puede quitar');
assert(ringing.includes('AlarmChallengeStore.getSequence(this,id)'));
assert(challenge.includes('setSequence(')&&challenge.includes('getSequence('));
assert(challenge.includes('input.length()<2||input.length()>5'));
assert(main.includes('scanBarcodeReference()')&&ringing.includes('ALL_CODE_TYPES'));
assert(main.includes('selectedAlarmSoundId')&&audio.includes('ns_alarm_sounds'));
assert(audio.includes('beginVolume()')&&audio.includes('AlarmOptionsStore.vibrates'),
 'Volumen gradual y vibración deben utilizar las opciones reales');
assert(main.includes('setAlarmSoundOptions(')&&
 fs.readFileSync('android/app/src/main/java/com/neurosync/familia/AlarmOptionsStore.java','utf8').includes('setAudio('),
 'El editor debe guardar sonido y vibración por alarma');
assert(ui.includes('name="volumePercent"')&&ui.includes('name="vibration"')&&ui.includes('name="gentleWake"'),
 'Los ajustes de audio deben ser editables');
assert(main.includes('takePersistableUriPermission'));
assert(ui.includes('data-sequence-step'),'El editor debe permitir ordenar retos');
assert(ui.includes("action('Elegir tono para esta alarma','sound-alarm')"),'Falta selector de audio individual');
const start=source.indexOf('window.NeuroAlarmAPI={');
const end=source.indexOf('\n})();\n</script>',start);
assert(start>=0&&end>start,'Falta puente de alarmas');
const account={username:'principal',role:'user'},events=[],model={tasks:[],packs:[],alarms:[],pet:{xp:3}};
const nat={setAlarmChallenge:()=>{},setAlarmOptions:()=>{},
 setAlarmMissionSequence:(id,json)=>{events.push(['sequence',id,JSON.parse(json)]);return true},
 setAlarmQrCode:(id,raw)=>{events.push(['qr',id,raw])},
 cancelAlarm:()=>{}};
const ctx={window:{dispatchEvent:()=>{}},session:account,model,nat,
 Date,Number,String,Object,Array,JSON,Map,Set,
 Event:class Event{constructor(name){this.type=name}},
 uid:()=> 'id_'+(model.alarms.length+1),day:()=> '2026-12-01',stamp:()=>Date.now(),
 active:xs=>xs.filter(x=>!x.deleted),owned:x=>x.owner===account.username,
 save:()=>true,render:()=>{},mark:()=>{},scheduleAlarm:()=>true,
 nsConfirm:()=>Promise.resolve(true),openModal:()=>{},by:()=>null,
 nsSleepRecords:()=>[],nsFocusRead:()=>({sessions:[]}),checkAlarmPermissions:()=>{}};
vm.createContext(ctx);
vm.runInContext(source.slice(start,end),ctx,{filename:'alarm_bridge.js'});
const api=ctx.window.NeuroAlarmAPI;
const date=new Date(Date.now()+48*3600000);
const day=[date.getFullYear(),String(date.getMonth()+1).padStart(2,'0'),String(date.getDate()).padStart(2,'0')].join('-');
function create(partial){return api.createOrUpdate(Object.assign({title:'Prueba',date:day,time:'09:00',repeat:'daily'},partial))}
assert(!create({challenge:'routine_custom',sequence:['math']}).ok,'Una misión no es secuencia');
assert(!create({challenge:'routine_custom',sequence:['math','invalid']}).ok,'Secuencia inválida');
const result=create({challenge:'routine_custom',sequence:['math','typing','walk','photo']});
assert(result.ok,'Secuencia válida debe crearse');
assert.deepEqual(Array.from(model.alarms[0].sequence),['math','typing','walk','photo']);
assert(events.some(e=>e[0]==='sequence'&&e[2].length===4));
const qr=create({challenge:'barcode',qrReady:'EJEMPLO-123'});
assert(qr.ok&&events.some(e=>e[0]==='qr'&&e[2]==='EJEMPLO-123'));
assert(!create({challenge:'barcode'}).ok,'Sin referencia no se puede programar un código nuevo');
console.log('Alarm Studio avanzado OK: pasos, foto, barcode, secuencia, sonidos y validación');
