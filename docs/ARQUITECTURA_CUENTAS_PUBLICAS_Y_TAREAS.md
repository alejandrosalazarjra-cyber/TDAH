# NeuroSync — arquitectura de cuentas públicas y tareas entre usuarios
Estado: especificación de implementación. **No hay todavía backend público desplegado ni registro público funcional.**
Fecha: 2026-11-18

## Decisión de producto

**Los usuarios NO configuran IP, VPN, teléfono servidor, puerto ni dirección HTTPS.**
El uso normal deberá ser:

1. Instalar la APK.
2. Crear cuenta o continuar con Google.
3. Elegir un nombre visible y, opcionalmente, un identificador único `@usuario`.
4. Buscar a una persona por su identificador o compartir una invitación de contacto.
5. Escribir tarea, elegir persona y pulsar «Enviar».
6. Recibir avisos, aceptar/rechazar, completar y consultar estados.

La función administrativa y los modos «host/client/cloud» actuales se conservan **solo para compatibilidad de pruebas** mientras se migra, pero no serán parte del flujo para consumidores finales. Nunca se deben eliminar las funciones existentes por cambiar el transporte.

## Arquitectura objetivo

```text
NeuroSync Android (usuario A)           NeuroSync Android (usuario B)
             |                                      |
     Sesión OAuth / email                 Sesión OAuth / email
             |                                      |
             +---------- HTTPS / TLS ---------------+
                                  |
                     API central de NeuroSync
                       |               |
                 PostgreSQL       cola de eventos
                       |               |
                tarea asignada    push FCM opcional
                                  |
                         Android del usuario B
```

- Una cuenta es global, estable, independiente de un dispositivo y sin relación con un administrador familiar.
- La **API** valida la sesión y **deduce el remitente del token**; jamás confía en un campo `sentBy` aportado por la app.
- El servicio usa una base de datos PostgreSQL y un almacén transaccional de eventos para entregas idempotentes.
- En la APK no hay secretos de servidor, claves privadas, contraseñas de base de datos ni tokens de administración.
- El **dominio HTTPS base** se configura al compilar la aplicación (ej. `NEUROSYNC_API_BASE_URL`) y nunca lo escribe la persona usuaria.
- El inicio de sesión con Google actual solo gestiona autorización personal de Drive: **NO debe tratarse como sesión autorizada ante el backend nuevo**. Para el sistema público se validará en el servidor un **Google ID token** con audiencia, emisor, vencimiento y firma; no se confundirá con un token de acceso a Drive. Alternativamente, se usará un proveedor gestionado de identidad con SDK Android.
- La búsqueda global por nombre completo/correo/teléfono **no se publica** sin controles de privacidad. Buscar por identificador explícito `@usuario` y confirmar invitaciones por defecto.
- Los contactos deben poder bloquear/reportar usuarios; límites de invitaciones y envíos por cuenta para evitar spam.

## Endpoints propuestos (versión 1)

Todos bajo `/v1`; respuestas JSON estructuradas y códigos HTTP correctos:

| Método | Ruta | Contrato |
| --- | --- | --- |
| POST | /auth/exchange | Intercambio seguro de credencial OAuth verificada en servidor por sesión pública |
| GET | /me | Identidad propia y preferencias |
| PATCH | /me | Nombre visible, identificador público y privacidad |
| GET | /users/lookup?handle=... | Solo perfil público mínimo, sujeto a controles antienumeración |
| POST | /contacts/invitations | Enviar invitación a una cuenta existente |
| GET | /contacts/invitations | Invitaciones pendientes de la cuenta |
| POST | /contacts/invitations/{id}/accept | Aceptar invitación |
| GET | /contacts | Contactos autorizados para enviar tareas |
| POST | /tasks | Enviar tarea a un contacto; cabecera `Idempotency-Key` |
| GET | /tasks?box=inbox | Tareas propias recibidas, paginación por cursor |
| GET | /tasks?box=sent | Tareas enviadas propias, solo metadatos y estados autorizados |
| POST | /tasks/{id}/accept | Solo destinatario |
| POST | /tasks/{id}/decline | Solo destinatario |
| POST | /tasks/{id}/complete | Solo destinatario tras aceptar |
| GET | /events?cursor=... | Sincronización incremental de cambios autorizados |
| POST | /devices/push-token | Registrar o revocar dispositivo push de la propia cuenta |

