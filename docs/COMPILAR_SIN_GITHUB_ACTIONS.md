# Compilar NeuroSync sin GitHub Actions ni instalar Android Studio

## Alternativa desde el navegador: Codemagic
La raíz del repositorio incluye `codemagic.yaml` con la compilación `neurosync-apk-preview`. Construye `com.neurosync.familia.preview` para instalar **junto** a la aplicación principal, y no reemplaza ni migra automáticamente datos de la versión instalada.

1. Visitar https://codemagic.io/ y entrar con GitHub.
2. Autorizar la aplicación Codemagic CI/CD únicamente al repositorio `elgatojhon8-droid/TDAH`.
3. Agregar el repositorio a Codemagic. Utilizar configuración con `codemagic.yaml`.
4. Seleccionar la rama **feature/compilacion-codemagic-sin-actions-2026-11** y el workflow **NeuroSync - APK de prueba independiente**.
5. Iniciar la compilación y, si termina correctamente, descargar `app-preview.apk` desde **Artifacts**.

## Qué hace el flujo
- Comprueba Java/Gradle y SDK Android en un equipo remoto.
- Ejecuta pruebas de módulos y de gestos de widgets.
- Empaqueta el HTML, JS/CSS actual, imágenes de Azul y opcionalmente Bosque.
- Compila `assemblePreview` y comprueba que la interfaz del APK es la del código actual.
- Publica el APK como artefacto descargable en Codemagic.

## Precauciones
- Requiere conectar explícitamente el repositorio a Codemagic. No se ha realizado esa autorización ni se ha lanzado la compilación en ese servicio.
- La versión preview tiene identificador de paquete diferente; no actualiza la app principal ni comparte automáticamente los datos de la instalación existente.
- Puede haber errores Java/Gradle no detectados por las comprobaciones de sintaxis previas. Se deben solucionar según los registros reales.
- Para una actualización de la aplicación original sin pérdida de datos se necesita usar **su firma de distribución original** y el mismo ID de paquete; no instalar una APK de otra firma encima.
- No proporcionar accesos bancarios, contraseñas ni secretos del usuario a servicios de compilación.

## Otra opción: Codespaces
En un Codespace con Java 17, Android SDK 35 y Gradle 8.11.1 instalados, copiar los mismos assets siguiendo `codemagic.yaml`, luego ejecutar `cd android && gradle assemblePreview`. Descargar el archivo `android/app/build/outputs/apk/preview/app-preview.apk` desde el explorador del Codespace. No requiere GitHub Actions pero sí configurar las herramientas Android dentro del Codespace.
