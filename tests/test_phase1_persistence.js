/** Pruebas de regresión de guardado local aisladas del WebView.
 * Ejecutar: node tests/test_phase1_persistence.js
 */
const fs=require('node:fs');
const assert=require('node:assert/strict');
const vm=require('node:vm');
const source=fs.readFileSync('index.html','utf8');
const from=source.indexOf('function nsValidModel(');
const to=source.indexOf('var nsDriveBackupTimer=null;',from);
assert(from>=0&&to>from,'Falta el bloque de persistencia de NeuroSync');
const state=new Map();
const store={
  getItem:key=>state.has(key)?state.get(key):null,
  setItem:(key,value)=>{if(store.fail)throw new Error('sin espacio');state.set(key,String(value));},
  removeItem:key=>state.delete(key)
};
const session={username:'google_prueba',role:'user'};
const context={localStorage:store,session,nat:null,nsStorageRecoveryBlocked:false,
  localKey:()=> 'ns_v2_'+session.username,blank:()=>({tasks:[],packs:[],alarms:[],pet:{xp:0}}),
  setTimeout:()=>0,say:()=>{},day:()=> '2026-11-17',stamp:Date.now,
  Date,JSON,Number,String,Array,Error,Math};
vm.createContext(context);
vm.runInContext(source.slice(from,to),context,{filename:'index-persistence.js'});
const original={tasks:[{id:'a',title:'Primera'}],packs:[],alarms:[],pet:{xp:4}};
const newer={tasks:[{id:'a',title:'Modificada'}],packs:[],alarms:[],pet:{xp:5}};
assert.equal(context.nsValidModel(original),true);
assert.equal(context.nsValidModel({tasks:[],packs:[],alarms:[]}),false);
context.nsLocalSave(original);
context.nsLocalSave(newer);
assert.equal(JSON.parse(store.getItem('ns_v2_google_prueba_anterior')).tasks[0].title,'Primera');
assert.equal(JSON.parse(store.getItem('ns_v2_google_prueba')).tasks[0].title,'Modificada');
state.set('ns_v2_google_prueba','{archivo incompleto');
assert.equal(context.load().tasks[0].title,'Primera','Debe restaurar la copia anterior');
assert.equal(JSON.parse(store.getItem('ns_v2_google_prueba')).tasks[0].title,'Primera');
state.set('ns_v2_google_prueba','{archivo incompleto');
state.delete('ns_v2_google_prueba_anterior');
state.delete('ns_v2_google_prueba_rescate');
context.nsStorageRecoveryBlocked=false;
const noRecovery=context.load();
assert.equal(noRecovery.tasks.length,0);
assert.equal(context.nsStorageRecoveryBlocked,true,'No sobrescribir datos corruptos sin copia');
assert.equal(state.get('ns_v2_google_prueba'),'{archivo incompleto');
store.fail=true;
assert.throws(()=>context.nsLocalSave(original),/sin espacio/);
console.log('Fase 1 OK: copias locales, lectura segura, recuperación y fallo de escritura');
