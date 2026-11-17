/** Fase 1e: persistencia local inmediata y copias cifradas agrupadas.
 * Ejecutar: node tests/test_phase1e_snapshot_batch.js
 */
const fs=require('node:fs'),vm=require('node:vm'),assert=require('node:assert/strict');
const html=fs.readFileSync('index.html','utf8');
const start=html.indexOf('function nsValidModel(');
const end=html.indexOf('var nsDriveBackupTimer=null;',start);
assert(start>0&&end>start,'No se localizaron los módulos de persistencia');
const local=new Map(),snapshots=new Map(),timers=new Map();
let next=0,nativeWrites=0,shouldFail=false;
const storage={
 getItem:k=>local.has(k)?local.get(k):null,
 setItem:(k,v)=>local.set(k,String(v)),
 removeItem:k=>local.delete(k)
};
const account={username:'user_a',role:'user'};
const nat={
 recoverNativeSnapshot:user=>snapshots.has(user)
  ?JSON.stringify({ok:true,model:JSON.parse(snapshots.get(user))})
  :JSON.stringify({ok:false}),
 saveNativeSnapshot:(user,json)=>{nativeWrites++;if(shouldFail)return false;snapshots.set(user,json);return true}
};
const context={nat,session:account,localStorage:storage,
 nsStorageRecoveryBlocked:false,localKey:()=> 'ns_v2_'+account.username,
 setTimeout:(fn,delay)=>{const id=++next;timers.set(id,{fn,delay});return id},
 clearTimeout:id=>timers.delete(id),by:()=>null,say:()=>{},
 blank:()=>({tasks:[],packs:[],alarms:[],pet:{xp:0}}),
 Date,JSON,Number,String,Array,Error,Math,Map};
vm.createContext(context);
vm.runInContext(html.slice(start,end),context);
function model(x){return {tasks:[{id:'1',owner:account.username,title:'Tarea '+x}],
 packs:[],alarms:[],pet:{xp:x}};}
for(let i=1;i<=30;i++)context.nsLocalSave(model(i));
assert.equal(JSON.parse(local.get('ns_v2_user_a')).pet.xp,30,
 'La última escritura al WebView debe ser inmediata');
assert.equal(nativeWrites,0,'No efectuar treinta cifrados SQLite en primer plano');
assert.equal(timers.size,1,'Una sola copia nativa pendiente');
assert.equal(context.nsFlushNativeSnapshot(),true);
assert.equal(nativeWrites,1,'Una copia por lote de cambios');
assert.equal(JSON.parse(snapshots.get('user_a')).pet.xp,30);
assert.equal(timers.size,0,'No conservar temporizadores después de un volcado');

context.nsLocalSave(model(31));
shouldFail=true;
assert.equal(context.nsFlushNativeSnapshot(),false,'El fallo nativo debe detectarse');
assert.equal(JSON.parse(snapshots.get('user_a')).pet.xp,30,
 'La copia válida anterior nunca se sobrescribe cuando falla un cifrado');
assert(context.nsNativePending,'Conservar última revisión en memoria para reintentar');
shouldFail=false;
assert.equal(context.nsFlushNativeSnapshot(),true);
assert.equal(JSON.parse(snapshots.get('user_a')).pet.xp,31);

context.nsLocalSave(model(32));
account.username='user_b';
assert.equal(context.nsFlushNativeSnapshot(),false,
 'No escribir la copia de A desde la sesión B');
assert(!snapshots.has('user_b'),'Sin fuga de datos entre cuentas');
assert(html.includes("window.addEventListener('pagehide',function(){nsFlushNativeSnapshot()"),
 'Descargar el último lote al cerrar WebView');
assert(html.includes("if(document.hidden){nsFlushNativeSnapshot()"),
 'Descargar al enviar la app a segundo plano');
assert(html.includes("function logout(){\n nsFlushNativeSnapshot();"),
 'Descargar al cerrar sesión');
console.log('Fase 1e OK: 30 cambios agrupados, persistencia inmediata, reintento y cuentas separadas');
