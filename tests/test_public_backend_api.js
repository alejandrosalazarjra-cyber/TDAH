/** Pruebas sin proveedor cloud: peticiones reales al handler con dependencias simuladas. */
'use strict';
const fs=require('node:fs');
const vm=require('node:vm');
const assert=require('node:assert/strict');
const {Readable}=require('node:stream');
const realCrypto=require('node:crypto');
const source=fs.readFileSync('backend/server.js','utf8');
new vm.Script(source,{filename:'backend/server.js'});
const actorId='11111111-1111-4111-8111-111111111111';
const recipientId='22222222-2222-4222-8222-222222222222';
const queries=[];
const fakePool={
 query:async(sql,args)=>{
  queries.push({sql,args});
  if(sql==='SELECT 1')return {rows:[{one:1}],rowCount:1};
  if(sql.includes('FROM ns_public_sessions'))return {
   rows:[{id:actorId,handle:'alejandro',display_name:'Alejandro',discoverable:true}],rowCount:1};
  if(sql.includes('FROM ns_public_users u')&&sql.includes('u.handle=$1'))
   return {rows:[],rowCount:0};
  if(sql.includes('FROM ns_public_users u')&&sql.includes('EXISTS (SELECT 1 FROM ns_contact_invitations'))
   return {rows:[],rowCount:0};
  if(sql.includes('FROM ns_contact_invitations i JOIN ns_public_users u'))
   return {rows:[],rowCount:0};
  if(sql.includes('FROM ns_user_events'))return {rows:[],rowCount:0};
  return {rows:[],rowCount:0};
 },
 connect:async()=>({query:(...args)=>fakePool.query(...args),release:()=>{}})
};
const fakeModule={exports:{}},fakeRequire=name=>{
 if(name==='pg')return {Pool:class {constructor(){return fakePool}}};
 if(name==='google-auth-library')return {
  OAuth2Client:class{async verifyIdToken(){throw Error('Invalid token')}}};
 return require(name);
};
const context={require:fakeRequire,module:fakeModule,
 process:{env:{DATABASE_URL:'postgresql://testing/not-real',GOOGLE_CLIENT_ID:'test-client'}},
 console,Buffer,URL,Error};
vm.runInNewContext(source,context,{filename:'server.js'});
const handler=fakeModule.exports.request;
async function send(method,path,payload,auth){
 const data=payload===undefined?'':JSON.stringify(payload);
 const req=Readable.from(data?[Buffer.from(data)]:[]);
 req.method=method;req.url=path;req.headers={};
 if(auth)req.headers.authorization='Bearer '+auth;
 const res={status:0,headers:{},body:'',
  writeHead(status,headers){this.status=status;this.headers=headers},
  end(content){this.body=content}};
 await handler(req,res);
 return {status:res.status,...JSON.parse(res.body)};
}
(async()=>{
 const token='a'.repeat(55);
 let r=await send('GET','/health');
 assert.equal(r.status,200);
 r=await send('GET','/v1/me');assert.equal(r.status,401);
 r=await send('GET','/v1/me',undefined,token);
 assert.equal(r.status,200);assert.equal(r.user.id,actorId);
 r=await send('GET','/v1/users/lookup?handle=abc',undefined,token);
 assert.equal(r.status,200);assert.equal(r.user,null);
 r=await send('GET','/v1/events?cursor=bad',undefined,token);
 assert.equal(r.status,400);
 r=await send('POST','/v1/tasks',{recipientId, title:'Comprar pan'},token);
 assert.equal(r.status,400,'Sin clave de idempotencia no se puede enviar');
 r=await send('GET','/v1/auth/status');
 assert.equal(r.status,200);
 assert.equal(r.loginEnabled,false,'El registro debe iniciar deshabilitado');
 r=await send('POST','/v1/auth/exchange',{idToken:'falso'});
 assert.equal(r.status,503,'No abrir cuentas sin habilitación explícita');
 assert(queries.some(x=>x.sql.includes('FROM ns_public_sessions')),
  'Identidad debe recuperarse desde la sesión del servidor');
 assert(source.includes('pg_advisory_xact_lock'),
  'Reintentos concurrentes deben serializarse');
 assert(source.includes('NOT EXISTS(SELECT 1 FROM ns_contact_blocks'),
  'Un bloqueo debe impedir envíos');
 assert(source.includes('AND recipient_id=$3 AND status=$4'),
  'No completar tareas ajenas');
 console.log('Public API OK: autenticación obligatoria, ruta salud, validación y permisos');
})().catch(e=>{console.error(e);process.exitCode=1});
