#!/usr/bin/env node
/* Pruebas funcionales de la máquina de estados de Azul, sin abrir Android.
 * Ejecutan el bloque real de index.html con temporizadores simulados. */
'use strict';
const assert=require('node:assert/strict');
const fs=require('node:fs');
const vm=require('node:vm');
const html=fs.readFileSync('index.html','utf8');
const from=html.indexOf('/* DrakoPlan / Azul: máquina de estados');
const to=html.indexOf('\nfunction logout()',from);
assert(from!==-1&&to>from,'No se encontró el módulo independiente del dragón');
const events=[];
const timers=new Map();
let seq=0;
const scene={dataset:{},setAttribute(name,value){this[name]=value}};
const energy={textContent:''};
const context={
 console,
 session:{username:'tester'},
 model:{pet:{xp:79,energy:100,plays:0,food:0}},
 nsDragonStages:[
  {name:'Huevo místico',min:0},{name:'Cría de dragón',min:80},
  {name:'Dragón joven',min:200},{name:'Guardián azul',min:420},
  {name:'Dragón ancestral',min:750},{name:'Dragón celestial',min:1200}
 ],
 by(id){if(id==='dragon-scene')return scene;if(id==='ns-dragon-energy')return energy;return null},
 nsDragonData(){return this.model.pet},
 nsDragonStage(xp){return [0,80,200,420,750,1200].reduce((n,min,i)=>xp>=min?i:n,0)},
 nsDragonMessage(message){events.push(message)},
 save(){},
 setTimeout(callback,ms){const id=++seq;timers.set(id,{callback,ms});return id},
 clearTimeout(id){timers.delete(id)}
};
context.nsDragonData=()=>context.model.pet;
context.nsDragonStage=(xp)=>[0,80,200,420,750,1200].reduce((n,min,i)=>xp>=min?i:n,0);
vm.createContext(context);
vm.runInContext(html.slice(from,to),context,{timeout:2000});
function state(){return scene.dataset.dragonAction}
function finish(){
 assert.equal(timers.size,1,'Solo debe haber un temporizador de acción');
 const item=[...timers.values()][0];timers.clear();item.callback();
}
assert.equal(context.nsDragonAnimate('walk'),true);
assert.equal(state(),'walk');
assert.equal(context.nsDragonAnimate('play'),true);
assert.equal(state(),'play','La interacción de mayor prioridad interrumpe caminar');
assert.equal(context.nsDragonAnimate('walk'),true);
assert.equal(state(),'play','Una caminata no interrumpe el juego');
finish();
assert.equal(state(),'walk','La siguiente acción en cola se reproduce');
finish();
assert.equal(state(),'idle');
context.nsDragonAnimate('sleep');
assert.equal(state(),'sleep');
assert.equal(timers.size,0,'Dormir continúa hasta que el usuario actúe');
context.nsDragonAnimate('walk');
assert.equal(state(),'walk','La mascota despierta al caminar');
finish();
assert.equal(state(),'idle');
context.nsDragonCheckEvolution(0);
assert.equal(state(),'idle','La primera visualización no debe disparar evolución');
context.model.pet.xp=80;
context.nsDragonCheckEvolution(1);
assert.equal(state(),'evolve');
context.nsDragonAnimate('eat');
assert.equal(state(),'evolve','La evolución tiene prioridad sobre comer');
finish();
assert.equal(state(),'eat','Comer sigue a la evolución');
finish();
assert.equal(state(),'idle');
assert(events.some(x=>x.includes('Evolución desbloqueada')));
context.model.pet.energy=10;
context.nsDragonAnimate('idle');
context.nsDragonPlay();
assert.equal(state(),'tired','Sin energía no se inicia el juego');
assert.equal(context.model.pet.plays,0);
context.nsDragonRest();
assert.equal(state(),'sleep');
assert.equal(context.model.pet.energy,35);
context.nsDragonPlay();
assert.equal(context.model.pet.energy,20);
assert.equal(context.model.pet.plays,1);
assert.equal(state(),'play');
finish();
assert.equal(state(),'idle');
context.nsDragonCheckEvolution(5);
assert.equal(state(),'evolve');
context.nsDragonCheckEvolution(5);
assert.equal(context.nsDragonPending.length,0,'No repetir evolución sin subir de etapa');
finish();
context.session={username:'otra-cuenta'};
context.nsDragonCheckEvolution(2);
assert.equal(state(),'idle','Cambiar de usuario no genera una evolución falsa');
assert.equal(context.nsDragonDisplayedStage,2);
assert.equal(context.nsDragonAnimate('inexistente'),false);
console.log('OK: prioridades, cola, despertar, energía, evolución y aislamiento por usuario');
