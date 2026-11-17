#!/usr/bin/env node
/* Pruebas de los atlas de las seis evoluciones: carga, poses y temporizadores. */
'use strict';
const assert=require('node:assert/strict');
const fs=require('node:fs');
const vm=require('node:vm');
const html=fs.readFileSync('index.html','utf8');
const a=html.indexOf('var nsDragonPoseFrameTimer=null');
const b=html.indexOf('var nsDragonAnimations={',a);
assert(a>0&&b>a,'No se encontró el renderizador de los atlas de Azul');
const timeouts=new Map(),intervals=new Map(),loaders=[];
let id=0,stage='0',tile=null;
const figure={
 dataset:{stage,accessory:'none'},isConnected:true,
 classList:{add(x){figure.hasAtlas=x==='has-atlas'}},
 querySelector(sel){return sel==='.ns-dragon-atlas'?tile:null},
 appendChild(node){tile=node}
};
let action='idle',reduced=false,hidden=false;
const context={
 session:{username:'tester'},model:{pet:{}},
 nsDragonActionState:action,
 by(name){return name==='pet-face'?figure:null},
 window:{matchMedia(){return {matches:reduced}}},
 document:{
  get hidden(){return hidden},
  addEventListener(){},
  createElement(name){assert.equal(name,'span');return {className:'',style:{},dataset:{},setAttribute(){}}}
 },
 Image:function(){loaders.push(this)},
 setInterval(fn,ms){const i=++id;intervals.set(i,{fn,ms});return i},
 clearInterval(i){intervals.delete(i)},
 setTimeout(fn,ms){const i=++id;timeouts.set(i,{fn,ms});return i},
 clearTimeout(i){timeouts.delete(i)}
};
vm.createContext(context);
vm.runInContext(html.slice(a,b),context,{timeout:2000});
function choose(state){context.nsDragonActionState=state;context.nsDragonPoseSet(state)}
function load(){const item=loaders.shift();assert(item,'Debe solicitar un atlas');item.onload()}
function expect(frame,selectedStage){
 assert.equal(tile.dataset.pose,frame);
 assert(tile.style.backgroundImage.includes('etapa-'+selectedStage+'.webp'));
}
for(let s=0;s<6;s++){
 figure.dataset.stage=String(s);
 tile=null;
 choose('idle');
 load();expect('idle',s+1);
 assert.equal(context.nsDragonAtlasCache[String(s+1)],'loaded');
 choose('walk');expect('walk-a',s+1);
 assert.equal(intervals.size,1);
 [...intervals.values()][0].fn();expect('walk-b',s+1);
 choose('sleep');expect('sleep',s+1);
 assert.equal(intervals.size,0);
 choose('eat');expect('eat',s+1);
 choose('play');expect('flap',s+1);
 choose('celebrate');expect('celebrate',s+1);
 choose('evolve');expect('flap',s+1);
 choose('idle');expect('idle',s+1);
 assert.equal(timeouts.size,1);
 const [key,blink]=[...timeouts.entries()][0];timeouts.delete(key);blink.fn();
 expect('blink',s+1);
 hidden=true;choose('idle');expect('idle',s+1);assert.equal(timeouts.size,0);
 hidden=false;reduced=true;choose('walk');expect('idle',s+1);assert.equal(intervals.size,0);
 reduced=false;
}
figure.dataset.accessory='star';tile=null;
context.nsDragonPoseSet('idle');
assert.equal(loaders.length,0,'Los accesorios equipados deben conservar el SVG existente');
console.log('OK: seis atlas, 8 poses, parpadeo, caminata, segundo plano y reducir movimiento');
