# NeuroSync — Auditoría y preparación de lanzamiento
**Fecha:** 18 de noviembre de 2026
**Repositorio:** `elgatojhon8-droid/TDAH`
**Rama auditada:** `feature/pulido-contable-estabilidad-2026-11`
**Estado:** DESARROLLO / PILOTO INTERNO. **NO APTO AÚN PARA PUBLICACIÓN GENERAL.**

## 1. Verificaciones reales realizadas

| Elemento | Resultado | Evidencia |
| --- | --- | --- |
| API Node/Render HTTPS | Activa (Live) | https://neurosync-public-api-staging-vz3h.onrender.com/health devuelve `{"ok":true}` |
| Registro público | Cerrado | `/v1/auth/status` devuelve `loginEnabled:false` |
| Base PostgreSQL 16 | Provisionada y migrada | Registros Render, migrador informa esquema verificado |
| Google Cloud | Proyecto OAuth `neurosync-515121` | Web Client ID configurado en Render, cliente Android Preview con SHA-1 verificado en Google |
| Política de privacidad | Servida por HTTPS | https://neurosync-public-api-staging-vz3h.onrender.com/privacy |
| Ruta web de eliminación de cuenta | Servida por HTTPS | https://neurosync-public-api-staging-vz3h.onrender.com/delete-account |
| Sintaxis JS de interfaz y API | Comprobada | Verificación estática de scripts |
| APK instalada en Android | **NO PROBADA** | Falta compilación ejecutada y prueba física |
| AAB firmado/validado para Play | **NO PROBADO** | Workflow preparado, no existe artefacto validado |

## 2. Problemas hallados y correcciones aplicadas

1. **Contrato desactualizado:** prueba de backend afirmaba que no existía despliegue público; se actualizó a staging desplegado pero registro cerrado.
2. **Protección entre cuentas:** bloqueo/desbloqueo, lista de bloqueos y denegación de envíos e invitaciones a bloqueados.
3. **Invitaciones:** aceptar y rechazar sin añadir contactos automáticamente.
4. **Tareas recibidas:** aceptar, rechazar y completar; el estado exige transición autorizada.
5. **Datos del envío:** antes el puente Android mandaba solo el título. Ahora envía JSON con título, repetición, subtareas, categoría, fecha y hora; API guarda y valida esos datos.
6. **Identidad visible:** las bandejas muestran nombre/alias del remitente y del destinatario, no solamente UUID.
7. **Privacidad:** endpoint de borrado autenticado con confirmación; revoca sesiones, elimina las tareas compartidas relacionadas y solicitudes de contacto; la APK intenta limpiar almacenamiento WebView de esa cuenta y las copias cifradas SQLite.
8. **Notificaciones de respaldo:** JobScheduler consulta solo eventos del usuario autenticado con cursor y revalidación de su sesión. Puede tardar al menos 15 minutos y NO sustituye FCM.
9. **Google Play:** compileSdk y targetSdk ajustados a Android 36; AGP a 8.10.1 y Gradle compatible para las pipelines. Se agregó compilación condicional de AAB release y verificación de artefacto firmado.
10. **Información pública:** páginas de privacidad y eliminación disponibles por HTTPS, con enlace desde la app. Contenido inicial sujeto a revisión jurídica y actualización antes de lanzamiento.

## 3. Bloqueos P0 antes de pruebas de usuarios

- [ ] **Compilar realmente APK Preview y descargarla:** GitHub Actions sigue fallando antes de ejecutar pasos; los últimos jobs no tienen registros accesibles. Se preparó alternativa Codemagic, pero no existe una sesión de Codemagic conectada y no se verificaron sus builds.
- [ ] **Instalar firma Preview estable como secreto del CI:** `NS_PREVIEW_SIGNING_KEY_B64`, `NS_PREVIEW_SIGNING_PASSWORD`, `NS_PREVIEW_SIGNING_ALIAS`; no colocar archivos privados en Git, logs, Render ni chat.
- [ ] **Ejecutar integración backend real con PostgreSQL desechable:** incluye invitaciones, bloqueos, idempotencia, estados y borrado. Solo se verificó sintaxis/contratos estáticos, no toda la ejecución transaccional.
- [ ] **Habilitar temporalmente OAuth únicamente en piloto autorizado:** actualmente `PUBLIC_REGISTRATION_ENABLED=false`; Google OAuth TESTING tiene solo un correo autorizado. Para prueba con 2 teléfonos se necesita al menos otra cuenta de prueba aprobada por el propietario.
- [ ] **Pruebas físicas Android 16 y otra versión**: permisos de alarma exacta, notificaciones, full-screen intents, permisos especiales, launcher minimalista, persistencia tras reinicio, multitarea y baja conectividad.
- [ ] **Regresión integral de TODOS los módulos:** alarmas estilo Alarmy, retos, tareas, calendario, rutinas/objetos, finanzas, Pomodoro, bienestar, dragón Azul y evolución/animaciones; estas pruebas no se declararán superadas sin ejecución.

