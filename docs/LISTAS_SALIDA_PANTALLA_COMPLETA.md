# NeuroSync — Listas de salida con revisión a pantalla completa

## Funciones añadidas
- Editor por objeto: un campo por elemento, botón Agregar, quitar elementos y sugerencias para Trabajo, Gimnasio, Estudio y Salida diaria.
- Fecha, hora y frecuencia opcionales (única, diaria, semanal, laborables o fines de semana).
- Lista principal con barra de progreso, Editar, Revisar y Eliminar.
- Pantalla inmersiva de revisión manual con casillas independientes y botón Todo listo, salir habilitado solo con todo marcado.
- Pantalla nativa Android DepartureChecklistActivity que abre desde una notificación programada y conserva las marcas.
- Receiver DepartureReminderReceiver que restaura horarios tras reiniciar Android y reinicia las marcas en cada salida recurrente.
- Integración con el modelo original model.packs y sincronización de checks nativos al abrir la app.
- Separación de registros nativos por cuenta autenticada mediante canAccessSnapshot en el puente Android.
- Una salida de emergencia permite cerrar la revisión con confirmación, aunque queden pendientes.

## Archivos
- modules/departure_studio.js / modules/departure_studio.css
- index.html — puente NeuroDepartureAPI
- android/app/src/main/java/com/neurosync/familia/DepartureReminderReceiver.java
- android/app/src/main/java/com/neurosync/familia/DepartureChecklistActivity.java
- android/app/src/main/java/com/neurosync/familia/MainActivity.java y AndroidManifest.xml
- tests/test_departure_studio.js

## Permisos y límites
La pantalla automática sobre la pantalla bloqueada depende de la autorización de pantalla completa, notificaciones, restricciones de batería y del fabricante. Sin permiso de alarma exacta, Android puede demorar el aviso. El usuario puede abrir la lista desde la notificación si no se muestra sola.
Los objetos podrán ser visibles desde la pantalla bloqueada: no incluir información sensible si otras personas pueden ver el teléfono.
En navegador, la revisión manual funciona, pero el horario automático requiere una APK Android que contenga este código. No hay geolocalización ni detección automática de salida.

## Pruebas
Se verificó sintaxis JavaScript y se ejecutaron comprobaciones directas de crear lista programada, marcar objetos, leer marcas guardadas nativamente y bloqueo de acceso a listas de otros usuarios. La suite se incorpora al workflow de GitHub Actions.
La compilación de Java y el funcionamiento real con el teléfono bloqueado están pendientes. No fusionar con main ni prometer APK hasta validar una compilación y pruebas físicas.