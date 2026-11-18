package com.neurosync.familia;

import android.content.Intent;
import android.util.Log;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Cliente público opcional. Ninguna credencial pasa al WebView.
 * Mantiene la conexión familiar de pruebas totalmente independiente.
 */
final class PublicAccountBridge {
  static final int SIGNIN_REQUEST=721;
  private final MainActivity activity;
  private final GoogleSignInClient google;
  private final ExecutorService worker=Executors.newSingleThreadExecutor();

  PublicAccountBridge(MainActivity activity){
    this.activity=activity;
    if(available()){
      GoogleSignInOptions options=new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestEmail().requestIdToken(BuildConfig.PUBLIC_GOOGLE_CLIENT_ID).build();
      google=GoogleSignIn.getClient(activity,options);
    }else google=null;
  }
  boolean available(){
    return BuildConfig.PUBLIC_API_BASE_URL.startsWith("https://") &&
      BuildConfig.PUBLIC_GOOGLE_CLIENT_ID.endsWith(".apps.googleusercontent.com");
  }
  private JSONObject failure(String message){
    try{return new JSONObject().put("ok",false).put("error",message);}
    catch(Exception ignored){return new JSONObject();}
  }
  private JSONObject request(String method,String path,JSONObject data,String token,String requestKey){
    if(!available())return failure("El servicio público aún no está habilitado en esta versión");
    if(path==null||!path.startsWith("/v1/")||
      path.contains("..")||path.contains("//")||path.contains("#")||
      !("GET".equals(method)||"POST".equals(method)||"PATCH".equals(method)))
      return failure("Solicitud inválida");
    HttpURLConnection connection=null;
    try{
      URL target=new URL(BuildConfig.PUBLIC_API_BASE_URL+path);
      if(!"https".equalsIgnoreCase(target.getProtocol()))return failure("HTTPS obligatorio");
      connection=(HttpURLConnection)target.openConnection();
      connection.setInstanceFollowRedirects(false);
      connection.setRequestMethod(method);
      connection.setConnectTimeout(5000);connection.setReadTimeout(8000);
      connection.setUseCaches(false);
      connection.setRequestProperty("Accept","application/json");
      if(token!=null&&!token.isEmpty())connection.setRequestProperty("Authorization","Bearer "+token);
      if(requestKey!=null&&!requestKey.isEmpty())connection.setRequestProperty("Idempotency-Key",requestKey);
      if(!"GET".equals(method)){
        byte[] raw=(data==null?"{}":data.toString()).getBytes(StandardCharsets.UTF_8);
        if(raw.length>16000)return failure("Solicitud demasiado grande");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type","application/json; charset=utf-8");
        connection.setFixedLengthStreamingMode(raw.length);
        try(OutputStream out=connection.getOutputStream()){out.write(raw);}
      }
      int status=connection.getResponseCode();
      if(status>=300&&status<400)return failure("Redirección de servidor no permitida");
      InputStream stream=status<400?connection.getInputStream():connection.getErrorStream();
      if(stream==null)return failure("El servidor no respondió");
      ByteArrayOutputStream result=new ByteArrayOutputStream();
      try(InputStream input=stream){
        byte[] bytes=new byte[4096];int read;
        while((read=input.read(bytes))!=-1){
          result.write(bytes,0,read);
          if(result.size()>150000)throw new IOException("Respuesta demasiado grande");
        }
      }
      JSONObject response=new JSONObject(result.toString("UTF-8"));
      if(status==401)PublicSessionVault.clear(activity);
      return response;
    }catch(Exception e){
      Log.w("NeuroSync","Solicitud pública no disponible: "+e.getClass().getSimpleName());
      return failure("No se pudo conectar con NeuroSync. Comprueba internet.");
    }finally{if(connection!=null)connection.disconnect();}
  }
  private JSONObject credentials(){
    return PublicSessionVault.restore(activity,"public",BuildConfig.PUBLIC_API_BASE_URL);
  }
  JSONObject restored(){
    if(!available())return failure("Servicio público no configurado");
    JSONObject saved=credentials();
    if(saved==null)return failure("Inicia sesión");
    JSONObject user=saved.optJSONObject("user");
    if(user==null||saved.optString("token").length()<40)return failure("Sesión inválida");
    return sessionUser(user);
  }
  private JSONObject sessionUser(JSONObject user){
    try{
      String id=user.optString("id","");
      if(!id.matches("[0-9a-fA-F-]{36}"))return failure("Identidad pública inválida");
      JSONObject normalized=new JSONObject()
        .put("username","public_"+id.replace("-","").toLowerCase(java.util.Locale.ROOT))
        .put("displayName",user.optString("display_name","Usuario"))
        .put("handle",user.optString("handle",""))
        .put("public",true).put("role","user");
      return new JSONObject().put("ok",true).put("user",normalized);
    }catch(Exception e){return failure("No se pudo recuperar el perfil");}
  }
  void login(){
    if(!available()||google==null){
      activity.googleCallback("neuroPublicLogin",failure("El servicio público todavía no está disponible").toString());return;
    }
    activity.startActivityForResult(google.getSignInIntent(),SIGNIN_REQUEST);
  }
  void onResult(Intent intent){
    try{
      GoogleSignInAccount account=GoogleSignIn.getSignedInAccountFromIntent(intent).getResult(ApiException.class);
      String idToken=account.getIdToken();
      if(idToken==null||idToken.isEmpty()){
        activity.googleCallback("neuroPublicLogin",failure("Google no entregó credenciales de identidad").toString());
        return;
      }
      worker.execute(()->{
        try{
          JSONObject reply=request("POST","/v1/auth/exchange",new JSONObject().put("idToken",idToken),"","");
          JSONObject user=reply.optJSONObject("user");
          String token=reply.optString("token","");
          if(!reply.optBoolean("ok")||user==null||token.length()<40){
            activity.googleCallback("neuroPublicLogin",failure(reply.optString("error","No se pudo iniciar sesión")).toString());return;
          }
          if(!PublicSessionVault.save(activity,token,user,"public",BuildConfig.PUBLIC_API_BASE_URL)){
            activity.googleCallback("neuroPublicLogin",failure("No se pudo proteger tu sesión en este teléfono").toString());return;
          }
          activity.googleCallback("neuroPublicLogin",sessionUser(user).toString());
        }catch(Exception e){
          activity.googleCallback("neuroPublicLogin",failure("No se pudo iniciar sesión con el servidor").toString());
        }
      });
    }catch(Exception e){
      activity.googleCallback("neuroPublicLogin",failure("No se pudo completar el acceso con Google").toString());
    }
  }
  JSONObject call(String method,String path,JSONObject data,String requestKey){
    JSONObject saved=credentials();
    if(saved==null)return failure("Inicia sesión en NeuroSync");
    String token=saved.optString("token","");
    if(token.length()<40)return failure("Sesión pública inválida");
    return request(method,path,data,token,requestKey);
  }
  JSONObject logout(){
    JSONObject saved=credentials();
    // Borrado local prioritario: ninguna llamada futura puede usar el token.
    PublicSessionVault.clear(activity);
    if(saved!=null){
      String token=saved.optString("token","");
      if(!token.isEmpty())worker.execute(()->request("POST","/v1/auth/logout",new JSONObject(),token,""));
    }
    try{return new JSONObject().put("ok",true);}catch(Exception e){return failure("Error");}
  }
  void shutdown(){worker.shutdownNow();}
}
