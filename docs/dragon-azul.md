# DrakoPlan — Dragón Azul (rama feature/dragon-azul)

## Alcance integrado
La mascota está implementada en `index.html`, que la `MainActivity` carga como interfaz WebView dentro de la APK. La nueva interacción conserva los datos existentes del objeto `model.pet`. La estructura continúa usando los seis umbrales anteriores: 0, 80, 200, 420, 750 y 1200 XP.

En esta entrega incremental se añadieron controles de caminar, jugar, dormir y celebrar; animaciones de cola y alimentación; energía (0–100) y contador de juegos. Los datos nuevos son opcionales para respaldos antiguos. Jugar consume 15 de energía y descansar recupera 25. Alimentar recupera 20. Las recompensas de tareas, foco y misiones existentes siguen siendo las fuentes de XP/monedas; jugar no genera recompensas ilimitadas.

## Pendiente antes del merge
- Recibir y comparar la imagen de referencia del dragón; **no se ha aprobado su aspecto final**.
- Construir y ejecutar la APK en un dispositivo Android real para observar cada animación (incluidos estados de pantalla suspendida y batería).
- Comprobar el renderizado en pantallas pequeñas, WebView Android y modo reducir movimiento.
- Comprobar que tareas, enfoques, copias/restauración y cuentas antiguas conservan su progreso.
- Diseñar e implementar la integración específica con hábitos una vez identificado un evento de hábito completado distinto de la vista de progreso.
- Pruebas de regresión y test de interacción automatizado: los tests estáticos no demuestran animación fluida.

## Prueba estática
`python3 -m unittest discover -s tests -p 'test_dragon_module.py'`

No se modificaron clases Android, autenticación, respaldos, sincronización, alarmas ni Google Drive. No se ha cambiado `main`.
