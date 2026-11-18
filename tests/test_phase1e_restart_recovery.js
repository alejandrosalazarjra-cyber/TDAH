/** Fase 1e: proceso WebView simulado se cierra y vuelve a abrir; SQLite permanece.
 * node tests/test_phase1e_restart_recovery.js
 */
const fs=require('node:fs'),vm=require('node:vm'),assert=require('node:assert/strict');
const html=fs.readFileSync('index.html','utf8');
const from=html.indexOf('function nsValidModel(');
const to=html.indexOf('var nsDriveBackupTimer=null;',from);
assert(from>0&&to>from,'No se encontraron las funciones de restauración');
const snapshots=new Map(),webview=new Map();
let calls=0;
const bridge={
 saveNativeSnapshot:(account,json)=>{snapshots.set(account,json);calls++;return true},
 recoverNativeSnapshot:account=>snapshots.has(account)
  ?JSON.stringify({ok:true,model:JSON.parse(snapshots.get(account))})
  :JSON.stringify({ok:false})
};
function launch(user){
 const session={username:user,role:'user'};
 const browser={
  getItem:k=>webview.has(k)?webview.get(k):null,
  setItem:(k,v)=>webview.set(k,String(v))
 };
 const c={session,nat:bridge,localStorage:browser,
  nsStorageRecoveryBlocked:false,localKey:()=> 'ns_v2_'+user,
  by:()=>null,blank:()=>({tasks:[],packs:[],alarms:[],pet:{xp:0}}),
  setTimeout:()=>1,clearTimeout:()=>{},say:()=>{},
  Date,Number,JSON,String,Array,Error,Math,Map};
 vm.createContext(c);
 vm.runInContext(html.slice(from,to),c);
 return c;
}
{
 const a=launch('persona_a');
 a.nsLocalSave({tasks:[{id:'1',title:'Antes del reinicio',owner:'persona_a'}],
  packs:[],alarms:[],pet:{xp:18},_localUpdatedAt:200});
 assert.equal(a.nsFlushNativeSnapshot(),true);
}
{
 // El WebView se reconstruye desde cero; se conserva la copia SQLite del SO.
 const b=launch('persona_a');
 assert.equal(b.load().tasks[0].title,'Antes del reinicio');
 assert.equal(b.load().pet.xp,18);
}
{
 // WebView ilegible después de reiniciar: recupera SQLite, no cambia de cuenta.
 webview.set('ns_v2_persona_a','{corrupto');
 const b=launch('persona_a');
 assert.equal(b.load().pet.xp,18);
 assert([...webview.keys()].some(x=>x.startsWith('ns_v2_persona_a_corrupto_')));
}
{
 const c=launch('persona_b');
 const fresh=c.load();
 assert.equal(fresh.tasks.length,0,'La segunda cuenta no puede leer a la primera');
}
assert(calls>=1);
console.log('Fase 1e OK: reinicio simulado, corrupción y aislamiento de cuentas');
