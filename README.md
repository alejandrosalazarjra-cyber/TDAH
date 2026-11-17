# NeuroSync Familia — Android

## Respaldo personal cifrado — actualización de seguridad

La exportación desde la APK ahora pide una contraseña de al menos 12 caracteres (con confirmación) y utiliza AES-256-GCM con sal y nonce aleatorios; la clave se deriva con PBKDF2-HMAC-SHA256 (210.000 iteraciones). El archivo resultante se guarda mediante el selector de documentos de Android, donde puede elegirse Google Drive si está disponible.

- Cada copia nueva incluye solamente tareas, listas y alarmas de la cuenta activa, junto a su mascota y sus plantillas.
- La aplicación solicita la contraseña al restaurar una copia cifrada; las copias JSON anteriores se siguen aceptando con una advertencia y filtrado de registros de la cuenta.
- **No olvides la contraseña**: no se guarda ni hay mecanismo de recuperación.
- **No es sincronización automática ni acceso con Google**: Drive se utiliza por ahora como destino elegido manualmente.
- Antes de reinstalar una APK verifica que puedes abrir la copia; comprueba también que la nueva compilación usa una firma compatible con la instalada.
- La modificación se ha guardado en el repositorio, pero falta validar compilación y restauración en un dispositivo real.

## NeuroSync 10.0 — diagnóstico de instalación y firma real

**Hallazgo verificado:** se descargaron las APK de GitHub Actions #49 y #50. Ambas contenían clases Android y HTML nuevos, pero las firmas eran diferentes:
- APK 8: certificado SHA-256 `f8e0e7f1967fcfe7849c33993b61fc2688524b22bd97c87b6f2e628d460de867`.
- APK 9: certificado SHA-256 `bef4ae4574370e4e591e4690c2428add3d49f1a4f296798cd1db8792f3c121a7`.

Android no permite actualizar una aplicación instalada con el mismo paquete y otra firma. Hasta ahora GitHub generaba una nueva firma de depuración en cada ejecución, por lo que las nuevas funciones podían existir en el APK descargado sin llegar a la instalación anterior.

**Prueba segura de la versión nueva:** GitHub ahora genera `NeuroSync-10-PRUEBA-INDEPENDIENTE`. Se instala al lado de la aplicación existente y muestra como nombre **NeuroSync PRUEBA 10**, con paquete `com.neurosync.familia.preview`. No comparte cuentas, tareas ni datos con la app anterior. Sirve exclusivamente para comprobar visualmente el diseño, las funciones y los permisos sin borrar información.

**Versión visible:** la pantalla de acceso y Mi día muestran la versión obtenida del paquete Android realmente instalado, y Mi día incluye accesos directos a los desafíos de alarma y al progreso privado. El instalador se verifica contra el HTML exacto y contra las clases incluidas en el APK, no solo contra archivos del repositorio.

### Firma estable para actualizaciones definitivas

Una única vez, desde el terminal de un GitHub Codespace autenticado en el repositorio, ejecuta:

```bash
bash scripts/setup-signing.sh
```

El script crea una llave privada de distribución y guarda sus credenciales como secretos cifrados de GitHub. Nunca publica la llave en el repositorio. Descarga **una copia de respaldo privada** de los archivos que el script crea en `signing/` antes de cerrar el Codespace; perderlos impide recuperar la firma.

Después ejecuta manualmente **Compilar APK NeuroSync**. Con los secretos presentes, GitHub publicará `NeuroSync-Familia-Firmada` (`app-release.apk`) con una firma estable. Las siguientes versiones firmadas con la misma llave podrán actualizarse normalmente. **La primera migración desde una APK debug anterior requiere exportar las tareas, desinstalar la APK vieja, instalar la release y restaurar el respaldo**; la nueva firma no puede conservar automáticamente los datos de la app que se desinstala.

No instales las APK de prueba o depuración para uso familiar permanente. Las credenciales de firma son tan importantes como un documento privado de identidad de la aplicación.

## NeuroSync 9.0 — Reloj, desafíos y contador personal

**Reloj de Google:** la opción de música intenta abrir el paquete `com.google.android.deskclock`, después la lista general de alarmas (`ACTION_SHOW_ALARMS`) y, finalmente, la acción para crear alarma. Estas opciones dependen del sistema y del fabricante. **Elegir una canción de YouTube Music requiere la integración dentro de Reloj de Google y una suscripción Premium para canciones concretas**. La alarma con música de Google Clock es distinta de las alarmas nativas de NeuroSync: no es posible combinar automáticamente ambas mediante esa integración.

**Desafíos al sonar:** al crear una alarma de NeuroSync se puede elegir **sin desafío**, **resolver tres sumas** o **tocar doce veces**. Las acciones de notificación no ofrecen detener directamente una alarma con desafío. Siempre se ofrece **Posponer cinco minutos** y una salida de emergencia **Detener sin desafío** con confirmación en la pantalla; el servicio tiene un límite de reproducción para evitar que siga sonando indefinidamente. La pantalla completa depende de los permisos y de las restricciones de Android.

