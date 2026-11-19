# NeuroSync — preparación del despliegue de pruebas (no producción)

**Estado (18 nov. 2026):** desplegado en Render como entorno **exclusivamente de pruebas**. API: https://neurosync-public-api-staging-vz3h.onrender.com ; PostgreSQL 16 privado de Render en plan Free. `GET /health` respondió `{"ok":true}` y `GET /v1/auth/status` respondió `{"ok":true,"loginEnabled":false}`. **El registro sigue deshabilitado** y no deben almacenarse datos reales. La base expira el 18 de diciembre de 2026.

## 1. Preparación técnica realizada

- `backend/Dockerfile`: ejecuta la API Node 22 como usuario no privilegiado.
- `backend/compose.yaml`: permite probar API + PostgreSQL en un equipo del desarrollador; la base de datos no expone puertos públicos.
- `backend/migrate.js`: aplica el esquema `schema_public_v1.sql` **solo** si se suministra `NEUROSYNC_PUBLIC_DB_CONFIRM=yes` y una URL PostgreSQL.
- `render.staging.yaml`: Blueprint opcional para Render con una web gratuita de prueba y un PostgreSQL privado gratuito, **caduca a los 30 días y no tiene backups**. El auto despliegue está desactivado.
- `.github/workflows/public-backend-check.yml`: pruebas Node aisladas de Android con PostgreSQL temporal y peticiones HTTP reales.
- Las credenciales de Render y las claves privadas de firma **no** se guardan en Git. La clave preview se genera por separado para el propietario, no se incluye en el repositorio.

## 2. Activación de pruebas e integración Android (desarrollador, no usuario final)

1. **Hecho:** Render conectado al repositorio privado `elgatojhon8-droid/TDAH`, rama `feature/pulido-contable-estabilidad-2026-11`.
2. **Hecho:** Blueprint `render.staging.yaml` aplicado a un servicio Free y un PostgreSQL Free; autodespliegue desactivado. Para información real será imprescindible una base permanente con respaldos.
3. **Hecho:** proyecto Google Cloud `neurosync-515121`, consentimiento EXTERNAL/TESTING y cliente OAuth Web `NeuroSync Backend`. Cliente Android preview: comprobar el estado en `docs/OAUTH_GOOGLE_NEUROSYNC.md`.
4. **Hecho:** `GOOGLE_CLIENT_ID` configurado en Render. `PUBLIC_REGISTRATION_ENABLED=false` y conexión PostgreSQL privada. El ID OAuth es público, pero las credenciales del servidor y las claves de firma siguen siendo privadas.
5. **Hecho:** migración aplicada y ruta `/health` verificada externamente. **Pendiente:** pruebas reales de autenticación, permisos y tareas desde dos dispositivos.
6. **Hecho:** URL pública de staging y Google Web Client ID integrados en las compilaciones de prueba; falta entregar firma preview a un gestor seguro de secretos de compilación.
7. Compilar `assemblePreview` y verificar las tareas recibidas y enviadas desde dos móviles reales, por Wi-Fi y por datos móviles. Hacer pruebas desconectado, cambio de cuenta y token expirado.

## 3. Limitaciones y seguridad

- No publicar `backend:8081` directamente; utilizar un proxy HTTPS administrado por el proveedor.
- La primera versión incluye límites **en memoria por proceso**, útiles como protección inicial; antes de abrir registros públicos hacen falta límites distribuidos, protección antispam, bloqueo/reportes de usuarios, supervisión, copias de seguridad y política de eliminación de datos.
- La API pública puede ejecutarse sin Netlify ni sin tener un teléfono permanente como servidor. El mantenimiento y disponibilidad dependen del servicio de alojamiento.
- El servidor gratuito de Render puede dormirse durante inactividad; el PostgreSQL gratuito expira 30 días tras crearse, con un periodo adicional limitado antes de su eliminación definitiva. Por eso es **exclusivamente** para pruebas desechables.
- La conexión Android se activa solo si las variables de compilación están correctamente configuradas; la versión familiar de pruebas sigue funcionando aparte.
- Las notificaciones push del servicio público, recuperación de cuenta y paginación incremental todavía requieren desarrollo y pruebas.

## 4. Comprobaciones antes de cualquier publicación

```bash
node --check backend/server.js
node --check backend/migrate.js
node tests/test_public_backend_contract.js
node tests/test_public_backend_api.js
```

Con PostgreSQL desechable `neurosync_tests`:

```bash
NEUROSYNC_PUBLIC_DB_CONFIRM=yes DATABASE_URL="postgresql://..." node backend/migrate.js
DATABASE_URL="postgresql://.../neurosync_tests" node tests/test_public_backend_database.js
```

Si una prueba falla, no activar el servidor para usuarios reales. Las instrucciones del proveedor pueden cambiar, y cualquier gasto o creación de recursos externos exige una decisión explícita del propietario.
