/** Contrato del futuro backend público. No presupone despliegue real. */
const fs=require('node:fs'),assert=require('node:assert/strict');
const schema=fs.readFileSync('backend/schema_public_v1.sql','utf8');
const docs=fs.readFileSync('docs/ARQUITECTURA_CUENTAS_PUBLICAS_Y_TAREAS.md','utf8');
for(const name of ['ns_public_users','ns_contact_invitations','ns_contact_blocks',
 'ns_public_tasks','ns_task_idempotency','ns_user_events','ns_device_push_tokens']){
 assert(schema.includes('CREATE TABLE IF NOT EXISTS '+name),'Falta tabla '+name);
}
assert(schema.includes('UNIQUE')&&schema.includes('PRIMARY KEY (sender_id,request_key)'),
 'Cada remitente usa una clave idempotente única');
assert(schema.includes('CHECK (sender_id <> recipient_id)'),
 'Evitar tareas enviadas a la propia cuenta');
assert(schema.includes('recipient_id,updated_at DESC,id'),
 'Indexar tareas por destinatario');
assert(schema.includes('ns_events_for_user'),
 'Sincronizar novedades mediante cursor por cuenta');
assert(schema.includes("state IN ('pending','accepted','declined','cancelled')"),
 'Validar invitaciones de contacto');
assert(schema.includes("status IN ('pending','accepted','declined','completed')"),
 'Estados de tarea definidos');
assert(docs.includes('NO configuran IP, VPN')&&docs.includes('Idempotency-Key'),
 'El usuario no debe configurar servidores y los envíos no deben duplicarse');
assert(docs.includes('NO debe tratarse como sesión autorizada ante el backend'),
 'Distinguir autorización de Drive de identidad pública');
assert(docs.includes('API pública desplegada en Render')&&
 docs.includes('el registro permanece bloqueado'),
 'El contrato debe reflejar el entorno staging real y no prometer registro abierto');
console.log('Contrato público OK: identidades, contactos, permisos y entrega idempotente');
