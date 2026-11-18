# NeuroSync — Pomodoro desde el launcher minimalista

## Qué se corrigió
El antiguo Estudio de concentración ocupaba una tarjeta demasiado grande dentro de Inicio y desplazaba otros elementos. Ahora el bloque visual original permanece disponible para el motor de datos, pero se oculta en Inicio y se accede al Pomodoro mediante una pantalla inmersiva.

## Gestos y navegación
- Deslizar hacia la izquierda desde el inicio minimalista Android abre el Pomodoro a pantalla completa.
- El gesto se reconoce desde el ScrollView con dispatchTouchEvent y umbral horizontal de 85 dp para no interferir con el desplazamiento vertical.
- El acceso normal a aplicaciones y las demás funciones del launcher se conservan.
- También se puede abrir desde el botón de Enfoque en NeuroSync.
- El usuario puede cerrar el Pomodoro sin terminar la sesión: la cuenta regresiva existente continúa y puede reabrirse.

## Tres ambientes distintos
- Calma: lago, luz cálida, colinas y bruma suave con movimiento lento.
- Bosque: vegetación, colinas con siluetas de árboles y neblina; cuando el video local está disponible, se reproduce en bucle y en silencio.
- Noche: cielo azul profundo, luna y estrellas con movimiento sutil.
- Los tres tienen fondo CSS animado que funciona sin internet y opción de sonido ambiental generado localmente mediante WebAudio, inicialmente silenciado.
- El sonido se puede activar o silenciar, se suspende cuando la app está oculta y termina al finalizar una sesión.
- Se respeta prefers-reduced-motion.

## Higgsfield
Se solicitó generación de tres videos ambientales en Higgsfield. El trabajo BOSQUE sí se generó y completó:
- ID: 5295b8d3-4aa6-475a-8b6f-7060243830c8
- Modelo: grok_video_v15_lite
- Archivo original: https://d8j0ntlcm91z4.cloudfront.net/user_3MDNXswrgR6N3gVdMwDFhTyzzP4/hf_20261118_051340_5295b8d3-4aa6-475a-8b6f-7060243830c8.mp4

Los otros dos trabajos no iniciaron porque Higgsfield indicó que el modelo no estaba cubierto por el plan de prueba. No se promete que existan videos de Calma o Noche. Sus fondos son animaciones CSS originales hasta contar con videos adicionales autorizados.
El workflow intenta incluir BOSQUE en assets/focus/bosque.mp4 mediante descarga HTTPS durante el build. Si el archivo no está disponible o falla la descarga, se usa la escena CSS local. La reproducción del video requiere comprobar en dispositivo la carga de WebView y su soporte para streaming desde los assets.

## Persistencia
Los botones de duración, pausa, continuación, finalización, la tarea activa, las estadísticas, las recompensas de Azul y el temporizador reutilizan el estado nsFocusState y las funciones existentes. La interfaz no crea un segundo timer ni genera recompensas falsas.

## Archivos y pruebas
- android/app/src/main/java/com/neurosync/familia/FocusLauncherActivity.java
- index.html: nsPomodoroExpanded, nsFocusFullscreen y NeuroFocusAPI.
- modules/pomodoro_studio.js y modules/pomodoro_studio.css
- android/app/src/main/java/com/neurosync/familia/MainActivity.java: assets de módulos y video local.
- .github/workflows/android-apk.yml: módulos, descarga opcional del video y validación.
- tests/test_pomodoro_studio.js

## Pendiente de comprobar en Android
- El gesto en launcher como pantalla HOME predeterminada.
- La apertura desde pantalla bloqueada no se solicita para Pomodoro (a diferencia de las alarmas).
- Que no se pierda sesión al minimizar, bloquear, reabrir o reiniciar el teléfono.
- Reproducción real del MP4 local en WebView (puede requerir manejo de solicitudes Range).
- Audio, movimiento reducido, duración, salida y superposición de diálogos.
- Compilación Android bloqueada por GitHub Actions que finaliza antes de los pasos. No hay APK verificada de esta rama.