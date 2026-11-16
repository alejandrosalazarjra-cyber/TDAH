# NeuroSync Familia — Android

Aplicación de organización personal y familiar, creada en el repositorio **TDAH** sin utilizar Netlify ni publicar en Google Play.

## Qué incluye

- Nueva interfaz móvil original, responsive, sin dependencias visuales de NeuroFlow.
- Pantalla de acceso por usuario y contraseña.
- Administrador: crea cuentas individuales, consulta miembros, asigna responsabilidades, crea tareas recurrentes y pasos pequeños.
- Miembros: visualizan sus tareas, listas y recordatorios, y pueden completar sus actividades.
- Mascota evolutiva y puntos por tareas completadas.
- Calendario, listas para llevar a gimnasio/trabajo, alarmas locales del sistema Android.
- Sincronización privada entre teléfonos mediante el servidor en el teléfono principal; guarda cambios locales cuando no hay conexión.

## Primer acceso

1. Instala la APK en el **celular principal** y abre NeuroSync.
2. Pulsa **Soy el administrador** para activar el servidor del teléfono.
3. Ingresa con usuario `administrador` y contraseña temporal `admin1234`.
4. En **Administración → Seguridad**, **cambia inmediatamente** esa contraseña por una propia y larga (al menos 10 caracteres).
5. Usa **Crear cuenta** para registrar `mama` y `hermano` (o los nombres que elijas) con contraseñas independientes.
6. Crea tareas y asígnalas a cada responsable. Si registras una hora, también se guarda un recordatorio para el dispositivo correspondiente.

**Seguridad:** `admin1234` es solo la clave inicial. El servidor rechaza la conexión remota con esa clave inicial para que no administres la familia sin cambiarla. Las contraseñas se almacenan como derivaciones PBKDF2 con sal, no en texto plano.

## Conectar teléfonos de mamá y hermano

1. Instala la misma APK en los otros teléfonos.
2. Instala y conecta **Tailscale** en los tres teléfonos, dentro de la misma red privada.
3. En tu teléfono principal, abre Tailscale y consulta su IP privada (habitualmente `100.x.x.x`).
4. En el otro teléfono, toca **Vincular mi celular** e introduce esa IP.
5. Entra con el usuario y la contraseña que creaste para esa persona.
6. Usa el icono ↻ para sincronizar. La sincronización también se solicita al volver a abrir la app, al realizar cambios y aproximadamente cada minuto **mientras está abierta**.

No abras el puerto `8765` al internet público. La VPN privada protege la conexión entre teléfonos; el servicio HTTP por sí mismo **no cifra** conexiones LAN sin Tailscale.

## Alarmas

Las alarmas se programan mediante **AlarmManager** y notificaciones del sistema, con repetición diaria opcional. La aplicación también crea recordatorios al asignar una tarea con hora. Concede permiso para notificaciones en Android 13 o superior. Android puede retrasarlas durante el ahorro de batería: no es un reemplazo certificado de una aplicación de despertador de alta precisión.

## Obtener APK para WhatsApp

GitHub → **Actions → Compilar APK NeuroSync** → última ejecución correcta → **Artifacts** → `NeuroSync-Familia-APK` → extraer `app-debug.apk`.

No necesitas Play Store ni Netlify. Es una APK de prueba, no una release definitiva. **IMPORTANTE:** los APK debug producidos por distintos ejecutores de GitHub pueden tener firmas diferentes y no permitir instalar una actualización sobre la anterior. Para esta versión quizá sea necesario desinstalar la anterior, lo que puede eliminar datos locales. Conserva respaldos antes de desinstalar y no elimines el único celular con la base principal sin una estrategia de recuperación.

## Limitaciones importantes

- **Todavía requiere pruebas reales en los tres teléfonos**: no basta con que compile.
- La sincronización ocurre al usar la app y cuando el servidor responde; no puede garantizarse 24/7. En Android 15+, un servicio `dataSync` está sujeto a un límite de horas en segundo plano; tras detenerse necesitarás volver a abrir la aplicación en el celular principal.
- Los cambios simultáneos del **mismo registro** se resuelven por timestamp de dispositivo. Falta reconciliación por campo y gestión avanzada de conflictos.
- Los datos se guardan localmente sin cifrado a nivel de base de datos; no almacenar secretos sensibles ni información médica.
- Aún falta gestión de permisos más detallada, restablecimiento de contraseñas, copia de seguridad central automática y firma estable para actualizaciones.
- La APK **no puede bloquear el desbloqueo de Android**; ello requiere componentes de gestión del dispositivo y permisos específicos.
- No hay infraestructura en Netlify. Para acceso remoto hace falta Tailscale u otra red privada.

## Compilación local

Requiere Java 17, Android SDK 35 y Gradle 8.11.1.

```sh
cp index.html android/app/src/main/assets/index.html
cd android
gradle --no-daemon assembleDebug
```

APK generado: `android/app/build/outputs/apk/debug/app-debug.apk`.
