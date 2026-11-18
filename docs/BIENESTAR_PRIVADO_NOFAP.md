# NeuroSync — Bienestar personal / NoFap

## Objetivo
Convertir el contador original de NoFap en un módulo de seguimiento voluntario, comprensible y no punitivo. No sugiere que la masturbación sea dañina por sí misma, ni se ofrecen diagnósticos o beneficios clínicos inventados por la abstinencia.

## Cambios
- Se conserva la clave local anterior `ns_private_progress_<usuario>` y el historial de fechas previas (`started`, `history`).
- Indicadores: días actuales, mejor período y cantidad de diarios registrados.
- Diario por fecha: ánimo 1 a 5, situación opcional y nota privada. Un registro diario sustituye al de ese mismo día si el usuario corrige la anotación.
- Ventana de pausa de un minuto con estrategias sencillas: respirar, levantarse, hidratarse, caminar, hablar con alguien o cambiar de actividad.
- Registro opcional de situaciones e intensidad. Estadísticas agregadas de las más frecuentes en los últimos treinta días.
- Mapa de actividad de cuatro semanas: indica días con diario escrito, no una calificación moral.
- Objetivo personal editable, fecha inicial corregible, nuevo comienzo sin perder historial, exportación JSON y eliminación de todos los datos de esa cuenta en ese dispositivo.
- No afecta XP, alimentos, estado ni evoluciones del dragón Azul.
- En la interfaz se indica que el registro es voluntario, privado y no constituye un diagnóstico clínico.

## Privacidad y límites
Los datos se mantienen en `localStorage` por nombre de cuenta y no se añaden al modelo familiar ni a las cargas de sincronización de tareas. No se implementa una llamada de red ni telemetría nueva en el módulo. El almacenamiento del WebView NO es una bóveda cifrada independiente: una persona con acceso al dispositivo, su copia de seguridad o herramientas de depuración podría extraer los datos. La exportación manual crea un archivo JSON sin cifrar: guardar en una ubicación privada. Borrar desde este panel elimina la clave local de la cuenta actual en este dispositivo, pero no puede asegurar la eliminación de exportaciones previas ni de respaldos que la plataforma haya realizado.

## Archivos
- `modules/wellbeing_studio.js` y `modules/wellbeing_studio.css`.
- `index.html`: añade la nueva UI sobre el contador anterior y expone `NeuroWellbeingAPI` local.
- `MainActivity.java`: rutas internas autorizadas para los archivos del módulo.
- `.github/workflows/android-apk.yml`: incluye los assets y la prueba.
- `tests/test_wellbeing_studio.js`: migración de historial, aislamiento entre usuarios y borrado.

## QA
Se verificó sintaxis JavaScript y se probó directamente creación, lectura, aislamiento entre cuentas y eliminación local. La compilación Android y el recorrido visual aún requieren APK, como las demás funciones recientes, porque los trabajos GitHub Actions están fallando antes de ejecutar los pasos. No fusionar en main antes de verificación real.