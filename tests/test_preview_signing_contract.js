'use strict';
const assert=require('node:assert/strict');
const fs=require('node:fs');
const gradle=fs.readFileSync('android/app/build.gradle','utf8');
const github=fs.readFileSync('.github/workflows/android-apk.yml','utf8');
const codemagic=fs.readFileSync('codemagic.yaml','utf8');
const gitignore=fs.readFileSync('.gitignore','utf8');
const guide=fs.readFileSync('docs/FIRMA_PREVIEW_ANDROID.md','utf8');
const sha='3E:F5:74:B7:F3:8F:09:FB:B4:81:60:BC:40:57:1E:A7:D6:A4:90:63';
assert(gradle.includes("create('neurosyncPreview')"),'Configuración separada de firma preview');
assert(gradle.includes("System.getenv('NS_PREVIEW_KEYSTORE_FILE')"),'Clave privada solo de entorno');
const preview=gradle.slice(gradle.indexOf('        preview {'),gradle.indexOf('        release {'));
assert(preview.includes('signingConfig signingConfigs.neurosyncPreview'),
 'La APK preview se firma con certificado de pruebas');
assert(!preview.includes('neurosyncRelease')&&!preview.includes("NS_KEYSTORE_FILE'"),
 'La APK preview no puede utilizar firma de distribución');
assert(preview.includes("buildConfigField 'String', 'PUBLIC_GOOGLE_CLIENT_ID', '\"\"'"),
 'El acceso público debe ocultarse sin clave preview estable');
for(const s of ['NS_PREVIEW_SIGNING_KEY_B64','NS_PREVIEW_SIGNING_PASSWORD','NS_PREVIEW_SIGNING_ALIAS']){
 assert(github.includes('secrets.'+s),'Falta secreto GitHub para '+s);
 assert(codemagic.includes(s),'Falta variable Codemagic para '+s);
 assert(guide.includes(s),'Falta documentación de '+s);
}
assert(github.includes(sha)&&codemagic.includes(sha)&&guide.includes(sha),
 'La huella OAuth Android ha de coincidir en todas las compilaciones');
assert(github.includes('-storepass:env NS_PREVIEW_SIGNING_PASSWORD'),
 'Verificar con contraseña por variable, sin imprimir secreto');
assert(gitignore.includes('*.jks')&&gitignore.includes('signing/'),
 'No permitir claves JKS en el repositorio');
console.log('Firma OAuth preview OK: keystore separado, SHA-1 estable, modo seguro sin secretos');
