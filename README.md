# NeuroSync Familia · APK Android

Proyecto Android de NeuroSync construido a partir de la interfaz web existente en `index.html`. No requiere publicar en Google Play.

## Construir y enviar el APK

1. Abrir **GitHub → Actions → Compilar APK NeuroSync**.
2. Ejecutar **Run workflow** si no se disparó automáticamente con el último commit.
3. Al finalizar correctamente, abrir el run y descargar el artefacto **NeuroSync-Familia-APK**.
4. Descomprimir el ZIP para encontrar `app-debug.apk`.
5. Enviar **ese APK** como documento por WhatsApp a mamá y hermano. Cada uno tendrá que autorizar la instalación desde WhatsApp o su administrador de archivos. Una APK de depuración es **para pruebas familiares**, no para distribución pública.

Esta compilación incorpora el HTML del repositorio en la APK. No es una simple pestaña que descarga la web privada de GitHub.

## Sincronización con tu celular como fuente principal

Cada celular conserva su copia de tareas y listas. El principal ejecuta un pequeño servicio HTTP local, autenticado, en el puerto `8765`. Para conectarte desde otra red, **instala Tailscale en los tres celulares**, agrégalos a la misma red privada y comprueba que pueden verse. No abras el puerto al internet público.

1. En el celular principal: **Familia → Configurar sincronización → Este es mi celular principal**. La aplicación genera un código de 32 caracteres y arranca el servidor.
2. Abre Tailscale en tu celular y consulta su IP privada **100.x.x.x**. Debe permanecer conectado.
3. En el celular de mamá y hermano: **Familia → Configurar sincronización → Conectar**. Escribe la IP privada del principal y el código.
4. Pulsa **Sincronizar ahora** en cada celular. También se intenta sincronizar al iniciar, al modificar tareas y aproximadamente cada minuto mientras la aplicación está abierta.
5. Cuando no hay conexión, las tareas pueden seguir modificándose localmente; se reintenta al recuperar conexión y abrir la aplicación.

**Limitaciones de esta primera implementación:** la sincronización abarca solo **tareas familiares y listas para salir**, no todos los módulos antiguos de NeuroSync. Los avisos actuales del HTML no equivalen a recordatorios Android garantizados con la app cerrada. No existen aún cuentas independientes con identidad comprobada, cifrado de base de datos ni permisos granulares: el código compartido autoriza a los miembros conectados a ver y editar todos los elementos familiares. Si dos personas cambian el mismo elemento sin conexión, prevalece el cambio cuyo reloj de dispositivo tenga el timestamp más reciente. Los borrados se conservan para evitar la reaparición de elementos. La comunicación a la IP privada está protegida por el túnel Tailscale **solo cuando Tailscale está conectado**; HTTP por sí mismo no cifra los datos en una Wi-Fi privada. Android puede suspender el servidor por ahorro energético o límites de servicios en segundo plano. El servicio no garantiza acceso permanente.

**Seguridad:** no compartas el código familiar públicamente, no expongas el puerto 8765 mediante port-forwarding y no guardes información sensible en esta versión experimental. Exporta respaldos periódicos. Para usarla en producción familiar será necesario probar instalación, sincronización, restauración, permisos y apagado/reinicio en tres dispositivos reales.

## Desarrollo local

Requiere Java 17, Android SDK 35 y Gradle 8.11.1.

```bash
cp index.html android/app/src/main/assets/index.html
cd android
gradle assembleDebug
```

El APK resultante estará en `android/app/build/outputs/apk/debug/app-debug.apk`. Para una APK de distribución a largo plazo, preparar una firma de release propia, mantenerla a salvo y firmar todas las actualizaciones con la misma clave. No subas la clave de firma al repositorio.

## Próximos controles necesarios

- Pruebas de compilación y ejecución en Android (no se han certificado aquí).
- Credenciales individuales y restricciones por cuenta/rol.
- Motor de sincronización con operaciones por campo y conflictos explícitos.
- Notificaciones programadas nativas y tareas de fondo con límites Android.
- Recuperación ante pérdida del celular central y migración a otro servidor.
