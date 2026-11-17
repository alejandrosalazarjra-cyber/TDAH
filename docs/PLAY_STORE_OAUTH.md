# NeuroSync — preparación para Google Play y OAuth

## Estado (2026-11-17)
Esta rama es una versión de prueba; compilar no demuestra que Google la haya aprobado ni que los permisos funcionen en todos los dispositivos.

## Corrección necesaria del inicio de sesión
Google Sign-In necesita un cliente OAuth **Android** con el mismo paquete y SHA-1 del certificado que firma el APK:
- Pruebas: `com.neurosync.familia.preview`.
- Distribución: `com.neurosync.familia`.
- Play App Signing puede generar un **SHA-1 distinto** del certificado de carga.
- En el login, si aparece error 10, se muestran el paquete y SHA-1 de la APK instalada en el apartado «Detalles técnicos para configurar Google».
- En Google Cloud Console → Google Auth Platform / Clients, registrar el cliente Android para cada paquete/certificado y la pantalla de consentimiento; habilitar Drive API y configurar el alcance `drive.appdata`, sujeto a las normas aplicables.
- No colocar la clave privada del certificado ni secretos OAuth en `index.html` ni en Git.
- El workflow admite `NS_SIGNING_KEY_B64`, `NS_SIGNING_PASSWORD` y `NS_SIGNING_ALIAS` en **GitHub Secrets**; cuando existen la APK de prueba usa también firma persistente.
- Sin esos secrets, GitHub Actions usa certificado de debug generado en CI; su SHA-1 puede cambiar entre compilaciones, por lo que no es adecuado para una prueba OAuth repetible.

## Sonido de alarmas
Las alarmas de NeuroSync admiten tonos del sistema y archivos locales de audio con autorización del usuario mediante `ACTION_OPEN_DOCUMENT`, permiso de lectura persistente y reproducción con `USAGE_ALARM`. El sonido predeterminado dispone de respaldo para los casos en los que no se puede leer un archivo seleccionado.

## Comprobación previa a Play Store (pendiente)
1. Publicar como AAB con firma de carga estable y habilitar Play App Signing.
2. Registrar en OAuth el certificado SHA-1 de Play App Signing y paquete definitivo.
3. Probar el inicio de sesión, revocación de consentimiento y restauración de Drive con al menos dos cuentas Google distintas.
4. Declarar el uso de `USE_FULL_SCREEN_INTENT` solo para **alarmas reales**, preparar evidencia y mostrar la solicitud controlada por Android.
5. Evaluar y declarar `SCHEDULE_EXACT_ALARM`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, `SYSTEM_ALERT_WINDOW` y acceso a estadísticas de uso según función y necesidad. No pedir accesos invasivos sin motivo.
6. Auditar `android:usesCleartextTraffic="true"`: restringir el tráfico a loopback o implementar un Network Security Config más limitado antes de distribución pública.
7. Eliminar cuentas de demostración y contraseñas administrativas conocidas antes de la publicación pública; migrar credenciales existentes con consentimiento.
8. Publicar política de privacidad real, descripción de uso de Drive y datos, formulario Data Safety y canal para borrar información.
9. Pruebas de pantalla bloqueada/desbloqueada, reinicio, fabricante, modo ahorro, pérdida de internet, suspensión de la app, audio y permisos denegados.
10. Revisar derechos del icono, diseños, assets y dependencias de terceros; ejecutar pruebas reales con un AAB de testing interno de Play.

Fuentes oficiales:
- https://developers.google.com/android/guides/client-auth
- https://developers.google.com/identity/siwg/best-practices
- https://support.google.com/googleplay/android-developer/answer/16558241

**No afirmar aprobación de Google Play** sin pasar su revisión de aplicación y permisos.
