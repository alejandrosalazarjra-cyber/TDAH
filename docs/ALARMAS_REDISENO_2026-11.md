# NeuroSync — Rediseño de Alarmas, implementación incremental

## Objetivo
Aplicar la claridad visual de alarmas de referencia suministradas por el usuario sin copiar su identidad, recursos protegidos o el comportamiento de otras aplicaciones. Se conservan la estética azul de NeuroSync y los servicios Android existentes.

## Funciones implementadas en la rama
1. **Mis alarmas:** próxima hora destacada, cuenta regresiva, alarmas ordenadas por próxima ocurrencia, lista de activación, edición, duplicación y eliminación con confirmación.
2. **Editor:** nombre, fecha, hora (selector Android nativo de `input type=time`), repetición única, diaria, semanal, cada 2/3 días o **días concretos L-M-X-J-V-S-D**. Misiones soportadas: matemáticas, memoria, escribir una frase, toques, movimiento con acelerómetro, QR y dos rutinas combinadas.
3. **Opciones:** posponer durante 1, 3, 5, 10 o 15 minutos; límite de 0, 1, 2, 3 o 5 veces. Configuración guardada por alarma en `AlarmOptionsStore`. Al detener una alarma se reinicia su contador.
4. **Pantalla sonando:** se reutiliza `AlarmRingActivity` con estética nocturna y hora azul grande, manteniendo desafíos existentes, salida de seguridad y apertura sobre bloqueo cuando Android lo permite.
5. **Ajustes:** pestaña propia para permisos, notificaciones, tonos locales y prueba rápida.
6. **Informe:** datos reales de `AlarmHistory` en los últimos 7 días (sonó, detenida, pospuesta), sin inventar estadísticas de sueño. Acceso al diario de sueño ya existente.
7. **Portada:** vista compacta de próximas alarmas, que enlaza al editor sin repetir acciones avanzadas.

## Reglas de seguridad
- La alarma no puede impedir apagar/desinstalar el teléfono mediante técnicas de evasión de Android. Se preserva la opción de detener sin desafío con confirmación.
- Solo se muestran misiones implementadas. No presentar caminar, sentadillas, reconocimiento de objetos en foto o combinaciones arbitrarias de 5 misiones como funcionales.
- No se solicitarán permisos peligrosos adicionales para lograr el diseño. Los permisos de pantalla completa siguen bajo control de Android.
- Al pausar una alarma no se borra el reto ni el QR. Al eliminarla sí se limpian.

## Compatibilidad
- Los registros antiguos sin `enabled` se consideran activos.
- Los registros antiguos sin `repeat` conservan compatibilidad con `daily`.
- Los registros antiguos sin opciones de posposición usan 5 min y hasta 3 posposiciones.
- La repetición `days:1,3,5` significa lunes, miércoles y viernes según calendario local de Android.
- El almacenamiento y recuperaciones de fases 1a–1e continúan intactos.

## Verificación
- El script JavaScript de `index.html` se comprobó sintácticamente y se ejecutaron pruebas directas de fechas y repeticiones por días.
- Prueba añadida al repositorio: `node tests/test_alarm_ui_redesign.js`, integrada en el pipeline de GitHub Actions.
- **Pendientes:** compilación APK, prueba táctil del editor en celular, pruebas de alarmas en pantalla bloqueada, reinicio y permisos denegados, verificación de audio/posposición máxima, y Google OAuth.
- Si GitHub Actions sigue terminando sin pasos ejecutados, NO afirmar que hay un APK nuevo listo. La versión anterior continúa como alternativa de pruebas.
- Rama de trabajo separada de `main`; no fusionar hasta validar.

## Pruebas físicas mínimas
1. Crear alarma para mañana con misión matemática, bloquear pantalla y confirmar apertura y sonido.
2. Seleccionar lunes/miércoles/viernes, verificar próxima fecha y persistencia después de reiniciar.
3. Elegir límite de una posposición, confirmar que la segunda no silencie la alarma.
4. Pausar/reactivar una alarma QR y confirmar que el código se conserva.
5. Editar y duplicar alarmas, y comprobar permisos de cuentas.
6. Abrir Informe y confirmar que solo aparecen eventos reales, no horas de sueño inventadas.
7. Seleccionar un tono local y comprobar reproducción, vibración y sonido de respaldo.
