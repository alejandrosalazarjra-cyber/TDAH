# Servidor público de NeuroSync — prototipo de desarrollo

Este directorio contiene una API Node.js + PostgreSQL **separada** del actual servidor Android/LAN. **Está desplegada en Render como entorno de PRUEBAS**, con registro público deshabilitado; la APK no está validada en dispositivos y no debe abrirse al público hasta cerrar los controles de seguridad y hacer pruebas reales.

## Preparación para un desarrollador (no para los usuarios)

Requiere Node.js 20+, PostgreSQL 14+ y un cliente Google OAuth configurado por el equipo desarrollador.

```bash
cd backend
npm install
# Crear la base de datos dedicada; NO usar la base local de la aplicación familiar.
psql "$DATABASE_URL" -v ON_ERROR_STOP=1 -f schema_public_v1.sql
DATABASE_URL='postgresql://...' GOOGLE_CLIENT_ID='cliente-oauth-autorizado.apps.googleusercontent.com' npm start
```

**Nunca** incluir `DATABASE_URL`, secretos de sesión o credenciales administrativas en la APK, Git ni variables accesibles al WebView.

Variables admitidas por el servidor:

- `DATABASE_URL`: conexión PostgreSQL privada, necesaria.
- `GOOGLE_CLIENT_ID`: audiencia permitida para Google ID tokens, necesaria.
- `HOST`: IP donde escucha Node. Por defecto `127.0.0.1`.
- `PORT`: puerto del servicio, por defecto `8081`.

El host predeterminado es local. Para uso externo se requiere un proxy HTTPS con certificado válido y una infraestructura autenticada. El proxy debe limitar tamaños y tráfico; no abrir el puerto Node directamente a internet.

## Integración con la APK (solo para desarrolladores)

Para compilar la APK con acceso público, el equipo debe configurar estas variables en el entorno de compilación:

- `NEUROSYNC_API_BASE_URL`: dominio público de la API, por ejemplo `https://api.ejemplo.invalid`. Solo admite HTTPS y **no es un secreto**.
- `NEUROSYNC_GOOGLE_WEB_CLIENT_ID`: identificador OAuth Web de Google usado como audiencia del ID token (también en `GOOGLE_CLIENT_ID` del backend). **No es una contraseña**.
- Las huellas SHA-1/SHA-256, paquete Android y variante de firma deben registrarse en el proyecto Google correspondiente; en APK `preview` el `applicationId` lleva el sufijo `.preview`.

Si faltan estas variables, **el botón de acceso público permanece oculto** y sigue funcionando la aplicación familiar anterior. El usuario nunca tiene que escribir una IP ni una URL.

El flujo nuevo usa `PublicAccountBridge.java`, una sesión independiente guardada en `PublicSessionVault.java`, y componentes de contactos/tareas de `modules/task_studio.js`. El token de la API se queda en Android Keystore y no llega al WebView. Los envíos pendientes se reintentan desde el modelo local. La edición de perfil admite `POST /v1/me` para compatibilidad con `HttpURLConnection` en Android, además de `PATCH`.

**Limitaciones de la integración actual:** servicio de prueba desplegado en `https://neurosync-public-api-staging-vz3h.onrender.com`, pero todavía no existe una compilación validada en teléfonos. La autenticación de Google necesita configuración real; las notificaciones push y la sincronización incremental del servicio público todavía no están conectadas. Los datos guardados en el modelo local de la app no se migran automáticamente desde Drive o el sistema familiar a la API pública.

## Endpoints implementados en esta etapa

`POST /v1/auth/exchange`: recibe un **Google ID token**, verifica su firma, audiencia, emisor y vencimiento mediante la biblioteca oficial, crea/recupera una cuenta y retorna una sesión opaca de siete días.

`GET /v1/me`, `PATCH /v1/me`, `POST /v1/auth/logout`: identidad, actualización de perfil y revocación.

`GET /v1/users/lookup?handle=...`: búsqueda solo de perfiles `discoverable=true`.

`POST /v1/contacts/invitations`, `GET /v1/contacts/invitations`, `POST /v1/contacts/invitations/{id}/accept`, `GET /v1/contacts`: contactos con invitación y aceptación.

`POST /v1/tasks`: solo entre contactos aceptados no bloqueados, con cabecera `Idempotency-Key`, control transaccional de duplicados y creación de eventos.

`GET /v1/tasks?box=inbox|sent`, `POST /v1/tasks/{id}/accept|decline|complete`, `GET /v1/events?cursor=...`: bandejas, cambios autorizados y novedades.

`GET /health`: conexión con PostgreSQL.

## Limitaciones antes de una prueba pública

- No existe todavía la integración Android para obtener un **Google ID token para este backend**; el inicio de sesión actual con Drive usa permisos diferentes.
- Faltan aplicar límites por IP/usuario, protección de búsqueda de handles, revocación global de sesiones, recuperación de cuentas, eliminación de datos y controles completos contra spam.
- Falta completar `POST /devices/push-token`, FCM y notificaciones reales del servicio público.
- El listado de tareas necesita paginación incremental para crecer más allá de 100 registros por bandeja.
- Faltan invitaciones por enlaces, bloqueo/reportes y aceptación de privacidad en interfaz.
- Deben ejecutarse pruebas de integración con PostgreSQL real, carga, seguridad e idempotencia concurrente.
- Nunca cambiar automáticamente las cuentas o los datos existentes del modo familiar.

El sistema objetivo no pedirá ninguna configuración de red a los usuarios: el desarrollador deberá incluir el dominio HTTPS en la versión distribuida de la APK.

## Pruebas básicas

Desde la raíz del repositorio:

```bash
node --check backend/server.js
node tests/test_public_backend_contract.js
node tests/test_public_backend_api.js
```

Las pruebas de solicitudes utilizan simulaciones de autenticación y base de datos. **No sustituyen** las pruebas de integración con PostgreSQL y Android reales.
