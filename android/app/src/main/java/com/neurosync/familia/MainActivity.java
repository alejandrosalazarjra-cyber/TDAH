package com.neurosync.familia;

import android.Manifest;
import android.app.*;
import android.os.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.webkit.*;
import android.widget.*;
import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.concurrent.*;
import java.util.regex.Pattern;

public final class MainActivity extends Activity {
  private WebView web;
  private final ExecutorService io=Executors.newSingleThreadExecutor();
  private final Handler main=new Handler(Looper.getMainLooper());
  private final android.content.SharedPreferences.OnSharedPreferenceChangeListener unused=null;
  private String snapshot="{\"tasks\":[],\"packs\":[]}";
  @Override public void onCreate(Bundle state){
    super.onCreate(state);
    getWindow().setStatusBarColor(Color.rgb(5,5,10));
    getWindow().setNavigationBarColor(Color.rgb(5,5,10));
    web=new WebView(this);
    setContentView(web);
    web.getSettings().setJavaScriptEnabled(true);
    web.getSettings().setDomStorageEnabled(true);
    web.getSettings().setAllowFileAccess(false);
    web.getSettings().setAllowContentAccess(false);
    web.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
    web.setWebChromeClient(new WebChromeClient());
    web.setWebViewClient(new WebViewClient(){
      @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest request){return !request.getUrl().toString().startsWith("https://appassets.androidplatform.net/");}
    });
    web.addJavascriptInterface(new NativeBridge(),"AndroidBridge");
    try(InputStream input=getAssets().open("index.html")){
      ByteArrayOutputStream bytes=new ByteArrayOutputStream();
      input.transferTo(bytes);
      web.loadDataWithBaseURL("https://appassets.androidplatform.net/index.html",bytes.toString("UTF-8"),"text/html","UTF-8",null);
    }catch(Exception ex){new AlertDialog.Builder(this).setMessage("No se encontró index.html en el APK").show();}
    snapshot=getPreferences(0).getString("local_snapshot",snapshot);
    String mode=getPreferences(0).getString("mode","");
    if(mode.equals("host"))startServer();
    main.postDelayed(new Runnable(){public void run(){sync();main.postDelayed(this,60000);}},15000);
    if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
      requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},25);
  }
  public final class NativeBridge {
    @JavascriptInterface public void configure(){runOnUiThread(()->chooseMode());}
    @JavascriptInterface public void familyChanged(String value){
      if(value==null||value.length()>500000)return;
      try{JSONObject parsed=new JSONObject(value);if(parsed.optJSONArray("tasks")==null||parsed.optJSONArray("packs")==null)return;}catch(Exception e){return;}
      synchronized(MainActivity.this){snapshot=value;getPreferences(0).edit().putString("local_snapshot",value).apply();}
      sync();
    }
    @JavascriptInterface public void syncNow(){sync();}
  }
  private void chooseMode(){
    new AlertDialog.Builder(this).setTitle("Sincronización familiar")
      .setItems(new String[]{"Este es mi celular principal (servidor)","Conectar como mamá o hermano","Sincronizar ahora","Ver configuración actual"},(dialog,which)->{
        if(which==0)enableHost();else if(which==1)configureClient();else if(which==2)sync();else showInfo();
      }).show();
  }
  private void enableHost(){
    new AlertDialog.Builder(this).setTitle("Celular principal")
      .setMessage("Esta opción deja tu teléfono como fuente central. Para compartir fuera de casa necesitas instalar y conectar Tailscale en los tres celulares. No publiques el puerto 8765 en internet.")
      .setPositiveButton("Activar",(d,w)->{
        String secret=getPreferences(0).getString("key","");
        if(secret.length()<32){
          byte[] raw=new byte[24];new SecureRandom().nextBytes(raw);
          secret=android.util.Base64.encodeToString(raw,android.util.Base64.NO_WRAP|android.util.Base64.URL_SAFE);
        }
        getPreferences(0).edit().putString("key",secret).putString("mode","host").apply();
        getSharedPreferences("neurosync_sync",0).edit().putString("secret",secret).apply();
        startServer();sync();showInfo();
      }).setNegativeButton("Cancelar",null).show();
  }
  private void configureClient(){
    LinearLayout inputs=new LinearLayout(this);inputs.setOrientation(LinearLayout.VERTICAL);inputs.setPadding(28,12,28,0);
    EditText host=new EditText(this);host.setSingleLine(true);host.setHint("IP Tailscale del celular principal");host.setText(getPreferences(0).getString("server",""));inputs.addView(host);
    EditText secret=new EditText(this);secret.setSingleLine(true);secret.setHint("Código privado de familia");secret.setText(getPreferences(0).getString("key",""));inputs.addView(secret);
    new AlertDialog.Builder(this).setTitle("Conectar a la familia").setView(inputs)
      .setPositiveButton("Guardar",(d,w)->{
        String address=host.getText().toString().trim(),code=secret.getText().toString().trim();
        if(!validPrivateIp(address)||code.length()<32){showMessage("Usa una IP privada de Tailscale o Wi-Fi y un código válido de 32 caracteres o más.");return;}
        getPreferences(0).edit().putString("mode","client").putString("server",address).putString("key",code).apply();
        stopService(new Intent(this,SyncService.class));
        sync();showMessage("Configuración guardada. La sincronización se ejecuta al abrir y periódicamente.");
      }).setNegativeButton("Cancelar",null).show();
  }
  private boolean validPrivateIp(String host){
    if(!host.matches("(?:\\d{1,3}\\.){3}\\d{1,3}"))return false;
    String[] part=host.split("\\.");int[] v=new int[4];
    try{for(int i=0;i<4;i++){v[i]=Integer.parseInt(part[i]);if(v[i]>255)return false;}}catch(Exception e){return false;}
    return v[0]==10||(v[0]==192&&v[1]==168)||(v[0]==172&&v[1]>=16&&v[1]<=31)||(v[0]==100&&v[1]>=64&&v[1]<=127);
  }
  private void startServer(){
    try{startForegroundService(new Intent(this,SyncService.class));}
    catch(Exception e){showMessage("No se pudo iniciar el servidor: "+e.getMessage());}
  }
  private void showInfo(){
    String mode=getPreferences(0).getString("mode","");
    String secret=getPreferences(0).getString("key","");
    String message=mode.equals("host")?"Servidor principal activo en el puerto 8765. Busca tu IP 100.x.x.x en Tailscale y comparte esa IP y este código solo con tu mamá y hermano:\n\n"+secret:
      mode.equals("client")?"Modo familiar conectado a "+getPreferences(0).getString("server","")+" (sincronización al abrir).":"Aún no configurado.";
    new AlertDialog.Builder(this).setTitle("Estado de sincronización").setMessage(message).setPositiveButton("Entendido",null).show();
  }
  private void showMessage(String text){runOnUiThread(()->new AlertDialog.Builder(this).setMessage(text).setPositiveButton("OK",null).show());}
  private void sync(){
    String mode=getPreferences(0).getString("mode","");
    if(mode.isEmpty())return;
    io.execute(()->{
      try{
        String local; synchronized(this){local=snapshot;}
        JSONObject in=new JSONObject(local),out;
        if(mode.equals("host"))out=SyncStore.merge(this,in);
        else {
          String host=getPreferences(0).getString("server",""),key=getPreferences(0).getString("key","");
          if(!validPrivateIp(host))return;
          HttpURLConnection c=(HttpURLConnection)new URL("http://"+host+":"+SyncService.PORT+"/sync").openConnection();
          c.setRequestMethod("POST");c.setConnectTimeout(6000);c.setReadTimeout(10000);c.setDoOutput(true);
          c.setRequestProperty("Content-Type","application/json");c.setRequestProperty("X-Family-Key",key);
          byte[] body=in.toString().getBytes(StandardCharsets.UTF_8);c.setFixedLengthStreamingMode(body.length);
          try(OutputStream stream=c.getOutputStream()){stream.write(body);}
          if(c.getResponseCode()!=200)throw new IOException("Servidor respondió "+c.getResponseCode());
          try(InputStream stream=c.getInputStream()){out=new JSONObject(new String(stream.readAllBytes(),StandardCharsets.UTF_8));}
          c.disconnect();
        }
        String result=out.toString();
        synchronized(this){snapshot=result;getPreferences(0).edit().putString("local_snapshot",result).apply();}
        runOnUiThread(()->web.evaluateJavascript("window.familyApplyRemote && window.familyApplyRemote("+JSONObject.quote(result)+")",null));
      }catch(Exception e){android.util.Log.w("NeuroSync","Sincronización pendiente: "+e.getMessage());}
    });
  }
  @Override public void onDestroy(){io.shutdownNow();if(web!=null)web.destroy();super.onDestroy();}
}
