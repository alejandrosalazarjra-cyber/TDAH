package com.neurosync.familia;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import org.json.JSONArray;
import org.json.JSONObject;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.concurrent.ConcurrentHashMap;

/** Gestión local de cuentas en el servidor principal. Nunca almacena contraseñas en claro.
 * admin1234 es exclusivamente la clave inicial para configurar el sistema;
 * la interfaz solicita cambiarla y las cuentas familiares necesitan una clave propia.
 */
public final class AccountStore {
  private static final SecureRandom RANDOM=new SecureRandom();
  private static final ConcurrentHashMap<String,String> TOKENS=new ConcurrentHashMap<>();
  private static final Object LOCK=new Object();
  private static final int MAX_ATTEMPTS=5;
  private static final long LOCK_DURATION=15*60*1000L;
  private static SharedPreferences attempts(Context c){return c.getSharedPreferences("ns_login_guard",Context.MODE_PRIVATE);}
  private static String guardKey(String name){return "user_"+name;}
  private static boolean locked(Context c,String name){
    return attempts(c).getLong(guardKey(name)+"_until",0)>System.currentTimeMillis();
  }
  private static void badAttempt(Context c,String name){
    SharedPreferences a=attempts(c);
    int failures=a.getInt(guardKey(name)+"_fails",0)+1;
    SharedPreferences.Editor edit=a.edit().putInt(guardKey(name)+"_fails",failures);
    if(failures>=MAX_ATTEMPTS)edit.putLong(guardKey(name)+"_until",System.currentTimeMillis()+LOCK_DURATION);
    edit.apply();
  }
  private static void clearAttempts(Context c,String name){
    attempts(c).edit().remove(guardKey(name)+"_fails").remove(guardKey(name)+"_until").apply();
  }
  private static boolean defaultAdminPassword(JSONObject user)throws Exception{
    return user!=null&&"administrador".equals(user.optString("username"))&&
      MessageDigest.isEqual(hash("admin1234",user.optString("salt")).getBytes(StandardCharsets.UTF_8),
      user.optString("hash").getBytes(StandardCharsets.UTF_8));
  }

