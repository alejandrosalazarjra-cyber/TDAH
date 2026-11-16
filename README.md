# NeuroSync Familia — Android

Aplicación de organización familiar en Android. **No utiliza Netlify ni necesita Google Play.**

## Inicio minimalista opcional de Android (v4.0)

La aplicación incluye un **launcher** nativo con estilo original inspirado en la reducción de distracciones: fondo negro, letras blancas, reloj grande, fecha, resumen de tareas, favoritos, lista completa de apps instaladas y búsqueda. No se instala como una capa de bloqueo ni sustituye el PIN, huella o pantalla de seguridad de Android.

**Activación:**
1. En NeuroSync, abre **Mi día → Abrir y configurar pantalla de inicio**.
2. En el launcher abre **⚙ → Elegir inicio predeterminado**.
3. Android te mostrará el diálogo oficial para elegir NeuroSync como aplicación de inicio. Confirma solo si quieres ese cambio.
4. Para volver al launcher original de OPPO, abre **⚙ → Volver al inicio original** y selecciona el inicio de tu fabricante en aplicaciones predeterminadas.

**Funciones de concentración implementadas:** pausa opcional antes de abrir una app (3, 5 o 10 segundos), horarios sencillos con un aviso previo, favoritos y consulta local de tiempo de uso con permiso voluntario del sistema. Estas funciones no bloquean otras apps a nivel de Android.

**Pendientes:** bloqueo real con límites de tiempo, filtrado de notificaciones, restricción de videos cortos, escala de grises de aplicaciones de terceros, carpetas, gestos y permisos de accesibilidad. No se han activado silenciosamente. El usuario debe poder cancelar o desactivar cualquier restricción; las llamadas, emergencias y ajustes esenciales deben seguir accesibles.

## Funciones disponibles en el código
- Interfaz móvil responsive con tareas, pasos, repeticiones y mascota evolutiva.
- Acceso por cuentas individuales: administrador, miembros familiares.
- Alarmas Android con sonido del sistema, pantalla emergente si Android lo autoriza, detener, posponer cinco minutos y prueba programada.
- Permisos guiados: notificaciones, alarmas exactas y notificaciones de pantalla completa.
- Sonido personalizado seleccionando un archivo de audio local del dispositivo.
- Acceso a Reloj de Google para configurar música de YouTube Music **dentro de Reloj**, no como reproducción directa desde NeuroSync.
- Widget **NeuroSync · Mi día** en el menú de widgets de la pantalla principal.
- Respaldo y restauración desde Administración con el selector de archivos de Android. Los datos se guardan localmente; exporta antes de reinstalar. Los archivos de respaldo no están cifrados: guárdalos en un lugar privado.
- Sincronización familiar: red local / Tailscale opcional o **servidor HTTPS Cloudflare Workers + D1**, preparado en `cloud/`.

## Inicio minimalista propio (versión 4.0)

La APK incluye **NeuroSync Inicio**, un launcher de Android que se puede elegir voluntariamente como pantalla de inicio:

- Reloj y fecha grandes sobre fondo oscuro, tipografía clara y sin iconos en el inicio.
- Aplicaciones instaladas en una lista con búsqueda, abiertas mediante Android.
- Favoritas: mantén pulsada cualquier aplicación para ponerla en inicio.
- Botón para volver a NeuroSync (agenda y tareas pendientes), teléfono y cámara.
- Widget/contador de tareas conectado al almacenamiento local de NeuroSync.
- Pausa consciente opcional por aplicación, con horario de inicio y fin configurable.
- Acceso voluntario a estadísticas de uso del dispositivo para mostrar tiempo aproximado de pantalla.
- Filtro de notificaciones **opt-in por aplicación**, con permiso especial: el servicio solo evalúa el nombre del paquete y descarta avisos seleccionados. No transmite contenido; respeta las notificaciones de llamadas, alarmas y servicios persistentes.

**Cómo activarlo:** abre NeuroSync → **Mi día → Inicio minimalista de Android → Vista previa**. Si te gusta, toca **Usar como inicio predeterminado** y acepta la solicitud del sistema. También puedes entrar a Ajustes → Aplicaciones predeterminadas → Aplicación de inicio.

**Cómo desactivarlo:** Ajustes Android → Aplicaciones predeterminadas → Aplicación de inicio → selecciona tu launcher original (p. ej., el del fabricante). No necesitas desinstalar NeuroSync.

