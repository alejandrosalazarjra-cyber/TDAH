# NeuroSync Familia — Android

## Alarmas y launcher — corrección NeuroSync 6.0

Al crear o probar una alarma, NeuroSync solicita **en el momento de uso** las autorizaciones necesarias: notificaciones, alarma exacta y, en Android 14+, acceso especial a pantalla completa. No se pide autorización de cámara, porque la aplicación abre la cámara instalada mediante una intención de Android y no accede por sí misma al sensor.

Las alarmas ahora disparan un servicio de sonido independiente de la pantalla, con tono del sistema y un tono de reserva, notificación de prioridad alta, acciones Detener y Posponer, y pantalla de control minimalista. La reproducción se detiene al seleccionar cualquiera de esas acciones y tiene límite de seguridad de tres minutos. Las alarmas exactas se anuncian mediante AlarmClockInfo cuando Android concede el permiso.

**Comportamiento esperado de Android:** con el dispositivo desbloqueado puede aparecer un aviso emergente con botones en lugar de ocupar la pantalla entera. Con la pantalla bloqueada y el permiso concedido debe abrirse la pantalla de alarma, pero algunos fabricantes restringen estas funciones. No se puede forzar legalmente el reemplazo de la pantalla de bloqueo protegida.

Para activar NeuroSync Inicio, entra en Mi día → Inicio minimalista → Usar como inicio predeterminado. El sistema mostrará su selector oficial y la aplicación comprobará el resultado. En teléfonos con personalizaciones del fabricante, consulta también Ajustes → Aplicaciones → Aplicaciones predeterminadas → Aplicación de inicio.

**Prueba recomendada:** crea una alarma en un minuto y comprueba primero el sonido con pantalla encendida; repite con pantalla bloqueada, verifica Detener y Posponer. Si no suena, verifica volumen de alarmas, que Android permita las notificaciones y que el fabricante no restrinja actividad de la app en segundo plano.



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

## NeuroSync 5.0 — actualización de concentración y estabilidad

- Sesiones de enfoque de 15, 25, 45 o 60 minutos desde el launcher, con contador de minutos restantes y cancelación voluntaria. Mientras la sesión esté activa, las aplicaciones marcadas para enfoque muestran una pausa consciente al abrirse desde NeuroSync.
- Arreglo para **posponer una alarma sin eliminar su próxima repetición diaria/semanal**. La alarma pospuesta se agenda como evento independiente.
- Prueba de estructura ampliada para detectar ausencia del temporizador y los controles de uso.
- GitHub Actions guarda el artefacto `NeuroSync-Registro-Error` cuando Gradle falla, para diagnosticar el error con evidencia en lugar de repetir compilaciones sin saber la causa.

**Pendiente:** compilación exitosa de la APK 5.0 y pruebas reales de funcionamiento en Android. Los controles de concentración son opcionales y no bloquean la pantalla protegida por PIN/huella ni el acceso a llamadas de emergencia. El bloqueo interno de videos cortos en otras apps y un filtro monocromático de todo el sistema no están implementados: requieren capacidades adicionales que no pueden garantizarse en todos los dispositivos.

## Bienestar digital y concentración — ampliación

Desde **NeuroSync Inicio → ⚙ Ajustes** ahora puedes:

- Configurar una pausa consciente al abrir determinadas aplicaciones: 0, 3, 5, 10 o 20 segundos. La cuenta regresiva puede cancelarse; no afecta a teléfono, emergencias o cámara.
- Configurar un objetivo diario **por aplicación** desde el menú de tres puntos de cada app (10 a 90 minutos o desactivado). Cuando alcance el objetivo y la aplicación se abra desde el launcher, se muestra una advertencia con opción de continuar. Requiere acceso a estadísticas de uso.
- Definir un horario en el que se aplican esas pausas.
- Establecer un objetivo diario orientativo de tiempo de uso (30 a 240 minutos o desactivado).
- Consultar un informe con las 10 aplicaciones de mayor uso del día. Requiere el permiso de estadísticas de uso de Android.
- Recibir un recordatorio al volver al launcher si alcanzaste el objetivo diario, cuando el permiso de uso esté concedido. **No es una notificación garantizada con la aplicación cerrada**, ni un bloqueo del teléfono.
- Abrir el apartado de Accesibilidad de Android para configurar escala de grises/corrección de color, cuando el fabricante la admita.
- Elegir aplicaciones cuyo contenido de notificaciones se filtre con autorización independiente.

Los controles de tiempo son voluntarios y locales al dispositivo. No usan permisos de accesibilidad para vigilar otras aplicaciones ni pretenden bloquear videos cortos dentro de YouTube, Instagram o TikTok. Ese bloqueo requiere capacidades específicas y comprobación de compatibilidad antes de ofrecerlo.

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
