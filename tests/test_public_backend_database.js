'use strict';
/** Integración real con PostgreSQL: solo ejecutar sobre base de pruebas desechable.
 * No usa Google real: crea sesiones de test de servidor, sin exponer secretos.
 */
const assert=require('node:assert/strict');
const http=require('node:http');
const {createHash,randomUUID,randomBytes}=require('node:crypto');
const {createRequire}=require('node:module');
const {resolve}=require('node:path');
const backendRequire=createRequire(resolve('backend/package.json'));
const {Pool}=backendRequire('pg');
const {request}=backendRequire('./server.js');
async function main(){
 const url=process.env.DATABASE_URL||'';
 if(!url.includes('neurosync_tests'))throw Error('La integración solo puede usar la base neurosync_tests');
 const db=new Pool({connectionString:url,max:3});
 const users=[randomUUID(),randomUUID(),randomUUID()];
 const [alice,bob,stranger]=users;
 const token=randomBytes(32).toString('base64url');
 const hash=createHash('sha256').update(token).digest('hex');
 const username='test_'+randomBytes(5).toString('hex');
 let server;
 try{
  for(const [i,id] of users.entries()){
   await db.query('INSERT INTO ns_public_users(id,provider_subject,handle,display_name,discoverable) VALUES($1,$2,$3,$4,true)',
    [id,'test:'+id,username+i,'Cuenta de prueba '+i]);
  }
  await db.query("INSERT INTO ns_public_sessions(token_hash,user_id,expires_at) VALUES($1,$2,now()+interval '1 hour')",[hash,alice]);
  const tokenBob=randomBytes(32).toString('base64url');
  const tokenStranger=randomBytes(32).toString('base64url');
  for(const [id,t] of [[bob,tokenBob],[stranger,tokenStranger]]){
   await db.query("INSERT INTO ns_public_sessions(token_hash,user_id,expires_at) VALUES($1,$2,now()+interval '1 hour')",
    [createHash('sha256').update(t).digest('hex'),id]);
  }
  const app=http.createServer(request);
  server=app;
  await new Promise(resolve=>app.listen(0,'127.0.0.1',resolve));
  const base='http://127.0.0.1:'+app.address().port;
  async function call(method,path,auth,body,key){
   const res=await fetch(base+path,{method,headers:{...(auth?{Authorization:'Bearer '+auth}:{}),
    ...(key?{'Idempotency-Key':key}:{}),...(body?{'content-type':'application/json'}:{})},
    body:body?JSON.stringify(body):undefined});
   return {status:res.status,data:await res.json()};
  }
  let answer=await call('POST','/v1/contacts/invitations',token,{recipientId:bob});
  assert.equal(answer.status,201);
  const inviteId=answer.data.invitation.id;
  answer=await call('POST','/v1/contacts/invitations/'+inviteId+'/accept',tokenBob);
  assert.equal(answer.status,200);
  const task={recipientId:bob,title:'Comprar pan',repeat:'none',steps:[]},key='idem_'+randomBytes(12).toString('hex');
  answer=await call('POST','/v1/tasks',token,task,key);
  assert.equal(answer.status,201);
  const taskId=answer.data.task.id;
  answer=await call('POST','/v1/tasks',token,task,key);
  assert.equal(answer.status,200);assert.equal(answer.data.task.id,taskId);
  answer=await call('POST','/v1/tasks',token,{...task,title:'Texto distinto'},key);
  assert.equal(answer.status,409);
  answer=await call('GET','/v1/tasks?box=inbox',tokenStranger);
  assert.equal(answer.status,200);assert(!answer.data.tasks.some(t=>t.id===taskId));
  answer=await call('POST','/v1/tasks/'+taskId+'/complete',tokenStranger);
  assert.equal(answer.status,409);
  answer=await call('POST','/v1/tasks/'+taskId+'/complete',tokenBob);
  assert.equal(answer.status,409,'No completar antes de aceptar');
  answer=await call('POST','/v1/tasks/'+taskId+'/accept',tokenBob);
  assert.equal(answer.status,200);
  answer=await call('POST','/v1/tasks/'+taskId+'/complete',tokenBob);
  assert.equal(answer.status,200);
  answer=await call('GET','/v1/tasks?box=sent',token);
  assert(answer.data.tasks.some(t=>t.id===taskId&&t.status==='completed'));
  answer=await call('GET','/v1/events?cursor=0',tokenStranger);
  assert.equal(answer.status,200);assert.equal(answer.data.events.length,0);
  answer=await call('GET','/v1/events/latest',tokenBob);
  assert.equal(answer.status,200);
  assert(/^\\d+$/.test(answer.data.nextCursor),'Cursor inicial de avisos debe ser entero');
  answer=await call('GET','/v1/events?cursor=0',tokenBob);
  assert.equal(answer.status,200);
  assert(answer.data.events.some(e=>e.event_type==='task_received'&&e.task_role==='recipient'));
  answer=await call('POST','/v1/contacts/block',token,{recipientId:bob});
  assert.equal(answer.status,200);
  answer=await call('GET','/v1/contacts',token);
  assert(!answer.data.contacts.some(c=>c.id===bob),'Bloquear oculta el contacto');
  answer=await call('GET','/v1/contacts/blocks',token);
  assert(answer.data.blocked.some(c=>c.id===bob));
  answer=await call('POST','/v1/tasks',token,{recipientId:bob,title:'No debería llegar'},'blocked_'+randomBytes(12).toString('hex'));
  assert.equal(answer.status,403,'No se puede enviar tareas a un usuario bloqueado');
  answer=await call('POST','/v1/contacts/unblock',token,{recipientId:bob});
  assert.equal(answer.status,200);
  answer=await call('GET','/v1/contacts',token);
  assert(answer.data.contacts.some(c=>c.id===bob));
  answer=await call('POST','/v1/me/delete',tokenBob,{confirm:'incorrecto'});
  assert.equal(answer.status,400,'Borrado exige confirmación exacta');
  answer=await call('POST','/v1/me/delete',tokenBob,{confirm:'ELIMINAR'});
  assert.equal(answer.status,200);
  answer=await call('GET','/v1/me',tokenBob);
  assert.equal(answer.status,401,'El token queda revocado al eliminar cuenta');
  answer=await call('GET','/v1/tasks?box=sent',token);
  assert(!answer.data.tasks.some(t=>t.id===taskId),'Se depuran tareas compartidas al borrar cuenta');
  answer=await call('GET','/v1/me',token);
  assert.equal(answer.status,200,'No se elimina al remitente ajeno');
  console.log('PostgreSQL integrado OK: identidades, permisos, bloqueo, eliminación, eventos y estados');
 }finally{
  if(server)await new Promise(resolve=>server.close(resolve));
  await db.query('DELETE FROM ns_user_events WHERE user_id=ANY($1::uuid[])',[users]);
  await db.query('DELETE FROM ns_task_idempotency WHERE sender_id=ANY($1::uuid[])',[users]);
  await db.query('DELETE FROM ns_public_tasks WHERE sender_id=ANY($1::uuid[])',[users]);
  await db.query('DELETE FROM ns_contact_invitations WHERE requester_id=ANY($1::uuid[]) OR recipient_id=ANY($1::uuid[])',[users,users]);
  await db.query('DELETE FROM ns_public_sessions WHERE user_id=ANY($1::uuid[])',[users]);
  await db.query('DELETE FROM ns_public_users WHERE id=ANY($1::uuid[])',[users]);
  await db.end();
 }
}
main().catch(e=>{console.error('Fallo de integración PostgreSQL:',e.message);process.exitCode=1});