**Alcance y límites:** esta función NO reemplaza la pantalla de bloqueo con PIN/huella, NO bloquea permanentemente aplicaciones ni videos cortos desde otros accesos, NO vuelve el teléfono monocromático a nivel del sistema, y NO activa el filtro de notificaciones sin autorización explícita. Los horarios solo controlan la pausa al abrir aplicaciones desde NeuroSync Inicio. No se usan permisos de accesibilidad ni administración del dispositivo para impedir funciones esenciales.

## Primer uso Android
1. En GitHub → Actions → **Compilar APK NeuroSync**, ejecuta manualmente el flujo y, si se completa, descarga el artefacto APK.
2. Instala el APK permitiendo la instalación desde el administrador de archivos; no requiere Play Store.
3. Si quieres usar el teléfono como servidor privado local: selecciona **Soy el administrador**, ingresa con usuario `administrador` y clave provisional `admin1234`, y **cámbiala inmediatamente**.
4. Si prefieres sincronización entre celulares solo con internet: publica antes el servidor de `cloud/`, usa **Conectar por internet** con la URL HTTPS y accede con el administrador creado al inicializar el servidor en Cloudflare (contraseña fuerte, nunca `admin1234`).
5. Crea cuentas para mamá y hermano desde Administración. En sus teléfonos solo tendrán que instalar el APK y escribir la misma URL del servicio y sus credenciales.
6. En Alarmas, pulsa **Activar permisos** y concede los permisos necesarios. Puede requerir pulsarlo nuevamente para cada permiso especial. Programa una **prueba para dentro de un minuto**.
7. Antes de desinstalar una versión, abre Administración → Respaldo personal → Exportar respaldo.
8. Para el widget: mantén pulsada una zona vacía de la pantalla de inicio → Widgets → NeuroSync · Mi día → Añadir.

## YouTube Music
La integración oficial con música de YouTube Music está disponible dentro de **Reloj de Google** (según disponibilidad y tipo de suscripción); NeuroSync abre Reloj para seleccionar la alarma y su música. Las alarmas propias de NeuroSync no pueden reproducir directamente pistas protegidas por YouTube Music. También puedes elegir un **archivo local** como sonido de NeuroSync.

## Sincronización sin Tailscale
La carpeta `cloud/` contiene un backend HTTPS con cuentas, sesiones, controles de acceso, límites de intentos de login y base D1. **Está preparado pero no desplegado**: requiere una cuenta Cloudflare, crear la base D1, configurar un secreto de arranque y publicar el Worker una única vez. Sigue `cloud/README.md`.

La cuenta del administrador gestiona miembros y tareas. En esta modalidad Cloudflare es la base central; no se depende de que el teléfono del administrador funcione como servidor. Los dispositivos conservan datos localmente y reintentan sincronizar cuando se abre o utiliza la aplicación y mientras está abierta. **No se promete sincronización instantánea con la aplicación completamente cerrada.**

## Limitaciones y seguridad
- La APK v4 debe **compilarse y probarse** en celulares reales; haber guardado código no demuestra que funcionen todas las alarmas ni todos los fabricantes.
- Android puede negar pantallas completas, restringir precisión de alarmas, ahorrar batería o retrasar notificaciones. Hay alternativas cuando los permisos no están disponibles.
- No usa permisos para bloquear el desbloqueo del sistema. Android requiere un modelo especial de administración del dispositivo para ciertas restricciones, que no debe simularse con una pantalla superpuesta.
- Las contraseñas no se almacenan en texto plano en el backend. La contraseña inicial local es temporal.
- Si dos personas editan el mismo registro desconectadas, la sincronización actual utiliza marcas de tiempo y todavía necesita resolución avanzada de conflictos.
- El Widget muestra información después de que la aplicación actualice sus datos. No incluye información privada de otras cuentas.
- Los APK de prueba pueden tener firmas distintas. **No desinstales una versión que contenga datos sin generar un respaldo o migrar la información.**
- No hay CDN o despliegue de Netlify.

## Desarrollo
Requiere Java 17, Gradle 8.11.1 y Android SDK 35.

```bash
cp index.html android/app/src/main/assets/index.html
cd android
gradle assembleDebug
```

APK resultante: `android/app/build/outputs/apk/debug/app-debug.apk`.
