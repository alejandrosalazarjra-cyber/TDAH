# DrakoPlan — Azul: evolución y animación 2D

## Estado actual en `feature/dragon-azul`
Se mantiene el módulo existente, los datos antiguos y los seis umbrales de XP: 0, 80, 200, 420, 750 y 1200. Los cambios no se han fusionado con `main`.

### Identidad visual por evolución
- Seis hojas de animación de 8 poses fueron generadas en Higgsfield, cada una tomando como referencia **el recorte de su etapa concreta** de la lámina aprobada (no el dragón genérico de otra etapa).
- Las 48 poses se compactaron en 6 atlas WebP con transparencia, uno por evolución: `assets/dragon/atlases/etapa-1.webp` hasta `etapa-6.webp`. Cada atlas mide 768×384 px con celdas de 192×192 en una cuadrícula 4×2.
- Los atlas conservan a **Huevo místico como huevo** y cada etapa mantiene su tamaño, apariencia y detalles propios. Las seis imágenes `assets/dragon/etapa-{1..6}.webp` siguen disponibles como respaldo visual.
- Las poses son: reposo, parpadeo, caminar paso A, caminar paso B, alas elevadas, dormir, comer y celebrar. Evolucionar utiliza la pose de alas elevadas y el brillo existente.
- Los atlas se precargan por etapa; no vuelven a descargarse en cada interacción. Las animaciones respetan «reducir movimiento» y se detienen cuando la app no está visible.
- Los accesorios anteriores (estrella y bufanda) siguen funcionando mediante el SVG original cuando están equipados. **Pendiente:** crear versiones de los accesorios compatibles con las hojas ilustradas.
- Los atlas son **animación por fotogramas**, no rig de huesos; la consistencia visual se mejora usando la referencia exacta de cada etapa, pero necesita revisión visual en Android para detectar artefactos entre imágenes generadas por IA.

### Máquina de estados y recompensas
- Estados: `idle`, `tired`, `walk`, `sleep`, `eat`, `play`, `celebrate`, `evolve`, con prioridades, duración, temporizadores y cola limitada.
- Las tareas y sesiones de concentración completadas disparan celebración, manteniendo sus premios existentes. La evolución se activa solo cuando se supera un umbral durante la sesión, no al restaurar una cuenta.
- Jugar consume 15 unidades de energía sin regalar XP, descansar recupera 25; alimentar conserva su comportamiento anterior. No se han agregado recompensas infinitas por repetir animaciones.

### APK Android
- `.github/workflows/android-apk.yml` copia solo los seis atlas y seis imágenes base a la carpeta assets de la APK. Los antiguos ocho WebP independientes de `assets/dragon/poses/` permanecen en GitHub por trazabilidad, pero ya no se incluyen en la APK.
- `MainActivity.java` intercepta únicamente rutas WebP locales admitidas de `/assets/dragon/` en el host interno de WebView. Los gráficos no dependen de servidores externos.

### Comprobaciones
- Sintaxis JavaScript del único bloque `<script>` comprobada.
- Máquina de estados ensayada con pruebas simuladas de prioridades y transición.
- Cambio de fotogramas ensayado para seis etapas: reposo, caminar, sueño, alimentación, celebración, parpadeo, ocultación y accesibilidad.
- Los scripts de CI ejecutan:
  - `python3 -m unittest discover -s tests -p 'test_dragon_module.py' -v`
  - `node tests/test_dragon_motion.js`
  - `node tests/test_dragon_poses.js`
- **Pendiente:** compilar efectivamente la APK en GitHub Actions, probar en celulares físicos, confirmar la identidad visual de las 48 poses y revisar posibles artefactos del recorte.

### Aislamiento
El trabajo se limita a los atlas, la presentación visual y las acciones del dragón, las rutas locales Android, el empaquetado, las pruebas y la documentación. No se modificaron autenticación, alarmas, backups ni sincronización. No se modificó `main`.