`POST /tasks`:
```json
{
  "recipientId": "uuid-del-contacto",
  "title": "Comprar pan",
  "dueAt": null,
  "repeat": "none",
  "steps": []
}
```
El servidor responde `201` con el ID de tarea y estado `pending`. Reintentar con la misma clave idempotente y el **mismo cuerpo** devuelve el mismo resultado. Reusar la clave con otro cuerpo devuelve `409`.

## Persistencia y privacidad

- Tabla de perfiles: UUID, proveedor de identidad externo, identificador único normalizado, nombre visible, configuración de búsqueda.
- Tabla de contactos: par dirigido, invitaciones, estado, bloqueo y fechas.
- Tabla de tareas: UUID, creador, destinatario, contenido, estado, fechas de creación y actualización, versión.
- Tabla de eventos: secuencia monotónica, tipo de evento, actor, recurso y marca temporal para sincronización.
- Tabla de idempotencia: `(sender_id, request_key)` único, huella del cuerpo, ID resultante.
- Las consultas del servidor deben filtrar estrictamente por destinatario o remitente según la operación, y las comprobaciones de permisos se ejecutan en la base de datos/transacción, no solo en JavaScript.
- Registrar auditoría mínima; nunca almacenar credenciales o datos personales en logs.
- TLS obligatorio, tokens de corta duración y revocación, contraseñas a cargo del proveedor de identidad cuando corresponda.
- Limitación de peticiones, límites de tamaño, protección contra abusos, backups y políticas claras de eliminación de cuenta/datos.

## Experiencia offline

- Crear un envío escribe primero la cola local cifrada o protegida por las capacidades del dispositivo.
- Los reintentos mantienen `Idempotency-Key` estable y no cambian el contenido.
- La API responde éxito solo tras confirmación transaccional en el servidor.
- La cola puede mostrar «Pendiente de conexión», «Enviado», «Aceptado», «Rechazado» y «Completado».
- Al recuperar conectividad, se sincronizan eventos con cursor. Nunca reemplazar todo el modelo a ciegas.
- Una notificación push es **una señal** de que hay novedades, no una fuente autoritativa de tareas: al abrir la app se verifica la sesión y se consulta la API.
- Para Android cerrado: notificaciones push a través de Firebase Cloud Messaging, con token de dispositivo y consentimiento; JobScheduler es fallback oportunista, no transporte inmediato garantizado.
- El teléfono recibe por internet sin servidor local permanente y sin depender de LAN. La red Wi-Fi y los datos móviles usan el **mismo endpoint HTTPS**. Una optimización LAN futura es opcional, no requisito.

## Migración sin romper NeuroSync

1. Mantener `SyncService` + `AccountStore` + datos locales como modo de compatibilidad para pruebas.
2. Crear backend público y sus pruebas; no cambiar ninguna cuenta actual automáticamente.
3. Agregar configuración compilada `NEUROSYNC_API_BASE_URL` sin secreto, y adaptador Android de identidad pública.
4. Distinguir explícitamente entre sesión de Google Drive y sesión verificada del backend.
5. Incorporar agenda de contactos y envío a `@usuario`.
6. Migrar bandeja offline con IDs e idempotencia, conservando datos previos.
7. Integrar FCM y pruebas de entrega en segundo plano.
8. Ejecutar pruebas de seguridad, carga y compatibilidad con las versiones instaladas.
9. Solo después, ocultar menús de IP/VPN/administrador del flujo público. Conservar la opción de migración/recuperación.

## Puertas de lanzamiento (obligatorias)

- Dos instalaciones independientes: registro, búsqueda/invitación, envío, aceptación y finalización.
- Reintentos tras caída de red y respuesta perdida sin duplicados.
- Sesión expirada, cuentas bloqueadas, usuario inexistente, cancelación de invitación.
- No poder leer, enviar, completar ni enumerar tareas de usuarios sin autorización.
- Android con app en primer plano, segundo plano, cerrada y sin notificaciones autorizadas.
- Migración que preserve alarmas, finanzas, mascota, Pomodoro y otros módulos existentes.
- APK firmada, instalada y probada en varios teléfonos; recuperación si backend está temporalmente caído.
- Costos y privacidad de backend documentados antes de habilitar registro abierto.

**Importante:** No se crean ahora direcciones públicas, proyectos cloud, usuarios externos ni tokens reales. Este documento es el contrato técnico para construir y probar esa infraestructura.
