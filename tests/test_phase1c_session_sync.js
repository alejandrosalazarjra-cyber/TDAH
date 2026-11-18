/** Fase 1c: cambios remotos no deben saltarse la persistencia o cruzar cuentas. */
const fs=require('node:fs');
const assert=require('node:assert/strict');
const vm=require('node:vm');
const source=fs.readFileSync('index.html','utf8');
const begin=source.indexOf('window.neuroSyncOnSync=function(');
const end=source.indexOf('function sync(){',begin);
assert(begin>=0&&end>begin,'Manejador de sincronización ausente');
const code=source.slice(begin,end);
function makeTest(blocked=false){
 let written=null,renders=0,notifications=[],scheduled=0,nativeNotices=[];
 const session={username:'mama',role:'user'};
 const model={tasks:[{id:'local',owner:'mama',title:'Local',updatedAt:11},
  {id:'archived',owner:'hermano',title:'Dato histórico local',updatedAt:6}],
  packs:[],alarms:[],pet:{xp:3}};
 const ctx={window:{},session,model,nsStorageRecoveryBlocked:blocked,syncing:true,
   by:()=>({set textContent(value){notifications.push(value)}}),
   nat:{scheduleAlarmRepeat:()=>true,notifyTaskReceived:(id,sender)=>{nativeNotices.push({id,sender});return true}},
   nsLocalSave:next=>{written=next; if(ctx.failWrite)throw Error('disk full')},
   active:xs=>xs.filter(x=>!x.deleted),scheduleAlarm:()=>{scheduled++},
   render:()=>renders++,say:message=>notifications.push(message),
   Date,JSON,Array,Number,String,Object,Map,Set,Error};
 vm.createContext(ctx);vm.runInContext(code,ctx);
 ctx.neuroSyncOnSync=ctx.window.neuroSyncOnSync;
 return {ctx,model,getWritten:()=>written,getRenders:()=>renders,getScheduled:()=>scheduled,notifications,nativeNotices};
}
{
 const t=makeTest();
 t.ctx.neuroSyncOnSync(JSON.stringify({ok:true,data:{
   tasks:[{id:'local',owner:'mama',title:'Remota nueva',updatedAt:22},
     {id:'foreign',owner:'hermano',title:'Privada ajena',updatedAt:500}],
   packs:[],alarms:[]}}),'mama');
 assert.equal(t.getWritten().tasks.find(x=>x.id==='local').title,'Remota nueva');
 assert(!t.getWritten().tasks.some(x=>x.id==='foreign'),'No aceptar datos privados de otra cuenta');
 assert(t.getWritten().tasks.some(x=>x.id==='archived'),'No borrar contenido local histórico');
 assert.equal(t.getRenders(),1);
}
{
 const t=makeTest();t.ctx.failWrite=true;
 t.ctx.neuroSyncOnSync(JSON.stringify({ok:true,data:{tasks:[
  {id:'local',owner:'mama',title:'Se perdería',updatedAt:300}
 ],packs:[],alarms:[]}}),'mama');
 assert.equal(t.ctx.model.tasks[0].title,'Local','No modificar el modelo visible tras fallo de escritura');
 assert.equal(t.getRenders(),0,'No simular éxito ante disco lleno');
 assert.equal(t.ctx.syncing,false);
}
{
 const t=makeTest(true);
 t.ctx.neuroSyncOnSync(JSON.stringify({ok:true,data:{tasks:[],packs:[],alarms:[]}}),'mama');
 assert.equal(t.getWritten(),null,'Nunca escribir si la recuperación está bloqueada');
}
{
 const t=makeTest();
 t.ctx.neuroSyncOnSync(JSON.stringify({ok:true,data:{tasks:[],packs:[],alarms:[]}}),'otro');
 assert.equal(t.getWritten(),null,'Descartar respuestas que pertenecen a otra sesión');
}
{
 const t=makeTest();
 t.ctx.neuroSyncOnSync(JSON.stringify({ok:false,error:'No se guardaron los cambios familiares.'}),'mama');
 assert.equal(t.getWritten(),null,'No persistir una sincronización rechazada por el servidor');
 assert.equal(t.getRenders(),0,'No renderizar datos como sincronizados si falló el guardado remoto');
 assert(t.notifications.some(x=>x.includes('No se guardaron')),
  'Mostrar el error de persistencia procedente del servidor');
}
// La primera recepción notifica una sola vez, después de persistir.
{
 const t=makeTest();
 const record={id:'sent_123456789',owner:'mama',sentBy:'hermano',title:'Comprar pan',updatedAt:40};
 const response=JSON.stringify({ok:true,data:{tasks:[record],packs:[],alarms:[]}});
 t.ctx.neuroSyncOnSync(response,'mama');
 assert.equal(t.nativeNotices.length,1,'Avisar cuando llega una tarea nueva');
 assert.equal(t.nativeNotices[0].sender,'hermano');
 t.ctx.neuroSyncOnSync(response,'mama');
 assert.equal(t.nativeNotices.length,1,'No volver a avisar al sincronizar la misma tarea');
}
{
 const t=makeTest();
 t.ctx.failWrite=true;
 t.ctx.neuroSyncOnSync(JSON.stringify({ok:true,data:{tasks:[
  {id:'sent_pending_1',owner:'mama',sentBy:'hermano',title:'No guardada',updatedAt:60}
 ],packs:[],alarms:[]}}),'mama');
 assert.equal(t.nativeNotices.length,0,'No avisar si falló el guardado local');
}
{
 const t=makeTest();
 t.ctx.neuroSyncOnSync(JSON.stringify({ok:true,data:{tasks:[
  {id:'sent_foreign_1',owner:'hermano',sentBy:'mama',title:'Ajena',updatedAt:60}
 ],packs:[],alarms:[]}}),'mama');
 assert.equal(t.nativeNotices.length,0,'No avisar de tareas privadas ajenas');
}
const javaNotices=fs.readFileSync('android/app/src/main/java/com/neurosync/familia/MainActivity.java','utf8');
assert(javaNotices.includes('notifyTaskReceived(String taskId,String sender)') &&
 javaNotices.includes('Notification.VISIBILITY_PRIVATE')&&
 javaNotices.includes('ns_received_task_notices'),
 'Android debe proteger y deduplicar las notificaciones');
