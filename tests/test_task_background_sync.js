/** Verificaciones estáticas del sondeo JobScheduler sin dependencia de Android SDK local. */
const fs=require('node:fs'),assert=require('node:assert/strict');
const root='android/app/src/main/';
const worker=fs.readFileSync(root+'java/com/neurosync/familia/TaskBackgroundSync.java','utf8');
const manifest=fs.readFileSync(root+'AndroidManifest.xml','utf8');
const main=fs.readFileSync(root+'java/com/neurosync/familia/MainActivity.java','utf8');
const sync=fs.readFileSync(root+'java/com/neurosync/familia/SyncStore.java','utf8');
const server=fs.readFileSync(root+'java/com/neurosync/familia/SyncService.java','utf8');
assert(manifest.includes('android:name=".TaskBackgroundSync"')&&
 manifest.includes('android.permission.BIND_JOB_SERVICE'),'Servicio JobScheduler registrado');
assert(worker.includes('extends JobService')&&worker.includes('.setPeriodic(15*60*1000L)'),
 'Job periódico no invasivo');
assert(worker.includes('JobInfo.NETWORK_TYPE_ANY')&&worker.includes('setPersisted(false)'),
 'No despertar sin conectividad ni antes del desbloqueo tras reinicio');
assert(worker.includes('SessionVault.restore(this,mode,location)'),
 'Token de sesión disponible solo desde la bóveda cifrada');
assert(worker.includes('getSharedPreferences("MainActivity",MODE_PRIVATE)'),
 'Servicio usa mismas preferencias de MainActivity');
assert(!worker.includes('getPreferences(0)'),'Los JobService no tienen Activity.getPreferences()');
assert(worker.includes('Notification.VISIBILITY_PRIVATE'),'Notificaciones discretas');
assert(worker.includes('username.equals(current.optJSONObject("user")'),
 'Revalidar la cuenta antes de mostrar avisos');
assert(worker.includes('if(previous==null)return'),'La primera consulta no debe inundar notificaciones');
assert(main.includes('TaskBackgroundSync.cancel(this)')&&
 main.includes('if(persisted)TaskBackgroundSync.schedule(MainActivity.this)'),
 'Cancelar al cerrar sesión y programar solo tras guardar cifrado');
assert(server.includes('route.equals("/notification-digest")')&&
 sync.includes('public static JSONObject notificationDigest('),
 'Ruta de sondeo protegida por sesión');
assert(sync.includes('username.equals(task.optString("owner"))')&&
 sync.includes('sentSummary(read(c),username)'),
 'Respuesta limitada a tareas recibidas y enviadas del usuario');
console.log('JobScheduler OK: sesión cifrada, permisos, privacidad y sondeo de tareas');
