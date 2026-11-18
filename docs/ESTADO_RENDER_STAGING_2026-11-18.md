# NeuroSync — estado del servidor público de PRUEBAS
Fecha: 2026-11-18

## Infraestructura creada en Render

- Workspace: My Workspace.
- Blueprint: `exs-dbv0fj3rjlhs73agjrg0` (autoSync desactivado; cambios del Blueprint requieren aprobación).
- Servicio: `neurosync-public-api-staging-vz3h`.
- URL: https://neurosync-public-api-staging-vz3h.onrender.com
- Service ID: `srv-dbv0l7nf3r2c73emtp30`.
- Plan: Free. Estado de deploy en Render: **live** en commit `0820c1532512443ad42f09f0bb8e3c0d8f58884a` (OAuth protegido, 18 nov. 2026).
- PostgreSQL 16: `neurosync-public-db-staging-vz3h`.
- Database ID: `dpg-dbv0ku7f3r2c73emsr20-a`.
- Plan PostgreSQL: Free, IP allowlist vacío, acceso externo deshabilitado.
- Vencimiento de PostgreSQL reportado por Render: 2026-12-18 a las 20:28:41 UTC.

## Verificaciones efectivamente observadas

- Render clonó el repositorio privado de GitHub con éxito.
- Construcción del contenedor Docker completada.
- Registros de servidor: «Esquema público NeuroSync v1 verificado».
- Registros de servidor: «NeuroSync API pública lista detrás de HTTPS».
- Render informó «Your service is live».
- La base PostgreSQL está en estado `available`.
- Se intentó una consulta read-only desde la integración Render, pero la base bloquea conexiones externas y se mantuvo esa protección.
- No se completó una consulta HTTPS independiente desde el entorno del asistente; no presentarla como una prueba superada.

## Progreso posterior: conexión OAuth preparada

- `GET /v1/auth/status` fue añadido para exponer únicamente `loginEnabled`; no devuelve IDs ni secretos. La ruta aún no pudo comprobarse mediante un cliente HTTP externo desde esta sesión.
- La creación de usuarios requiere tanto `GOOGLE_CLIENT_ID` como `PUBLIC_REGISTRATION_ENABLED=true`. En esta fase no se ha configurado ninguno de esos valores; el registro sigue deshabilitado.
- La APK pública utiliza `requestIdToken` con la audiencia Web Client ID, no el token personal de Google Drive.
- Las compilaciones de GitHub Actions y Codemagic conocen la URL HTTPS de Render y aceptan la variable del desarrollador `NEUROSYNC_GOOGLE_WEB_CLIENT_ID`.
- El error de firma Google API 10 se identifica y muestra el paquete y SHA-1 al desarrollador, sin revelar credenciales.
- Pasos detallados: `docs/OAUTH_GOOGLE_NEUROSYNC.md`.
- Render verificó la migración y reportó servicio **live** tras este despliegue.
- La compilación de APK y el inicio de sesión en dispositivos aún no se han verificado.

## Limitaciones obligatorias

**Este despliegue NO debe utilizarse con usuarios reales ni anunciarse como listo para Google Play pública.**

- `GOOGLE_CLIENT_ID` no está configurado. El modo staging permite iniciar el servidor sin OAuth, pero `POST /v1/auth/exchange` responde 503 y no crea cuentas.
- Para activar login público, crear cliente OAuth Web de Google, registrar paquete/huellas Android, configurar `GOOGLE_CLIENT_ID` en Render y `NEUROSYNC_API_BASE_URL` más `NEUROSYNC_GOOGLE_WEB_CLIENT_ID` en la compilación.
- Faltan notificaciones FCM, pruebas reales en dos móviles y pruebas independientes contra PostgreSQL.
- Render Free puede suspender el web service al quedar inactivo.
- PostgreSQL Free expira a los 30 días y no cuenta con respaldo gestionado. No almacenar datos importantes.
- La sincronización del repositorio es manual. Nuevos commits en GitHub no equivalen a despliegues automáticos.

## Referencias internas

- `backend/server.js`: backend Node.
- `backend/migrate.js`: migración segura.
- `backend/schema_public_v1.sql`: tablas públicas.
- `backend/start.sh`: migración e inicio del servicio.
- `render.staging.yaml`: configuración declarativa.
- `tests/test_public_backend_database.js`: prueba de integración PostgreSQL para ejecutar en un entorno controlado.

## Próxima puerta de habilitación

1. Configurar OAuth con credenciales reales del proyecto del desarrollador.
2. Verificar login y emisión/revocación de sesión con dos cuentas de prueba.
3. Probar invitaciones, tareas, idempotencia y permisos usando dispositivos distintos.
4. Implementar las notificaciones de Android con app cerrada y monitoreo.
5. Elegir almacenamiento persistente con respaldo antes del lanzamiento abierto.
