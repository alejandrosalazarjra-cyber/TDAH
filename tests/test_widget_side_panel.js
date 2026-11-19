/* QA estático: panel de widgets del launcher minimalista y privacidad. */
const fs=require('node:fs'),assert=require('node:assert/strict');
const file=fs.readFileSync('android/app/src/main/java/com/neurosync/familia/FocusLauncherActivity.java','utf8');
const workflow=fs.readFileSync('.github/workflows/android-apk.yml','utf8');
const html=fs.readFileSync('index.html','utf8');
assert(file.includes('private void showWidgetsPanel()'),'Panel de widgets inexistente');
assert(file.includes('private void showWidgetPreferences()'),'Falta personalización');
assert(file.includes('dx>dp(85)&&Math.abs(dx)>Math.abs(dy)*1.4f'),'Swipe derecha no implementado');
assert(file.includes('dx<-dp(85)&&Math.abs(dx)>Math.abs(dy)*1.4f'),'No perder gesto izquierda');
assert(file.includes('dy<-dp(85)'),'Swipe arriba debe conservar acceso a apps');
assert(file.includes('private static final String[] PANEL_IDS='));
for(const widget of ['tasks','alarms','focus','finance','lists','progress'])
 assert(file.includes('"'+widget+'"'),'No existe widget '+widget);
assert(file.includes('preferences.getString("widget_order"'));
assert(file.includes('saveWidgetOrder('));
assert(file.includes('Collections.swap(list,at,at-1)'));
assert(file.includes('Collections.swap(list,at,at+1)'));
assert(file.includes('checked.setOnCheckedChangeListener'));
assert(file.includes('finance.getBoolean("enabled",false)'),'No publicar montos sin permiso financiero');
assert(file.includes('finanzas')||file.includes('Finanzas'));
assert(!file.includes('NoFap')||file.includes('no se muestra en Inicio'));
const home=file.slice(file.indexOf('private void renderHome(){'),file.indexOf('private void addHomeApp('));
assert(!home.includes('getLong("balance"'),'La pantalla minimalista inicial no debe exponer el saldo');
assert(file.includes('openNeuroSync("finance")'));
assert(workflow.includes('node tests/test_widget_side_panel.js'));
// El puente financiero conserva validación de identidad en Android.
const activity=fs.readFileSync('android/app/src/main/java/com/neurosync/familia/MainActivity.java','utf8');
assert(activity.includes('updateFinanceLauncherSummary(')&&activity.includes('canAccessSnapshot(username)'));
assert(html.includes('finance'));
console.log('Widgets lateral OK: swipe, personalización, orden, privacidad y navegación.');
