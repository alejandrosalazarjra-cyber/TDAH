# NeuroSync — Alarm Studio v1 (17 noviembre 2026)

## Objetivo
Rediseño propio, inspirado en la organización visual de aplicaciones de despertador, sin copiar los recursos ni el contenido de terceros. Conserva el diseño azul de NeuroSync y todas las otras secciones.

## Entregado en esta rama
1. **Mis alarmas:** resumen de la próxima alarma, horas destacadas, lista conectada a los datos existentes y acciones Editar, Duplicar y Eliminar (con confirmación). Se conserva la programación nativa.
2. **Nueva / Editar:** etiqueta, fecha, hora, repetición única, diaria, semanal, cada 2/3 días, o por días concretos de lunes a domingo. Configuración individual de posposición (1, 3, 5, 10 y 15 minutos). Reutiliza `scheduleAlarm()`, `save()` y `AlarmReceiver`; no dice que se ha guardado cuando Android rechaza la programación.
3. **Selector de misiones:** matemática, matemática avanzada, memoria, escritura, toques, agitar, rutinas existentes y QR mediante el editor avanzado que registra el código con la cámara. Permite secuenciar de 2 a 5 retos compatibles (sin QR en combinación) usando `AlarmChallengeStore.validSequence()` y `AlarmRingActivity`.
4. **Pantalla de alarma sonando:** hora grande azul, contraste nocturno, título, progreso, posponer con duración configurada y salida segura siempre disponible; conserva permisos y pantalla de bloqueo.
5. **Ajustes:** reaprovecha controles Android de permisos, prueba de alarma, selección de sonido del teléfono y abrir Reloj de Google para utilizar YouTube Music Premium mediante su integración oficial. La música de YouTube no suena directamente dentro de los retos de NeuroSync.
6. **Informe:** cuenta eventos nativos de los últimos siete días, muestra historial y enlaza al módulo ya existente de sueño, hábitos y concentración.

## Límites expresos
- No se implementó el bloqueo del botón de apagado, desinstalación, DRM ni protección que el sistema no permite.
- No se implementaron todavía misiones de fotografía validada, caminar por GPS/pasos, sentadillas por reconocimiento ni escáner universal de barras; el selector muestra solo misiones nativas disponibles.
- El sonido de la alarma usa el mecanismo nativo existente con tono de respaldo. El control de volumen global o progresivo, la personalización de fondo por alarma y el límite máximo de posposiciones requieren una segunda revisión con pruebas Android reales.
- El informe no inventa duración de sueño: solo utiliza eventos reales y abre Progreso para las anotaciones del usuario.
- No hay certificación de Play Store, ni garantía de apertura en pantalla completa sin permisos autorizados del sistema.
- La rama está pendiente de compilación y prueba física, especialmente permisos, batería, Android 13-16 y recuperación tras reinicio. No fusionar `main` sin QA.

## Compatibilidad y pruebas
- El editor antiguo permanece para registrar códigos QR con la cámara. La nueva interfaz se integra en `index.html` sin migraciones destructivas.
- Se añadieron pruebas de estructura, sintaxis JS, cálculo de próxima alarma, días particulares, secuencias, preferencias de posposición y salidas seguras en `tests/test_alarm_studio.js`.
- Los tests son unitarios/estáticos, no sustituyen pruebas manuales de sensores, audio ni pantalla bloqueada.
