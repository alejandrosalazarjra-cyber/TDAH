'use strict';
/** Barreras estáticas de lanzamiento. La prueba E2E Android requiere un APK real. */
const assert=require('node:assert/strict');
const fs=require('node:fs');
const backend=fs.readFileSync('backend/server.js','utf8');
const main=fs.readFileSync('android/app/src/main/java/com/neurosync/familia/MainActivity.java','utf8');
const poll=fs.readFileSync('android/app/src/main/java/com/neurosync/familia/TaskBackgroundSync.java','utf8');
const bridge=fs.readFileSync('android/app/src/main/java/com/neurosync/familia/PublicAccountBridge.java','utf8');
const snapshot=fs.readFileSync('android/app/src/main/java/com/neurosync/familia/NativeSnapshotStore.java','utf8');
const html=fs.readFileSync('index.html','utf8');
const ui=fs.readFileSync('modules/task_studio.js','utf8');
const contract=fs.readFileSync('docs/ARQUITECTURA_CUENTAS_PUBLICAS_Y_TAREAS.md','utf8');
new (require('node:vm').Script)(backend);
new (require('node:vm').Script)(ui);
assert(backend.includes("path==='/v1/me/delete'")&&backend.includes("data.confirm!=='ELIMINAR'"),
 'Borrado remoto autenticado y explícito');
assert(backend.includes("DELETE FROM ns_public_sessions WHERE user_id=$1"),
 'Borrar sesiones al eliminar la cuenta');
assert(backend.includes("DELETE FROM ns_task_idempotency WHERE task_id IN"),
 'Limpiar claves idempotentes antes de borrar tareas');
assert(backend.includes("path==='/v1/contacts/block'")&&
 backend.includes("path==='/v1/contacts/unblock'"),
 'Bloquear y desbloquear usuarios');
assert(backend.includes("path==='/v1/events/latest'")&&backend.includes("AS task_role"),
 'El sondeo de eventos distingue emisor y destinatario');
assert(main.includes("publicDeleteAccount(String confirmation)")&&
 main.includes("NativeSnapshotStore.get(MainActivity.this).erase(account)"),
 'Android borra copias propias tras éxito remoto');
assert(bridge.includes("PublicSessionVault.clear(activity)")&&
 bridge.includes("JSONObject deleteAccount()"),'La clave de sesión nunca pasa a JS');
assert(snapshot.includes('synchronized boolean erase(String account)'),
 'SQLite proporciona borrado específico por cuenta');
assert(poll.includes("pollPublic()")&&poll.includes("PublicSessionVault.restore(this"),
 'El sondeo público usa token de bóveda cifrada');
assert(poll.includes("!token.equals(still.optString")&&
 poll.includes('Notification.VISIBILITY_PRIVATE'),
 'Revalidación de sesión y avisos privados');
assert(ui.includes('data-delete-public-account')&&ui.includes('data-contact-block')&&
 ui.includes('data-decline'), 'Controles visibles de privacidad y rechazo');
assert(html.includes('publicDeleteAccount:function()')&&html.includes('session=null;model=null;'),
 'Después del borrado no queda una sesión activa en JS');
assert(contract.includes('API pública desplegada en Render')&&
 contract.includes('el registro permanece bloqueado'),
 'El contrato no contradice el estado del entorno piloto');
console.log('Readiness básico OK: API, cuentas, bloqueo, borrado y sondeo sin secretos WebView');