## 4. Bloqueos P0 antes de abrir Play Store al público

- [ ] **Persistencia de datos:** PostgreSQL Free de Render vence el **18 de diciembre de 2026** y no tiene respaldos gestionados. Migrar a base persistente con backups probados, restauración y monitorización. Servidor Free entra en reposo.
- [ ] **FCM para avisos oportunos con app cerrada**, reintentos y registro/revocación segura de tokens de dispositivos, o indicar formalmente la limitación de avisos tardíos.
- [ ] **Prevención de abuso más robusta:** límites de solicitudes distribuidos, cuotas por cuenta, reportes/moderación, bloqueo de spam, auditoría y monitorización de errores.
- [ ] **Sesiones de larga duración:** actualmente tokens del backend duran 7 días. Diseñar renovación verificable / reautenticación, evitando pérdida silenciosa de cuenta.
- [ ] **Política de privacidad revisada, Data Safety, permisos especiales y política de eliminación completa** con plazo de atención fuera de la app, retención/backup y atención de solicitudes.
- [ ] **Prueba de datos personales:** nunca usar nombres/documentos/salud reales en staging efímero.
- [ ] **Build AAB firmada con clave release distinta de Preview**, clave de subida protegida, Play App Signing y SHA-1 de firma Play registrado en Google Cloud para el paquete principal `com.neurosync.familia`.
- [ ] **Play Console:** ficha, capturas originales, ícono, declaración de anuncios, clasificación, declaraciones de acceso a datos/seguridad, pruebas internas/cerradas. Cuentas personales nuevas podrían requerir 12 probadores durante 14 días; verificar elegibilidad de la cuenta.
- [ ] **QA y accesibilidad:** lanzamiento rápido, uso sin red, contraste/lectura, pantallas pequeñas, consumo de batería, rotación, grabaciones/diagnóstico de fallos, y pruebas de privacidad entre cuentas.

## 5. Alcance probado vs no probado

La sintaxis y varias condiciones estructurales están verificadas, y Render reporta `live`. **Una comprobación estática no demuestra que la aplicación compile ni que una tarea llegue a otro teléfono.** No existen artefactos APK/AAB confirmados ni pruebas instrumentadas Android realizadas por este asistente.

**No se activó el registro público**, no se creó infraestructura de pago y no se subió ninguna clave privada.

## 6. Próxima ruta reproducible

1. Conectar Codemagic a GitHub y permitir acceso a `elgatojhon8-droid/TDAH` o solucionar disponibilidad de runners GitHub Actions.
2. Configurar secretos de firma Preview en CI, disparar `neurosync-apk-preview`, revisar errores y verificar SHA-1 del APK resultante.
3. Ejecutar pruebas con PostgreSQL de test aislado y comprobar que la API pasa todas las transiciones, bloqueo y eliminación.
4. Añadir segunda cuenta Google TESTING, habilitar temporalmente inscripción para piloto, probar dos dispositivos y cerrar nuevamente el registro si corresponde.
5. Registrar y verificar bugs de las funciones locales/alarma/finanzas/mascota, corregirlos con QA sobre APK real.
6. Crear un servicio de datos con persistencia y backups antes de almacenar cuentas reales; incorporar FCM y completar requisitos Play.

**Criterio de salida:** no declarar "lista para Play" sin APK/AAB firmada, login verificado, pruebas entre dos cuentas y respaldo persistente confiable.
