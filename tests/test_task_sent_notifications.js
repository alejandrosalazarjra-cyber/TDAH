/** Avisos de tareas enviadas: transiciones, privacidad, deduplicación. */
const fs=require('node:fs'),assert=require('node:assert/strict'),vm=require('node:vm');
const html=fs.readFileSync('index.html','utf8');
const first=html.indexOf('function nsSentStatusKey(');
const last=html.indexOf('window.neuroSyncOnSync=function(',first);
assert(first>0&&last>first,'No se encontró detector de estados de envíos');
const native=fs.readFileSync('android/app/src/main/java/com/neurosync/familia/MainActivity.java','utf8');
const backend=fs.readFileSync('android/app/src/main/java/com/neurosync/familia/SyncStore.java','utf8');
assert(native.includes('notifySentTaskStatus(String taskId,String status)'));
assert(native.includes('Notification.VISIBILITY_PRIVATE'));
assert(backend.includes('.put("sentTasks",sentSummary(database,actor.optString("username","")))'));
const cache=new Map(),notices=[],messages=[];
let fail=false;
const session={username:'mama',role:'member'};
const context={session,nat:{notifySentTaskStatus:(id,status)=>{notices.push({who:session.username,id,status});return true}},
 localStorage:{getItem:key=>cache.has(key)?cache.get(key):null,
 setItem:(key,value)=>{if(fail)throw Error('disk full');cache.set(key,String(value))}},
 say:msg=>messages.push(msg),Date,JSON,Array,Object,Set};
vm.createContext(context);
vm.runInContext(html.slice(first,last),context);
const one=status=>[{id:'sent_task-12345678',recipient:'hermano',deliveryStatus:status}];
context.nsCheckSentStatus(one('pending'),'mama');
assert.equal(notices.length,0,'Primera lectura es línea base, sin avisos históricos');
context.nsCheckSentStatus(one('accepted'),'mama');
assert.equal(notices.length,1,'Debe avisar de aceptación');
context.nsCheckSentStatus(one('accepted'),'mama');
assert.equal(notices.length,1,'No repetir una aceptación');
context.nsCheckSentStatus(one('completed'),'mama');
assert.equal(notices.length,2,'Debe avisar de finalización');
context.nsCheckSentStatus(one('completed'),'mama');
assert.equal(notices.length,2,'No repetir finalización');
session.username='hermano';
context.nsCheckSentStatus(one('accepted'),'mama');
assert.equal(notices.length,2,'No consultar avisos de otra cuenta');
context.nsCheckSentStatus(one('accepted'),'hermano');
assert.equal(notices.length,2,'La nueva cuenta construye su propia línea base');
session.username='mama';
fail=true;
context.nsCheckSentStatus([{id:'sent_second-12345678',deliveryStatus:'pending'}],'mama');
assert.equal(notices.length,2,'Sin almacenamiento local, no se crean avisos');
fail=false;
assert.equal(cache.has('ns_sent_status_v1_hermano'),true,'Estado aislado por cuenta');
console.log('Estados enviados OK: aceptación, finalización, sin duplicados y sin mezcla de cuentas');
