'use strict';
/** Contrato de interfaz de Azul: botones no duplicados y paseo sin XP artificial. */
const fs=require('node:fs');
const vm=require('node:vm');
const assert=require('node:assert/strict');
const html=fs.readFileSync('index.html','utf8');
function single(id){
 const matches=html.match(new RegExp('<[a-z]+[^<>]*id="'+id+'"','gi'))||[];
 assert.equal(matches.length,1,'El elemento '+id+' debe aparecer exactamente una vez');
}
for(const id of ['ns-companion-title','ns-companion-stage','ns-dragon-energy',
 'dragon-walk','pet-feed','dragon-scene','pet-face','dragon-message',
 'ns-dragon-play-controls','ns-dragon-extra-actions','dragon-pet','dragon-name',
 'dragon-decorate','dragon-next','dragon-start-focus','dragon-coach',
 'dragon-day-suggestions','dragon-missions','dragon-room-select'])single(id);
const care=html.indexOf('class="ns-companion-care"');
const scene=html.indexOf('id="ns-companion-scene-slot"');
assert(care>0&&scene>care,'Comer y Caminar deben aparecer arriba de Azul');
const top=html.slice(care,scene);
assert(top.includes('id="dragon-walk"')&&top.includes('id="pet-feed"'),
 'Botones principales visibles, sin desplegar secciones');
assert(html.includes('<summary>Personalizar y otras acciones</summary>'));
assert(html.includes('<summary>Misiones y concentración</summary>'));
assert(!html.includes('id="pet-detail"'),'No dejar explicación decorativa de evolución');
assert(!html.includes('YouTube Music'),'No reintroducir secciones musicales obsoletas');
assert(html.includes('id="dragon-xp-fill"')&&html.includes('id="pet-level"'),
 'Preservar etapa y progreso de evolución');
assert(html.includes("by('dragon-decorate').onclick")&&
 html.includes("by('dragon-room-select').onchange"),
 'No quitar personalización ni habitaciones');
const init=html.slice(html.indexOf('function nsDragonInitActions(){'),
 html.indexOf('\nfunction logout(){',html.indexOf('function nsDragonInitActions(){')));
assert(init.includes("['play','🎾 Jugar']")&&init.includes("['sleep','🌙 Dormir']"));
assert(!init.includes("['eat'")&&!init.includes("['walk'"),
 'Evitar duplicar Comer y Caminar en las acciones secundarias');
for(const script of html.matchAll(/<script(?:[^>]*)>([\s\S]*?)<\/script>/g))
 if(script[1].trim())new vm.Script(script[1]);
const start=html.indexOf('function nsDragonWalk(){');
const end=html.indexOf('\nfunction nsDragonInitActions(){',start);
assert(start>0&&end>start,'El paseo debe tener una implementación explícita');
const actions=[],messages=[];let saves=0,updates=0;
const state={session:{username:'prueba'},model:{pet:{energy:100,walks:0,food:30,xp:80}},
 nsDragonData(){return state.model.pet},
 nsDragonAnimate:x=>actions.push(x),nsDragonMessage:t=>messages.push(t),
 nsDragonUpdateEnergy:()=>updates++,save:()=>saves++};
vm.createContext(state);
vm.runInContext(html.slice(start,end),state);
state.nsDragonWalk();
assert.equal(state.model.pet.energy,92);
assert.equal(state.model.pet.walks,1);
assert.equal(state.model.pet.food,30);
assert.equal(state.model.pet.xp,80,'Caminar no permite acumular XP ilimitadamente');
assert.equal(saves,1);assert.equal(updates,1);assert.equal(actions.at(-1),'walk');
state.model.pet.energy=5;
state.nsDragonWalk();
assert.equal(state.model.pet.energy,5,'No caminar sin energía suficiente');
assert.equal(state.model.pet.walks,1);
assert.equal(actions.at(-1),'tired');
console.log('Azul UI OK: cuidados arriba, funciones preservadas, texto limpio, paseo seguro');
