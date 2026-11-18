# NeuroSync Finanzas — primera fase implementada

## Diseño
Nueva sección «Finanzas» con fondo claro, tarjetas azul/blanco, saldo total, ingresos, gastos, análisis por categoría y gráfico comparativo de seis meses. Moneda: COP sin decimales. El modo minimalista muestra un resumen **solo con consentimiento expreso**.

## Operaciones contables
- Cuentas de efectivo y banco iniciales; creación de nuevas cuentas con saldo inicial.
- Ingresos, gastos y transferencias entre cuentas. Las transferencias no inflan ni ingresos ni gastos.
- Libro de movimientos por fecha y categoría.
- Presupuesto mensual simple y porcentaje utilizado.
- Saldo actual de cada cuenta = saldo inicial + ingresos − gastos ± transferencias.
- Todos los importes se validan como enteros seguros y no negativos.
- El libro se guarda por cuenta en la clave local `ns_private_finance_v1_<usuario>` y no se adjunta automáticamente a la sincronización familiar.

## Gráficos
- Barras por categoría del mes seleccionado.
- Comparación de ingresos y gastos de seis meses consecutivos.
- Datos reales de los movimientos registrados, sin valores inventados.

## Resumen del launcher minimalista
- Tarjeta compacta debajo del acceso al Pomodoro.
- Saldo actual, ingresos del mes corriente y gastos del mes corriente.
- Toque abre la ruta `finance`.
- Desactivado por defecto. Activación mediante el control de privacidad del módulo.
- La actualización exige una cuenta autenticada (validación `canAccessSnapshot` en Android).
- Al cerrar sesión se borra la información visual de las preferencias del launcher.
- Las cifras pueden ser visibles para quien tenga acceso a la pantalla inicial del teléfono. No activar si comparte el dispositivo.

## Archivos
- `modules/finance_studio.js`, `modules/finance_studio.css`.
- `index.html`: navegación y puente `NeuroFinanceAPI`.
- `MainActivity.java`: actualización autenticada del resumen y rutas de recursos.
- `FocusLauncherActivity.java`: tarjeta compacta opcional.
- `tests/test_finance_studio.js`: saldos, transferencias y privacidad.
- `.github/workflows/android-apk.yml`: empaquetado y pruebas.

## Límites y siguientes fases
- La primera etapa es manual: **no** importa movimientos automáticamente de bancos, no emite contabilidad oficial ni calcula impuestos.
- No incluye aún amortización de créditos, conciliación de extractos, préstamos ni OCR de recibos.
- Los datos privados se guardan actualmente en almacenamiento WebView y no están protegidos por una bóveda de cifrado independiente. No asumir respaldo automático ni sincronización bancaria.
- Las pruebas directas de JavaScript y lógica contable pasaron con un escenario de dos cuentas: efectivo 160000 COP, banco 720000 COP, saldo 880000 COP, ingresos 500000 COP y gastos 20000 COP, con transferencia interna 80000 COP excluida de flujos.
- Pendiente compilación y prueba Android real; no fusionar `main` sin validar que el launcher y la vista financiera abren correctamente.
