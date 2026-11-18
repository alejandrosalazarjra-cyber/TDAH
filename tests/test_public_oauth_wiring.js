'use strict';
const fs=require('node:fs'),assert=require('node:assert/strict'),vm=require('node:vm');
const api=fs.readFileSync('backend/server.js','utf8');
const apk=fs.readFileSync('android/app/src/main/java/com/neurosync/familia/PublicAccountBridge.java','utf8');
const html=fs.readFileSync('index.html','utf8');
const github=fs.readFileSync('.github/workflows/android-apk.yml','utf8');
const codemagic=fs.readFileSync('codemagic.yaml','utf8');
const guide=fs.readFileSync('docs/OAUTH_GOOGLE_NEUROSYNC.md','utf8');

new vm.Script(api);
for(const part of html.matchAll(/<script(?:[^>]*)>([\s\S]*?)<\/script>/g))
 if(part[1].trim())new vm.Script(part[1]);
assert(api.includes("process.env.PUBLIC_REGISTRATION_ENABLED!=='true'"),
 'El backend no abre cuentas por configurar OAuth accidentalmente');
assert(api.includes("path==='/v1/auth/status'")&&
 api.includes("loginEnabled:!!oauth"),'Exponer solo estado booleano de registro');
assert(apk.includes('.requestIdToken(BuildConfig.PUBLIC_GOOGLE_CLIENT_ID)'),
 'Google debe entregar ID token para la audiencia Web Client');
assert(apk.includes('code==10')&&apk.includes('configurationRequired'),
 'La APK debe diagnosticar un SHA-1 o paquete no registrado');
assert(html.includes("by('ns-google-fingerprint').textContent='Paquete: '"),
 'Los diagnósticos deben estar disponibles sin mostrar secretos');
assert(github.includes('NEUROSYNC_GOOGLE_WEB_CLIENT_ID: ${{ vars.NEUROSYNC_GOOGLE_WEB_CLIENT_ID }}'),
 'APK GitHub Actions debe admitir configuración OAuth del desarrollador');
assert(codemagic.includes('NEUROSYNC_GOOGLE_WEB_CLIENT_ID'),
 'APK Codemagic debe admitir la misma configuración');
assert(guide.includes('com.neurosync.familia.preview')&&guide.includes('Play App Signing'),
 'Registrar firma correcta de ambas variantes Android');
console.log('OAuth público OK: registro cerrado por defecto, Google Web ID token y firmas Android');
