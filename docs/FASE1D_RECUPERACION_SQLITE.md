# NeuroSync — Fase 1d: segunda copia local cifrada (17 de noviembre de 2026)

## Alcance implementado
- Se **conserva** `localStorage` como origen de datos de la interfaz, con sus copias `_anterior` y `_rescate`. No se ejecuta migración destructiva.
- Una clase Android nueva, `NativeSnapshotStore`, almacena una copia adicional en SQLite privado de la aplicación. Usa **AES-GCM**, clave no exportable generada en AndroidKeyStore y cuenta identificada por SHA-256. Guarda dos revisiones (actual y anterior) en transacción SQLite.
- El WebView crea o actualiza la copia cuando se guarda una versión válida. Al abrir el programa, si `localStorage` es válido, tiene prioridad. Si no lo es, intenta recuperar SQLite y preserva el texto dañado con el sufijo `_corrupto_`; si tampoco se recupera, conserva la lógica anterior de recuperación/bloqueo.
- El puente Java `AndroidBridge` verifica la identidad autenticada antes de leer/escribir un respaldo; no depende únicamente del nombre de usuario recibido desde JavaScript.
- En **Usuario → Respaldo y datos → Comprobar copia en este teléfono** se puede revisar si existe un respaldo recuperable en la instalación.

## Limitaciones y seguridad
- **Copia local no equivale a backup externo.** Borrar los datos de la app, desinstalarla o instalar una variante con otra identidad de paquete puede hacer que esta copia deje de existir. Drive y las exportaciones manuales siguen siendo necesarios.
- Se almacena contenido de tareas, alarmas y mascota, **sin contraseñas ni tokens OAuth**. Se cifra antes de insertarlo en SQLite. Las claves no pueden exportarse a otro teléfono.
- Si SQLite o KeyStore fallan, `localStorage` continúa funcionando; la app no proclama que la segunda copia esté disponible.
- Para proteger rendimiento, antes de publicar deben medirse los tiempos de guardado real en dispositivos con 100, 500 y 2.000 registros: actualmente la llamada cifrada es síncrona.
- No está terminada la migración completa a Room/SQLite como base principal. Esta fase es un puente de recuperación **aditivo**.

## Pruebas realizadas y pendientes
- Prueba directa de las funciones JS con dos cuentas simuladas, corrupción del WebView, recuperación independiente, preservación del registro dañado: completada en entorno aislado.
- Prueba Node `tests/test_phase1d_native_recovery.js`: añadida al proyecto y al workflow.
- **Pendientes:** ejecutar la prueba completa del pipeline, compilar APK, instalar sobre la variante de prueba sin perder los datos actuales, reiniciar con alarma pendiente, comprobar el botón de estado, simular corrupción sin usar datos reales y medir el rendimiento.
- GitHub Actions ha mostrado fallos antes de ejecutar pasos en esta cuenta. No se ha compilado esta fase; mantener `main` intacta hasta lograr compilación y pruebas reales.

## Regresión y reversión
- Para retroceder, volver a la rama anterior `stability/fase1c-sesiones-sincronizacion-protegida`. El almacenamiento WebView anterior sigue sin cambios.
- No eliminar la tabla SQLite desde una migración futura sin respaldo y verificación explícita.
