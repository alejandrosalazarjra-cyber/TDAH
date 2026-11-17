# DrakoPlan — mascota Azul (rama feature/dragon-azul)

## Implementación actual
- Seis etapas de XP: 0, 80, 200, 420, 750 y 1200.
- Seis WebP locales en assets/dragon/etapa-{1..6}.webp, con respaldo SVG si el WebP no carga; la imagen no se vuelve a cargar en cada render si la etapa sigue igual.
- La compilación Android copia los WebP dentro de la APK; MainActivity sirve únicamente esas seis rutas desde WebView. No requieren internet.
- Máquina de estados con prioridades, cola y transiciones temporizadas: idle, tired, walk, sleep, eat, play, celebrate, evolve.
- Ocho ciclos visuales CSS: respiración, caminata, sueño, alimentación, juego, celebración, transformación y brillo mágico. Respetan reducir movimiento.
- Celebración al completar tareas y sesiones de concentración; evolución únicamente cuando sube el nivel durante la sesión. La primera vista de otra cuenta no dispara una falsa evolución.
- Jugar consume 15 de energía sin otorgar XP; descansar recupera 25. Se preservan premios previos de tareas, alimentación y enfoque.

## Límite visual importante
Los WebP de Higgsfield son ilustraciones individuales, no personajes articulados. Los ciclos aplican transformaciones a la imagen completa: las alas, patas, cola y párpados no se animan por separado en los WebP. El SVG de respaldo conserva sus propios movimientos internos. Un rig completo requiere recursos separados para cada articulación o fotogramas dibujados para las seis etapas.

## Verificación
- Sintaxis JS del documento comprobada y prueba funcional de la máquina de estados ejercitada en un entorno simulado.
- Contratos: python3 -m unittest discover -s tests -p 'test_dragon_module.py'
- Prueba dinámica: node tests/test_dragon_motion.js
- El workflow de la APK ejecutará ambas pruebas antes de compilar.
- Pendientes: ejecutar el workflow completo Android, probar en WebView real, medir rendimiento y memoria, revisar el recorte de las seis imágenes, regresiones y accesibilidad.
- Pendiente: integrar hábitos completados reales después de identificar sus eventos existentes.

## Aislamiento
Solo cambian el módulo del dragón, sus WebP, lectura Android de esos archivos, empaquetado, pruebas y documentación. No se modificó main ni las funciones de autenticación, alarmas, copias de seguridad o sincronización.

## Ocho poses Higgsfield de la evolución Cría
- Hoja fuente generada en Higgsfield con créditos del usuario (trabajo 4aa9c2e4-c0cf-472a-ba0b-5469662359e4).
- Recortes de 320 × 320 píxeles WebP RGBA (transparencia), ubicados en assets/dragon/poses: idle, blink, walk-a, walk-b, flap, sleep, eat y celebrate.
- Las ocho poses se usan exclusivamente en la segunda etapa (índice 1, Cría). Las etapas 1 y 3–6 mantienen sus sprites de evolución para evitar perder la progresión.
- En Cría, caminar alterna walk-a/walk-b; en reposo, parpadea periódicamente; las demás acciones seleccionan una imagen distintiva. El cambio de fotogramas se detiene si se oculta la app o está activada la opción de reducir movimiento.
- Si alguna pose falla, se utiliza etapa-2.webp; si falla también, permanece el SVG de respaldo.
- Los ocho recursos se empaquetan como archivos locales en la APK y las rutas Android WebView se limitan a los nombres conocidos.
- Pruebas: node tests/test_dragon_poses.js; python3 -m unittest discover -s tests -p 'test_dragon_module.py'.
- Pendiente ampliar las poses a cada una de las otras cinco evoluciones, validar en un teléfono y refinar la consistencia del dibujo. Los WebP son fotogramas por postura, no un esqueleto separado en partes.
