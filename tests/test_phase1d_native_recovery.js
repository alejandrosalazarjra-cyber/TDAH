/** Fase 1d: la copia independiente de Android recupera datos corruptos sin cruzar cuentas.
 * node tests/test_phase1d_native_recovery.js
 */
const assert=require('node:assert/strict');
const fs=require('node:fs');
const vm=require('node:vm');
const html=fs.readFileSync('index.html','utf8');
const start=html.indexOf('function nsValidModel(');
const end=html.indexOf('var nsDriveBackupTimer=null;',start);
assert(start>=0&&end>start,'Faltan las funciones de recuperación');
const state=new Map(),native=new Map(),messages=[];
const storage={
 getItem:k=>state.has(k)?state.get(k):null,
 setItem:(k,v)=>state.set(k,String(v)),
 removeItem:k=>state.delete(k)
};
const bridge={
 saveNativeSnapshot:(user,json)=>{native.set(user,json);return true},
 recoverNativeSnapshot:user=>native.has(user)
    ?JSON.stringify({ok:true,model:JSON.parse(native.get(user))})
    :JSON.stringify({ok:false,reason:'empty'})
};
const session={username:'persona_a',role:'user'};
const context={nat:bridge,session,localStorage:storage,nsStorageRecoveryBlocked:false,
 localKey:()=> 'ns_v2_'+session.username,by:()=>null,blank:()=>({tasks:[],packs:[],alarms:[],pet:{xp:0}}),
 setTimeout:cb=>{messages.push('scheduled');return messages.length},clearTimeout:()=>{},say:x=>messages.push(x),
 Date,JSON,Number,String,Array,Error,Math,Map};
vm.createContext(context);
vm.runInContext(html.slice(start,end),context);
const first={tasks:[{id:'1',owner:'persona_a',title:'Primer dato'}],
 packs:[],alarms:[],pet:{xp:20}};
context.nsLocalSave(first);
assert.equal(context.nsFlushNativeSnapshot(),true);
assert.equal(JSON.parse(native.get('persona_a')).pet.xp,20);
const key='ns_v2_persona_a';
state.set(key,'{corrupto');
const fixed=context.load();
assert.equal(fixed.tasks[0].title,'Primer dato');
assert.equal(JSON.parse(state.get(key)).pet.xp,20);
assert([...state.keys()].some(k=>k.startsWith(key+'_corrupto_')),
 'La carga recuperada no debe borrar el archivo dañado sin guardar copia');

session.username='persona_b';
const second={tasks:[{id:'2',owner:'persona_b',title:'Privado'}],
 packs:[],alarms:[],pet:{xp:7}};
context.nsLocalSave(second);
assert.equal(context.nsFlushNativeSnapshot(),true);
assert.equal(JSON.parse(native.get('persona_b')).pet.xp,7);
state.set('ns_v2_persona_b','{corrupto');
assert.equal(context.load().tasks[0].title,'Privado',
 'La copia de una cuenta no debe recibir datos de otra');
assert.equal(JSON.parse(native.get('persona_a')).pet.xp,20);

session.username='persona_c';
const fresh=context.load();
assert.equal(fresh.tasks.length,0);
assert(!native.has('persona_c')||JSON.parse(native.get('persona_c')).tasks.length===0);

const java=fs.readFileSync('android/app/src/main/java/com/neurosync/familia/NativeSnapshotStore.java','utf8');
const bridgeCode=fs.readFileSync('android/app/src/main/java/com/neurosync/familia/MainActivity.java','utf8');
assert(java.includes('SQLiteOpenHelper')&&java.includes('db.beginTransaction()')&&
 java.includes('db.setTransactionSuccessful()')&&java.includes('db.endTransaction()'),
 'SQLite debe conservar atomicidad en las copias');
assert(java.includes('AndroidKeyStore')&&java.includes('AES/GCM/NoPadding'),
 'Cifrado autenticado obligatorio para el respaldo');
assert(java.includes('MessageDigest.getInstance("SHA-256")'),
 'Los respaldos deben estar separados por hash de cuenta');
assert(bridgeCode.includes('saveNativeSnapshot(')&&bridgeCode.includes('recoverNativeSnapshot('),
 'El puente Android debe admitir escritura y lectura de copia secundaria');
assert(bridgeCode.includes('canAccessSnapshot(')&&bridgeCode.includes('!canAccessSnapshot(username)'),
 'El puente Android debe comprobar la identidad antes de consultar copias');
console.log('Fase 1d OK: recuperación SQLite simulada, separación de cuentas y transacciones cifradas');
