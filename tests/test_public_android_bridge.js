/** Conexión pública opcional de APK: no se debe romper el modo familiar. */
'use strict';
const fs=require('node:fs'),assert=require('node:assert/strict'),vm=require('node:vm');
const root='android/app/src/main/java/com/neurosync/familia/';
const main=fs.readFileSync(root+'MainActivity.java','utf8');
const bridge=fs.readFileSync(root+'PublicAccountBridge.java','utf8');
const vault=fs.readFileSync(root+'PublicSessionVault.java','utf8');
const familyVault=fs.readFileSync(root+'SessionVault.java','utf8');
const gradle=fs.readFileSync('android/app/build.gradle','utf8');
const web=fs.readFileSync('index.html','utf8');
const studio=fs.readFileSync('modules/task_studio.js','utf8');

new vm.Script(studio,{filename:'modules/task_studio.js'});
for(const match of web.matchAll(/<script(?:[^>]*)>([\s\S]*?)<\/script>/g)){
 if(match[1].trim())new vm.Script(match[1],{filename:'index.inline.js'});
}
assert(gradle.includes('PUBLIC_API_BASE_URL')&&gradle.includes('PUBLIC_GOOGLE_CLIENT_ID'),
 'Configurar endpoint y cliente OAuth en compilación');
assert(gradle.includes('NEUROSYNC_API_BASE_URL')&&gradle.includes('NEUROSYNC_GOOGLE_WEB_CLIENT_ID'),
 'El desarrollador, no el usuario, debe configurar la conexión');
assert(bridge.includes('.requestIdToken(BuildConfig.PUBLIC_GOOGLE_CLIENT_ID)'),
 'Usar ID token específico del backend, no token de Google Drive');
assert(bridge.includes('PublicSessionVault.save(')&&bridge.includes('PublicSessionVault.restore('),
 'Persistir y restaurar sesión pública cifrada');
assert(!bridge.includes('addJavascriptInterface'),
 'El cliente de la API no debe exponer tokens mediante JavaScript');
assert(bridge.includes('if(status==401)PublicSessionVault.clear(activity)'),
 'Invalidar sesión ante error de autorización');
assert(vault.includes('NeuroSyncPublicSession')&&familyVault.includes('NeuroSyncFamilySession'),
 'Claves AndroidKeyStore independientes');
assert(vault.includes('ns_secure_public_session')&&familyVault.includes('ns_secure_session'),
 'Preferencias cifradas separadas');
assert(main.includes('PublicAccountBridge.SIGNIN_REQUEST')&&
 main.includes('@JavascriptInterface public String publicContacts()')&&
 main.includes('@JavascriptInterface public String publicSendTask('),
 'Login, contactos y envío disponibles para la interfaz');
assert(web.includes('id="ns-public-login"')&&
 web.includes('nat.publicAvailable&&nat.publicAvailable()'),
 'Mostrar login público solo cuando está configurado');
assert(web.includes("u.public?'public':u.google?'google':'owner'"),
 'Recordar proveedor sin confundir Google Drive');
assert(web.includes('if(!session||session.google||session.public||!nat||syncing)return;'),
 'Nunca ejecutar la sincronización LAN con una cuenta pública');
assert(web.includes('if(!wasPublic&&nat&&nat.logout)nat.logout()'),
 'No revocar la sesión familiar al cerrar la pública');
assert(studio.includes('ns-ts-profile-form')&&studio.includes('ns-ts-lookup-form')&&
 studio.includes('data-invite-accept')&&studio.includes('data-invite-send'),
 'Ofrecer perfil, búsqueda e invitaciones');
console.log('Puente público Android OK: sesiones separadas, permisos y UI opt-in');
