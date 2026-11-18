const fs=require('node:fs'),vm=require('node:vm'),assert=require('node:assert/strict');
const html=fs.readFileSync('index.html','utf8');
const script=fs.readFileSync('modules/task_studio.js','utf8');
const css=fs.readFileSync('modules/task_studio.css','utf8');
const android=fs.readFileSync('android/app/src/main/java/com/neurosync/familia/MainActivity.java','utf8');
const workflow=fs.readFileSync('.github/workflows/android-apk.yml','utf8');
new vm.Script(script,{filename:'task_studio.js'});
for(const name of ['CATALOG','PRESETS','renderTasks','renderStats','renderPane']){
 assert(script.includes(name),'Falta '+name);
}
assert(script.includes('ns-ts-heatmap'),'Debe incluir mapa anual de actividad');
assert(script.includes('data-habit='),'Debe permitir seleccionar hábitos');
assert(script.includes('data-use='),'Debe permitir usar plantillas');
assert(script.includes('ns-ts-quick-form'),'Debe permitir agregar tareas rápidas');
assert(script.includes("api.toggleToday("),'Debe usar el modelo existente');
assert(html.includes('src="./modules/task_studio.js"'));
assert(html.includes('href="./modules/task_studio.css"'));
assert(android.includes('"/modules/task_studio.js"')&&android.includes('"/modules/task_studio.css"'));
assert(workflow.includes('cp modules/task_studio.js modules/task_studio.css android/app/src/main/assets/modules/'));
assert(css.includes(':focus-visible')&&css.includes('prefers-reduced-motion'));
const start=html.indexOf('/* Bandeja de salida aislada'),end=html.indexOf('\n})();\n</script>',start);
assert(start>0&&end>start,'Falta conexión de Task Studio a los datos');
const tasks=[],templates=[];let uid=0,saveCount=0;
const owner={username:'propietario',role:'user'},model={tasks,packs:[],alarms:[],pet:{xp:0,food:0}};
const storageState=new Map();
const storage={getItem:k=>storageState.has(k)?storageState.get(k):null,
 setItem:(k,v)=>{if(storage.fail)throw Error('disk full');storageState.set(k,String(v));}};
const mocks={window:{dispatchEvent:()=>{}},model,session:owner,localStorage:storage,Date,Number,Array,Object,JSON,Set,Map,
 uid:()=> 'task_'+(++uid),stamp:()=>123,day:()=> '2026-11-18',nsTemplateRead:()=>templates,
 nsTemplateWrite:x=>{templates.splice(0,templates.length,...x)},
 nsPlanRender:()=>{},nsCleanCategory:x=>x||'Personal',active:xs=>xs.filter(x=>!x.deleted),
 allTasks:()=>tasks.filter(t=>t.owner===owner.username),save:()=>{saveCount++;return true},
 render:()=>{},nsConfirm:async()=>true,openModal:()=>{},show:()=>{},nsFocusRead:()=>({sessions:[]}),
 nsTemplateCreateTask:t=>tasks.push({id:'from_template',owner:owner.username,title:t.title,doneDates:[]})};
const api=new Function(...Object.keys(mocks),html.slice(start,end)+'\nreturn window.NeuroTasksAPI;')(...Object.values(mocks));
assert(api.create({title:'Beber agua',category:'Salud',repeat:'daily'}));
assert.equal(tasks.length,1);assert.equal(tasks[0].repeat,'daily');
assert(api.toggleToday(tasks[0].id));assert(tasks[0].doneDates.includes('2026-11-18'));
assert(api.saveTemplate({title:'Viaje',steps:['Documentos','Maleta'],category:'Viajes'}));
assert.equal(api.templates().length,1);assert(api.useTemplate(templates[0].id));
assert.equal(tasks.length,2);
tasks.push({id:'foreign',title:'Privado',owner:'hermano',doneDates:[]});
assert(!api.toggleToday('foreign'),'No permitir editar datos de otra persona');
assert.equal(api.list().length,2);
assert(!api.create({title:'   '}),'Rechazar tareas vacías');
assert.equal(api.canShare(),false,'Un integrante no administrador no debe compartir tareas globales');
assert(api.create({title:'Privada',shared:true}));
assert.equal(tasks.at(-1).owner,'propietario','Ignorar opción de compartir de usuarios normales');
owner.role='admin';
assert.equal(api.canShare(),true);
assert(api.create({title:'Cena familiar',shared:true}));
assert.equal(tasks.at(-1).owner,'todos','Administrador puede compartir al crear');
assert(api.toggleToday(tasks.at(-1).id),'Integrante autenticado puede marcar tarea familiar');
assert(api.create({title:'Revisar nevera'}));
const last=tasks.at(-1);assert.equal(last.owner,'propietario');
assert(api.share(last.id),'Administrador puede compartir tarea existente');
assert.equal(last.owner,'todos');
owner.role='user';
assert(!api.share(tasks[0].id),'Un integrante normal no puede compartir una tarea ajena');
assert(script.includes('data-send=')&&script.includes('name="recipient"'),
 'Task Studio debe permitir enviar tareas desde cuentas normales');


