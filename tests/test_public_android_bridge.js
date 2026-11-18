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
assert(web.includes('if(session.public){nsOutboxFlush();return}'),
 'Cuentas públicas reintentan pendientes sin invocar la sincronización LAN');
assert(web.includes('if(!wasPublic&&nat&&nat.logout)nat.logout()'),
 'No revocar la sesión familiar al cerrar la pública');
assert(studio.includes('ns-ts-profile-form')&&studio.includes('ns-ts-lookup-form')&&
 studio.includes('data-invite-accept')&&studio.includes('data-invite-send'),
 'Ofrecer perfil, búsqueda e invitaciones');

// Ejercitar el adaptador JS con una cuenta pública simulada, sin credenciales reales.
const begin=web.indexOf('/* Bandeja de salida aislada');
const end=web.indexOf('\n})();\n</script>',begin);
assert(begin>=0&&end>begin,'No se encontró la API de tareas pública');
const store=new Map(),delivered=[],id='22222222-2222-4222-8222-222222222222';
let online=true;
const account={username:'public_demo',public:true,role:'user'};
const fakeNative={
 publicContacts:()=>JSON.stringify({ok:true,contacts:[{id,handle:'mama',display_name:'Mamá'}]}),
 publicSendTask:(recipient,key,title)=>{
  if(!online)throw Error('offline');
  delivered.push({recipient,key,title});
  return JSON.stringify({ok:true,task:{id:'33333333-3333-4333-8333-333333333333',status:'pending'}});
 },
 publicTasks:box=>JSON.stringify({ok:true,tasks:box==='sent'?
  [{id:'33333333-3333-4333-8333-333333333333',
    title:'Comprar pan',recipient_id:id,status:'pending'}]:[]}),
 publicMe:()=>JSON.stringify({ok:true,user:{handle:'demo',display_name:'Demo',discoverable:false}}),
 publicLookup:()=>JSON.stringify({ok:true,user:{id,handle:'mama',display_name:'Mamá'}}),
 publicInvite:()=>JSON.stringify({ok:true}),
 publicAcceptInvite:()=>JSON.stringify({ok:true}),
 publicUpdateProfile:()=>JSON.stringify({ok:true,user:{handle:'demo',display_name:'Demo'}}),
 publicTaskAction:()=>JSON.stringify({ok:true,task:{status:'accepted'}})
};
const state={window:{},session:account,model:{tasks:[],packs:[],alarms:[],pet:{}},nat:fakeNative,
 localStorage:{getItem:k=>store.get(k)||null,setItem:(k,v)=>store.set(k,String(v))},
 day:()=> '2026-11-18',uid:()=> 'request_12345678',stamp:()=>123,Date,JSON,Number,
 Array,String,Object,Set,Map,nsTemplateRead:()=>[],nsFocusRead:()=>({sessions:[]}),
 nsCleanCategory:x=>x,allTasks:()=>[],render:()=>{},save:()=>true,
 nsConfirm:async()=>true,active:x=>x,show:()=>{},openModal:()=>{}};
const publicAPI=new Function(...Object.keys(state),web.slice(begin,end)+
 '\nreturn window.NeuroTasksAPI;')(...Object.values(state));
assert.equal(publicAPI.publicAccount(),true);
assert.equal(publicAPI.contacts()[0].id,id,'Contactos públicos se seleccionan por UUID');
assert.equal(publicAPI.publicLookup('@mama').user.handle,'mama');
assert(publicAPI.publicInvite(id).ok);
assert(publicAPI.publicUpdateProfile('demo','Demo',true).ok);
const created=publicAPI.sendTo(id,{id:'send_task_1234',title:'Comprar pan'});
assert(created.ok&&!created.queued,'Debe confirmar entrega remota');
assert.equal(delivered[0].recipient,id);
assert.equal(JSON.parse(delivered[0].title).title,'Comprar pan',
 'El puente debe enviar un JSON íntegro, no solo el título');
assert.equal(publicAPI.sent().tasks[0].deliveryStatus,'pending');
assert(publicAPI.updateReceived('33333333-3333-4333-8333-333333333333','accepted').ok);
online=false;
const queued=publicAPI.sendTo(id,{id:'send_task_4567',title:'Sin conexión'});
assert(queued.queued,'Envío sin conexión debe quedar en cola');
assert.equal(publicAPI.outbox().length,1);
online=true;
assert.equal(publicAPI.retryOutbox().delivered,1);
assert.equal(publicAPI.outbox().length,0);

// Un rechazo de autorización HTTP no puede presentarse como una pérdida de internet.
fakeNative.publicSendTask=()=>JSON.stringify({ok:false,httpStatus:403,error:'Contacto no autorizado'});
const rejection=publicAPI.sendTo(id,{id:'send_task_unauth_123',title:'Solo contactos'});
assert.equal(rejection.ok,false,'Un rechazo de servidor no debe parecer envío pendiente');
assert.equal(publicAPI.outbox()[0].failed,'Contacto no autorizado');
assert.equal(publicAPI.sent().tasks[0].deliveryStatus,'failed',
 'La bandeja distingue rechazos reales de desconexiones');
const countBefore=publicAPI.outbox().length;
publicAPI.retryOutbox();
assert.equal(publicAPI.outbox().length,countBefore,
 'Los envíos rechazados no se reintentan automáticamente');

console.log('Puente público Android OK: sesiones separadas, permisos y UI opt-in');
