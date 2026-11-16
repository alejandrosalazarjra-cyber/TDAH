const encoder=new TextEncoder();
const safeUser=name=>typeof name==='string'&&/^[a-z0-9_-]{3,30}$/.test(name);
const json=(body,status=200)=>new Response(JSON.stringify(body),{status,headers:{'content-type':'application/json; charset=utf-8','cache-control':'no-store','x-content-type-options':'nosniff'}});
const fail=(message,status=400)=>json({ok:false,error:message},status);
function randomHex(length=32){const bytes=crypto.getRandomValues(new Uint8Array(length));return [...bytes].map(x=>x.toString(16).padStart(2,'0')).join('')}
async function digest(text){const b=await crypto.subtle.digest('SHA-256',encoder.encode(text));return [...new Uint8Array(b)].map(x=>x.toString(16).padStart(2,'0')).join('')}
async function derive(password,salt){
 const key=await crypto.subtle.importKey('raw',encoder.encode(password),{name:'PBKDF2'},false,['deriveBits']);
 const bits=await crypto.subtle.deriveBits({name:'PBKDF2',salt:encoder.encode(salt),iterations:180000,hash:'SHA-256'},key,256);
 return [...new Uint8Array(bits)].map(x=>x.toString(16).padStart(2,'0')).join('');
}
function constantEq(a,b){
 if(typeof a!=='string'||typeof b!=='string')return false;
 let diff=a.length^b.length;for(let i=0;i<Math.max(a.length,b.length);i++)diff|=(a.charCodeAt(i)||0)^(b.charCodeAt(i)||0);
 return diff===0;
}
async function actor(req,db){
 const h=req.headers.get('authorization')||'';
 if(!h.startsWith('Bearer '))return null;
 const token=h.slice(7).trim();if(token.length<48||token.length>200)return null;
 const hash=await digest(token);
 const row=await db.prepare('SELECT users.username,users.role FROM sessions JOIN users ON users.username=sessions.username WHERE sessions.token_hash=? AND sessions.expires_at>? AND users.active=1').bind(hash,Date.now()).first();
 return row||null;
}
async function createUser(db,username,password,role){
 const salt=randomHex(18),hash=await derive(password,salt);
 await db.prepare('INSERT INTO users(username,role,pass_salt,pass_hash,active) VALUES (?,?,?,?,1)').bind(username,role,salt,hash).run();
}
async function dispatch(req,env){
 if(req.method!=='POST')return fail('Solo peticiones POST',405);
 const path=new URL(req.url).pathname;
 if(!['/bootstrap','/login','/users','/create-user','/change-password','/sync'].includes(path))return fail('Ruta desconocida',404);
 const length=Number(req.headers.get('content-length')||'0');
 if(length>400000)return fail('Petición demasiado grande',413);
 let payload;
 try{const raw=await req.text();if(raw.length>400000)return fail('Petición demasiado grande',413);payload=JSON.parse(raw)}catch(e){return fail('Formato JSON inválido')}
 if(!payload||typeof payload!=='object'||Array.isArray(payload))return fail('Datos inválidos');
 const db=env.DB;
 if(path==='/bootstrap'){
   if(!env.BOOTSTRAP_SECRET||env.BOOTSTRAP_SECRET.length<30)return fail('Secreto no configurado',503);
   const secret=req.headers.get('x-bootstrap-secret')||'';
   if(!constantEq(env.BOOTSTRAP_SECRET,secret))return fail('No autorizado',403);
   const exists=await db.prepare('SELECT COUNT(*) AS total FROM users').first();
   if(exists.total>0)return fail('El administrador ya está registrado',409);
   const username=String(payload.username||'').toLowerCase(),password=payload.password;
   if(!safeUser(username)||typeof password!=='string'||password.length<12||password.length>128)return fail('Datos iniciales inválidos');
   await createUser(db,username,password,'admin');
   return json({ok:true});
 }
 if(path==='/login'){
   const username=String(payload.username||'').toLowerCase(),password=payload.password;
   if(!safeUser(username)||typeof password!=='string'||password.length>128)return fail('Credenciales incorrectas',401);
   const u=await db.prepare('SELECT * FROM users WHERE username=? AND active=1').bind(username).first();
   if(!u)return fail('Credenciales incorrectas',401);
   const test=await derive(password,u.pass_salt);
   if(!constantEq(test,u.pass_hash))return fail('Credenciales incorrectas',401);
   const token=randomHex(32);const hash=await digest(token);
   await db.prepare('INSERT INTO sessions(token_hash,username,expires_at) VALUES (?,?,?)').bind(hash,username,Date.now()+7*86400000).run();
   return json({ok:true,token,user:{username:u.username,role:u.role,active:true}});
 }
 const me=await actor(req,db);
 if(!me)return fail('Inicia sesión de nuevo',401);
 if(path==='/users'){
   if(me.role!=='admin')return fail('Sin permisos',403);
   const users=await db.prepare('SELECT username,role,active FROM users ORDER BY username').all();
   return json({ok:true,users:users.results.map(u=>({...u,active:!!u.active}))});
 }
 if(path==='/create-user'){
   if(me.role!=='admin')return fail('Solo el administrador crea cuentas',403);
   const username=String(payload.username||'').toLowerCase(),pass=payload.password;
   if(!safeUser(username)||typeof pass!=='string'||pass.length<10||pass.length>128)return fail('Usuario o contraseña inválidos');
   if(await db.prepare('SELECT username FROM users WHERE username=?').bind(username).first())return fail('El usuario ya existe',409);
   await createUser(db,username,pass,'member');
   return json({ok:true,user:username});
 }
 if(path==='/change-password'){
   const current=payload.current,next=payload.next;
   if(typeof current!=='string'||typeof next!=='string'||next.length<10||next.length>128)return fail('Contraseña nueva inválida');
   const u=await db.prepare('SELECT * FROM users WHERE username=?').bind(me.username).first();
   if(!constantEq(await derive(current,u.pass_salt),u.pass_hash))return fail('Contraseña actual incorrecta',403);
   const salt=randomHex(18),hash=await derive(next,salt);
   await db.batch([
     db.prepare('UPDATE users SET pass_salt=?,pass_hash=? WHERE username=?').bind(salt,hash,me.username),
     db.prepare('DELETE FROM sessions WHERE username=?').bind(me.username)
   ]);
   return json({ok:true,reauth:true});
 }
 const kinds=['tasks','packs','alarms'];
 for(const kind of kinds){
   if(payload[kind]!=null&&!Array.isArray(payload[kind]))return fail('Formato de colección incorrecto');
   const arr=payload[kind]||[];
   if(arr.length>1000)return fail('Demasiados registros');
   for(const value of arr){
     if(!value||typeof value!=='object'||Array.isArray(value))continue;
     const id=value.id,owner=value.owner;
     if(typeof id!=='string'||id.length<3||id.length>100||!safeUser(owner)&&owner!=='todos')continue;
     const original=await db.prepare('SELECT owner,updated_at,payload FROM records WHERE kind=? AND id=?').bind(kind,id).first();
     const canEdit=me.role==='admin'||owner===me.username||owner==='todos'&&kind==='tasks';
     if(!canEdit)continue;
     if(original && me.role!=='admin' && original.owner!==me.username && original.owner!=='todos')continue;
     if(original && original.owner!==owner && me.role!=='admin')continue;
     const timestamp=Number(value.updatedAt)||0;
     if(timestamp<=0||timestamp>Date.now()+300000||original&&timestamp<=original.updated_at)continue;
     let stored=value;
     if(me.role!=='admin'&&owner==='todos'){
       if(!original||value.deleted)continue;
       const prior=JSON.parse(original.payload);
       stored={...prior,doneDates:Array.isArray(value.doneDates)?value.doneDates:prior.doneDates,stepDates:value.stepDates||prior.stepDates,updatedAt:timestamp};
     }
     const encoded=JSON.stringify(stored);
     if(encoded.length>12000)continue;
     await db.prepare('INSERT INTO records(id,kind,owner,updated_at,payload) VALUES(?,?,?,?,?) ON CONFLICT(kind,id) DO UPDATE SET owner=excluded.owner,updated_at=excluded.updated_at,payload=excluded.payload WHERE excluded.updated_at>records.updated_at')
      .bind(id,kind,stored.owner,timestamp,encoded).run();
   }
 }
 const data={tasks:[],packs:[],alarms:[]};
 for(const kind of kinds){
   const query=me.role==='admin'?'SELECT payload FROM records WHERE kind=? LIMIT 2000':'SELECT payload FROM records WHERE kind=? AND (owner=? OR owner=?) LIMIT 2000';
   const statement=db.prepare(query);
   const rows=await (me.role==='admin'?statement.bind(kind).all():statement.bind(kind,me.username,'todos').all());
   data[kind]=rows.results.map(r=>JSON.parse(r.payload));
 }
 return json({ok:true,data});
}
export default {
 async fetch(req,env){
  try{
   if(!env.DB)return fail('Base no configurada',503);
   return await dispatch(req,env);
  }catch(e){
   console.error('Error de servidor',e?.name||'Error');
   return fail('Error interno del servicio',500);
  }
 }
};