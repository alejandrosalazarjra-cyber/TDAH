package com.neurosync.familia;

import android.Manifest;
import android.app.*;
import android.os.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.webkit.*;
import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Contenedor nativo de NeuroSync: acceso por cuentas, sincronización y alarmas.
 * La interfaz principal se distribuye dentro de la APK (no depende de Netlify).
 */
public final class MainActivity extends Activity {
  private String pendingNavigation="";
  private String allowedNavigation(String value){
    if(value==null)return "";
    switch(value){
      case "home":case "tasks":case "calendar":case "bags":case "alarms":
      case "habits":case "focus":case "dragon":case "admin":case "finance":return value;
      default:return "";
    }
  }
  private void deliverNavigation(){
    if(web==null||pendingNavigation.isEmpty())return;
    String route=pendingNavigation;
    pendingNavigation="";
    web.evaluateJavascript("window.neuroNavigateFromLauncher && window.neuroNavigateFromLauncher("+JSONObject.quote(route)+")",null);
  }
  @Override protected void onNewIntent(Intent intent){
    super.onNewIntent(intent);
    setIntent(intent);
    pendingNavigation=allowedNavigation(intent==null?null:intent.getStringExtra("neurosync_view"));
    deliverNavigation();
  }
  private WebView web;
  private GoogleAccountBridge googleAccounts;
  void googleCallback(String name,String json){
    if(web!=null)web.evaluateJavascript("window."+name+" && window."+name+"("+JSONObject.quote(json)+")",null);
  }
  private String backupPending="";
  private boolean qrEnrollmentPending=false;
  private final ExecutorService networkWorker=Executors.newSingleThreadExecutor();
  private volatile String sessionToken="";
  private volatile JSONObject sessionUser=null;
  private String currentMode(){return getPreferences(0).getString("mode","");}
  private String currentEndpoint(){
    String mode=currentMode();
    if("cloud".equals(mode))return getPreferences(0).getString("cloud","");
    if("client".equals(mode))return getPreferences(0).getString("server","");
    return "host";
  }
  private void restoreNativeSession(){
    JSONObject saved=SessionVault.restore(this,currentMode(),currentEndpoint());
    if(saved==null)return;
    String token=saved.optString("token","");
    JSONObject user=saved.optJSONObject("user");
    if(token.length()<30||user==null)return;
    if(localHost()){
      JSONObject actual=AccountStore.identity(this,token);
      if(actual==null){SessionVault.clear(this);return;}
      user=actual;
    }
    sessionToken=token;
    sessionUser=user;
  }
  private void forgetNativeSession(){
    if(localHost()&&!sessionToken.isEmpty())AccountStore.revoke(this,sessionToken);
    SessionVault.clear(this);
    sessionUser=null;sessionToken="";
  }
  @Override public void onCreate(Bundle state){
    super.onCreate(state);
    pendingNavigation=allowedNavigation(getIntent()==null?null:getIntent().getStringExtra("neurosync_view"));
    restoreNativeSession();
    googleAccounts=new GoogleAccountBridge(this);
    getWindow().setStatusBarColor(Color.rgb(246,248,252));
    getWindow().setNavigationBarColor(Color.rgb(255,255,255));
    getWindow().getDecorView().setSystemUiVisibility(android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
    web=new WebView(this);
    setContentView(web);
    WebSettings settings=web.getSettings();
    settings.setJavaScriptEnabled(true);settings.setDomStorageEnabled(true);
    settings.setAllowFileAccess(false);settings.setAllowContentAccess(false);
    settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
    web.setWebChromeClient(new WebChromeClient());
    web.setWebViewClient(new WebViewClient(){
      @Override public void onPageFinished(WebView view,String url){
        super.onPageFinished(view,url);
        deliverNavigation();
      }
      // Solo recursos locales de la mascota; no altera la red, cuentas ni las alarmas.
      @Override public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request){
        String path=request.getUrl().getPath();
        // Módulos internos de alarmas: ruta exacta, sin permitir acceso a archivos arbitrarios.
        if("appassets.androidplatform.net".equals(request.getUrl().getHost()) && path!=null){
          String resource=null,mime=null;
          if("/modules/alarm_studio.js".equals(path)){
            resource="modules/alarm_studio.js";mime="text/javascript";
          }else if("/modules/alarm_studio.css".equals(path)){
            resource="modules/alarm_studio.css";mime="text/css";
          }else if("/modules/task_studio.js".equals(path)){
            resource="modules/task_studio.js";mime="text/javascript";
          }else if("/modules/task_studio.css".equals(path)){
            resource="modules/task_studio.css";mime="text/css";
          }else if("/modules/departure_studio.js".equals(path)){
            resource="modules/departure_studio.js";mime="text/javascript";
          }else if("/modules/departure_studio.css".equals(path)){
            resource="modules/departure_studio.css";mime="text/css";
          }else if("/modules/pomodoro_studio.js".equals(path)){
            resource="modules/pomodoro_studio.js";mime="text/javascript";
          }else if("/modules/pomodoro_studio.css".equals(path)){
            resource="modules/pomodoro_studio.css";mime="text/css";
          }
          else if("/modules/wellbeing_studio.js".equals(path)){
            resource="modules/wellbeing_studio.js";mime="text/javascript";
          }else if("/modules/wellbeing_studio.css".equals(path)){
            resource="modules/wellbeing_studio.css";mime="text/css";
          }
          else if("/modules/finance_studio.js".equals(path)){
            resource="modules/finance_studio.js";mime="text/javascript";
          }else if("/modules/finance_studio.css".equals(path)){
            resource="modules/finance_studio.css";mime="text/css";
          }else if("/assets/focus/bosque.mp4".equals(path)){
            resource="focus/bosque.mp4";mime="video/mp4";
          }
          if(resource!=null)try{
            return new WebResourceResponse(mime,"UTF-8",getAssets().open(resource));
          }catch(IOException error){
            android.util.Log.w("NeuroSync","No se pudo cargar módulo interno de alarmas",error);
            return null;
          }
        }
        if("appassets.androidplatform.net".equals(request.getUrl().getHost())
            && path!=null && path.matches("/assets/dragon/(?:etapa-[1-6]|atlases/etapa-[1-6])\\.webp")){
          try{
            String file=path.substring("/assets/".length());
            return new WebResourceResponse("image/webp",null,getAssets().open(file));
          }catch(IOException e){return null;}
        }
        return super.shouldInterceptRequest(view,request);
      }
      @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest request){
        return !request.getUrl().toString().startsWith("https://appassets.androidplatform.net/");
      }
    });
    web.addJavascriptInterface(new NativeBridge(),"AndroidBridge");
    try(InputStream in=getAssets().open("index.html")){
      ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int count;
      while((count=in.read(buffer))!=-1)b.write(buffer,0,count);
      web.loadDataWithBaseURL("https://appassets.androidplatform.net/index.html",b.toString("UTF-8"),"text/html","UTF-8",null);
    }catch(Exception ex){new AlertDialog.Builder(this).setMessage("No se pudo cargar la interfaz de NeuroSync").setPositiveButton("Aceptar",null).show();}
    if("host".equals(getPreferences(0).getString("mode","")))startServer();
    // Los permisos se solicitan al crear/probar una alarma, no al abrir la aplicación.
  }
  @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
    super.onActivityResult(requestCode,resultCode,data);
    if(requestCode==GoogleAccountBridge.REQUEST_SIGNIN){
      googleAccounts.onResult(data);return;
    }
    // Registro de referencia QR iniciado desde el formulario web.
    if(qrEnrollmentPending){
      com.google.zxing.integration.android.IntentResult scan=
        com.google.zxing.integration.android.IntentIntegrator.parseActivityResult(requestCode,resultCode,data);
      if(scan!=null){
        qrEnrollmentPending=false;
        String value=scan.getContents();
        if(web!=null){
          String argument=value==null?"null":org.json.JSONObject.quote(value);
          web.evaluateJavascript("window.neuroQrEnrollment && window.neuroQrEnrollment("+argument+")",null);
        }
        return;
      }
    }
    if(requestCode==194){
      boolean selected=holdsHomeRole();
      new AlertDialog.Builder(this).setTitle(selected?"Inicio activado":"Inicio sin cambiar")
        .setMessage(selected?"NeuroSync es ahora tu pantalla de inicio.":"Android no seleccionó NeuroSync. Abre Ajustes → Aplicaciones predeterminadas → Aplicación de inicio. Si tu fabricante no lo permite, conserva la pantalla original.")
        .setPositiveButton("Entendido",null).show();
      return;
    }
    if(resultCode!=RESULT_OK||data==null||data.getData()==null)return;
    android.net.Uri uri=data.getData();
    if(requestCode==191){
      try{
        int flags=data.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION;
        getContentResolver().takePersistableUriPermission(uri,flags);
        getSharedPreferences("ns_sound",0).edit().putString("uri",uri.toString()).apply();
        if(web!=null)web.evaluateJavascript("window.neuroAlarmSoundSelected && window.neuroAlarmSoundSelected()",null);
      }catch(Exception ex){android.util.Log.w("NeuroSync","No se pudo guardar audio",ex);}
    }else if(requestCode==192){
      try(java.io.OutputStream out=getContentResolver().openOutputStream(uri)){
        if(out==null)throw new IOException("No se pudo abrir el destino");
        out.write(backupPending.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        if(web!=null)web.evaluateJavascript("window.neuroBackupMessage && window.neuroBackupMessage('Respaldo guardado')",null);
      }catch(Exception e){android.util.Log.e("NeuroSync","Error exportando respaldo",e);}
      finally{backupPending="";}
    }else if(requestCode==193){
      try(java.io.InputStream in=getContentResolver().openInputStream(uri)){
        if(in==null)throw new IOException("No se pudo leer el archivo");
        ByteArrayOutputStream buffer=new ByteArrayOutputStream();
        byte[] chunk=new byte[8192];int n;
        while((n=in.read(chunk))!=-1){buffer.write(chunk,0,n);if(buffer.size()>2000000)throw new IOException("Respaldo demasiado grande");}
        String json=buffer.toString("UTF-8");
        JSONObject parsed=new JSONObject(json);
        if(!"neurosync-encrypted-backup".equals(parsed.optString("format")) &&
           (parsed.optJSONArray("tasks")==null||parsed.optJSONArray("packs")==null||parsed.optJSONArray("alarms")==null))
          throw new IOException("Formato de respaldo incompatible");
        if(web!=null)web.evaluateJavascript("window.neuroImportData && window.neuroImportData("+JSONObject.quote(json)+")",null);
      }catch(Exception e){android.util.Log.e("NeuroSync","Error importando respaldo",e);}
    }
  }
  private void requestNextAlarmPermission(){
    if(Build.VERSION.SDK_INT>=33 &&
       checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){
      requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},101);return;
    }
    if(Build.VERSION.SDK_INT>=31 && !((AlarmManager)getSystemService(ALARM_SERVICE)).canScheduleExactAlarms()){
      try{startActivity(new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
        android.net.Uri.parse("package:"+getPackageName())));}
      catch(Exception e){openAppSettings();}
      return;
    }
    if(Build.VERSION.SDK_INT>=34 && !getSystemService(NotificationManager.class).canUseFullScreenIntent()){
      try{startActivity(new Intent(android.provider.Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
        android.net.Uri.parse("package:"+getPackageName())));}
      catch(Exception e){openAppSettings();}
      return;
    }
    new AlertDialog.Builder(this).setTitle("Alarmas configuradas")
      .setMessage("Ya puedes probar la alarma. Verifica también que el volumen de alarmas del teléfono no esté en cero.")
      .setPositiveButton("Entendido",null).show();
  }
  @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] grants){
    super.onRequestPermissionsResult(request,permissions,grants);
    if(request==101 && grants.length>0 && grants[0]==PackageManager.PERMISSION_GRANTED)
      new AlertDialog.Builder(this).setTitle("Notificaciones activadas")
        .setMessage("Continúa con los demás permisos para que las alarmas aparezcan cuando el celular esté bloqueado.")
        .setPositiveButton("Continuar",(d,w)->requestNextAlarmPermission()).show();
  }
  private boolean holdsHomeRole(){
    if(Build.VERSION.SDK_INT<29)return false;
    android.app.role.RoleManager role=getSystemService(android.app.role.RoleManager.class);
    return role!=null&&role.isRoleHeld(android.app.role.RoleManager.ROLE_HOME);
  }
  private void requestHomeRole(){
    if(holdsHomeRole()){
      new AlertDialog.Builder(this).setTitle("Inicio configurado")
        .setMessage("NeuroSync ya es tu pantalla de inicio predeterminada.")
        .setPositiveButton("Entendido",null).show();return;
    }
    if(Build.VERSION.SDK_INT>=29){
      android.app.role.RoleManager role=getSystemService(android.app.role.RoleManager.class);
      if(role!=null&&role.isRoleAvailable(android.app.role.RoleManager.ROLE_HOME)){
        try{startActivityForResult(role.createRequestRoleIntent(android.app.role.RoleManager.ROLE_HOME),194);return;}
        catch(Exception e){android.util.Log.w("NeuroSync","No se pudo abrir selector HOME",e);}
      }
    }
    try{startActivity(new Intent(android.provider.Settings.ACTION_HOME_SETTINGS));}
    catch(Exception e){openAppSettings();}
  }
  private boolean localHost(){return "host".equals(getPreferences(0).getString("mode",""));}
  private JSONObject remote(String path,JSONObject request){
    HttpURLConnection c=null;
    try{
      String mode=getPreferences(0).getString("mode","");
      URL url;
      if("cloud".equals(mode)){
        String base=getPreferences(0).getString("cloud","");
        if(!validCloudUrl(base))return AccountStore.fail("Configura el servidor HTTPS familiar");
        url=new URL(base+path);
      }else{
        String ip=getPreferences(0).getString("server","");
        if(!validPrivateIp(ip))return AccountStore.fail("Configura una IP privada válida");
        url=new URL("http://"+ip+":"+SyncService.PORT+path);
      }
      c=(HttpURLConnection)url.openConnection();c.setRequestMethod("POST");
      c.setConnectTimeout(5000);c.setReadTimeout(8000);c.setDoOutput(true);c.setUseCaches(false);
      c.setRequestProperty("Content-Type","application/json");
      if(!sessionToken.isEmpty())c.setRequestProperty("Authorization","Bearer "+sessionToken);
      byte[] bytes=request.toString().getBytes(StandardCharsets.UTF_8);
      if(bytes.length>500000)return AccountStore.fail("Datos demasiado grandes");
      c.setFixedLengthStreamingMode(bytes.length);
      try(OutputStream out=c.getOutputStream()){out.write(bytes);}
      InputStream in=(c.getResponseCode()<400)?c.getInputStream():c.getErrorStream();
      if(in==null)return AccountStore.fail("No hubo respuesta del servidor");
      ByteArrayOutputStream read=new ByteArrayOutputStream();byte[] buf=new byte[8192];int n;
      try(InputStream r=in){while((n=r.read(buf))!=-1){read.write(buf,0,n);if(read.size()>600000)throw new IOException("Respuesta demasiado grande");}}
      return new JSONObject(read.toString("UTF-8"));
    }catch(Exception e){return AccountStore.fail("No se pudo conectar al servidor familiar. Comprueba internet y la dirección del servicio.");}
    finally{if(c!=null)c.disconnect();}
  }
  private JSONObject call(String path,JSONObject args){
    if(sessionUser==null)return AccountStore.fail("Inicia sesión");
    if(!localHost())return remote(path,args);
    if(AccountStore.identity(this,sessionToken)==null)return AccountStore.fail("Sesión caducada. Inicia sesión de nuevo.");
    switch(path){
      case "/users":return AccountStore.list(this,sessionUser);
      case "/create-user":return AccountStore.create(this,sessionUser,args.optString("username"),args.optString("password"));
      case "/change-password":return AccountStore.change(this,sessionUser,args.optString("current"),args.optString("next"));
      case "/sync":return SyncStore.merge(this,args,sessionUser);
      default:return AccountStore.fail("Función no encontrada");
    }
  }
  private JSONObject payload(String... pairs){
    JSONObject r=new JSONObject();
    try{for(int i=0;i+1<pairs.length;i+=2)r.put(pairs[i],pairs[i+1]);}catch(Exception ignored){}
    return r;
  }
  private JSONObject success(String message){
    try{return new JSONObject().put("ok",true).put("message",message);}
    catch(Exception error){return new JSONObject();}
  }
  private static boolean validPrivateIp(String host){
    if(host==null||!host.matches("(?:\\d{1,3}\\.){3}\\d{1,3}"))return false;
    String[] p=host.split("\\.");int[] n=new int[4];
    try{for(int i=0;i<4;i++){n[i]=Integer.parseInt(p[i]);if(n[i]<0||n[i]>255)return false;}}catch(Exception e){return false;}
    return n[0]==10||(n[0]==192&&n[1]==168)||(n[0]==172&&n[1]>=16&&n[1]<=31)||(n[0]==100&&n[1]>=64&&n[1]<=127);
  }
  private void openAppSettings(){
    startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
      android.net.Uri.parse("package:"+getPackageName())));
  }
  private static boolean validCloudUrl(String url){
    try{
      java.net.URI u=new java.net.URI(url);
      return "https".equalsIgnoreCase(u.getScheme())&&u.getHost()!=null&&u.getHost().length()>3
        &&u.getUserInfo()==null&&u.getQuery()==null&&u.getFragment()==null
        &&(u.getPath()==null||u.getPath().isEmpty()||u.getPath().equals("/"));
    }catch(Exception e){return false;}
  }
  private void startServer(){runOnUiThread(()->{
    try{startForegroundService(new Intent(this,SyncService.class));}
    catch(Exception e){android.util.Log.w("NeuroSync","No se pudo iniciar servicio: "+e.getMessage());}
  });}
  public final class NativeBridge {
    @JavascriptInterface public String configureHost(){
      if(!localHost()){
        forgetNativeSession();
        getPreferences(0).edit().putString("mode","host").remove("server").apply();
      }
      startServer();
      return success("Celular principal activo. Comparte la IP privada de Tailscale con tu familia.").toString();
    }
    @JavascriptInterface public String configureRemote(String ip){
      if(!validPrivateIp(ip))return AccountStore.fail("Usa la IP privada de Tailscale (100.x.x.x) o Wi-Fi").toString();
      forgetNativeSession();
      getPreferences(0).edit().putString("mode","client").putString("server",ip).apply();
      runOnUiThread(()->stopService(new Intent(MainActivity.this,SyncService.class)));
      return success("Conectado al servidor "+ip+". Inicia sesión con tu usuario familiar.").toString();
    }
    @JavascriptInterface public String configureCloud(String base){
      if(base==null)return AccountStore.fail("Dirección vacía").toString();
      String url=base.trim().replaceAll("/+$","");
      if(!validCloudUrl(url))return AccountStore.fail("Escribe una dirección HTTPS válida del servidor familiar").toString();
      forgetNativeSession();
      getPreferences(0).edit().putString("mode","cloud").putString("cloud",url).apply();
      runOnUiThread(()->stopService(new Intent(MainActivity.this,SyncService.class)));
      return success("Servidor en internet configurado. Inicia sesión con tu cuenta familiar.").toString();
    }
    @JavascriptInterface public String login(String username,String password){
      String mode=getPreferences(0).getString("mode","");
      if(mode.isEmpty())return AccountStore.fail("Primero activa tu teléfono como administrador o vincúlalo a uno principal").toString();
      JSONObject result=localHost()?AccountStore.authenticate(MainActivity.this,username,password)
        :remote("/login",payload("username",username,"password",password));
      if(result.optBoolean("ok")){
        sessionToken=result.optString("token","");
        sessionUser=result.optJSONObject("user");
        boolean persisted=SessionVault.save(MainActivity.this,sessionToken,sessionUser,currentMode(),currentEndpoint());
        try{
          result.put("sessionPersistent",persisted);
          if(!persisted)result.put("sessionWarning",
            "No se pudo conservar la sesión en este dispositivo. Puede ser necesario iniciar sesión otra vez.");
        }catch(Exception ignored){}
        // El aviso de contraseña inicial se gestiona en la interfaz.
        result.remove("token"); // El secreto de sesión permanece exclusivamente en Java.
      }
      return result.toString();
    }
    /** Diagnóstico público: paquete y huella del certificado, nunca claves privadas. */
    @JavascriptInterface public String googleOAuthDiagnostics(){
      JSONObject info=new JSONObject();
      try{
        java.security.MessageDigest digest=java.security.MessageDigest.getInstance("SHA-1");
        byte[] cert=null;
        if(Build.VERSION.SDK_INT>=28){
          android.content.pm.PackageInfo pkg=getPackageManager().getPackageInfo(getPackageName(),PackageManager.GET_SIGNING_CERTIFICATES);
          android.content.pm.SigningInfo signing=pkg.signingInfo;
          android.content.pm.Signature[] signatures=signing.hasMultipleSigners()?signing.getApkContentsSigners():signing.getSigningCertificateHistory();
          if(signatures!=null&&signatures.length>0)cert=signatures[0].toByteArray();
        }else{
          android.content.pm.PackageInfo pkg=getPackageManager().getPackageInfo(getPackageName(),PackageManager.GET_SIGNATURES);
          if(pkg.signatures!=null&&pkg.signatures.length>0)cert=pkg.signatures[0].toByteArray();
        }
        StringBuilder hash=new StringBuilder();
        if(cert!=null){for(byte value:digest.digest(cert)){if(hash.length()>0)hash.append(':');hash.append(String.format(java.util.Locale.US,"%02X",value&255));}}
        info.put("packageName",getPackageName()).put("sha1",hash.toString())
          .put("note","Registra este paquete y SHA-1 en un cliente OAuth de Android en Google Cloud. Play App Signing usa otro certificado.");
      }catch(Exception ex){android.util.Log.w("NeuroSync","Error consultando certificado",ex);}
      return info.toString();
    }
    @JavascriptInterface public void loginGoogle(){
      runOnUiThread(()->googleAccounts.login());
    }
    @JavascriptInterface public String restoreGoogleSession(){
      return googleAccounts.restored().toString();
    }
    @JavascriptInterface public void backupGoogleDrive(String json){
      googleAccounts.backup(json);
    }
    @JavascriptInterface public void refreshGoogleDrive(){
      googleAccounts.restoreDrive();
    }
    @JavascriptInterface public void logoutGoogle(){
      googleAccounts.logout();
    }
    @JavascriptInterface public boolean adminModeEnabled(){
      return localHost() || currentMode().isEmpty();
    }
    @JavascriptInterface public String loginOwner(String username,String password){
      if(currentMode().isEmpty()){
        getPreferences(0).edit().putString("mode","host").apply();
        runOnUiThread(()->startServer());
      }
      if(!localHost())return AccountStore.fail("El acceso de administrador solo es local").toString();
      JSONObject result=AccountStore.authenticate(MainActivity.this,username,password);
      if(!result.optBoolean("ok"))return result.toString();
      JSONObject user=result.optJSONObject("user");
      if(user==null||!"admin".equals(user.optString("role"))){
        AccountStore.revoke(MainActivity.this,result.optString("token"));
        return AccountStore.fail("Esta opción es exclusiva del administrador").toString();
      }
      sessionToken=result.optString("token","");
      sessionUser=user;
      boolean persisted=SessionVault.save(MainActivity.this,sessionToken,sessionUser,currentMode(),currentEndpoint());
        try{
          result.put("sessionPersistent",persisted);
          if(!persisted)result.put("sessionWarning",
            "No se pudo conservar la sesión en este dispositivo. Puede ser necesario iniciar sesión otra vez.");
        }catch(Exception ignored){}
      result.remove("token");
      return result.toString();
    }
    /** Las copias nativas se limitan a la identidad autenticada en Android.
     * El identificador recibido del WebView NO concede acceso a otras cuentas.
     */
    private boolean canAccessSnapshot(String username){
      if(username==null||username.isEmpty())return false;
      if(sessionUser!=null&&username.equals(sessionUser.optString("username"))&&
          !sessionToken.isEmpty()&&
          (!localHost()||AccountStore.identity(MainActivity.this,sessionToken)!=null))return true;
      try{
        JSONObject google=googleAccounts.restored();
        JSONObject account=google.optJSONObject("user");
        return google.optBoolean("ok")&&account!=null&&
          username.equals(account.optString("username"));
      }catch(Exception ignored){return false;}
    }
    @JavascriptInterface public boolean saveNativeSnapshot(String username,String json){
      if(!canAccessSnapshot(username))return false;
      return NativeSnapshotStore.get(MainActivity.this).save(username,json);
    }
    @JavascriptInterface public String recoverNativeSnapshot(String username){
      if(!canAccessSnapshot(username))return "{\"ok\":false,\"reason\":\"unauthorized\"}";
      return NativeSnapshotStore.get(MainActivity.this).recover(username);
    }
    @JavascriptInterface public String restoreSession(){
      try{
        if(sessionUser==null||sessionToken.isEmpty())return AccountStore.fail("No hay sesión guardada").toString();
        if(localHost() && AccountStore.identity(MainActivity.this,sessionToken)==null){
          forgetNativeSession();return AccountStore.fail("Sesión local revocada").toString();
        }
        return new JSONObject().put("ok",true).put("user",sessionUser).toString();
      }catch(Exception e){return AccountStore.fail("No se pudo restaurar la sesión").toString();}
    }
    @JavascriptInterface public String getUsers(){return call("/users",new JSONObject()).toString();}
    @JavascriptInterface public String createAccount(String username,String password){
      return call("/create-user",payload("username",username,"password",password)).toString();
    }
    @JavascriptInterface public String changePassword(String current,String next){
      JSONObject response=call("/change-password",payload("current",current,"next",next));
      if(response.optBoolean("ok"))forgetNativeSession();
      return response.toString();
    }
    @JavascriptInterface public String syncFamily(String json){
      try{
        if(json==null||json.length()>500000)return AccountStore.fail("Sincronización demasiado grande").toString();
        JSONObject request=new JSONObject(json);
        return call("/sync",request).toString();
      }catch(Exception e){return AccountStore.fail("Datos de sincronización inválidos").toString();}
    }
    @JavascriptInterface public void syncFamilyAsync(String json,String requestingUser){
      if(json==null||json.length()>500000)return;
      networkWorker.execute(()->{
        JSONObject result;
        try{result=call("/sync",new JSONObject(json));}
        catch(Exception error){result=AccountStore.fail("No se pudo sincronizar");}
        String value=JSONObject.quote(result.toString());
        runOnUiThread(()->{
          if(web!=null)web.evaluateJavascript("window.neuroSyncOnSync && window.neuroSyncOnSync("+value+","+JSONObject.quote(requestingUser)+")",null);
        });
      });
    }
    @JavascriptInterface public void openPermissionsPanel(String target){
      runOnUiThread(()->{
        try{
          Intent settings;
          switch(target==null?"":target){
            case "overlay":
              settings=new Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                android.net.Uri.parse("package:"+getPackageName()));break;
            case "usage":
              settings=new Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS);break;
            case "notifications":
              settings=new Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE,getPackageName());break;
            case "app":
              openAppSettings();return;
            default:return;
          }
          startActivity(settings);
        }catch(Exception ignored){openAppSettings();}
      });
    }
    @JavascriptInterface public String alarmScheduleDiagnostics(){
      try{return AlarmReceiver.diagnostics(MainActivity.this).toString();}
      catch(Exception e){return "{}";}
    }
    @JavascriptInterface public String alarmPermissions(){
      try{
        JSONObject j=new JSONObject();
        NotificationManager n=getSystemService(NotificationManager.class);
        boolean notifications=Build.VERSION.SDK_INT<33 || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED;
        boolean exact=Build.VERSION.SDK_INT<31 || ((AlarmManager)getSystemService(ALARM_SERVICE)).canScheduleExactAlarms();
        boolean full=Build.VERSION.SDK_INT<34 || n.canUseFullScreenIntent();
        boolean allowed=n.areNotificationsEnabled();
        android.app.NotificationChannel alarmChannel=Build.VERSION.SDK_INT>=26?n.getNotificationChannel("neurosync_alarms_v4_visual"):null;
        boolean high=alarmChannel==null||alarmChannel.getImportance()>=NotificationManager.IMPORTANCE_HIGH;
        return j.put("notifications",notifications&&allowed).put("exact",exact).put("fullScreen",full).put("channelHigh",high).toString();
      }catch(Exception e){return "{}";}
    }
    @JavascriptInterface public void requestAlarmPermissions(){
      runOnUiThread(()->{
        new AlertDialog.Builder(MainActivity.this)
          .setTitle("Permisos de las alarmas")
          .setMessage("NeuroSync necesita notificaciones para avisarte, alarmas exactas para respetar la hora y permiso de pantalla completa cuando el celular esté bloqueado. Android te pedirá cada autorización por separado.")
          .setNegativeButton("Ahora no",null)
          .setPositiveButton("Continuar",(dialog,which)->requestNextAlarmPermission())
          .show();
      });
    }
    @JavascriptInterface public String buildInfo(){
      try{
        android.content.pm.PackageInfo pkg=getPackageManager().getPackageInfo(getPackageName(),0);
        JSONObject result=new JSONObject();
        result.put("version",pkg.versionName);
        result.put("code",android.os.Build.VERSION.SDK_INT>=28?pkg.getLongVersionCode():pkg.versionCode);
        result.put("package",getPackageName());
        result.put("features",new org.json.JSONArray()
          .put("Alarma con retos").put("Progreso privado").put("Sesión persistente")
          .put("Bandeja de ideas").put("Launcher minimalista"));
        return result.toString();
      }catch(Exception ex){return "{}";}
    }
    /** Configura la hora mediante API oficial; selección musical queda en la app Reloj. */
    @JavascriptInterface public void createYouTubeMusicClockAlarm(){
      runOnUiThread(()->{
        java.util.Calendar now=java.util.Calendar.getInstance();
        new android.app.TimePickerDialog(MainActivity.this,(picker,hour,minute)->{
          new AlertDialog.Builder(MainActivity.this)
           .setTitle("Canción de YouTube Music")
           .setMessage("Se abrirá Reloj de Android con la hora elegida. Dentro de esa alarma, toca Sonido → YouTube Music y elige tu canción con Premium. La música sonará desde Reloj; los retos de NeuroSync no controlan esa alarma.")
           .setNegativeButton("Cancelar",null)
           .setPositiveButton("Abrir Reloj",(dialog,which)->{
             Intent intent=new Intent(android.provider.AlarmClock.ACTION_SET_ALARM)
               .putExtra(android.provider.AlarmClock.EXTRA_HOUR,hour)
               .putExtra(android.provider.AlarmClock.EXTRA_MINUTES,minute)
               .putExtra(android.provider.AlarmClock.EXTRA_MESSAGE,"NeuroSync · Música")
               .putExtra(android.provider.AlarmClock.EXTRA_SKIP_UI,false);
             try{startActivity(intent);}
             catch(Exception ex){
               android.util.Log.w("NeuroSync","Reloj no disponible",ex);
               new AlertDialog.Builder(MainActivity.this)
                .setTitle("Reloj no disponible")
                .setMessage("Instala o abre Reloj de Google y elige Sonido → YouTube Music.")
                .setPositiveButton("Entendido",null).show();
             }
           }).show();
        },now.get(java.util.Calendar.HOUR_OF_DAY),now.get(java.util.Calendar.MINUTE),true).show();
      });
    }
    @JavascriptInterface public void openSystemClock(){
      runOnUiThread(()->{
        // ACTION_SET_ALARM no es compatible con todas las apps de reloj/OEM.
        // Abrimos la app real antes de mostrar el mensaje de error.
        Intent[] choices=new Intent[]{
          getPackageManager().getLaunchIntentForPackage("com.google.android.deskclock"),
          new Intent(android.provider.AlarmClock.ACTION_SHOW_ALARMS),
          new Intent(android.provider.AlarmClock.ACTION_SET_ALARM)
            .putExtra(android.provider.AlarmClock.EXTRA_MESSAGE,"NeuroSync")
            .putExtra(android.provider.AlarmClock.EXTRA_SKIP_UI,false)
        };
        for(Intent intent:choices){
          if(intent==null)continue;
          try{
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);return;
          }catch(Exception ignored){}
        }
        new AlertDialog.Builder(MainActivity.this).setTitle("Abrir Reloj")
          .setMessage("Android no permitió abrir una app de reloj desde NeuroSync. Abre Reloj de Google manualmente, elige una alarma y selecciona YouTube Music como sonido.")
          .setPositiveButton("Entendido",null).show();
      });
    }
    @JavascriptInterface public void scanQrReference(){
      runOnUiThread(()->{
        qrEnrollmentPending=true;
        try{
          new com.google.zxing.integration.android.IntentIntegrator(MainActivity.this)
            .setDesiredBarcodeFormats(com.google.zxing.integration.android.IntentIntegrator.QR_CODE)
            .setPrompt("Escanea el QR físico que usarás para desactivar la alarma")
            .setBeepEnabled(false).setOrientationLocked(false).initiateScan();
        }catch(Exception ex){
          qrEnrollmentPending=false;
          android.widget.Toast.makeText(MainActivity.this,"No se pudo abrir la cámara QR",android.widget.Toast.LENGTH_LONG).show();
        }
      });
    }
    @JavascriptInterface public void setAlarmQrCode(String id,String value){
      AlarmChallengeStore.setQr(MainActivity.this,id,value);
    }
    @JavascriptInterface public void setAlarmFadeSeconds(int seconds){
      if(seconds!=0&&seconds!=15&&seconds!=30&&seconds!=60)return;
      getSharedPreferences("ns_alarm_audio_settings",MODE_PRIVATE).edit()
        .putInt("fadeSeconds",seconds).apply();
    }
    @JavascriptInterface public int getAlarmFadeSeconds(){
      return getSharedPreferences("ns_alarm_audio_settings",MODE_PRIVATE).getInt("fadeSeconds",0);
    }
    @JavascriptInterface public void setAlarmOptions(String id,int snoozeMinutes,int snoozeLimit){
      AlarmOptionsStore.set(MainActivity.this,id,snoozeMinutes,snoozeLimit);
    }
    @JavascriptInterface public void setAlarmChallenge(String id,String challenge){
      AlarmChallengeStore.set(MainActivity.this,id,challenge);
    }
    @JavascriptInterface public void openMinimalHome(){
      runOnUiThread(()->startActivity(new Intent(MainActivity.this,FocusLauncherActivity.class)));
    }
    @JavascriptInterface public void requestDefaultHome(){
      runOnUiThread(()->requestHomeRole());
    }
    @JavascriptInterface public String homeStatus(){
      try{
        JSONObject j=new JSONObject();
        j.put("selected",holdsHomeRole());
        j.put("available",Build.VERSION.SDK_INT>=29);
        return j.toString();
      }catch(Exception e){return "{}";}
    }
    /** Compartir el temporizador de concentración con NeuroSync Inicio. */
    @JavascriptInterface public void setLauncherFocusUntil(long until){
      long now=System.currentTimeMillis();
      if(until<now || until>now+4*60*60*1000L)until=0;
      getSharedPreferences("focus_launcher",MODE_PRIVATE).edit().putLong("focus_session_until",until).apply();
      TodayWidget.refreshState(MainActivity.this);
      FocusWidget.refresh(MainActivity.this);
    }
    @JavascriptInterface public String launcherFocusStatus(){
      long until=getSharedPreferences("focus_launcher",MODE_PRIVATE).getLong("focus_session_until",0);
      return String.valueOf(Math.max(0,until-System.currentTimeMillis()));
    }
    @JavascriptInterface public String alarmEventHistory(){
      return AlarmHistory.read(MainActivity.this);
    }
    @JavascriptInterface public boolean updateFinanceLauncherSummary(
        String username,boolean visible,long balance,long income,long expense){
      if(!canAccessSnapshot(username))return false;
      // Opt-in explícito. Un saldo persistido en pantalla HOME se considera sensible.
      // No almacenar el nombre de usuario ni IDs financieros en las preferencias del launcher.
      android.content.SharedPreferences prefs=getSharedPreferences("ns_finance_launcher",MODE_PRIVATE);
      prefs.edit().putBoolean("enabled",visible)
        .putLong("balance",visible?balance:0).putLong("income",visible?income:0)
        .putLong("expense",visible?expense:0).apply();
      return true;
    }
    @JavascriptInterface public void updateWidgetNextAlarm(String when){
      TodayWidget.setNextAlarm(MainActivity.this,when);
    }
    @JavascriptInterface public void updateWidget(int pending){
      TodayWidget.refresh(MainActivity.this,Math.max(0,Math.min(pending,9999)));
    }
    @JavascriptInterface public String encryptPersonalBackup(String data,String password){
      try { return BackupCrypto.encrypt(data,password); }
      catch(Exception e) { android.util.Log.w("NeuroSync","No se pudo cifrar el respaldo",e); return ""; }
    }
    @JavascriptInterface public String decryptPersonalBackup(String data,String password){
      try { return BackupCrypto.decrypt(data,password); }
      catch(Exception e) { android.util.Log.w("NeuroSync","Error descifrando respaldo",e); return ""; }
    }
    @JavascriptInterface public void exportBackup(String data){
      if(data==null||data.length()>2000000)return;
      try{
        JSONObject check=new JSONObject(data);
        if(!"neurosync-encrypted-backup".equals(check.optString("format")) || check.optInt("version")!=1)return;
      }catch(Exception ex){return;}
      runOnUiThread(()->{
        backupPending=data;
        Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_TITLE,"NeuroSync-respaldo-cifrado.json");
        startActivityForResult(intent,192);
      });
    }
    @JavascriptInterface public void importBackup(){
      runOnUiThread(()->{
        Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        startActivityForResult(intent,193);
      });
    }
    @JavascriptInterface public void chooseAlarmSound(){
      runOnUiThread(()->{
        try{
          Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT);
          intent.addCategory(Intent.CATEGORY_OPENABLE);
          intent.setType("audio/*");
          intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
          startActivityForResult(intent,191);
        }catch(Exception e){android.util.Log.e("NeuroSync","Selector de sonidos no disponible",e);}
      });
    }
    @JavascriptInterface public boolean testAlarm(){
      return AlarmReceiver.schedule(MainActivity.this,"test_neurosync","Prueba de alarma",System.currentTimeMillis()+60000L,false);
    }
    /** Los identificadores nativos de salida llevan una cuenta verificada. */
    private String departureKey(String username,String id){
      if(!canAccessSnapshot(username)||id==null||id.length()<2||id.length()>90)return null;
      return username+"::"+id;
    }
    @JavascriptInterface public boolean scheduleDepartureList(String username,String id,String title,String itemsJson,long when,String repeat){
      String key=departureKey(username,id);
      return key!=null&&DepartureReminderReceiver.schedule(MainActivity.this,key,title,itemsJson,when,repeat);
    }
    @JavascriptInterface public void cancelDepartureList(String username,String id){
      String key=departureKey(username,id);
      if(key!=null)DepartureReminderReceiver.cancel(MainActivity.this,key);
    }
    @JavascriptInterface public String departureListState(String username,String id){
      String key=departureKey(username,id);
      if(key==null)return "{}";
      try{
        JSONObject data=DepartureReminderReceiver.get(MainActivity.this,key);
        if(key.equals(data.optString("id")))data.put("id",id);
        return data.toString();
      }catch(Exception ignored){return "{}";}
    }
    @JavascriptInterface public boolean markDepartureItem(String username,String id,int index,boolean checked){
      String key=departureKey(username,id);
      return key!=null&&DepartureReminderReceiver.mark(MainActivity.this,key,index,checked);
    }
    @JavascriptInterface public void openDepartureChecklist(String username,String id){
      String key=departureKey(username,id);
      if(key==null||DepartureReminderReceiver.get(MainActivity.this,key).optJSONArray("items")==null)return;
      runOnUiThread(()->startActivity(new Intent(MainActivity.this,DepartureChecklistActivity.class).putExtra("id",key)));
    }
    @JavascriptInterface public boolean scheduleAlarmRepeat(String id,String title,long when,String repeat){
      try{return AlarmReceiver.scheduleRepeat(MainActivity.this,id,title,when,repeat);}
      catch(Exception e){return false;}
    }
    @JavascriptInterface public boolean scheduleAlarm(String id,String title,long when,boolean daily){
      try{return AlarmReceiver.schedule(MainActivity.this,id,title,when,daily);}
      catch(Exception e){return false;}
    }
    @JavascriptInterface public void cancelAlarm(String id){AlarmReceiver.cancel(MainActivity.this,id);}
    @JavascriptInterface public void logout(){forgetNativeSession();}
  }
  @Override public void onBackPressed(){
    if(web.canGoBack())web.goBack();else new AlertDialog.Builder(this).setMessage("¿Cerrar NeuroSync?")
      .setNegativeButton("Cancelar",null).setPositiveButton("Salir",(d,w)->finish()).show();
  }
  @Override public void onDestroy(){if(googleAccounts!=null)googleAccounts.shutdown();networkWorker.shutdownNow();if(web!=null)web.destroy();super.onDestroy();}
}