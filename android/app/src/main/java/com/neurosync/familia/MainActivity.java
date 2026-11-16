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
  private WebView web;
  private final ExecutorService networkWorker=Executors.newSingleThreadExecutor();
  private volatile String sessionToken="";
  private volatile JSONObject sessionUser=null;
  @Override public void onCreate(Bundle state){
    super.onCreate(state);
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
    if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
      requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},11);
  }
  private boolean localHost(){return "host".equals(getPreferences(0).getString("mode",""));}
  private JSONObject remote(String path,JSONObject request){
    HttpURLConnection c=null;
    try{
      String ip=getPreferences(0).getString("server","");
      if(!validPrivateIp(ip))return AccountStore.fail("Configura una IP privada válida");
      URL url=new URL("http://"+ip+":"+SyncService.PORT+path);
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
    }catch(Exception e){return AccountStore.fail("No se pudo conectar al celular principal. Comprueba Tailscale y que el servidor esté activo.");}
    finally{if(c!=null)c.disconnect();}
  }
  private JSONObject call(String path,JSONObject args){
    if(sessionUser==null)return AccountStore.fail("Inicia sesión");
    if(!localHost())return remote(path,args);
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
  private void startServer(){runOnUiThread(()->{
    try{startForegroundService(new Intent(this,SyncService.class));}
    catch(Exception e){android.util.Log.w("NeuroSync","No se pudo iniciar servicio: "+e.getMessage());}
  });}
  public final class NativeBridge {
    @JavascriptInterface public String configureHost(){
      if(!localHost()){
        getPreferences(0).edit().putString("mode","host").remove("server").apply();
        sessionToken="";sessionUser=null;
      }
      startServer();
      return success("Celular principal activo. Comparte la IP privada de Tailscale con tu familia.").toString();
    }
    @JavascriptInterface public String configureRemote(String ip){
      if(!validPrivateIp(ip))return AccountStore.fail("Usa la IP privada de Tailscale (100.x.x.x) o Wi-Fi").toString();
      getPreferences(0).edit().putString("mode","client").putString("server",ip).apply();
      sessionToken="";sessionUser=null;
      runOnUiThread(()->stopService(new Intent(MainActivity.this,SyncService.class)));
      return success("Conectado al servidor "+ip+". Inicia sesión con tu usuario familiar.").toString();
    }
    @JavascriptInterface public String login(String username,String password){
      String mode=getPreferences(0).getString("mode","");
      if(mode.isEmpty())return AccountStore.fail("Primero activa tu teléfono como administrador o vincúlalo a uno principal").toString();
      JSONObject result=localHost()?AccountStore.authenticate(MainActivity.this,username,password)
        :remote("/login",payload("username",username,"password",password));
      if(result.optBoolean("ok")){
        sessionToken=result.optString("token","");
        sessionUser=result.optJSONObject("user");
        // El aviso de contraseña inicial se gestiona en la interfaz.
        result.remove("token"); // El secreto de sesión permanece exclusivamente en Java.
      }
      return result.toString();
    }
    @JavascriptInterface public String getUsers(){return call("/users",new JSONObject()).toString();}
    @JavascriptInterface public String createAccount(String username,String password){
      return call("/create-user",payload("username",username,"password",password)).toString();
    }
    @JavascriptInterface public String changePassword(String current,String next){
      return call("/change-password",payload("current",current,"next",next)).toString();
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
    @JavascriptInterface public String alarmPermissions(){
      try{
        JSONObject j=new JSONObject();
        NotificationManager n=getSystemService(NotificationManager.class);
        boolean notifications=Build.VERSION.SDK_INT<33 || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED;
        boolean exact=Build.VERSION.SDK_INT<31 || ((AlarmManager)getSystemService(ALARM_SERVICE)).canScheduleExactAlarms();
        boolean full=Build.VERSION.SDK_INT<34 || n.canUseFullScreenIntent();
        return j.put("notifications",notifications).put("exact",exact).put("fullScreen",full).toString();
      }catch(Exception e){return "{}";}
    }
    @JavascriptInterface public void requestAlarmPermissions(){
      runOnUiThread(()->{
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){
          requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},101);return;
        }
        if(Build.VERSION.SDK_INT>=31 && !((AlarmManager)getSystemService(ALARM_SERVICE)).canScheduleExactAlarms()){
          try{startActivity(new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
            android.net.Uri.parse("package:"+getPackageName())));}catch(Exception e){openAppSettings();}
          return;
        }
        if(Build.VERSION.SDK_INT>=34 && !getSystemService(NotificationManager.class).canUseFullScreenIntent()){
          try{startActivity(new Intent(android.provider.Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
             android.net.Uri.parse("package:"+getPackageName())));}catch(Exception e){openAppSettings();}
          return;
        }
        new AlertDialog.Builder(MainActivity.this).setTitle("Alarmas listas")
          .setMessage("Los permisos principales están habilitados. Crea una alarma de prueba para verificar que suene.")
          .setPositiveButton("Entendido",null).show();
      });
    }
    @JavascriptInterface public void openSystemClock(){
      runOnUiThread(()->{
        try{
          Intent i=new Intent(android.provider.AlarmClock.ACTION_SET_ALARM);
          i.putExtra(android.provider.AlarmClock.EXTRA_MESSAGE,"NeuroSync");
          i.putExtra(android.provider.AlarmClock.EXTRA_SKIP_UI,false);
          startActivity(i);
        }catch(Exception e){
          new AlertDialog.Builder(MainActivity.this).setMessage(
           "No se encontró una aplicación de reloj compatible. Instala Reloj de Google para elegir música de YouTube Music si está disponible.")
           .setPositiveButton("Aceptar",null).show();
        }
      });
    }
    @JavascriptInterface public boolean testAlarm(){
      return AlarmReceiver.schedule(MainActivity.this,"test_neurosync","Prueba de alarma",System.currentTimeMillis()+60000L,false);
    }
    @JavascriptInterface public boolean scheduleAlarm(String id,String title,long when,boolean daily){
      try{return AlarmReceiver.schedule(MainActivity.this,id,title,when,daily);}
      catch(Exception e){return false;}
    }
    @JavascriptInterface public void cancelAlarm(String id){AlarmReceiver.cancel(MainActivity.this,id);}
    @JavascriptInterface public void logout(){sessionUser=null;sessionToken="";}
  }
  @Override public void onBackPressed(){
    if(web.canGoBack())web.goBack();else new AlertDialog.Builder(this).setMessage("¿Cerrar NeuroSync?")
      .setNegativeButton("Cancelar",null).setPositiveButton("Salir",(d,w)->finish()).show();
  }
  @Override public void onDestroy(){networkWorker.shutdownNow();if(web!=null)web.destroy();super.onDestroy();}
}