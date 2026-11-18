# NeuroSync — Panel lateral personalizable del launcher minimalista

## Objetivo
Sustituir widgets poco útiles por tarjetas nativas que el usuario pueda elegir, ocultar y ordenar, sin saturar la página principal.

## Gestos
- **Deslizar hacia la izquierda** en Inicio: Pomodoro (se preserva el gesto existente).
- **Deslizar hacia la derecha** en Inicio: nueva página «Mis widgets».
- **Deslizar hacia arriba** en Inicio: listado de aplicaciones.
- También quedan accesos directos desde Inicio, Ajustes y botones de volver. Los gestos horizontales exigen 85 dp y predominio del desplazamiento horizontal para no interferir con scroll vertical.

## Widgets disponibles
1. Tareas: cantidad pendiente, enlace a tareas.
2. Próxima alarma: fecha/hora local conocida, enlace al módulo.
3. Concentración: minutos de la sesión activa, enlace a Pomodoro.
4. Finanzas: saldo e ingresos/gastos del mes, solo después de consentimiento específico en Finanzas; de lo contrario muestra acceso sin montos.
5. Listas para salir: acceso al módulo. Por ahora no presenta recuentos inventados de objetos.
6. Mi progreso: acceso a estadísticas, sin inventar un valor de progreso.

Estos son **paneles internos de NeuroSync**, no widgets arbitrarios de otras apps ni AppWidgetHost. No necesitan privilegios extra ni la capacidad de leer datos de terceros.

## Personalización
- Desde «Mis widgets» → «Editar» o desde Ajustes → «Personalizar mis widgets».
- Activación/desactivación por casilla.
- Reordenar con ↑ y ↓.
- Persistencia mediante SharedPreferences `focus_launcher:widget_order`; la preferencia es del **dispositivo/launcher**, no se sincroniza con las cuentas de la familia.
- Orden inicial: tareas, alarmas, concentración y listas; finanzas y progreso son opt-in.
- Los datos íntimos de NoFap no se muestran en el launcher.

## Privacidad y rendimiento
- El inicio principal ya no presenta el saldo financiero. La tarjeta financiera solo puede verse en el panel lateral si se habilitó previamente en el módulo de Finanzas.
- Los widgets se construyen con vistas Android livianas y solo leen resúmenes locales existentes. No crean servicios persistentes ni descargan datos de internet.
- Los valores pueden depender de que la interfaz principal haya actualizado los resúmenes. La próxima alarma y las tareas podrían estar desactualizadas hasta la próxima sincronización.
- Al cerrar sesión, el puente financiero existente oculta las cifras en las preferencias del launcher.

## Archivos y pruebas
- `FocusLauncherActivity.java`: página, gesto, persistencia de orden, personalización y tarjetas.
- `tests/test_widget_side_panel.js`: comprobaciones estáticas de gestos, datos reales y privacidad.
- `.github/workflows/android-apk.yml`: ejecuta la regresión antes de compilar Android.
- Las verificaciones directas pasaron. Sigue pendiente compilación Android, desplazamiento real en el teléfono, lectura de tarjetas por TalkBack y distintas resoluciones. No afirmar APK instalada o verificada hasta completar esas pruebas.
