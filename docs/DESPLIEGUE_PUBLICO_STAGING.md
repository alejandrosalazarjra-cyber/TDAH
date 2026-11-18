# NeuroSync — preparación del despliegue de pruebas (no producción)

**Estado:** archivos de despliegue listos en GitHub. **No se ha creado un servidor, base de datos ni cuenta en Render.** No permitir registro de usuarios reales en este entorno de pruebas.

## 1. Preparación técnica realizada

- `backend/Dockerfile`: ejecuta la API Node 22 como usuario no privilegiado.
- `backend/compose.yaml`: permite probar API + PostgreSQL en un equipo del desarrollador; la base de datos no expone puertos públicos.
- `backend/migrate.js`: aplica el esquema `schema_public_v1.sql` **solo** si se suministra `NEUROSYNC_PUBLIC_DB_CONFIRM=yes` y una URL PostgreSQL.
- `render.staging.yaml`: Blueprint opcional para Render con una web gratuita de prueba y un PostgreSQL privado gratuito, **caduca a los 30 días y no tiene backups**. El auto despliegue está desactivado.
- `.github/workflows/public-backend-check.yml`: pruebas Node aisladas de Android con PostgreSQL temporal y peticiones HTTP reales.
- No se han generado ni guardado contraseñas, tokens o claves privadas.

## 2. Para activar el entorno de pruebas, el desarrollador (no el usuario final)

1. Crear o conectar una cuenta de alojamiento compatible con Node.js + PostgreSQL, por ejemplo [Render](https://render.com/). Conectar el repositorio `elgatojhon8-droid/TDAH` con permisos de lectura y elegir la rama `feature/pulido-contable-estabilidad-2026-11`.
2. Importar el archivo **`render.staging.yaml`** como Blueprint, o configurar manualmente un servicio Docker usando `backend/Dockerfile`. **Revisar las condiciones del plan antes de aceptarlo**. Para almacenar información real escoger una base con backups, retención y políticas de seguridad adecuados, nunca la base gratuita temporal.
3. Crear un proyecto Google Cloud del desarrollador, configurar la pantalla de consentimiento OAuth y obtener un **OAuth Client ID de tipo Web**. La aplicación Android debe registrarse con su paquete y las huellas de firma SHA apropiadas. El cliente Web debe coincidir con el valor `GOOGLE_CLIENT_ID` en el backend.
4. Introducir `GOOGLE_CLIENT_ID` en los secretos/variables del servidor del proveedor, **no** en Git ni mediante un mensaje de chat. La conexión PostgreSQL `DATABASE_URL` debe ir por la red privada del proveedor.
5. Aplicar las migraciones a una base **nueva**, comprobar `GET /health` y realizar pruebas de inscripción con cuentas de prueba; confirmar que otro usuario no puede consultar ni completar tareas ajenas.
6. Configurar en el entorno de compilación Android `NEUROSYNC_API_BASE_URL=https://dominio-real-del-servicio` y `NEUROSYNC_GOOGLE_WEB_CLIENT_ID=...`. Esta configuración viene integrada en la APK; ningún usuario final debe escribir esas variables.
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
