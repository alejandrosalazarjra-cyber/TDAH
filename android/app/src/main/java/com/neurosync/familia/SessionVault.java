package com.neurosync.familia;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import org.json.JSONObject;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** Session data encrypted with a hardware-backed key when Android provides one.
 * Never exposes the bearer token to JavaScript or Android WebView localStorage.
 */
public final class SessionVault {
  private static final String ALIAS="NeuroSyncFamilySession";
  private static final String PREF="ns_secure_session";
  private SessionVault(){}
  private static SecretKey key() throws Exception {
    KeyStore ks=KeyStore.getInstance("AndroidKeyStore");ks.load(null);
    SecretKey found=(SecretKey)ks.getKey(ALIAS,null);
    if(found!=null)return found;
    KeyGenerator gen=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
    gen.init(new KeyGenParameterSpec.Builder(ALIAS,
        KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT)
        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
        .setKeySize(256).build());
    return gen.generateKey();
  }
  private static SharedPreferences prefs(Context ctx){
    return ctx.getSharedPreferences(PREF,Context.MODE_PRIVATE);
  }
  public static synchronized boolean save(Context ctx,String token,JSONObject user,String mode,String location){
    if(token==null||token.length()<30||user==null||mode==null||location==null)return false;
    try{
      JSONObject data=new JSONObject().put("token",token).put("user",user)
        .put("mode",mode).put("location",location);
      Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.ENCRYPT_MODE,key());
      byte[] encrypted=cipher.doFinal(data.toString().getBytes(StandardCharsets.UTF_8));
      boolean stored=prefs(ctx).edit().putString("iv",Base64.encodeToString(cipher.getIV(),Base64.NO_WRAP))
        .putString("ciphertext",Base64.encodeToString(encrypted,Base64.NO_WRAP)).commit();
      if(!stored)android.util.Log.w("NeuroSync","El guardado de la sesión no se confirmó");
      return stored;
    }catch(Exception e){android.util.Log.e("NeuroSync","No se pudo proteger la sesión",e);return false;}
  }
  public static synchronized JSONObject restore(Context ctx,String mode,String location){
    try{
      String iv=prefs(ctx).getString("iv",""),encoded=prefs(ctx).getString("ciphertext","");
      if(iv.isEmpty()||encoded.isEmpty())return null;
      Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,Base64.decode(iv,Base64.NO_WRAP)));
      JSONObject result=new JSONObject(new String(cipher.doFinal(Base64.decode(encoded,Base64.NO_WRAP)),StandardCharsets.UTF_8));
      if(!mode.equals(result.optString("mode"))||!location.equals(result.optString("location")))return null;
      return result;
    }catch(Exception e){
      clear(ctx);return null;
    }
  }
  public static synchronized void clear(Context ctx){prefs(ctx).edit().clear().commit();}
}