'use strict';
/** NeuroSync Public API — etapa inicial. No sustituye el servidor familiar.
 * Requiere HTTPS terminado en proxy seguro, PostgreSQL y Google OAuth Client ID.
 * No escucha en una interfaz pública sin configuración explícita.
 */
const http=require('node:http');
const {randomUUID,randomBytes,createHash}=require('node:crypto');
const {Pool}=require('pg');
const {OAuth2Client}=require('google-auth-library');

const DB=process.env.DATABASE_URL;
const GOOGLE_CLIENT_ID=process.env.GOOGLE_CLIENT_ID;
const PORT=Number(process.env.PORT||8081);
const HOST=process.env.HOST||'127.0.0.1';
const pool=DB?new Pool({connectionString:DB,max:10,connectionTimeoutMillis:5000}):null;
const oauth=GOOGLE_CLIENT_ID?new OAuth2Client(GOOGLE_CLIENT_ID):null;
const sha256=value=>createHash('sha256').update(value).digest('hex');
const uuid=s=>typeof s==='string'&&/^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(s);
class ApiError extends Error{
 constructor(status,message){super(message);this.status=status;}
}
const reject=(status,message)=>{throw new ApiError(status,message)};
function respond(res,status,data){
 res.writeHead(status,{'content-type':'application/json; charset=utf-8',
 'cache-control':'no-store','x-content-type-options':'nosniff'});
 res.end(JSON.stringify(data));
}
async function body(req){
 let chunks=[],size=0;
 for await(const part of req){
  size+=part.length;
  if(size>16000)reject(413,'Solicitud demasiado grande');
  chunks.push(part);
 }
 try{
  const value=JSON.parse(Buffer.concat(chunks).toString('utf8')||'{}');
  if(!value||typeof value!=='object'||Array.isArray(value))reject(400,'Formato JSON inválido');
  return value;
 }catch(e){if(e instanceof ApiError)throw e;reject(400,'Formato JSON inválido')}
}
async function transaction(fn){
 const client=await pool.connect();
 try{await client.query('BEGIN');const result=await fn(client);await client.query('COMMIT');return result;}
 catch(e){await client.query('ROLLBACK');throw e;}
 finally{client.release();}
}
async function actor(req){
 const auth=req.headers.authorization||'';
 if(!/^Bearer [A-Za-z0-9_-]{40,100}$/.test(auth))reject(401,'Inicia sesión');
 const hash=sha256(auth.slice(7));
 const result=await pool.query(
  `SELECT u.id,u.handle,u.display_name,u.discoverable
   FROM ns_public_sessions s JOIN ns_public_users u ON u.id=s.user_id
   WHERE s.token_hash=$1 AND s.revoked_at IS NULL AND s.expires_at>now()
     AND u.disabled_at IS NULL`,[hash]);
 if(!result.rowCount)reject(401,'Sesión vencida o revocada');
 return result.rows[0];
}
async function exchange(data){
 if(!oauth||typeof data.idToken!=='string'||data.idToken.length>10000)
  reject(400,'Identidad de Google no configurada o inválida');
 let identity;
 try{
  const ticket=await oauth.verifyIdToken({idToken:data.idToken,audience:GOOGLE_CLIENT_ID});
  identity=ticket.getPayload();
  if(!identity||!identity.sub||!identity.email_verified)reject(401,'Cuenta no verificada');
 }catch(e){if(e instanceof ApiError)throw e;reject(401,'No se pudo verificar la identidad')}
 const subject='google:'+identity.sub;
 return transaction(async db=>{
  const existing=await db.query('SELECT id,handle,display_name FROM ns_public_users WHERE provider_subject=$1 AND disabled_at IS NULL',[subject]);
  let user=existing.rows[0];
  if(!user){
   const id=randomUUID(),handle='user_'+id.replace(/-/g,'').slice(0,20);
   const display=String(identity.name||'Usuario').trim().slice(0,100)||'Usuario';
   const created=await db.query(
    'INSERT INTO ns_public_users(id,provider_subject,handle,display_name) VALUES($1,$2,$3,$4) RETURNING id,handle,display_name',
    [id,subject,handle,display]);user=created.rows[0];
  }
  const token=randomBytes(32).toString('base64url');
  await db.query("INSERT INTO ns_public_sessions(token_hash,user_id,expires_at) VALUES($1,$2,now()+interval '7 days')",[sha256(token),user.id]);
  return {status:200,data:{ok:true,token,expiresIn:604800,user}};
 });
}
async function contacts(db,user){
 const found=await db.query(
  `SELECT u.id,u.handle,u.display_name
   FROM ns_contact_invitations i JOIN ns_public_users u
    ON u.id=CASE WHEN i.requester_id=$1 THEN i.recipient_id ELSE i.requester_id END
   WHERE (i.requester_id=$1 OR i.recipient_id=$1)
     AND i.state='accepted' AND u.disabled_at IS NULL
     AND NOT EXISTS (SELECT 1 FROM ns_contact_blocks b
       WHERE (b.blocker_id=$1 AND b.blocked_id=u.id)
          OR (b.blocker_id=u.id AND b.blocked_id=$1))
   ORDER BY u.handle LIMIT 100`,[user.id]);
 return found.rows;
}
function inviteAllowed(data){
 if(!uuid(data.recipientId))reject(400,'Destinatario inválido');
}
async function handler(req,res){
 if(!pool)reject(503,'Servicio todavía no configurado');
 const url=new URL(req.url,'http://localhost');
 const path=url.pathname;
 if(path==='/health'&&req.method==='GET'){
  await pool.query('SELECT 1');return respond(res,200,{ok:true});
 }
 if(path==='/v1/auth/exchange'&&req.method==='POST'){
  const result=await exchange(await body(req));return respond(res,result.status,result.data);
 }
 const user=await actor(req);
 if(path==='/v1/auth/logout'&&req.method==='POST'){
  await pool.query('UPDATE ns_public_sessions SET revoked_at=now() WHERE token_hash=$1 AND user_id=$2',
    [sha256(req.headers.authorization.slice(7)),user.id]);
  return respond(res,200,{ok:true});
 }
 if(path==='/v1/me'&&req.method==='GET')return respond(res,200,{ok:true,user});
 if(path==='/v1/me'&&(req.method==='PATCH'||req.method==='POST')){
  const data=await body(req),name=String(data.displayName||'').trim();
  const handle=String(data.handle||'').toLowerCase().trim().replace(/^@/,'');
  if(name.length<1||name.length>100||!/^[a-z0-9_]{3,30}$/.test(handle)||
    typeof data.discoverable!=='boolean')reject(400,'Perfil inválido');
  const updated=await pool.query(
   'UPDATE ns_public_users SET display_name=$1,handle=$2,discoverable=$3 WHERE id=$4 RETURNING id,handle,display_name,discoverable',
   [name,handle,data.discoverable,user.id]);
  return respond(res,200,{ok:true,user:updated.rows[0]});
 }
 if(path==='/v1/users/lookup'&&req.method==='GET'){
  const handle=(url.searchParams.get('handle')||'').toLowerCase().replace(/^@/,'');
  if(!/^[a-z0-9_]{3,30}$/.test(handle))reject(400,'Identificador inválido');
  const found=await pool.query(
   `SELECT u.id,u.handle,u.display_name FROM ns_public_users u
    WHERE u.handle=$1 AND u.discoverable=true AND u.disabled_at IS NULL AND u.id<>$2
    AND NOT EXISTS (SELECT 1 FROM ns_contact_blocks b
      WHERE (b.blocker_id=u.id AND b.blocked_id=$2)
         OR (b.blocker_id=$2 AND b.blocked_id=u.id)) LIMIT 1`,[handle,user.id]);
  return respond(res,200,{ok:true,user:found.rows[0]||null});
 }
 if(path==='/v1/contacts'&&req.method==='GET'){
  return respond(res,200,{ok:true,contacts:await contacts(pool,user)});
 }
 if(path==='/v1/contacts/invitations'&&req.method==='POST'){
  const data=await body(req);inviteAllowed(data);
  if(data.recipientId===user.id)reject(400,'No puedes invitarte');
  const result=await transaction(async db=>{
   const contact=await db.query(
    `SELECT id FROM ns_public_users u WHERE id=$1 AND discoverable=true AND disabled_at IS NULL
     AND NOT EXISTS(SELECT 1 FROM ns_contact_blocks b WHERE
       (b.blocker_id=$1 AND b.blocked_id=$2) OR (b.blocker_id=$2 AND b.blocked_id=$1))`,
    [data.recipientId,user.id]);
   if(!contact.rowCount)reject(404,'Usuario no disponible');
   const existing=await db.query(
    `SELECT id,state FROM ns_contact_invitations
     WHERE ((requester_id=$1 AND recipient_id=$2) OR (requester_id=$2 AND recipient_id=$1))
       AND state IN ('pending','accepted') LIMIT 1`,[user.id,data.recipientId]);
   if(existing.rowCount)return {status:200,data:{ok:true,invitation:existing.rows[0]}};
   const created=await db.query(
    `INSERT INTO ns_contact_invitations(id,requester_id,recipient_id)
      VALUES($1,$2,$3) RETURNING id,state`,[randomUUID(),user.id,data.recipientId]);
   return {status:201,data:{ok:true,invitation:created.rows[0]}};
  });
  return respond(res,result.status,result.data);
 }
 if(path==='/v1/contacts/invitations'&&req.method==='GET'){
  const result=await pool.query(
   `SELECT i.id,i.state,i.created_at,u.handle,u.display_name
    FROM ns_contact_invitations i JOIN ns_public_users u ON u.id=i.requester_id
    WHERE i.recipient_id=$1 AND i.state='pending' ORDER BY i.created_at DESC LIMIT 100`,[user.id]);
  return respond(res,200,{ok:true,invitations:result.rows});
 }
 const accept=path.match(/^\/v1\/contacts\/invitations\/([a-f0-9-]{36})\/accept$/i);
 if(accept&&req.method==='POST'){
  const result=await pool.query(
   `UPDATE ns_contact_invitations i SET state='accepted',changed_at=now()
    WHERE i.id=$1 AND i.recipient_id=$2 AND i.state='pending'
    AND NOT EXISTS(SELECT 1 FROM ns_contact_blocks b WHERE
      (b.blocker_id=i.requester_id AND b.blocked_id=$2) OR
      (b.blocker_id=$2 AND b.blocked_id=i.requester_id))
    RETURNING id,state`,[accept[1],user.id]);
  if(!result.rowCount)reject(404,'Invitación no disponible');
  return respond(res,200,{ok:true,invitation:result.rows[0]});
 }
 if(path==='/v1/tasks'&&req.method==='POST'){
  const data=await body(req);
  const recipientId=data.recipientId,key=req.headers['idempotency-key'];
  const title=typeof data.title==='string'?data.title.trim():'';
  const repeat=data.repeat||'none';
  const steps=Array.isArray(data.steps)?data.steps:[];
  if(!uuid(recipientId)||recipientId===user.id||!/^[A-Za-z0-9_-]{8,100}$/.test(key||'')||
   !title||title.length>100||!['none','daily','weekly'].includes(repeat)||
   steps.length>20||steps.some(s=>typeof s!=='string'||!s.trim()||s.length>140))
   reject(400,'Datos de tarea inválidos');
  const canonical={recipientId,title,repeat,steps};
  const digest=sha256(JSON.stringify(canonical));
  const result=await transaction(async db=>{
   // Serialización por usuario/clave para reintentos concurrentes.
   await db.query('SELECT pg_advisory_xact_lock(hashtext($1),hashtext($2))',[user.id,key]);
   const past=await db.query(
    `SELECT i.payload_sha256,t.id,t.status,t.recipient_id
     FROM ns_task_idempotency i JOIN ns_public_tasks t ON t.id=i.task_id
     WHERE i.sender_id=$1 AND i.request_key=$2`,[user.id,key]);
   if(past.rowCount){
    if(past.rows[0].payload_sha256!==digest)reject(409,'La clave ya se utilizó con otra tarea');
    return {status:200,data:{ok:true,task:{id:past.rows[0].id,status:past.rows[0].status},duplicate:true}};
   }
   const allowed=await db.query(
    `SELECT 1 FROM ns_public_users u WHERE u.id=$2 AND u.disabled_at IS NULL
     AND EXISTS (SELECT 1 FROM ns_contact_invitations i
      WHERE i.state='accepted' AND ((i.requester_id=$1 AND i.recipient_id=$2)
         OR (i.recipient_id=$1 AND i.requester_id=$2)))
     AND NOT EXISTS(SELECT 1 FROM ns_contact_blocks b WHERE
      (b.blocker_id=$1 AND b.blocked_id=$2) OR (b.blocker_id=$2 AND b.blocked_id=$1))`,
    [user.id,recipientId]);
   if(!allowed.rowCount)reject(403,'Solo puedes enviar tareas a contactos confirmados');
   const id=randomUUID();
   await db.query(
    `INSERT INTO ns_public_tasks(id,sender_id,recipient_id,title,body)
     VALUES($1,$2,$3,$4,$5::jsonb)`,
    [id,user.id,recipientId,title,JSON.stringify({repeat,steps})]);
   await db.query(
    'INSERT INTO ns_task_idempotency(sender_id,request_key,payload_sha256,task_id) VALUES($1,$2,$3,$4)',
    [user.id,key,digest,id]);
   await db.query(
    `INSERT INTO ns_user_events(user_id,task_id,event_type) VALUES($1,$3,'task_received'),($2,$3,'task_received')`,
    [recipientId,user.id,id]);
   return {status:201,data:{ok:true,task:{id,status:'pending'}}};
  });
  return respond(res,result.status,result.data);
 }
 if(path==='/v1/tasks'&&req.method==='GET'){
  const box=url.searchParams.get('box');
  if(!['inbox','sent'].includes(box))reject(400,'Bandeja inválida');
  const column=box==='inbox'?'recipient_id':'sender_id';
  const rows=await pool.query(
   `SELECT id,title,status,created_at,updated_at,version,
      sender_id,recipient_id FROM ns_public_tasks
    WHERE ${column}=$1 ORDER BY updated_at DESC,id DESC LIMIT 100`,[user.id]);
  return respond(res,200,{ok:true,tasks:rows.rows});
 }
 const action=path.match(/^\/v1\/tasks\/([a-f0-9-]{36})\/(accept|decline|complete)$/i);
 if(action&&req.method==='POST'){
  const transitions={accept:['pending','accepted'],decline:['pending','declined'],complete:['accepted','completed']};
  const [previous,next]=transitions[action[2]];
  const result=await transaction(async db=>{
   const task=await db.query(
    `UPDATE ns_public_tasks SET status=$1,version=version+1,
       updated_at=now(),accepted_at=CASE WHEN $1='accepted' THEN now() ELSE accepted_at END,
       completed_at=CASE WHEN $1='completed' THEN now() ELSE completed_at END
     WHERE id=$2 AND recipient_id=$3 AND status=$4
     RETURNING id,status,sender_id,recipient_id`,[next,action[1],user.id,previous]);
   if(!task.rowCount)reject(409,'La tarea no está disponible para ese cambio');
   const record=task.rows[0],event='task_'+next;
   await db.query('INSERT INTO ns_user_events(user_id,task_id,event_type) VALUES($1,$3,$4),($2,$3,$4)',
    [record.sender_id,record.recipient_id,record.id,event]);
   return record;
  });
  return respond(res,200,{ok:true,task:{id:result.id,status:result.status}});
 }
 if(path==='/v1/events'&&req.method==='GET'){
  const cursor=url.searchParams.get('cursor')||'0';
  if(!/^\d{1,18}$/.test(cursor))reject(400,'Cursor inválido');
  const events=await pool.query(
   `SELECT sequence,task_id,event_type,created_at FROM ns_user_events
    WHERE user_id=$1 AND sequence>$2::bigint ORDER BY sequence ASC LIMIT 100`,
    [user.id,cursor]);
  return respond(res,200,{ok:true,events:events.rows,
    nextCursor:events.rows.length?String(events.rows.at(-1).sequence):cursor});
 }
 reject(404,'Función no encontrada');
}
async function request(req,res){
 try{
  if(!['GET','POST','PATCH'].includes(req.method))reject(405,'Método no permitido');
  await handler(req,res);
 }catch(e){
  const status=e instanceof ApiError?e.status:(e.code==='23505'?409:500);
  if(status>=500)console.error('Error interno de API:',e.code||e.name);
  respond(res,status,{ok:false,error:status>=500?'Error interno del servicio':e.message});
 }
}
if(require.main===module){
 if(!pool||!oauth||!Number.isInteger(PORT)||PORT<1||PORT>65535){
  console.error('Falta DATABASE_URL, GOOGLE_CLIENT_ID o PORT válido. API pública no iniciada.');
  process.exit(1);
 }
 const server=http.createServer(request);
 server.requestTimeout=15000;
 server.listen(PORT,HOST,()=>console.log('NeuroSync API pública lista detrás de HTTPS'));
}
module.exports={request,sha256};
