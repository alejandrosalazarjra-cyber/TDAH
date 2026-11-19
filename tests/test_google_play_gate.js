'use strict';
/** Solo contratos de estructura; NO sustituye compilación, pruebas o revisión de Google Play. */
const fs=require('node:fs'),assert=require('node:assert/strict'),vm=require('node:vm');
const android=fs.readFileSync('android/app/build.gradle','utf8');
const plugin=fs.readFileSync('android/build.gradle','utf8');
const github=fs.readFileSync('.github/workflows/android-apk.yml','utf8');
const codemagic=fs.readFileSync('codemagic.yaml','utf8');
const legal=fs.readFileSync('backend/legal_pages.js','utf8');
const api=fs.readFileSync('backend/server.js','utf8');
const native=fs.readFileSync('android/app/src/main/java/com/neurosync/familia/MainActivity.java','utf8');
new vm.Script(legal);
new vm.Script(api);
assert(/compileSdk\s+36\b/.test(android)&&/targetSdk\s+36\b/.test(android),
 'Google Play requiere API 36 para la versión móvil nueva');
assert(plugin.includes("version '8.10.1'"),'Plugin AGP compatible con API 36');
assert(github.includes('platforms;android-36')&&github.includes('build-tools;36.0.0'),
 'GitHub debe instalar SDK 36');
assert(codemagic.includes('platforms;android-36')&&codemagic.includes('build-tools;36.0.0'),
 'Codemagic debe instalar SDK 36');
assert(github.includes('bundleRelease')&&github.includes('NeuroSync-Google-Play-AAB'),
 'Se debe generar y publicar un bundle verificado como artefacto');
assert(codemagic.includes('bundleRelease')&&codemagic.includes('*.aab'),
 'La compilación alternativa debe producir un AAB si hay firma');
assert(legal.includes("'/privacy'")&&legal.includes("'/delete-account'"),
 'Se requieren páginas de privacidad y eliminación');
assert(api.includes("serveLegal(res,new URL(req.url"),
 'Páginas legales servidas por HTTPS antes de exigir token');
assert(native.includes('openLegalPage(String page)'),
 'El usuario puede abrir los enlaces legales desde la APK');
console.log('Play gate estático OK: API 36, AAB y avisos legales');
