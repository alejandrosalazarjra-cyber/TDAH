# Referencias funcionales de DrakoPlan — inventario inicial
Fuentes inspeccionadas: `minimalista.zip` (471 entradas), `Alarmy_26.43.0_1794852718_latestmodapks.com.zip` (5496 entradas), `Todoery-+To+Do+List+&+Planner-1-01-18-0807-gp-modyolo.zip` (754 entradas).

Los archivos son APK compiladas; inventariar recursos no equivale a probar en Android. No copiar código, diseños, audios, binarios ni otros recursos de terceros. Recrear capacidades y estética general mediante implementación original.

## Launcher minimalista
- Pantalla inicial, reloj, búsqueda y favoritos: existente en `FocusLauncherActivity`.
- Foco, Pomodoro, interrupción consciente, retrasos y temporizador: parte implementada, revisar exhaustivamente.
- Modos de interrupción con matemáticas, temporizador y mensajes: detectados en recursos; los tipos deben diseñarse y probarse en DrakoPlan.
- Filtro opcional de notificaciones, widgets de reloj, calendario, gestos, fondos y estadísticas de uso: detectados; comprobar restricciones y políticas de Android. Denegación de permisos no puede bloquear otras funciones.

## Alarmy
- Alarmas exactas, repeticiones, posponer/detener, notificaciones emergentes, sonido y pantalla completa: código parcial existente en `AlarmReceiver` y `AlarmRingActivity`; probar con pantalla encendida/bloqueada/reinicio.
- Misiones detectadas en recursos: matemáticas, sacudir, QR/código de barras, fotografía, reconocimiento visual, escribir frases, memoria, muchos toques, caminar/pasos, sentadillas y rutinas. Separar existentes de pendientes.
- Recursos de seguimiento de sueño y animaciones de éxito/error: evaluar implementación original, consumo energético, privacidad y accesibilidad.

## Todoery
- Plantillas de tareas (ejercicio, lectura, agua, sueño, compras, medicamentos, familia), listas, pasos, recordatorios, calendario, importación/sincronización, widgets, enfoque, rachas y acciones de notificaciones: detectados en recursos; auditar uno por uno contra la aplicación real.

## Matriz obligatoria de pruebas
Para **cada** función registrar: fuente concreta; objetivo del usuario; UI; activador; permisos; ejecución en primer plano y segundo plano; respuesta con pantalla bloqueada; estado sin conexión; persistencia; accesibilidad; prueba ejecutada; capturas y logs; resultado (pendiente / implementado sin probar / verificado / bloqueado por sistema).

Prioridad P1: alarmas y notificaciones. P2: launcher y concentración. P3: tareas, plantillas, widgets y calendario. P4: ventanas y estética consistente. P5: Play Store, permisos y seguridad.

**Precaución**: launcher alternativo y alarmas deben funcionar sin permisos restringidos opcionales cuando sea viable; no se garantiza anular bloqueo del sistema, forzar overlays ni interceptar notificaciones de terceros. El módulo dragón está reservado a la rama de trabajo independiente.
