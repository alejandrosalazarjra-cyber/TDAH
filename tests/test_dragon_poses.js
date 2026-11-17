#!/usr/bin/env node
/* Comprueba el selector de poses con temporizadores controlados (sin WebView).
   Las imágenes reales se comprueban con test_dragon_module.py. */
'use strict';
const assert=require('node:assert/strict');
const fs=require('node:fs');
const vm=require('node:vm');
const html=fs.readFileSync('index.html','utf8');
const from=html.indexOf('var nsDragonPoseFrameTimer=null');
const to=html.indexOf('var nsDragonAnimations={',from);
assert(from>0&&to>from,'No existe el módulo de poses');
let counter=0;
const timeout=new Map(),interval=new Map();
const image={dataset:{},isConnected:true,src:''};
const figure={dataset:{stage:'1'},querySelector(selector){
  assert.equal(selector,'img.ns-dragon-sprite');return image;
}};
const browser={
 by(id){return id==='pet-face'?figure:null},
 window:{matchMedia(){return {matches:browser.reduced}}},
 document:{hidden:false,addEventListener(){}},
 setInterval(fn,ms){const id=++counter;interval.set(id,{fn,ms});return id},
 clearInterval(id){interval.delete(id)},
 setTimeout(fn,ms){const id=++counter;timeout.set(id,{fn,ms});return id},
 clearTimeout(id){timeout.delete(id)},
 nsDragonActionState:'idle',
 session:{username:'tester'},
 model:{pet:{}},
 reduced:false
};
vm.createContext(browser);
vm.runInContext(html.slice(from,to),browser,{timeout:2000});
function state(s){browser.nsDragonActionState=s;browser.nsDragonPoseSet(s)}
function expect(name){assert.equal(image.src,'assets/dragon/poses/'+name+'.webp')}
state('walk');expect('walk-a');assert.equal(interval.size,1);
[...interval.values()][0].fn();expect('walk-b');
state('sleep');expect('sleep');assert.equal(interval.size,0);
state('eat');expect('eat');
state('play');expect('flap');
state('celebrate');expect('celebrate');
state('evolve');expect('flap');
state('idle');expect('idle');assert.equal(timeout.size,1);
const first=[...timeout.entries()][0];timeout.delete(first[0]);
first[1].fn();expect('blink');
browser.document.hidden=true;state('idle');expect('idle');assert.equal(timeout.size,0);
browser.document.hidden=false;browser.reduced=true;state('walk');
expect('idle');assert.equal(interval.size,0);
browser.reduced=false;figure.dataset.stage='2';state('sleep');
expect('idle');assert.equal(timeout.size,0);
console.log('OK: 8 poses, caminata alterna, parpadeo, segundo plano, accesibilidad y etapas');