  private AccountStore(){}
  private static SharedPreferences prefs(Context c){return c.getSharedPreferences("ns_accounts",Context.MODE_PRIVATE);}
  private static String random(int bytes){byte[] raw=new byte[bytes];RANDOM.nextBytes(raw);return Base64.encodeToString(raw,Base64.NO_WRAP|Base64.URL_SAFE);}
  private static String hash(String password,String salt)throws Exception{
    byte[] raw=Base64.decode(salt,Base64.URL_SAFE|Base64.NO_WRAP);
    PBEKeySpec spec=new PBEKeySpec(password.toCharArray(),raw,180000,256);
    try{return Base64.encodeToString(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(),Base64.NO_WRAP);}
    finally{spec.clearPassword();}
  }
  private static JSONArray users(Context c)throws Exception{
    String saved=prefs(c).getString("users","");
    if(saved.isEmpty()){
      JSONArray a=new JSONArray();a.put(make("administrador","admin1234","admin"));
      prefs(c).edit().putString("users",a.toString()).commit();
      return a;
    }
    return new JSONArray(saved);
  }
  private static JSONObject make(String user,String password,String role)throws Exception{
    String salt=random(18);
    return new JSONObject().put("username",user).put("role",role).put("salt",salt)
      .put("hash",hash(password,salt)).put("active",true);
  }
  private static SharedPreferences sessions(Context ctx){
    return ctx.getSharedPreferences("ns_local_session_index",Context.MODE_PRIVATE);
  }
  private static String tokenHash(String token)throws Exception{
    byte[] dig=MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
    return Base64.encodeToString(dig,Base64.NO_WRAP);
  }
  public static void revoke(Context ctx,String token){
    try{
      String username=TOKENS.remove(token);
      if(username==null){
        JSONObject user=identity(ctx,token);
        username=user==null?null:user.optString("username");
      }
      if(username!=null){
        String hash=sessions(ctx).getString("token_"+username,"");
        if(MessageDigest.isEqual(hash.getBytes(StandardCharsets.UTF_8),
          tokenHash(token).getBytes(StandardCharsets.UTF_8))){
          sessions(ctx).edit().remove("token_"+username).apply();
        }
      }
    }catch(Exception ignored){}
  }
  private static JSONObject find(JSONArray users,String username){
    for(int i=0;i<users.length();i++){JSONObject u=users.optJSONObject(i);if(u!=null&&username.equals(u.optString("username")))return u;}return null;
  }
  private static String norm(String user){return user==null?"":user.trim().toLowerCase(java.util.Locale.ROOT);}
  public static JSONObject authenticate(Context c,String username,String password){
    try{
      synchronized(LOCK){
        String name=norm(username);
        if(!name.matches("[a-z0-9_\\-]{3,30}")||password==null||password.length()>160)return fail("Credenciales incorrectas");
        if(locked(c,name))return fail("Demasiados intentos. Intenta de nuevo dentro de 15 minutos.");
        JSONObject found=find(users(c),name);
        if(found==null||!found.optBoolean("active")){
          badAttempt(c,name);return fail("Credenciales incorrectas");
        }
        String actual=hash(password,found.optString("salt"));
        if(!MessageDigest.isEqual(actual.getBytes(StandardCharsets.UTF_8),found.optString("hash").getBytes(StandardCharsets.UTF_8))){
          badAttempt(c,name);return fail("Credenciales incorrectas");
        }
        clearAttempts(c,name);
        String token=random(32);TOKENS.put(token,name);
        sessions(c).edit().putString("token_"+name,tokenHash(token)).commit();
        return new JSONObject().put("ok",true).put("token",token).put("user",publicUser(found))
          .put("mustChangePassword",defaultAdminPassword(found));
      }
    }catch(Exception e){return fail("No se pudo iniciar sesión");}
  }
  public static JSONObject publicUser(JSONObject u)throws Exception{
    return new JSONObject().put("username",u.optString("username")).put("role",u.optString("role")).put("active",u.optBoolean("active",true));
  }
  public static JSONObject identity(Context c,String token){
    try{
      if(token==null||token.length()<30)return null;
      String username=TOKENS.get(token);
      if(username==null){
        JSONArray all=users(c);
        String hashed=tokenHash(token);
        for(int i=0;i<all.length();i++){
          JSONObject candidate=all.optJSONObject(i);
          if(candidate==null)continue;
          String check=sessions(c).getString("token_"+candidate.optString("username"),"");
          if(!check.isEmpty()&&MessageDigest.isEqual(check.getBytes(StandardCharsets.UTF_8),
            hashed.getBytes(StandardCharsets.UTF_8))){
            username=candidate.optString("username");
            TOKENS.put(token,username);break;
          }
        }
      }
      if(username==null)return null;
      JSONObject user=find(users(c),username);
      return user!=null&&user.optBoolean("active")?publicUser(user):null;
    }catch(Exception ex){return null;}
  }
  public static JSONObject list(Context c,JSONObject actor){
    try{
      if(actor==null||!"admin".equals(actor.optString("role")))return fail("Sin permisos");
      JSONArray result=new JSONArray(), all=users(c);
      for(int i=0;i<all.length();i++)result.put(publicUser(all.getJSONObject(i)));
      return new JSONObject().put("ok",true).put("users",result);
    }catch(Exception e){return fail("No se pudo consultar usuarios");}
  }
  public static JSONObject create(Context c,JSONObject actor,String username,String password){
    try{
      synchronized(LOCK){
        if(actor==null||!"admin".equals(actor.optString("role")))return fail("Solo el administrador puede crear cuentas");
        String name=norm(username);
        if(!name.matches("[a-z0-9_\\-]{3,30}"))return fail("Usuario: 3 a 30 caracteres, letras, números, guión o _");
        if(password==null||password.length()<10||password.length()>128)return fail("La contraseña debe tener entre 10 y 128 caracteres");
        JSONArray all=users(c);
        JSONObject admin=find(all,"administrador");
        if(defaultAdminPassword(admin))return fail("Cambia primero la contraseña provisional del administrador.");
        if(find(all,name)!=null)return fail("Ese usuario ya existe");
        all.put(make(name,password,"member"));
        prefs(c).edit().putString("users",all.toString()).commit();
        return new JSONObject().put("ok",true).put("user",name);
      }
    }catch(Exception e){return fail("Error creando la cuenta");}
  }
  public static JSONObject change(Context c,JSONObject actor,String previous,String next){
    try{
      synchronized(LOCK){
        if(actor==null)return fail("Inicia sesión");
        if(next==null||next.length()<10||next.length()>128)return fail("Contraseña nueva: mínimo 10 caracteres");
        JSONArray all=users(c);JSONObject user=find(all,actor.optString("username"));
        if(user==null)return fail("No encontrado");
        String expected=hash(previous,user.optString("salt"));
        if(!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),user.optString("hash").getBytes(StandardCharsets.UTF_8)))return fail("Contraseña actual incorrecta");
        JSONObject changed=make(user.optString("username"),next,user.optString("role"));
        user.put("hash",changed.optString("hash")).put("salt",changed.optString("salt"));
        prefs(c).edit().putString("users",all.toString()).commit();
        final String userName=user.optString("username");
        TOKENS.entrySet().removeIf(entry->userName.equals(entry.getValue()));
        sessions(c).edit().remove("token_"+userName).commit();
        return new JSONObject().put("ok",true);
      }
    }catch(Exception e){return fail("No se pudo cambiar la contraseña");}
  }
  public static JSONObject fail(String error){try{return new JSONObject().put("ok",false).put("error",error);}catch(Exception e){return new JSONObject();}}
}