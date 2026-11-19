'use strict';
/** Migración inicial, ejecutar una sola vez por entorno.
 * Pide confirmación explícita de destino para proteger la base familiar.
 */
const fs=require('node:fs');
const {Client}=require('pg');
const filename=require('node:path').join(__dirname,'schema_public_v1.sql');
async function main(){
 const url=process.env.DATABASE_URL;
 if(!url||process.env.NEUROSYNC_PUBLIC_DB_CONFIRM!=='yes'){
  console.error('Configura DATABASE_URL y NEUROSYNC_PUBLIC_DB_CONFIRM=yes para migrar una base PÚBLICA dedicada.');
  process.exitCode=1;return;
 }
 const parsed=new URL(url);
 if(!['postgres:','postgresql:'].includes(parsed.protocol))throw Error('DATABASE_URL no es PostgreSQL');
 const client=new Client({connectionString:url,connectionTimeoutMillis:5000});
 await client.connect();
 try{
  // Una sola sentencia por migración. PostgreSQL controla el BEGIN/COMMIT de esquema.
  await client.query(fs.readFileSync(filename,'utf8'));
  console.log('Esquema público NeuroSync v1 verificado');
 }finally{await client.end()}
}
if(require.main===module)main().catch(e=>{
 console.error('No se pudo aplicar el esquema público:',e.code||e.name);
 process.exitCode=1;
});
module.exports={main};