**Contador NoFap:** sección personal para elegir desde qué fecha contar, consultar días transcurridos y registrar nuevos comienzos conservando un historial. El dato se guarda **solo en el WebView del dispositivo para la cuenta activa**, no se envía a `/sync` ni aparece en los respaldos familiares. No pretende diagnosticar, culpabilizar ni presentar la masturbación como un problema médico por sí misma. Se puede utilizar como seguimiento de un objetivo personal voluntario.

## NeuroSync 8.0 — sesión persistente y organización clara

**Inicio de sesión:** NeuroSync recuerda la cuenta seleccionada al cerrar la app, salir al launcher o reiniciar el teléfono. Guarda el token de acceso cifrado mediante **Android Keystore y AES-GCM**, nunca la contraseña. Al abrirla nuevamente intenta restaurar el usuario desde Android, sin solicitar credenciales. Pulsa **Salir de la cuenta** para borrar el token y revocar la sesión local. Cambiar contraseña también invalida la sesión por seguridad.

Para usuarios conectados a Cloudflare, la interfaz puede mostrar la información local sin internet, pero la sesión remota está sujeta a la expiración del servidor (actualmente 7 días). La sincronización y los permisos siguen exigiendo autorización válida del servidor cuando vuelva la conexión. No debe presentarse como sesión de internet sin vencimiento hasta que se implemente un sistema seguro de renovación.

**Organización para reducir carga mental:**
- **Captura rápida:** escribe ideas sin fecha ni obligación inmediata. Quedan en la **Bandeja de entrada** hasta que decidas planificarlas.
- **Solo una cosa ahora:** muestra la actividad prioritaria del día y el siguiente paso concreto, sin la distracción de toda la lista.
- **Tres prioridades:** limita lo que aparece en la pantalla de inicio, aunque las demás tareas permanecen disponibles.
- **Pasos pequeños:** confirma uno por uno y observa cómo progresa la actividad.
- **Temporizador de cinco minutos:** ayuda a empezar con una acción breve; puede detenerse en cualquier momento. No emite una alarma nativa si la aplicación está completamente cerrada.
- **Prioridad y energía:** cada tarea puede etiquetarse por importancia y energía requerida.
- **Edición real de tareas:** cambia nombre, responsable, fecha, hora, repetición y pasos; los recordatorios relacionados se actualizan.
- **Mascota:** sigue evolucionando con las tareas completadas, pero no penaliza los descansos.

Estas herramientas son opciones de organización, no un tratamiento ni un diagnóstico de TDAH.

**Compatibilidad:** los datos previos de tareas, alarmas, listas y mascota se conservan al actualizar sin desinstalar, siempre que Android acepte la firma de la APK. No desinstales una versión con información importante sin exportar antes un respaldo.

## NeuroSync 7.0 — corrección de permisos y diseño compacto

- **Android puede bloquear el permiso especial de escucha de notificaciones** en una APK distribuida por WhatsApp. NeuroSync ya no solicita ese acceso ni registra el servicio correspondiente. En la lista de aplicaciones → tres puntos → «Ajustar sus notificaciones en Android» se abre el panel oficial de notificaciones de la aplicación elegida, sin leer mensajes ni intentar desactivar protecciones del fabricante.
- **Inicio minimalista más compacto:** reloj, fecha, tarjetas y accesos con alturas reducidas, tipografía uniforme sin depender de la fuente personalizada del teléfono y elipsis para nombres largos. Se conservan accesos a teléfono, cámara, búsqueda, agenda, favoritas y concentración.
- **Cuentas más protegidas:** tras cinco intentos de inicio de sesión fallidos se aplica un bloqueo temporal de quince minutos. El administrador debe cambiar la contraseña inicial `admin1234` antes de crear cuentas de otras personas.
- **Verificación del APK:** el flujo muestra el tamaño real de `app-debug.apk` y su SHA-256. El artefacto ZIP de GitHub suele ser más pequeño que la APK sin comprimir.
- **Tamaño y seguridad:** no se agregan archivos basura para llegar artificialmente a 20 MB. La aplicación utiliza el motor WebView instalado en Android, por eso puede tener un APK muy liviano. Los controles de seguridad se verifican por permisos, código, firma, sesiones y pruebas, no por tamaño. La APK actual sigue siendo de pruebas y puede estar firmada con una clave debug diferente entre ejecuciones.

**Limitación:** el filtro automático de notificaciones de otras aplicaciones no está disponible en esta versión; se sustituyó por controles nativos del sistema para evitar el permiso restringido. La sincronización familiar por internet todavía requiere desplegar el servidor Cloudflare de `cloud/`.

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
