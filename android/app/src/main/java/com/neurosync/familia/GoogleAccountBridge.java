package com.neurosync.familia;

import android.app.Activity;
import android.accounts.Account;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import org.json.JSONObject;
import org.json.JSONArray;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Autenticación de Google respaldada por Play Services. 
 * Requiere registrar aplicación Android y SHA-1 de su firma en Google Cloud;
 * no se solicitan contraseñas de Google dentro de NeuroSync.
 * Los respaldos se guardan en appDataFolder, nunca en toda la unidad.
 */
final class GoogleAccountBridge {
 static final int REQUEST_SIGNIN=720;
 private static final String SCOPE="https://www.googleapis.com/auth/drive.appdata";
 private static final String FILE="neurosync-copia-personal-v1.json";
 private final MainActivity activity;
 private final ExecutorService io=Executors.newSingleThreadExecutor();
 private final Handler main=new Handler(Looper.getMainLooper());
 private final GoogleSignInClient client;
 private volatile boolean busy=false;
 private String pendingBackup=null; // Última versión pendiente, sin perder cambios mientras se sube otra.

 GoogleAccountBridge(MainActivity activity){
  this.activity=activity;
  GoogleSignInOptions opts=new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
   .requestEmail().requestScopes(new Scope(SCOPE)).build();
  client=GoogleSignIn.getClient(activity,opts);
 }
 private void emit(String func,String value){
  main.post(()->activity.googleCallback(func,value));
 }
 private JSONObject user(GoogleSignInAccount a) throws Exception {
  String id=a.getId();
  if(id==null||id.isEmpty())throw new IllegalStateException("Identidad de Google incompleta");
  return new JSONObject().put("username","google_"+id).put("role","user")
   .put("google",true).put("email",a.getEmail()==null?"":a.getEmail())
   .put("displayName",a.getDisplayName()==null?"":a.getDisplayName());
 }
 JSONObject restored(){
  try {
   GoogleSignInAccount a=GoogleSignIn.getLastSignedInAccount(activity);
   return a==null?new JSONObject().put("ok",false):
    new JSONObject().put("ok",true).put("user",user(a));
  }catch(Exception e){return new JSONObject();}
 }
 void login(){activity.startActivityForResult(client.getSignInIntent(),REQUEST_SIGNIN);}
 void onResult(Intent data){
  Task<GoogleSignInAccount> task=GoogleSignIn.getSignedInAccountFromIntent(data);
  try{
   GoogleSignInAccount a=task.getResult(ApiException.class);
   emit("neuroGoogleLogin",new JSONObject().put("ok",true).put("user",user(a)).toString());
   restoreDrive();
  }catch(Exception ex){
   Log.w("NeuroSync","Error de acceso Google",ex);
   int code=ex instanceof ApiException?((ApiException)ex).getStatusCode():-1;
   String problem=code==10
     ?"La conexión Google de esta compilación no está registrada. Es una configuración del desarrollador, no un error de tu cuenta."
     :code==7?"No se pudo contactar con Google. Comprueba tu conexión e inténtalo nuevamente."
     :code==12501?"Cancelaste el inicio de sesión. Puedes volver a intentarlo."
     :"No se pudo completar el inicio de sesión. Código de Google: "+code;
   try{
    emit("neuroGoogleLogin",new JSONObject().put("ok",false)
      .put("error",problem).put("code",code).put("configurationRequired",code==10).toString());
   }catch(org.json.JSONException ignored){emit("neuroGoogleLogin","{}");}
  }
 }
 void logout(){
  // No revocar la identidad hasta que terminen las copias ya aceptadas por la cola.
  // El hilo de IO no abre pantallas: signOut se entrega al hilo principal.
  try{io.execute(()->main.post(()->client.signOut()));}
  catch(java.util.concurrent.RejectedExecutionException ignored){main.post(()->client.signOut());}
 }
 private GoogleSignInAccount account() throws Exception {
  GoogleSignInAccount a=GoogleSignIn.getLastSignedInAccount(activity);
  if(a==null||a.getEmail()==null||a.getId()==null)throw new Exception("Inicia sesión con Google");
  return a;
 }
 private String token(GoogleSignInAccount a) throws Exception {
  return GoogleAuthUtil.getToken(activity,new Account(a.getEmail(),"com.google"),"oauth2:"+SCOPE);
 }
 private static String request(String method,String url,String token,String type,byte[] body) throws Exception {
  HttpURLConnection connection=(HttpURLConnection)new URL(url).openConnection();
  try{
   connection.setRequestMethod("PATCH".equals(method)?"POST":method);
   if("PATCH".equals(method))connection.setRequestProperty("X-HTTP-Method-Override","PATCH");
   connection.setConnectTimeout(12000);connection.setReadTimeout(18000);
   connection.setRequestProperty("Authorization","Bearer "+token);
   connection.setRequestProperty("Accept","application/json");
   if(body!=null){
    connection.setDoOutput(true);
    connection.setRequestProperty("Content-Type",type);
    connection.setFixedLengthStreamingMode(body.length);
    try(OutputStream out=connection.getOutputStream()){out.write(body);}
   }
   int status=connection.getResponseCode();
   InputStream in=status>=400?connection.getErrorStream():connection.getInputStream();
   if(in==null)throw new IOException("Respuesta vacía de Drive: "+status);
   ByteArrayOutputStream bytes=new ByteArrayOutputStream();
   byte[] buffer=new byte[8192];int n;
   try(InputStream input=in){while((n=input.read(buffer))!=-1){
    bytes.write(buffer,0,n);
    if(bytes.size()>2500000)throw new IOException("Respaldo demasiado grande");
   }}
   String result=bytes.toString("UTF-8");
   if(status>=400)throw new IOException("Google Drive HTTP "+status+": "+result.substring(0,Math.min(200,result.length())));
   return result;
  }finally{connection.disconnect();}
 }
 private String fileId(String token) throws Exception {
  String q=URLEncoder.encode("name = '"+FILE+"' and 'appDataFolder' in parents and trashed = false","UTF-8");
  JSONObject list=new JSONObject(request("GET","https://www.googleapis.com/drive/v3/files?spaces=appDataFolder&fields=files(id,name)&q="+q,
   token,null,null));
  JSONArray files=list.optJSONArray("files");
  return files!=null&&files.length()>0?files.getJSONObject(0).getString("id"):null;
 }
 /** Una única subida activa y una pendiente (siempre la más reciente).
  * La petición anterior no descarta cambios realizados durante la subida.
  */
 void backup(String json){
  if(json==null||json.length()>1200000){
   emit("neuroDriveStatus","Respaldo de Drive demasiado grande; conserva una copia local.");
   return;
  }
  synchronized(this){
   pendingBackup=json;
   if(busy)return;
   busy=true;
  }
  try{
   io.execute(()->{
    while(true){
     String copy;
     synchronized(GoogleAccountBridge.this){
      copy=pendingBackup;
      pendingBackup=null;
      if(copy==null){busy=false;return;}
     }
     try{
      uploadBackup(copy);
      emit("neuroDriveStatus","Copia de Drive actualizada");
     }catch(Exception ex){
      Log.w("NeuroSync","No se pudo respaldar en Drive",ex);
      // Conservar la última copia en memoria hasta otro intento explícito.
      // No reintentar en bucle ni guardar información personal sin cifrar
      // en nuevas preferencias persistentes.
      synchronized(GoogleAccountBridge.this){
       if(pendingBackup==null)pendingBackup=copy;
       busy=false;
      }
      emit("neuroDriveStatus","Respaldo local guardado; Drive pendiente de conexión. Se reintentará al guardar.");
      return;
     }
    }
   });
  }catch(java.util.concurrent.RejectedExecutionException ex){
   synchronized(this){busy=false;}
   emit("neuroDriveStatus","Drive no disponible; respaldo local conservado");
  }
 }
 private void uploadBackup(String json) throws Exception {
  GoogleSignInAccount a=account();String access=token(a);
  JSONObject payload=new JSONObject(json);
  if(!a.getId().equals(payload.optString("googleId")))throw new IOException("La copia no corresponde a la cuenta activa");
  String id=fileId(access);
  if(id==null){
   String boundary="ns"+Long.toHexString(System.nanoTime());
   String metadata=new JSONObject().put("name",FILE).put("parents",new JSONArray().put("appDataFolder")).toString();
   ByteArrayOutputStream body=new ByteArrayOutputStream();
   body.write(("--"+boundary+"\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n"+metadata+
    "\r\n--"+boundary+"\r\nContent-Type: application/json\r\n\r\n").getBytes(StandardCharsets.UTF_8));
   body.write(json.getBytes(StandardCharsets.UTF_8));
   body.write(("\r\n--"+boundary+"--\r\n").getBytes(StandardCharsets.UTF_8));
   request("POST","https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart&fields=id",
     access,"multipart/related; boundary="+boundary,body.toByteArray());
  }else{
   request("PATCH","https://www.googleapis.com/upload/drive/v3/files/"+URLEncoder.encode(id,"UTF-8")+"?uploadType=media",
     access,"application/json; charset=UTF-8",json.getBytes(StandardCharsets.UTF_8));
  }
 }

 void restoreDrive(){
  io.execute(()->{
   try{
    GoogleSignInAccount a=account();String access=token(a);String id=fileId(access);
    if(id==null){emit("neuroDriveStatus","Drive vinculado; se creará la primera copia al guardar");return;}
    String json=request("GET","https://www.googleapis.com/drive/v3/files/"+URLEncoder.encode(id,"UTF-8")+"?alt=media",
      access,null,null);
    JSONObject data=new JSONObject(json);
    if(!a.getId().equals(data.optString("googleId")))throw new IOException("Cuenta del respaldo no coincide");
    emit("neuroGoogleBackupRestored",json);
   }catch(Exception ex){
    Log.w("NeuroSync","No se pudo leer copia Drive",ex);
    emit("neuroDriveStatus","No se pudo consultar Drive. Revisa permisos e internet");
   }
  });
 }
 void shutdown(){io.shutdownNow();}
}
