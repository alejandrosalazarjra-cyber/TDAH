package com.neurosync.familia;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.MessageDigest;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Copia secundaria cifrada de la información de NeuroSync.
 *
 * No sustituye WebView localStorage ni migra/borrra los datos existentes. 
 * Dos revisiones conservadas en SQLite, escritura transaccional y AES/GCM con AndroidKeyStore.
 * Clave privada no exportable; base aislada dentro del almacenamiento privado de la APK.
 * No almacenar aquí tokens OAuth o contraseñas.
 */
final class NativeSnapshotStore extends SQLiteOpenHelper {
  private static final String DATABASE="ns_local_recovery.db";
  private static final String KEY_ALIAS="NeuroSyncLocalRecoveryV1";
  private static final int MAX_BYTES=1600000;
  private static final String TABLE="snapshots";
  private static NativeSnapshotStore singleton;

  private NativeSnapshotStore(Context context){super(context.getApplicationContext(),DATABASE,null,1);}
  static synchronized NativeSnapshotStore get(Context context){
    if(singleton==null)singleton=new NativeSnapshotStore(context);
    return singleton;
  }

  @Override public void onCreate(SQLiteDatabase db){
    db.execSQL("CREATE TABLE "+TABLE+" (account TEXT NOT NULL, revision INTEGER NOT NULL, "+
      "nonce TEXT NOT NULL, ciphertext TEXT NOT NULL, saved_at INTEGER NOT NULL, "+
      "PRIMARY KEY(account,revision))");
  }
  @Override public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion){
    // Migraciones futuras: nunca borrar silenciosamente las copias existentes.
    if(oldVersion!=newVersion)throw new IllegalStateException("Migración de respaldos no disponible");
  }

  private static SecretKey key() throws Exception{
    KeyStore store=KeyStore.getInstance("AndroidKeyStore");
    store.load(null);
    SecretKey result=(SecretKey)store.getKey(KEY_ALIAS,null);
    if(result!=null)return result;
    KeyGenerator generator=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
    generator.init(new KeyGenParameterSpec.Builder(KEY_ALIAS,
        KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT)
      .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
      .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
      .setKeySize(256).build());
    return generator.generateKey();
  }
  private static String accountHash(String account) throws Exception{
    if(account==null||account.isEmpty()||account.length()>180)
      throw new IllegalArgumentException("Cuenta inválida");
    byte[] bytes=MessageDigest.getInstance("SHA-256").digest(account.getBytes(StandardCharsets.UTF_8));
    StringBuilder out=new StringBuilder();
    for(byte b:bytes)out.append(String.format(java.util.Locale.US,"%02x",b&255));
    return out.toString();
  }
  private static void validate(String account,String json) throws Exception{
    accountHash(account);
    if(json==null||json.getBytes(StandardCharsets.UTF_8).length>MAX_BYTES)
      throw new IllegalArgumentException("Tamaño de copia inválido");
    JSONObject obj=new JSONObject(json);
    if(!(obj.opt("tasks") instanceof JSONArray)||
       !(obj.opt("packs") instanceof JSONArray)||
       !(obj.opt("alarms") instanceof JSONArray)||
       !(obj.opt("pet") instanceof JSONObject))
      throw new IllegalArgumentException("Modelo incompleto");
  }
  private static String[] encrypt(String json) throws Exception{
    Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
    cipher.init(Cipher.ENCRYPT_MODE,key());
    byte[] encrypted=cipher.doFinal(json.getBytes(StandardCharsets.UTF_8));
    return new String[]{Base64.encodeToString(cipher.getIV(),Base64.NO_WRAP),
      Base64.encodeToString(encrypted,Base64.NO_WRAP)};
  }
  private static String decrypt(String nonce,String encrypted) throws Exception{
    Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
    cipher.init(Cipher.DECRYPT_MODE,key(),
      new GCMParameterSpec(128,Base64.decode(nonce,Base64.NO_WRAP)));
    return new String(cipher.doFinal(Base64.decode(encrypted,Base64.NO_WRAP)),
      StandardCharsets.UTF_8);
  }

  /**
   * Guarda una revisión actual y preserva la anterior dentro de una única transacción.
   * Nunca borra una copia válida para sustituirla por datos incompletos.
   */
  synchronized boolean save(String account,String json){
    try{
      validate(account,json);
      String hash=accountHash(account);
      SQLiteDatabase db=getWritableDatabase();
      String[] encrypted=encrypt(json);
      db.beginTransaction();
      try{
        String previousNonce=null,previousCipher=null;
        long previousAt=0;
        try(Cursor c=db.query(TABLE,new String[]{"nonce","ciphertext","saved_at"},
            "account=? AND revision=0",new String[]{hash},null,null,null)){
          if(c.moveToFirst()){previousNonce=c.getString(0);previousCipher=c.getString(1);previousAt=c.getLong(2);}
        }
        if(previousNonce!=null){
          // No reemplazar un snapshot antiguo mientras no se pueda descifrar.
          // Si se pierde la clave, el WebView sigue conservando su copia original.
          try{
            String old=decrypt(previousNonce,previousCipher);
            validate(account,old);
            if(old.equals(json)){db.setTransactionSuccessful();return true;}
            ContentValues history=new ContentValues();
            history.put("account",hash);history.put("revision",1);
            history.put("nonce",previousNonce);history.put("ciphertext",previousCipher);
            history.put("saved_at",previousAt);
            if(db.insertWithOnConflict(TABLE,null,history,SQLiteDatabase.CONFLICT_REPLACE)<0)
              throw new IllegalStateException("No se pudo guardar la revisión anterior");
          }catch(javax.crypto.AEADBadTagException ex){
            Log.w("NeuroSync","Copia SQLite actual ilegible, se conserva para recuperación",ex);
            return false;
          }
        }
        ContentValues current=new ContentValues();
        current.put("account",hash);current.put("revision",0);
        current.put("nonce",encrypted[0]);current.put("ciphertext",encrypted[1]);
        current.put("saved_at",System.currentTimeMillis());
        if(db.insertWithOnConflict(TABLE,null,current,SQLiteDatabase.CONFLICT_REPLACE)<0)
          throw new IllegalStateException("No se pudo guardar la copia SQLite");
        db.setTransactionSuccessful();
      }finally{db.endTransaction();}
      return true;
    }catch(Exception e){
      Log.w("NeuroSync","Copia secundaria SQLite no disponible",e);
      return false;
    }
  }

  /** Eliminar exclusivamente las dos revisiones cifradas de esta cuenta. */
  synchronized boolean erase(String account){
    try{
      String hash=accountHash(account);
      SQLiteDatabase db=getWritableDatabase();
      return db.delete(TABLE,"account=?",new String[]{hash})>=0;
    }catch(Exception ex){
      Log.w("NeuroSync","No se pudo limpiar la copia local al eliminar cuenta",ex);
      return false;
    }
  }

  synchronized String recover(String account){
    JSONObject response=new JSONObject();
    try{
      String hash=accountHash(account);
      SQLiteDatabase db=getReadableDatabase();
      for(int slot=0;slot<2;slot++){
        try(Cursor c=db.query(TABLE,new String[]{"nonce","ciphertext","saved_at"},
            "account=? AND revision=?",new String[]{hash,String.valueOf(slot)},null,null,null)){
          if(!c.moveToFirst())continue;
          try{
            String value=decrypt(c.getString(0),c.getString(1));
            validate(account,value);
            return response.put("ok",true).put("model",new JSONObject(value))
              .put("previous",slot==1).put("savedAt",c.getLong(2)).toString();
          }catch(Exception corrupt){Log.w("NeuroSync","Revisión secundaria ilegible",corrupt);}
        }
      }
      return response.put("ok",false).put("reason","no_valid_snapshot").toString();
    }catch(Exception ex){
      Log.w("NeuroSync","No se pudo consultar SQLite",ex);
      try{return response.put("ok",false).put("reason","unavailable").toString();}
      catch(Exception ignored){return "{\"ok\":false}";}
    }
  }
}
