# Servidor público de NeuroSync — prototipo de desarrollo

Este directorio contiene una API Node.js + PostgreSQL **separada** del actual servidor Android/LAN. **No está desplegada ni conectada a la APK**, y no debe exponerse al público hasta cerrar los controles de seguridad y hacer pruebas reales.

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
