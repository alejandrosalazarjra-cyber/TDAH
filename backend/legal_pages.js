'use strict';
/** Páginas legales públicas de NeuroSync — revisar antes de publicar en Google Play.
 * Sin rastreo, cookies, credenciales ni dependencias de terceros.
 */
function layout(title,body){
 return '<!doctype html><html lang="es"><head><meta charset="utf-8">'+
 '<meta name="viewport" content="width=device-width,initial-scale=1">'+
 '<title>'+title+' · NeuroSync</title>'+
 '<style>body{font:16px/1.6 system-ui,Arial,sans-serif;max-width:48rem;'+
 'margin:2rem auto;padding:0 1.2rem;color:#18253b;background:#fff}'+
 'a{color:#1559b1}h1,h2{line-height:1.25}'+
 'footer{border-top:1px solid #ddd;margin-top:2rem;padding-top:1rem}</style>'+
 '</head><body><main>'+body+'</main><footer><a href="/privacy">Privacidad</a>'+
 ' · <a href="/delete-account">Eliminar una cuenta</a> · NeuroSync</footer></body></html>';
}
const pages={
 '/':layout('NeuroSync',
  '<h1>NeuroSync</h1><p>Organiza tus tareas y comparte asignaciones con contactos autorizados.</p>'+
  '<p>Este servidor aloja únicamente la API de pruebas de NeuroSync. El registro público está deshabilitado.</p>'+
  '<p><a href="/privacy">Privacidad y datos</a> · <a href="/delete-account">Eliminar mi cuenta</a></p>'),
 '/privacy':layout('Política de privacidad',
  '<h1>Privacidad de NeuroSync</h1><p><strong>Última actualización:</strong> 18 de noviembre de 2026.</p>'+
  '<p>Esta página corresponde a la versión de pruebas de NeuroSync. Será revisada antes de su publicación general.</p>'+
  '<h2>Datos gestionados</h2>'+
  '<p>Al iniciar sesión con Google, NeuroSync valida la identidad mediante Google y conserva un identificador técnico de cuenta, el nombre visible, un alias público opcional y preferencias de visibilidad. No almacenamos tu contraseña de Google ni el token de identidad enviado al iniciar sesión.</p>'+
  '<p>Al compartir una tarea, se almacenan el remitente, destinatario, texto de la tarea, pasos opcionales, estado de aceptación y eventos asociados. El servidor almacena un hash del token de sesión. No existe cifrado de extremo a extremo para las tareas compartidas.</p>'+
  '<p>Las alarmas, notas de hábitos, datos de finanzas, progreso y mascota se guardan principalmente en tu dispositivo; la opción voluntaria de respaldo mediante Google Drive requiere autorización independiente del usuario.</p>'+
  '<h2>Finalidad y acceso</h2><p>Utilizamos esos datos para autenticación, contactos autorizados, envío de tareas, estados y seguridad. Las tareas solo pueden consultarlas las cuentas autorizadas correspondientes. Usamos Google para identidad y Render para alojamiento de la API y base de datos. No vendemos datos personales ni los utilizamos para anuncios.</p>'+
  '<h2>Conservación y eliminación</h2><p>Las sesiones del servidor tienen vencimiento, y los datos de tareas se conservan mientras la cuenta exista o hasta que sean eliminados. Las copias locales pertenecen al dispositivo. La base gratuita de pruebas es temporal y puede eliminarse cuando termine el piloto; no se deben almacenar datos importantes allí.</p>'+
  '<p>Podrás eliminar tu cuenta desde la aplicación, en Tareas → Mis contactos → Privacidad y cuenta, o solicitar el proceso a través de <a href="/delete-account">esta página</a>.</p>'+
  '<h2>Contacto</h2><p>Para preguntas sobre privacidad: <a href="mailto:elgatojhon8@gmail.com">elgatojhon8@gmail.com</a>.</p>'),
 '/delete-account':layout('Eliminación de cuenta',
  '<h1>Eliminar tu cuenta de NeuroSync</h1>'+
  '<p>NeuroSync permite eliminar definitivamente la cuenta pública, las sesiones activas, las invitaciones, los bloqueos y las tareas compartidas asociadas a ella. No es posible recuperar una cuenta eliminada.</p>'+
  '<h2>Desde la aplicación</h2><p>Inicia sesión, abre <strong>Tareas → Mis contactos → Privacidad y cuenta</strong> y pulsa <strong>Eliminar definitivamente mi cuenta</strong>. La aplicación te solicitará dos confirmaciones antes de enviar la solicitud autenticada.</p>'+
  '<h2>Solicitar eliminación sin la aplicación</h2>'+
  '<p>Si no puedes entrar a la aplicación, envía una solicitud a '+
  '<a href="mailto:elgatojhon8@gmail.com?subject=Solicitud%20de%20eliminaci%C3%B3n%20de%20cuenta%20NeuroSync">elgatojhon8@gmail.com</a>'+
  ' indicando el alias de la cuenta. No envíes contraseñas ni códigos de Google. Verificaremos la titularidad antes de eliminar datos.</p>'+
  '<h2>Datos afectados</h2><p>Se eliminan del servicio los datos de tu perfil, sesiones e interacciones compartidas. La aplicación intenta borrar las copias locales correspondientes. Los datos en copias de seguridad externas voluntarias pueden requerir eliminación desde el proveedor correspondiente.</p>'+
  '<p>El servidor de pruebas no sustituye los servicios de retención y respaldo que requerirá la publicación final.</p>')
};
function serveLegal(res,path){
 const html=pages[path];if(!html)return false;
 res.writeHead(200,{'content-type':'text/html; charset=utf-8','cache-control':'public,max-age=300',
  'x-content-type-options':'nosniff','x-frame-options':'DENY','referrer-policy':'no-referrer',
  'content-security-policy':"default-src 'none'; style-src 'unsafe-inline'; base-uri 'none'; form-action 'none'"});
 res.end(html);return true;
}
module.exports={serveLegal};
