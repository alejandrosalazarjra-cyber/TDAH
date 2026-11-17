# NeuroSync — Fase 1e: rendimiento y recuperación al reiniciar

## Objetivo
Mejorar la fluidez al guardar tareas y preservar una copia de recuperación sin reemplazar la estructura actual. Se mantiene la rama 1d como punto de partida; no se modifica `main`.

## Implementado
- El estado completo sigue guardándose **inmediatamente** en `localStorage` con su historial de recuperación, tal como en la Fase 1d.
- Las escrituras secundarias a SQLite cifrado se **agrupan con una espera de 1300 ms**: varios cambios rápidos generan una sola copia final en vez de cifrar y escribir la base con cada edición.
- Se reutiliza la misma cadena JSON serializada para el respaldo secundario, evitando codificar el modelo dos veces.
- Se intenta vaciar el lote pendiente al cerrar sesión, al enviar la app a segundo plano, al ocurrir `pagehide` y cuando se comprueba el respaldo desde el menú del usuario. **No se garantiza que Android invoque estos eventos si el proceso es terminado abruptamente**; el guardado primario en el WebView permanece inmediato.
- Un fallo de SQLite deja pendiente en memoria la última copia; se intenta de nuevo ante otro cambio o solicitud de volcado. No se afirma que haya una copia externa.
- Si se corrompe el estado principal, se comparan las revisiones válidas del historial WebView y SQLite por `_localUpdatedAt` para recuperar la más reciente, sin borrar el texto corrupto.

## Seguridad y límites
- SQLite continúa siendo una **segunda copia cifrada**, no una migración total a Room ni una copia de Google Drive.
- La copia solo está disponible para la identidad autenticada dentro del mismo paquete Android; desinstalar la aplicación o borrar sus datos puede eliminarla.
- El volcado de SQLite sigue siendo síncrono cuando ocurre (capa JavaScript -> Android). Se reduce su frecuencia, pero la optimización completa requiere una cola nativa en segundo plano y pruebas de estrés.
- Sin credenciales OAuth correctamente registradas, Google Drive y el ingreso de Google permanecen sin validación funcional.
- Las compilaciones recientes de GitHub Actions terminan sin pasos; esto impide entregar un APK nuevo verificado por ahora.

## Pruebas
- `tests/test_phase1e_snapshot_batch.js`: 30 cambios rápidos -> una sola escritura SQLite; reintento tras error; rechazo si cambia la cuenta; comprobación de ganchos de segundo plano y cierre.
- `tests/test_phase1e_restart_recovery.js`: recreación simulada del contexto WebView, corrupción de su almacenamiento y recuperación de la copia cifrada por usuario.
- Ambas se integran en el workflow antes del compilado de Android.
- Además se ejecutaron comprobaciones directas aisladas: restauración en contexto nuevo, aislamiento por cuenta, preferencia por historial más reciente y conservación de datos dañados.

## Pruebas pendientes en dispositivos físicos
1. Editar 100/500/2000 tareas y cronometrar guardado y fluidez con una APK compilada.
2. Enviar la app a segundo plano y volver a abrirla; revisar integridad del progreso y alarma próxima.
3. Forzar detención y reiniciar el teléfono, recuperar sesión con usuario local y Google.
4. Denegar permisos de alarma, apagado y reinicio; validar condiciones del fabricante.
5. Confirmar copia cifrada desde «Usuario → Respaldo y datos».
6. Revisar acceso OAuth y Drive mediante certificados y cuentas de prueba oficiales.

## Retroceso seguro
Revertir a `stability/fase1d-sqlite-copia-recuperacion-android` sin eliminar datos existentes. No fusionar esta rama hasta pasar pruebas Android reales.
