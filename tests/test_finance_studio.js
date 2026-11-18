/* NeuroSync Finanzas: invariantes contables, reportes y privacidad del launcher. */
const assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm');
const script=fs.readFileSync('modules/finance_studio.js','utf8');
const html=fs.readFileSync('index.html','utf8');
const android=fs.readFileSync('android/app/src/main/java/com/neurosync/familia/MainActivity.java','utf8');
const launcher=fs.readFileSync('android/app/src/main/java/com/neurosync/familia/FocusLauncherActivity.java','utf8');
const workflow=fs.readFileSync('.github/workflows/android-apk.yml','utf8');
new vm.Script(script);
for(const match of html.matchAll(/<script(?:[^>]*)>([\s\S]*?)<\/script>/g))if(match[1].trim())new vm.Script(match[1]);
assert(html.includes('id="view-finance"')&&html.includes('data-view="finance"'));
assert(html.includes("window.NeuroFinanceAPI={"));
assert(html.includes("window.NeuroFinanceStudio.hideLauncher()"),'Ocultar resumen al cerrar sesión');
assert(script.includes('ns_private_finance_v1_'),'Se requiere separación por cuenta');
assert(script.includes('const data=stats(book,month(today()))'),'Launcher debe mostrar el mes actual');
assert(script.includes('launcher:false'),'Privacidad desactivada de forma predeterminada');
assert(android.includes('canAccessSnapshot(username)'),'Actualizar launcher solo desde cuenta verificada');
assert(launcher.includes('getBoolean("enabled",false)'),'Ocultar resumen hasta consentimiento');
assert(launcher.includes('openNeuroSync("finance")'),'Resumen abre finanzas');
assert(workflow.includes('cp modules/finance_studio.js modules/finance_studio.css'));
const start=script.indexOf('function balanceFor('),end=script.indexOf('function accountOptions(',start);
assert(start>0&&end>start);
const impl=new Function('month',script.slice(start,end)+'\nreturn {balanceFor,stats}');
const month=x=>String(x).slice(0,7),{balanceFor,stats}=impl(month);
const book={
 accounts:[{id:'cash',name:'Efectivo',opening:100000},{id:'bank',name:'Banco',opening:300000}],
 entries:[
  {type:'income',account:'bank',amount:500000,date:'2026-11-12',category:'Salario'},
  {type:'expense',account:'cash',amount:20000,date:'2026-11-12',category:'Alimentación'},
  {type:'transfer',from:'bank',to:'cash',amount:80000,date:'2026-11-13'}
 ],budgets:{'2026-11':150000}
};
assert.equal(balanceFor(book,'cash'),160000);
assert.equal(balanceFor(book,'bank'),720000);
const report=stats(book,'2026-11');
assert.equal(report.balance,880000);
assert.equal(report.income,500000,'Transferencias NO son ingresos');
assert.equal(report.expense,20000,'Transferencias NO son gastos');
assert.equal(report.remaining,130000);
assert.equal(report.categories[0][0],'Alimentación');
const noActivity=stats(book,'2026-10');
assert.equal(noActivity.income,0);
assert.equal(noActivity.expense,0);
assert.equal(noActivity.balance,880000,'Saldo de cuentas = saldo actual, no movimientos del mes');
console.log('Finanzas OK: asientos, conciliación de saldos, transferencias, categorías, privacidad del launcher');
