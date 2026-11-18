# NeuroSync — Google OAuth para el piloto Android (18 nov. 2026)

## Estado actual
Render (staging) está activo en https://neurosync-public-api-staging-vz3h.onrender.com.
La API necesita un Google OAuth **Web Client ID** real, además de un cliente OAuth **Android** para cada paquete/firma.
El servidor inicia con login público **cerrado**; solo se abre al configurar `PUBLIC_REGISTRATION_ENABLED=true` expresamente después de pruebas.

No crear ni compartir contraseñas de Google ni claves privadas. Un Client ID de OAuth no es un secreto; un Client Secret sí lo es y **no se requiere en el flujo Android de ID token**.

## A. Datos Android que ya conocemos
- Aplicación de desarrollo/producción: `com.neurosync.familia`.
- APK preview instalable junto a la principal: `com.neurosync.familia.preview`.
- Web Client ID: se debe crear en Google Cloud; termina en `.apps.googleusercontent.com`.
- **SHA-1**: depende del certificado con el que se firmó cada APK. No se puede inferir del paquete ni inventar.
- Con Google Play App Signing, registrar también la huella del certificado de firma de Play; puede diferir de la local/de subida.

NeuroSync ya expone un diagnóstico de SHA-1 mediante `AndroidBridge.googleOAuthDiagnostics()`, que lee el certificado de la instalación actual sin revelar claves. Al ocurrir el error Google API 10 muestra los datos técnicos al desarrollador.

## B. Crear clientes OAuth en Google Cloud
1. Entrar en https://console.cloud.google.com/auth/overview con la **cuenta del desarrollador**.
2. Crear/seleccionar proyecto Google Cloud e introducir nombre, información de marca y datos de contacto necesarios para la pantalla de consentimiento. Mantener la app en pruebas mientras corresponda.
3. En **Clients**, crear un **OAuth Client ID tipo Web application**. Copiar únicamente el **Client ID** (NO el secreto).
4. En el mismo proyecto, crear **OAuth Client ID tipo Android** para `com.neurosync.familia.preview` y registrar su SHA-1. Crear otro para `com.neurosync.familia` y la firma correspondiente. Si cambia la firma, registrar otro par paquete/huella o mantener una firma estable.
5. Para Play Store, incorporar la huella SHA-1 del certificado de Play App Signing desde Play Console > App Integrity. No reemplazar ciegamente la huella de pruebas.
6. Si Google exige usuarios de prueba en la configuración OAuth, agregarlos en Google Cloud antes de abrir la APK a terceros.

Fuentes Google: https://codelabs.developers.google.com/sign-in-with-google-android y https://developers.google.com/identity/sign-in/android/backend-auth .

## C. Conectar Render sin exponer claves
1. Abrir https://dashboard.render.com/web/srv-dbv0l7nf3r2c73emtp30
2. En **Environment**, crear `GOOGLE_CLIENT_ID` con el **Web Client ID** exacto, no el Android Client ID.
3. Mantener inicialmente `PUBLIC_REGISTRATION_ENABLED=false` (o sin definir), para no aceptar registros mientras se comprueba la compilación.
4. Guardar y desplegar las variables en Render, comprobar `GET /v1/auth/status`. Debe responder `{"ok":true,"loginEnabled":false}`.
5. Solo cuando dos teléfonos piloto funcionen con el SHA-1 correcto y cuentas de prueba, cambiar `PUBLIC_REGISTRATION_ENABLED=true` y verificar inicio de sesión. No usar aún datos importantes: PostgreSQL Free expira el 18 de diciembre de 2026.

## D. Compilar la APK sin pedir configuraciones al usuario
- GitHub Actions, en **Settings > Secrets and variables > Actions > Variables**, crear `NEUROSYNC_GOOGLE_WEB_CLIENT_ID` con el mismo Web Client ID.
- El workflow `.github/workflows/android-apk.yml` ya incluye `NEUROSYNC_API_BASE_URL=https://neurosync-public-api-staging-vz3h.onrender.com`.
- Codemagic: colocar `NEUROSYNC_GOOGLE_WEB_CLIENT_ID` en las variables de entorno del workflow de la aplicación. La URL de Render se configura automáticamente en `codemagic.yaml`.
- Mantener firma APK estable al probar OAuth; si el APK usa una firma diferente del SHA-1 registrado, Google devolverá un error de configuración.
- La APK sin Client ID continúa en modo familiar, sin ofrecer el acceso público.

## E. Puertas de prueba
1. API y PostgreSQL respondan y conserven esquema correcto (ya observado en logs Render).
2. `GET /v1/auth/status` refleje registro bloqueado hasta que se active intencionadamente.
3. Login Android con dos cuentas de Google de prueba; recuperar/revocar sesión.
4. Buscar @usuario, enviar y aceptar invitación; enviar/aceptar/completar una tarea, y reintento tras pérdida de internet.
5. Desconexión, cambio de cuenta, cierre de sesión y rechazo de acceso no autorizado.
6. Solo después, evaluar notificaciones con FCM, persistencia y backups para lanzamiento público.

**Importante:** la creación del proyecto OAuth es una acción del propietario de Google Cloud. No se ha realizado desde ChatGPT ni existen IDs o huellas verificadas en el código.
