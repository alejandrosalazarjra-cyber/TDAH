# NeuroSync · Alarmas: repetición personalizada y posponer seguro

## Integración
Esta rama continúa `feature/habitos-plantillas-informes-2026-11`, que incluye Alarm Studio, las alarmas nativas Android, hábitos, plantillas y los informes. No sustituye ni borra la implementación existente.

## Repetición por días de la semana
- Nuevo selector visual de siete días en **Mis alarmas → Crear / Editar → Repetición → Elegir días**.
- La programación se codifica como `days:SSSSSSS`, con posiciones de **domingo a sábado**, valores 0 o 1. Ejemplo `days:0101010` significa lunes, miércoles y viernes.
- La máscara debe contener al menos un día activo; no se permite guardar una selección vacía.
- Java Android usa `java.util.Calendar` para avanzar por días locales evitando los desplazamientos por sumas fijas de milisegundos.
- WebView también usa la máscara para el resumen de la próxima alarma.
- Compatible con las opciones anteriores: diaria, semanal, cada dos/tres días, entre semana, fines de semana y única.

## Posposición segura
- `AlarmReceiver.snooze` devuelve un booleano: `true` solamente si Android programó la siguiente alarma y se confirmó su persistencia local.
- El contador de posposiciones, el evento de historial y la detención de audio suceden solo **después** de esos pasos.
- La pantalla de alarma deja de cerrarse automáticamente si una posposición falla; muestra una advertencia.
- Sigue existiendo el mecanismo seguro para detener una alarma sin completar un desafío.

## Seguridad y compatibilidad
- No se solicita acceso a aplicaciones ajenas ni se evade la configuración de Android.
- Los permisos de notificación y alarma exacta continúan siendo administrados por Android.
- El historial y los cambios siguen siendo por usuario, siguiendo el modelo original de NeuroSync.

## QA
- `tests/test_alarm_studio_days.js` verifica selector, máscaras, validación, orden de guardado de la posposición y seguridad de la pantalla.
- Validación directa realizada: sintaxis del módulo web, creación con `days:0101010`, rechazo de `days:0000000`.
- **Pendiente**: compilación Gradle y pruebas Android reales: bloqueo de pantalla, reinicio, cambio de zona horaria y fallos de programación con los permisos denegados.
- **No se afirma APK generada** hasta que GitHub Actions ejecute los pasos y entregue un artefacto verificable.