// Cualquier usuario puede enviar una tarea a otro sin permiso de administrador.
let sent=[];
mocks.nat={
 taskContacts:()=>JSON.stringify({ok:true,users:[{username:'mama'},{username:'hermano'}]}),
 sendTaskToUser:(to,id,json)=>{sent.push({to,id,task:JSON.parse(json)});return JSON.stringify({ok:true,id:'sent_123',recipient:to});}
};
const remoteApi=new Function(...Object.keys(mocks),html.slice(start,end)+'\nreturn window.NeuroTasksAPI;')(...Object.values(mocks));
owner.role='user';
assert.deepEqual(remoteApi.contacts(),['mama','hermano']);
const transfer=remoteApi.sendTo('mama',{id:'task_test_123',title:'Comprar pan',repeat:'none',category:'Personal'});
assert.equal(transfer.ok,true);
assert.equal(sent.length,1);
assert.equal(sent[0].to,'mama');
assert.equal(sent[0].task.title,'Comprar pan');
assert.equal(remoteApi.sendTo('propietario',{title:'No válido'}).ok,false,'No enviarse a sí mismo');

const outboxKey='ns_task_outbox_v1_propietario';
assert.equal(remoteApi.outbox().length,0,'El envío exitoso debe salir de la bandeja local');
let offline=true;
mocks.nat.sendTaskToUser=(to,id,json)=>{if(offline)throw Error('offline');
 sent.push({to,id,task:JSON.parse(json)});return JSON.stringify({ok:true,id:'sent_offline',recipient:to});};
const offlineApi=new Function(...Object.keys(mocks),html.slice(start,end)+'\nreturn window.NeuroTasksAPI;')(...Object.values(mocks));
const queued=offlineApi.sendTo('mama',{id:'task_offline_987',title:'Traer llaves'});
assert.equal(queued.ok,true);
assert.equal(queued.queued,true,'No declarar entrega cuando falla la conexión');
assert.equal(offlineApi.outbox().length,1,'Persistir un envío pendiente por cuenta');
const same=offlineApi.sendTo('mama',{id:'task_offline_987',title:'Traer llaves'});
assert.equal(same.queued,true);
assert.equal(offlineApi.outbox().length,1,'No duplicar reintentos');
const editedRetry=offlineApi.sendTo('mama',{id:'task_offline_987',title:'Texto distinto'});
assert.equal(editedRetry.queued,true);
assert.equal(offlineApi.outbox()[0].task.title,'Traer llaves',
 'Un reintento no debe cambiar los datos ya almacenados');

assert.equal(offlineApi.sent().tasks[0].deliveryStatus,'queued','Mostrar pendientes aun sin servidor');
offline=false;
const flushed=offlineApi.retryOutbox();
assert.equal(flushed.delivered,1,'Enviar al recuperar conexión');
assert.equal(offlineApi.outbox().length,0,'Retirar de la bandeja solo tras éxito remoto');
storage.fail=true;
const blocked=offlineApi.sendTo('mama',{id:'task_blocked_987',title:'No perder'});
assert.equal(blocked.ok,false,'No enviar si el teléfono no puede guardar la cola');
storage.fail=false;
assert.equal(offlineApi.outbox().length,0,'Un error de almacenamiento no debe crear un envío falso');
assert(!storageState.has('ns_task_outbox_v1_hermano'),'No crear cola de otra cuenta');
assert(script.includes('Pendiente de conexión'),'Mostrar envío sin conexión en la interfaz');
assert(html.includes('Math.min(1,items.length)'),
 'Limitar los reintentos síncronos para no bloquear la pantalla');

assert(html.includes("window.addEventListener('online'"),'Reintentar al recuperar la conexión');

assert(script.includes('data-send=')&&script.includes('name="recipient"'),
 'Cualquier usuario debe tener selector de envío');
const backend=fs.readFileSync('android/app/src/main/java/com/neurosync/familia/SyncStore.java','utf8');
const accounts=fs.readFileSync('android/app/src/main/java/com/neurosync/familia/AccountStore.java','utf8');
const service=fs.readFileSync('android/app/src/main/java/com/neurosync/familia/SyncService.java','utf8');
assert(backend.includes('public static JSONObject sendTask(')&&backend.includes('.put("owner",to)'),
 'Servidor asigna tarea al destinatario autenticado');
assert(backend.includes('nameUUIDFromBytes')&&backend.includes('if(tasks.containsKey(id))'),
 'Los reintentos deben evitar duplicados');
assert(accounts.includes('public static JSONObject contacts('),'Listar destinatarios sin permisos de administrador');
assert(service.includes('route.equals("/send-task")'),'Habilitar envío desde teléfonos conectados');
assert(script.includes('data-tab="received"')&&script.includes('data-tab="sent"'),
 'El panel debe mostrar bandejas de recibidas y enviadas');
assert(script.includes('data-accept=')&&script.includes('data-complete='),
 'Las tareas recibidas deben poder aceptarse y completarse');
assert(backend.includes('public static JSONObject sentTasks(')&&
 backend.includes('public static JSONObject updateDelivery('),
 'El servidor debe gestionar estados y mostrar los envíos');
assert(backend.includes('actor.optString("username").equals(task.optString("owner"))'),
 'Solo el destinatario puede aceptar o completar sus tareas');
assert(backend.includes('change.put("deliveryStatus",old.optString("deliveryStatus","pending"))'),
 'Un cliente desactualizado no puede sobrescribir estados del servidor');
assert(backend.includes('if(old==null){')&&backend.includes('change.remove("sentBy")'),
 'El servidor debe ignorar remitentes falsos incluidos en cambios normales');
assert(html.includes("if(t.deliveryStatus!=='accepted')return false;"),
 'La finalización rápida requiere aceptación previa');
assert(saveCount>=2);
console.log('Task Studio OK: hábitos, tareas rápidas, plantillas, aislamiento y gráficos');
