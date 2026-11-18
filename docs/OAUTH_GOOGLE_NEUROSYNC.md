# NeuroSync — Google OAuth para el piloto Android (18 nov. 2026)

## Estado actual
Render (staging) está activo en https://neurosync-public-api-staging-vz3h.onrender.com.
El proyecto Google Cloud **NeuroSync** (`neurosync-515121`) y el cliente OAuth **Web application** `NeuroSync Backend` ya están creados. Web Client ID público: `1033210706025-rgmt44l71ho2cuakl1o2qfs5j6ijsjkq.apps.googleusercontent.com`. Falta crear los clientes **Android** cuando tengamos las huellas SHA-1 de certificados de firma reales.
El servidor inicia con login público **cerrado**; solo se abre al configurar `PUBLIC_REGISTRATION_ENABLED=true` expresamente después de pruebas.

No crear ni compartir contraseñas de Google ni claves privadas. Un Client ID de OAuth no es un secreto; un Client Secret sí lo es y **no se requiere en el flujo Android de ID token**.

## A. Datos Android que ya conocemos
- Aplicación de desarrollo/producción: `com.neurosync.familia`.
- APK preview instalable junto a la principal: `com.neurosync.familia.preview`.
- Web Client ID: `1033210706025-rgmt44l71ho2cuakl1o2qfs5j6ijsjkq.apps.googleusercontent.com` (identificador público incorporado por defecto a los builds preview).
- **SHA-1**: depende del certificado con el que se firmó cada APK. No se puede inferir del paquete ni inventar.
- Con Google Play App Signing, registrar también la huella del certificado de firma de Play; puede diferir de la local/de subida.

NeuroSync ya expone un diagnóstico de SHA-1 mediante `AndroidBridge.googleOAuthDiagnostics()`, que lee el certificado de la instalación actual sin revelar claves. Al ocurrir el error Google API 10 muestra los datos técnicos al desarrollador.

## B. Crear clientes OAuth en Google Cloud
1. **Hecho (18 nov. 2026):** creado proyecto `neurosync-515121` desde Google Cloud con la cuenta del desarrollador, sin activar facturación.
2. **Hecho:** pantalla de consentimiento de app `NeuroSync`, audiencia EXTERNAL, publicación en estado TESTING. Único test user añadido: `elgatojhon8@gmail.com`.
3. **Hecho:** creado cliente OAuth `NeuroSync Backend` de tipo WEB APPLICATION. Se usa solo el Client ID público, nunca el client secret.
4. En el mismo proyecto, crear **OAuth Client ID tipo Android** para `com.neurosync.familia.preview` y registrar su SHA-1. Crear otro para `com.neurosync.familia` y la firma correspondiente. Si cambia la firma, registrar otro par paquete/huella o mantener una firma estable.
5. Para Play Store, incorporar la huella SHA-1 del certificado de Play App Signing desde Play Console > App Integrity. No reemplazar ciegamente la huella de pruebas.
6. Si Google exige usuarios de prueba en la configuración OAuth, agregarlos en Google Cloud antes de abrir la APK a terceros.

Fuentes Google: https://codelabs.developers.google.com/sign-in-with-google-android y https://developers.google.com/identity/sign-in/android/backend-auth .

## C. Conectar Render sin exponer claves
1. **Hecho:** en Render, servicio https://dashboard.render.com/web/srv-dbv0l7nf3r2c73emtp30, se configuró `GOOGLE_CLIENT_ID` con el Web Client ID real.
2. **Hecho:** `PUBLIC_REGISTRATION_ENABLED=false` permanece establecido para impedir inscripciones hasta comprobar clientes Android y prueba piloto.
3. **Hecho:** Render desplegó las variables y marcó el servicio `live`. Cliente HTTP externo comprobó `GET /health` → `{"ok":true}` y `GET /v1/auth/status` → `{"ok":true,"loginEnabled":false}`.
5. Solo cuando dos teléfonos piloto funcionen con el SHA-1 correcto y cuentas de prueba, cambiar `PUBLIC_REGISTRATION_ENABLED=true` y verificar inicio de sesión. No usar aún datos importantes: PostgreSQL Free expira el 18 de diciembre de 2026.

## D. Compilar la APK sin pedir configuraciones al usuario
- **Hecho:** GitHub Actions tiene el Web Client ID público como valor predeterminado de `NEUROSYNC_GOOGLE_WEB_CLIENT_ID`. Se puede sobrescribir con una variable del repositorio.
- El workflow `.github/workflows/android-apk.yml` ya incluye `NEUROSYNC_API_BASE_URL=https://neurosync-public-api-staging-vz3h.onrender.com`.
- **Hecho:** Codemagic tiene el mismo Web Client ID público como predeterminado; su variable de entorno puede sobrescribirlo. La URL de Render ya se configura automáticamente en `codemagic.yaml`.
- Mantener firma APK estable al probar OAuth; si el APK usa una firma diferente del SHA-1 registrado, Google devolverá un error de configuración.
- La APK sin Client ID continúa en modo familiar, sin ofrecer el acceso público.

## E. Puertas de prueba
1. API y PostgreSQL respondan y conserven esquema correcto (ya observado en logs Render).
2. `GET /v1/auth/status` refleje registro bloqueado hasta que se active intencionadamente.
3. Login Android con dos cuentas de Google de prueba; recuperar/revocar sesión.
4. Buscar @usuario, enviar y aceptar invitación; enviar/aceptar/completar una tarea, y reintento tras pérdida de internet.
5. Desconexión, cambio de cuenta, cierre de sesión y rechazo de acceso no autorizado.
6. Solo después, evaluar notificaciones con FCM, persistencia y backups para lanzamiento público.

**Pendiente:** crear el OAuth Android con la huella SHA-1 de una APK preview firmada de forma estable y verificar dos teléfonos. No hay APK validada ni cuentas públicas habilitadas. No incorporar privados, secretos, ni credenciales de firma al repositorio.
