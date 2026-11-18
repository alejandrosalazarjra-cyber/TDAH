/* NeuroSync Finanzas (COP) — cuentas + libro de movimientos por usuario.
 * Valores en pesos enteros; transferencias no son ingresos ni gastos.
 * Se conserva localmente por cuenta y jamás se mezcla con el modelo familiar.
 */
(function(){
'use strict';
const root=document.getElementById('ns-finance-studio');if(!root)return;
const CATS={expense:['Alimentación','Transporte','Vivienda','Servicios','Salud','Estudio','Entretenimiento','Compras','Otros'],
 income:['Salario','Producción','Ventas','Reembolso','Otros']};
const today=()=>{const d=new Date();return [d.getFullYear(),String(d.getMonth()+1).padStart(2,'0'),String(d.getDate()).padStart(2,'0')].join('-')};
const $=q=>root.querySelector(q);
const esc=x=>String(x??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const cop=n=>new Intl.NumberFormat('es-CO',{style:'currency',currency:'COP',maximumFractionDigits:0}).format(n);
const peso=x=>{const n=Number(x);return Number.isSafeInteger(n)&&n>=0&&n<=999999999999?n:null};
const month=x=>String(x||'').slice(0,7);
// Verificación calendaria estricta: Date.parse puede normalizar fechas imposibles.
function validDate(value){
 if(typeof value!=='string'||!/^\d{4}-\d{2}-\d{2}$/.test(value))return false;
 const [year,mon,day]=value.split('-').map(Number);
 if(year<1900||year>9999||mon<1||mon>12||day<1)return false;
 return day<=new Date(Date.UTC(year,mon,0)).getUTCDate();
}
// Rechazar libros dañados sin modificar el respaldo almacenado.
function validBook(b){
 if(!b||b.version!==1||!Array.isArray(b.accounts)||!Array.isArray(b.entries)||
    !b.budgets||typeof b.budgets!=='object'||Array.isArray(b.budgets)||
    b.accounts.length<1||b.accounts.length>30)return false;
 const ids=new Set(),entryIds=new Set();
 for(const a of b.accounts){
  if(!a||typeof a.id!=='string'||!a.id||ids.has(a.id)||
     typeof a.name!=='string'||typeof a.opening!=='number'||peso(a.opening)===null)return false;
  ids.add(a.id);
 }
 for(const e of b.entries){
  if(!e||typeof e.id!=='string'||!e.id||entryIds.has(e.id)||
    !validDate(e.date)||typeof e.amount!=='number'||peso(e.amount)===null||e.amount<1||
    !['income','expense','transfer'].includes(e.type))return false;
  entryIds.add(e.id);
  if(e.voidedAt!=null&&(typeof e.voidedAt!=='string'||!Number.isFinite(Date.parse(e.voidedAt))))return false;
  if(e.type==='transfer'){
   if(!ids.has(e.from)||!ids.has(e.to)||e.from===e.to)return false;
  }else if(!ids.has(e.account))return false;
 }
 for(const [period,value] of Object.entries(b.budgets)){
  if(!/^\d{4}-(0[1-9]|1[0-2])$/.test(period)||typeof value!=='number'||peso(value)===null)return false;
 }
 return true;
}
const uuid=()=>String(Date.now())+'-'+Math.random().toString(36).slice(2,10);
const state={selected:month(today()),tab:'overview',notice:''};
function user(){return window.NeuroFinanceAPI?.account()||null}
function key(){return 'ns_private_finance_v1_'+user()}
function newBook(){return {version:1,accounts:[{id:'cash',name:'Efectivo',opening:0},{id:'bank',name:'Banco',opening:0}],entries:[],budgets:{},launcher:false}}
function read(){
 if(!user())return newBook();
 try{
  const raw=localStorage.getItem(key());if(!raw)return newBook();
  const book=JSON.parse(raw);
  if(!validBook(book))throw Error('Formato inválido');
  return book;
 }catch(e){state.notice='No se puede leer el libro financiero. No se sobrescribirá hasta recuperar sus datos.';return null}
}
function native(book){
 if(!window.NeuroFinanceAPI?.updateLauncher||!user())return;
 const data=stats(book,month(today()));
 // Nunca mostrar cifras en launcher sin consentimiento explícito.
 try{window.NeuroFinanceAPI.updateLauncher(!!book.launcher,
   Math.max(-999999999999,Math.min(999999999999,data.balance)),
   data.income,data.expense);
 }catch(e){}
}
function write(book){
 if(!user()||!book)return false;
 try{
  const value=JSON.stringify(book);
  localStorage.setItem(key(),value);
  if(localStorage.getItem(key())!==value)throw Error('Escritura incompleta');
  native(book);return true;
 }catch(e){state.notice='No fue posible guardar. No se confirma el movimiento.';return false}
}
function balanceFor(book,id){
 let total=book.accounts.find(x=>x.id===id)?.opening||0;
 for(const e of book.entries){
  if(e.voidedAt)continue;
  if(e.type==='income'&&e.account===id)total+=e.amount;
  if(e.type==='expense'&&e.account===id)total-=e.amount;
  if(e.type==='transfer'){if(e.from===id)total-=e.amount;if(e.to===id)total+=e.amount}
 }
 return total;
}
function stats(book,period){
 const selected=book.entries.filter(e=>!e.voidedAt&&month(e.date)===period);
 const income=selected.filter(e=>e.type==='income').reduce((n,e)=>n+e.amount,0);
 const expense=selected.filter(e=>e.type==='expense').reduce((n,e)=>n+e.amount,0);
 return {income,expense,balance:book.accounts.reduce((n,a)=>n+balanceFor(book,a.id),0),
  categories:CATs(selected),remaining:(Number(book.budgets[period])||0)-expense};
}
function CATs(entries){
 const out={};for(const e of entries)if(e.type==='expense')out[e.category]=(out[e.category]||0)+e.amount;
 return Object.entries(out).sort((a,b)=>b[1]-a[1]);
}
function accountOptions(book){return book.accounts.map(a=>'<option value="'+esc(a.id)+'">'+esc(a.name)+'</option>').join('')}
function title(book){
 const p=stats(book,state.selected);
 return '<header class="ns-fi-head"><div><span>NEUROSYNC · CONTABILIDAD PERSONAL</span><h2>Mi dinero</h2>'+
  '<p>Pesos colombianos · tus registros privados</p></div><button type="button" data-finance="add">+ Movimiento</button></header>'+
  '<div class="ns-fi-summary"><div class="ns-fi-balance"><small>Saldo total registrado</small><strong>'+cop(p.balance)+'</strong></div>'+
  '<div><small>Ingresos del mes</small><strong class="ns-fi-positive">'+cop(p.income)+'</strong></div>'+
  '<div><small>Gastos del mes</small><strong>'+cop(p.expense)+'</strong></div></div>';
}
function charts(book){
 const p=stats(book,state.selected),max=Math.max(1,...p.categories.map(x=>x[1]));
 const list=p.categories.length?p.categories.map(([name,value])=>'<div class="ns-fi-chart-row"><div><strong>'+esc(name)+'</strong><span>'+cop(value)+'</span></div><div class="ns-fi-track"><i style="width:'+(value/max*100).toFixed(2)+'%"></i></div></div>').join(''):'<p class="ns-fi-empty">Sin gastos registrados en este mes.</p>';
 const date=new Date(state.selected+'-01T12:00:00'),series=Array.from({length:6},(_,i)=>{
  const d=new Date(date);d.setMonth(date.getMonth()-(5-i));
  const m=[d.getFullYear(),String(d.getMonth()+1).padStart(2,'0')].join('-');
  const x=stats(book,m);return {month:m,label:d.toLocaleDateString('es-CO',{month:'short'}),income:x.income,expense:x.expense};
 });
 const top=Math.max(1,...series.flatMap(x=>[x.income,x.expense]));
 return '<section class="ns-fi-panel"><h3>Gastos por categoría</h3>'+list+'</section>'+
 '<section class="ns-fi-panel"><h3>Ingresos y gastos · últimos 6 meses</h3>'+
 '<div class="ns-fi-bars" role="img" aria-label="Comparación mensual de ingresos y gastos">'+series.map(x=>
  '<div><div class="ns-fi-pair"><i class="ns-fi-in" title="Ingresos '+cop(x.income)+'" style="height:'+Math.max(2,x.income/top*100)+'%"></i>'+
  '<i class="ns-fi-out" title="Gastos '+cop(x.expense)+'" style="height:'+Math.max(2,x.expense/top*100)+'%"></i></div>'+
  '<small>'+esc(x.label)+'</small></div>').join('')+'</div><p class="ns-fi-hint">Azul: ingresos. Gris: gastos. Las transferencias no cuentan como ninguno de los dos.</p></section>';
}
function ledger(book){
 const list=book.entries.filter(e=>month(e.date)===state.selected).slice().sort((a,b)=>b.date.localeCompare(a.date)||b.created-a.created);
 return '<section class="ns-fi-panel"><div class="ns-fi-inline"><h3>Movimientos</h3><span>'+list.length+' registrados</span></div>'+
 (list.length?list.map(e=>'<div class="ns-fi-entry"><div><b>'+esc(e.note||e.category||'Transferencia')+'</b>'+
  '<small>'+esc(e.date)+' · '+esc(e.type==='transfer'?'Transferencia':e.category)+'</small></div>'+
  '<div class="ns-fi-entry-actions"><strong class="'+(e.type==='income'&&!e.voidedAt?'ns-fi-positive':'')+'">'+(e.voidedAt?'Anulado · ':e.type==='expense'?'−':e.type==='income'?'+':'')+cop(e.amount)+'</strong>'+
  (e.voidedAt?'<small class="ns-fi-void-note">Anulado el '+esc(String(e.voidedAt).slice(0,10))+'</small>':'<button type="button" data-finance="void" data-entry-id="'+esc(e.id)+'" aria-label="Anular este movimiento">Anular</button>')+'</div></div>').join(''):
  '<p class="ns-fi-empty">Aún no hay movimientos.</p>')+'</section>';
}
function render(){
 if(!user()){root.replaceChildren();return}
 const book=read();
 if(!book){root.innerHTML='<p role="alert">'+esc(state.notice)+'</p>';return}
 const p=stats(book,state.selected);
 root.innerHTML=title(book)+'<nav class="ns-fi-tabs">'+
  '<button data-finance="overview" '+(state.tab==='overview'?'aria-current="page"':'')+'>Resumen</button>'+
  '<button data-finance="accounts" '+(state.tab==='accounts'?'aria-current="page"':'')+'>Cuentas</button>'+
  '<button data-finance="budget" '+(state.tab==='budget'?'aria-current="page"':'')+'>Presupuesto</button></nav>'+
  '<label class="ns-fi-month">Período <input type="month" id="ns-fi-month" value="'+esc(state.selected)+'"></label>'+
  (state.tab==='overview'?charts(book)+ledger(book):
   state.tab==='accounts'?'<section class="ns-fi-panel"><h3>Cuentas</h3>'+book.accounts.map(a=>
    '<div class="ns-fi-entry"><strong>'+esc(a.name)+'</strong><b>'+cop(balanceFor(book,a.id))+'</b></div>').join('')+
    '<button data-finance="account">+ Crear cuenta</button></section>':
    '<section class="ns-fi-panel"><h3>Límite mensual</h3><p>Presupuesto '+cop(book.budgets[state.selected]||0)+
    ' · Gastado '+cop(p.expense)+'</p><div class="ns-fi-track"><i style="width:'+Math.min(100,(book.budgets[state.selected]?p.expense/book.budgets[state.selected]*100:0))+'%"></i></div>'+
    '<form id="ns-fi-budget"><label>Presupuesto de '+esc(state.selected)+' (COP)<input name="amount" type="number" min="0" step="1" required value="'+(book.budgets[state.selected]||0)+'"></label><button>Guardar límite</button></form></section>')+
  '<label class="ns-fi-privacy"><input id="ns-fi-launcher" type="checkbox" '+(book.launcher?'checked':'')+'>'+
  '<span>Mostrar resumen financiero en el inicio minimalista</span></label>'+
  '<p class="ns-fi-hint">Desactivado de forma predeterminada por privacidad. Las cifras podrían verse al desbloquear el teléfono. No se comparten con los miembros de la familia.</p>'+
  '<div class="ns-fi-tools"><button type="button" data-finance="export">Exportar respaldo JSON</button></div>'+
  '<p class="ns-fi-hint" role="status">'+esc(state.notice)+'</p>'+
  '<div id="ns-fi-modal"></div>';
}
function modal(type){
 const book=read();if(!book)return;
 const element=$('#ns-fi-modal');if(!element)return;
 const kinds=[['expense','Gasto'],['income','Ingreso'],['transfer','Transferencia']];
 element.innerHTML='<div class="ns-fi-shade" role="dialog" aria-modal="true" aria-label="Registrar movimiento">'+
  '<form id="ns-fi-form" class="ns-fi-dialog"><h3>'+(type==='account'?'Nueva cuenta':'Registrar movimiento')+'</h3>'+
  (type==='account'?'<label>Nombre de la cuenta<input name="name" required maxlength="50" placeholder="Ej. Nequi"></label>'+
   '<label>Saldo inicial (COP)<input name="opening" type="number" min="0" step="1" required value="0"></label>':
  '<label>Tipo<select name="type">'+kinds.map(([v,t])=>'<option value="'+v+'">'+t+'</option>').join('')+'</select></label>'+
  '<label>Monto (COP)<input name="amount" type="number" min="1" step="1" required placeholder="25000"></label>'+
  '<label>Fecha<input name="date" type="date" required value="'+today()+'"></label>'+
  '<label>Cuenta de origen<select name="account">'+accountOptions(book)+'</select></label>'+
  '<label class="ns-fi-to" hidden>Cuenta de destino<select name="to">'+accountOptions(book)+'</select></label>'+
  '<label class="ns-fi-category">Categoría<select name="category">'+CATS.expense.map(x=>'<option>'+esc(x)+'</option>').join('')+'</select></label>'+
  '<label>Descripción<input name="note" maxlength="120" placeholder="Opcional"></label>')+
  '<div class="ns-fi-modal-buttons"><button type="submit">Guardar</button><button type="button" data-finance="cancel">Cancelar</button></div>'+
  '<p role="alert" data-finance-error></p></form></div>';
 element.querySelector('select[name=type]')?.addEventListener('change',e=>{
  const v=e.target.value;element.querySelector('.ns-fi-to').hidden=v!=='transfer';
  element.querySelector('.ns-fi-category').hidden=v==='transfer';
  const cat=element.querySelector('select[name=category]');
  cat.innerHTML=(CATS[v]||[]).map(x=>'<option>'+esc(x)+'</option>').join('');
 });
 const form=element.querySelector('form');
 form.onsubmit=e=>{
  e.preventDefault();const v=Object.fromEntries(new FormData(form).entries()),b=read();
  if(!b)return;
  const amount=peso(type==='account'?v.opening:v.amount),msg=form.querySelector('[data-finance-error]');
  if(amount===null||(type!=='account'&&amount<1)){msg.textContent='Introduce un monto válido en pesos enteros.';return}
  if(type==='account'){
   if(!v.name.trim()||b.accounts.length>=30){msg.textContent='Revisa el nombre o el límite de cuentas.';return}
   b.accounts.push({id:uuid(),name:v.name.trim().slice(0,50),opening:amount});
  }else{
   if(!validDate(v.date)||v.date>today()){
    msg.textContent='La fecha debe ser válida y no futura.';return}
   if(!b.accounts.some(a=>a.id===v.account)){msg.textContent='Cuenta inválida.';return}
   if(v.type==='transfer'&&(!b.accounts.some(a=>a.id===v.to)||v.to===v.account)){msg.textContent='Elige otra cuenta de destino.';return}
   const record={id:uuid(),created:Date.now(),date:v.date,type:v.type,amount,note:(v.note||'').trim().slice(0,120)};
   if(v.type==='transfer'){record.from=v.account;record.to=v.to}
   else{record.account=v.account;record.category=(CATS[v.type]||[]).includes(v.category)?v.category:'Otros'}
   b.entries.push(record);
  }
  if(!write(b)){msg.textContent=state.notice;return}
  state.notice='Movimiento guardado';render();
 };
}
/* La anulación conserva el asiento histórico y revierte su efecto contable.
 * No eliminar físicamente datos que puedan explicar movimientos anteriores.
 */
function voidEntry(id){
 const book=read();if(!book)return false;
 const entry=book.entries.find(x=>x.id===id);
 if(!entry||entry.voidedAt)return false;
 entry.voidedAt=new Date().toISOString();
 if(!write(book))return false;
 state.notice='Movimiento anulado. Se conserva como referencia en el historial.';
 render();return true;
}
function exportBook(){
 const book=read();if(!book)return false;
 const payload={format:'neurosync-finanzas-v1',exportedAt:new Date().toISOString(),
  account:user(),book};
 const blob=new Blob([JSON.stringify(payload,null,2)],{type:'application/json'});
 const url=URL.createObjectURL(blob),a=document.createElement('a');
 a.href=url;a.download='neurosync-finanzas-'+today()+'.json';
 document.body.appendChild(a);a.click();a.remove();
 setTimeout(()=>URL.revokeObjectURL(url),1000*10);
 return true;
}
root.addEventListener('click',e=>{
 const b=e.target.closest('button[data-finance]');if(!b)return;
 const a=b.dataset.finance;
 if(a==='add'||a==='account'){modal(a);return}
 if(a==='cancel'){$('#ns-fi-modal').replaceChildren();return}
 if(a==='export'){exportBook();return}
 if(a==='void'){
  const id=b.dataset.entryId;if(!id)return;
  const entry=read()?.entries.find(x=>x.id===id&&!x.voidedAt);
  if(!entry)return;
  $('#ns-fi-modal').innerHTML='<div class="ns-fi-shade" role="dialog" aria-modal="true" aria-label="Confirmar anulación">'+
   '<div class="ns-fi-dialog"><h3>¿Anular movimiento?</h3>'+
   '<p>Se corregirán los saldos y se conservará el movimiento en el historial.</p>'+
   '<strong>'+esc(entry.note||entry.category||'Movimiento')+' · '+cop(entry.amount)+'</strong>'+
   '<div class="ns-fi-modal-buttons"><button type="button" data-finance="confirm-void" data-entry-id="'+esc(id)+'">Confirmar anulación</button>'+
   '<button type="button" data-finance="cancel">Cancelar</button></div></div></div>';
  return;
 }
 if(a==='confirm-void'){voidEntry(b.dataset.entryId);return}
 if(['overview','accounts','budget'].includes(a)){state.tab=a;render()}
});
root.addEventListener('change',e=>{
 if(e.target.id==='ns-fi-month'){state.selected=e.target.value;render();return}
 if(e.target.id==='ns-fi-launcher'){
  const b=read();if(!b)return;b.launcher=e.target.checked;
  if(!write(b)){e.target.checked=!e.target.checked;return}
  state.notice=b.launcher?'Resumen activado para el launcher':'Resumen ocultado';render();
 }
});
root.addEventListener('submit',e=>{
 if(e.target.id!=='ns-fi-budget')return;
 e.preventDefault();const book=read(),n=peso(e.target.elements.amount.value);
 if(!book||n===null)return;
 book.budgets[state.selected]=n;
 if(write(book)){state.notice='Presupuesto guardado';render()}
});
window.NeuroFinanceStudio={render,syncLauncher:function(){if(user()){const b=read();if(b)native(b)}},hideLauncher:function(){
 try{if(window.NeuroFinanceAPI?.updateLauncher&&user())window.NeuroFinanceAPI.updateLauncher(false,0,0,0)}catch(e){}
}};
render();
window.NeuroFinanceStudio.syncLauncher();
})();
