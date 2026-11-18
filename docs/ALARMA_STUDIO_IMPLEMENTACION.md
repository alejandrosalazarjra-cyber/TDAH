# NeuroSync — Alarm Studio (17 de noviembre de 2026)

## Objetivo
Implementar un módulo de alarmas profesional con diseño original, tomando como referencias las capturas de Alarmy compartidas en la conversación sin copiar recursos, marcas ni código.

## Seis experiencias
1. **Mis alarmas:** próxima alarma (hora y cuenta atrás), lista de alarmas de cada usuario, activar/desactivar y edición por toque.
2. **Editor:** nombre, fecha/hora nativa, repeticiones (una vez, diaria, semanal, cada 2/3 días, lunes-viernes, sábados-domingos), misión, duración y límite de posposición.
3. **Misiones:** desafíos disponibles realmente soportados por `AlarmRingActivity`: matemáticas, matemáticas avanzadas, memoria, escritura, toques, movimiento, QR físico (mediante registro en editor clásico), dos rutinas anteriores y **nueva rutina de cinco desafíos consecutivos**.
4. **Alarma sonando:** actividad Android nativa, pantalla completa en bloqueo donde Android lo permite, reloj azul y controles grandes, misión y salida segura "Detener sin desafío".
5. **Ajustes:** permisos exactos, pantalla completa y notificaciones; sonido local, enlace a Reloj de Google con música compatible, preferencias de posponer para alarmas futuras.
6. **Informe:** estadísticas basadas solo en eventos nativos `AlarmHistory` (sonó, pospuesta, detenida) y enlace al diario de sueño ya existente; el diario no mide sueño automáticamente.

## Arquitectura
- `modules/alarm_studio.js`: vistas y navegación, sin almacenar cuentas ni reimplementar el motor de alarmas.
- `modules/alarm_studio.css`: estilo oscuro y azul, controles de 44px o más, contraste, navegación accesible.
- `index.html`: API restringida `NeuroAlarmAPI` que usa `model.alarms`, `save()`, `scheduleAlarm()` y permisos originales. No crea una segunda base ni duplica alarmas.
- `android/app/src/main/java/com/neurosync/familia/AlarmOptionsStore.java`: duración y máximo de posposiciones; contador por ID.
- `AlarmReceiver.java`: repetición de días laborales/fines de semana con Calendar; máximo real de posposiciones en el receptor y en la notificación.
- `AlarmRingActivity.java`: visual nativa oscura, gran hora azul y rutina de cinco desafíos; se conserva vía segura.
- Workflow: empaqueta CSS/JS en `android/app/src/main/assets/modules` y ejecuta test de regresión previo a Gradle.

## Límites que NO debemos presentar como implementados
- Fotografía verificada, análisis de objetos con IA, pasos o sentadillas mediante sensores, desinstalación bloqueada, varios retos configurables arbitrariamente, supervisión continua del sueño: son **funciones futuras**. No se simulan con botones que anuncien éxito.
- YouTube Music no ofrece una API pública de reproducción independiente para utilizar canciones como sonido interno de la alarma. El flujo oficial lleva al Reloj de Google; requiere las condiciones de ese servicio y no se controla con retos de NeuroSync.
- El permiso de pantalla completa está sujeto a Android; no se evita la revisión de Google Play ni se interponen pantallas a la fuerza.
- La opción de posponer por defecto se guarda al crear alarmas, pero no modifica retroactivamente las ya existentes.
- El editor clásico se conserva como vía de registro QR hasta que se integre un capturador seguro en el nuevo editor.
- Las pruebas Node de integración y sintaxis no son equivalentes a probar el hardware y el bloqueo de Android.

## QA obligatoria antes de fusionar
1. Ejecutar `node tests/test_alarm_studio.js` y la suite existente.
2. Compilar la APK de preview, confirmar empaquetado de ambos archivos de módulo y revisar ausencia de errores JS.
3. Crear alarma 1 minuto, reiniciar teléfono, probar pantalla bloqueada, audio y vibración; comparar 3 fabricantes si es posible.
4. Probar crear/editar/eliminar, repetición lunes-viernes y fines de semana, zona horaria, desactivación y cierre de sesión.
5. Posposición máxima cero, una y tres; verificar los controles de notificación y el límite en `AlarmRingActivity`.
6. Probar cinco retos secuenciales, opción segura de detener y QR registrado.
7. Validar informes contra eventos y no presentar registros faltantes como tiempos de sueño.
8. Si GitHub Actions falla antes de ejecutar pasos, **no entregar APK ni afirmar compilación exitosa**.
