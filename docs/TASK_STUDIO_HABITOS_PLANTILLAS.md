# NeuroSync · Task Studio · 17 de noviembre de 2026

## Referencias
Cinco capturas aportadas por el usuario: lista de hábitos organizada en áreas, creación rápida de tareas mediante panel, tablero de actividad y enfoque, pantalla con categorías y vista de plantillas con subtareas. Se tomaron como referencias de organización, no se reprodujeron ilustraciones, marcas ni código de terceros.

## Implementación
- **Tareas y hábitos**: panel azul claro, indicadores de completadas y pendientes, pestañas de estado y categorías horizontales.
- **Catálogo de hábitos**: Salud, Vida diaria, Movimiento, Mente y Reducir; los hábitos se agregan como **tareas recurrentes de la cuenta**, usando exactamente `model.tasks`.
- **Captura rápida**: formulario compacto con nombre, categoría y frecuencia, con acceso al editor avanzado existente.
- **Plantillas**: catálogo predeterminado de compras, viaje, trabajo, estudio y sueño. Se crean tareas ordinarias con pasos reales. También se pueden guardar plantillas propias reutilizando `nsTemplateRead`/`nsTemplateWrite`.
- **Informes**: mapa de 12 semanas (84 días), serie semanal de tareas completadas y minutos de Pomodoro reales. No estima valores si no existen registros.
- **Azul**: completar un hábito suma XP y alimento mediante el mismo flujo de guardado; desmarcar un hábito no duplica recompensas.
- **Seguridad**: lectura por usuario y cambios solo en tareas propias, sin nueva base de datos ni peticiones de red.

## Archivos
- `modules/task_studio.js`
- `modules/task_studio.css`
- `index.html` interfaz `NeuroTasksAPI`
- `MainActivity.java` sirve solo rutas de assets específicas
- Workflow empaqueta módulos y ejecuta `tests/test_task_studio.js`

## Límites y pruebas pendientes
- El mapa se basa en fechas registradas de tareas completadas; no es un sistema de medición del tiempo de uso.
- Los hábitos utilizan repetición diaria o semanal; una programación por días específicos requiere extender el motor de recurrencias original.
- La plantilla se guarda mediante el mecanismo local existente; sincronización Google Drive y restauración completa deben probarse en el teléfono.
- No se ha compilado APK de esta rama ni ejecutado la interfaz en Android real: GitHub Actions ha fallado antes de ejecutar los pasos.
- Antes de fusionar: pruebas de desplazamiento en móviles estrechos, accesibilidad, navegación con lectores de pantalla, integración con tareas antiguas, respaldos y recompensas de Azul.