const syncStore=fs.readFileSync('android/app/src/main/java/com/neurosync/familia/SyncStore.java','utf8');
assert(syncStore.includes('if(!c.getSharedPreferences("ns_family",0).edit()') &&
 syncStore.includes('AccountStore.fail("No se guardaron los cambios familiares.'),
 'El servidor no debe responder ok:true si SharedPreferences.commit() falla');
assert(syncStore.includes('updated>now+300000L'),
 'Rechazar actualizaciones fechadas demasiado lejos en el futuro');
assert(source.includes("ns_last_session_provider"),'Guardar la preferencia del proveedor');
assert(source.includes("nsPreferredProvider!=='signed_out'"),'Respetar el cierre de sesión');
assert(source.includes("nsPreferredProvider!=='google'"),'No sustituir cuenta Google ausente por admin local');
// Un login exitoso no garantiza que la sesión se conserve si falla AndroidKeyStore.
const vault=fs.readFileSync('android/app/src/main/java/com/neurosync/familia/SessionVault.java','utf8');
const main=fs.readFileSync('android/app/src/main/java/com/neurosync/familia/MainActivity.java','utf8');
assert(vault.includes('synchronized boolean save('),
 'La persistencia cifrada debe informar éxito o fracaso');
assert(vault.includes('boolean stored=prefs(ctx).edit()')&&vault.includes('return stored;'),
 'La escritura cifrada debe comprobar SharedPreferences.commit()');
assert((main.match(/boolean persisted=SessionVault\.save\(/g)||[]).length===2,
 'Ambos inicios de sesión nativos deben comprobar el guardado');
assert(main.includes('result.put("sessionPersistent",persisted)'),
 'La capa Android debe notificar el estado de persistencia');
assert(source.includes('if(res.sessionPersistent===false)say('),
 'La interfaz debe advertir al usuario si tendrá que volver a iniciar sesión');
console.log('Fase 1c OK: aislamiento, transacción local, bloqueo y sesiones');
