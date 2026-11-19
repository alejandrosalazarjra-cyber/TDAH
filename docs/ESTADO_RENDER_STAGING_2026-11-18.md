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

- `GET /v1/auth/status` fue añadido para exponer únicamente `loginEnabled`; no devuelve IDs ni secretos. Se comprobó por cliente HTTP externo: `GET /v1/auth/status` → `{"ok":true,"loginEnabled":false}`.
- La creación de usuarios requiere tanto `GOOGLE_CLIENT_ID` como `PUBLIC_REGISTRATION_ENABLED=true`. `GOOGLE_CLIENT_ID` ya se configuró en Render con el cliente Web del proyecto `neurosync-515121`; `PUBLIC_REGISTRATION_ENABLED=false` mantiene cerrado el registro.
- La APK pública utiliza `requestIdToken` con la audiencia Web Client ID, no el token personal de Google Drive.
- Las compilaciones GitHub Actions y Codemagic conocen la URL HTTPS de Render y tienen Web Client ID público predeterminado (sobrescribible mediante `NEUROSYNC_GOOGLE_WEB_CLIENT_ID`).
- El error de firma Google API 10 se identifica y muestra el paquete y SHA-1 al desarrollador, sin revelar credenciales.
- Pasos detallados: `docs/OAUTH_GOOGLE_NEUROSYNC.md`.
- Render verificó la migración y reportó servicio **live** tras este despliegue.
- Google Auth Platform configurado: público EXTERNAL en TESTING, cliente Web `NeuroSync Backend`, y `elgatojhon8@gmail.com` como único test user; no se activó facturación.
- API verificada externamente: `GET /health` → `{"ok":true}`.
- La compilación de APK y el inicio de sesión en dispositivos aún no se han verificado.

## 18 nov. 2026: Certificado preview de Google registrado

- Proyecto Google Cloud `neurosync-515121`, cliente OAuth Android `NeuroSync Preview` creado para `com.neurosync.familia.preview`.
- SHA-1 del certificado firmado exclusivamente para pruebas: `3E:F5:74:B7:F3:8F:09:FB:B4:81:60:BC:40:57:1E:A7:D6:A4:90:63`.
- Cliente OAuth Android público: `1033210706025-9bjc7dkp34gmsf7v8qq3bchpr3ca6fh8.apps.googleusercontent.com`.
- Clave privada y contraseña generadas fuera del repositorio; el usuario debe conservar los archivos de recuperación del certificado y configurar tres GitHub Secrets seguros. No deben guardarse claves privadas en Git ni en Render.
- `android/app/build.gradle` usa `neurosyncPreview` solo con clave propia; GitHub Actions y Codemagic validan el SHA-1 antes de firmar. En ausencia de clave estable, la APK preview oculta el login público.
- Ver `docs/FIRMA_PREVIEW_ANDROID.md` para instalación de secretos y `docs/OAUTH_GOOGLE_NEUROSYNC.md` para Google.
- **No se compiló ni probó una APK Android real en esta etapa.** GitHub Actions falla sin iniciar pasos (runner_id 0, steps 0); se añadieron y verificaron contratos estáticos de seguridad pero no reemplazan pruebas de ejecución.

## Limitaciones obligatorias

**Este despliegue NO debe utilizarse con usuarios reales ni anunciarse como listo para Google Play pública.**

- `GOOGLE_CLIENT_ID` está configurado. El registro sigue cerrado por la variable `PUBLIC_REGISTRATION_ENABLED=false`; `POST /v1/auth/exchange` responde 503 hasta habilitar expresamente los registros.
- Para activar login piloto, obtener SHA-1 de una APK firmada estable, crear el cliente OAuth Android con el paquete correcto, comprobar ID token en Android y habilitar de forma controlada la bandera de registro.
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
