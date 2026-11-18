# NeuroSync — Pulido contable y estabilidad

## Objetivo
Corregir una limitación importante en Finanzas: antes se podían crear movimientos pero no anular asientos erróneos. En contabilidad personal no debe borrarse el rastro de una corrección.

## Cambios
- Cada registro activo tiene una acción **Anular**. La aplicación pide confirmación en una ventana propia y conserva la fecha de anulación mediante `voidedAt`.
- El movimiento anulado sigue visible en el historial, pero deja de contar en saldos, ingresos, gastos, porcentajes de presupuesto y gráficos.
- Las transferencias internas continúan sin contarse como ingresos ni gastos, incluso cuando se anulan.
- Se añadió **Exportar respaldo JSON** para disponer de una copia manual del libro por cuenta. El archivo no está cifrado; debe guardarse en una ubicación privada.
- Se añadió información visual para identificar movimientos anulados y se respetaron los estilos azul/blanco y los controles táctiles del resto del módulo.
- No se modificaron los datos de trabajadores, contraseñas, miembros de la familia ni otras secciones.

## Comprobación directa
Se probó un libro ficticio con dos cuentas y 3 registros (ingreso de COP 500.000, gasto COP 20.000 y transferencia interna COP 80.000).

| Indicador | Antes de anular | Después de anular el gasto |
|---|---:|---:|
| Saldo total | 880.000 | 900.000 |
| Ingresos noviembre | 500.000 | 500.000 |
| Gastos noviembre | 20.000 | 0 |
| Presupuesto restante | 130.000 | 150.000 |
| Registros en historial | 3 | 3 |

Sintaxis JavaScript validada. Pruebas de regresión ampliadas en `tests/test_finance_studio.js`.

## Estado
Los cambios se encuentran en la rama `feature/pulido-contable-estabilidad-2026-11`, descendiente de la rama de compilación Codemagic y del panel de widgets. **No están fusionados con main**.

La compilación Android y los ensayos con usuarios/dispositivos siguen pendientes. No es una APK ya probada. La exportación usa la descarga web estándar y requiere verificación especial en Android WebView antes de depender de ella como único respaldo.
