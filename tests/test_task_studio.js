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
const start=html.indexOf('window.NeuroTasksAPI={'),end=html.indexOf('\n})();\n</script>',start);
assert(start>0&&end>start,'Falta conexión de Task Studio a los datos');
const tasks=[],templates=[];let uid=0,saveCount=0;
const owner={username:'propietario',role:'user'},model={tasks,packs:[],alarms:[],pet:{xp:0,food:0}};
const mocks={window:{dispatchEvent:()=>{}},model,session:owner,Date,Number,Array,Object,JSON,Set,Map,
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
assert(saveCount>=2);
console.log('Task Studio OK: hábitos, tareas rápidas, plantillas, aislamiento y gráficos');
