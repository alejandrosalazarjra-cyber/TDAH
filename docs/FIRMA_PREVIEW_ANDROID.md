# NeuroSync Preview — firma estable de pruebas y Google OAuth

**Estado: 18 de noviembre de 2026.** Se generó un certificado de firma para la versión **preview** (no producción). Su cliente Google OAuth Android fue creado en el proyecto `neurosync-515121`.

## Certificado de prueba (datos públicos)

- Application ID: `com.neurosync.familia.preview`
- Alias de la clave: `neurosync_preview`
- Huella SHA-1: `3E:F5:74:B7:F3:8F:09:FB:B4:81:60:BC:40:57:1E:A7:D6:A4:90:63`
- Huella SHA-256: `95:01:F1:FA:E9:BB:A5:B5:FF:87:F0:E5:8B:B0:27:1A:A7:1A:3F:56:D4:C3:C4:D5:69:ED:AC:77:73:57:B4:D0`
- OAuth Android client ID público: `1033210706025-9bjc7dkp34gmsf7v8qq3bchpr3ca6fh8.apps.googleusercontent.com`
- Google OAuth Web client ID (el que usa `.requestIdToken` en Android): `1033210706025-rgmt44l71ho2cuakl1o2qfs5j6ijsjkq.apps.googleusercontent.com`
- Google Auth Platform: EXTERNAL, modo TESTING, sin publicación ni facturación.
- **Nunca** incluir la clave JKS, contraseña, clave en Base64 ni una copia en GitHub, Render, artefactos públicos o la APK.

## Verificación directa en Google Cloud

El 18 de noviembre de 2026 se comprobó a través del navegador conectado de Google Cloud que el cliente OAuth Android `NeuroSync Preview` **ya existe** en el proyecto `neurosync-515121` con el paquete y SHA-1 exactos indicados arriba. No se creó un duplicado y el cliente OAuth Web no se modificó.

El workflow ahora verifica dos etapas: primero la huella del JKS cargado desde los secretos y, al terminar la compilación, la huella del **APK producido** con `apksigner verify --print-certs`. Si son diferentes del cliente OAuth Android, la compilación debe fallar. Sin secretos, la compilación omite el login público.

**Esta verificación no significa que ya se haya compilado o instalado un APK.** La compilación está pendiente de los tres secretos de firma y de contar con un ejecutor de CI disponible.

## Cómo activar firma persistente en GitHub Actions

El propietario debe descargar y proteger los tres archivos de firma de pruebas entregados en la conversación donde se generó la clave:

- `neurosync-preview-oauth.jks` (clave privada).
- `neurosync-preview-firma-privada.txt` (alias y contraseña; archivo privado).
- `neurosync-preview-key-b64.txt` (clave privada codificada en Base64, solo para gestor de secretos).

En GitHub > repositorio `elgatojhon8-droid/TDAH` > **Settings > Secrets and variables > Actions > New repository secret**, crear **tres secretos** exactos:

1. `NS_PREVIEW_SIGNING_KEY_B64`: contenido completo del archivo `neurosync-preview-key-b64.txt`.
2. `NS_PREVIEW_SIGNING_PASSWORD`: contraseña del archivo privado.
3. `NS_PREVIEW_SIGNING_ALIAS`: `neurosync_preview`.

**No enviar estos valores por chat.** Se pueden introducir directamente en GitHub. Los secretos permanecen cifrados en la plataforma de compilación y la clave se descodifica en una carpeta temporal del ejecutor. Los logs nunca deben imprimir la contraseña ni el Base64.

El workflow `.github/workflows/android-apk.yml` valida que la clave tenga exactamente la huella SHA-1 registrada en Google Cloud. Cuando falta uno de los tres secretos, la compilación puede seguir en modo **sin Google público** y conserva el resto de las funciones familiares. Si existe una clave incorrecta, la verificación **falla** antes de compilar. La firma **preview** no reemplaza ni usa la clave `release`.

Codemagic admite los mismos tres secretos como variables protegidas del workflow, según `codemagic.yaml`. La clave se guarda temporalmente y se borra al finalizar.

## Comprobar la huella de un archivo JKS

```bash
keytool -list -v -keystore neurosync-preview-oauth.jks -alias neurosync_preview
```

La consola solicita la contraseña. Verifica que aparezca la huella SHA-1 exactamente igual a la anterior. **No compartas la salida completa si contiene información sensible.**

## Consideraciones imprescindibles

- Sin el mismo JKS/contraseña en la compilación, el binario no tendrá esa firma y **Google Sign-In no funcionará correctamente** para el OAuth Android registrado.
- La clave de preview sirve únicamente para las APK instalables de prueba; **no** debe reutilizarse como firma de lanzamiento en Google Play.
- Google Play App Signing tiene una firma de aplicación propia, cuya SHA-1 debe registrarse como cliente OAuth Android adicional cuando Play Console la muestre.
- Si cambias de clave preview, habrá que actualizar el cliente OAuth Android y potencialmente desinstalar las APK preview anteriores.
- Aún **no existe una APK firmada y probada** con esta clave en un teléfono. El registro de la API en Render sigue bloqueado mediante `PUBLIC_REGISTRATION_ENABLED=false`.
- Para probar dos cuentas, el propietario debe registrar las cuentas Google como test users y habilitar el backend de forma controlada; no hacerlo antes de verificar que el binario tiene la firma correcta.
