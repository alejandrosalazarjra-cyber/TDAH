package com.neurosync.familia;

import android.util.Base64;
import org.json.JSONObject;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/** Respaldo portátil cifrado: PBKDF2-HMAC-SHA256 + AES-256-GCM.
 * La contraseña no se almacena. Sin ella no es posible restaurar el archivo.
 * Formato JSON versionado para permitir migraciones posteriores.
 */
public final class BackupCrypto {
  private static final int ITERATIONS = 210000;
  private static final SecureRandom RANDOM = new SecureRandom();
  private BackupCrypto() {}

  private static byte[] decode(String encoded, int expected) {
    byte[] bytes = Base64.decode(encoded, Base64.NO_WRAP);
    if (expected > 0 && bytes.length != expected) throw new IllegalArgumentException("Parámetros de respaldo inválidos");
    return bytes;
  }
  private static SecretKeySpec derive(String password, byte[] salt, int rounds) throws Exception {
    if (password == null || password.length() < 12) throw new IllegalArgumentException("La contraseña necesita al menos 12 caracteres");
    if (rounds < 210000 || rounds > 600000) throw new IllegalArgumentException("Número de iteraciones inválido");
    PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, rounds, 256);
    try {
      byte[] derived = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
      return new SecretKeySpec(derived, "AES");
    } finally { spec.clearPassword(); }
  }
  public static String encrypt(String plain, String password) throws Exception {
    if (plain == null || plain.length() > 1000000) throw new IllegalArgumentException("Respaldo demasiado grande");
    JSONObject parsed = new JSONObject(plain);
    if (parsed.optJSONArray("tasks") == null || parsed.optJSONArray("packs") == null || parsed.optJSONArray("alarms") == null)
      throw new IllegalArgumentException("Datos de respaldo inválidos");
    byte[] salt = new byte[16], nonce = new byte[12];
    RANDOM.nextBytes(salt); RANDOM.nextBytes(nonce);
    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
    cipher.init(Cipher.ENCRYPT_MODE, derive(password,salt,ITERATIONS), new GCMParameterSpec(128,nonce));
    byte[] encrypted = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
    return new JSONObject().put("format","neurosync-encrypted-backup")
      .put("version",1).put("kdf","PBKDF2-HMAC-SHA256").put("iterations",ITERATIONS)
      .put("cipher","AES-256-GCM").put("salt",Base64.encodeToString(salt,Base64.NO_WRAP))
      .put("nonce",Base64.encodeToString(nonce,Base64.NO_WRAP))
      .put("data",Base64.encodeToString(encrypted,Base64.NO_WRAP)).toString();
  }
  public static String decrypt(String payload, String password) throws Exception {
    if (payload == null || payload.length() > 2000000) throw new IllegalArgumentException("Archivo demasiado grande");
    JSONObject data = new JSONObject(payload);
    if (!"neurosync-encrypted-backup".equals(data.optString("format")) || data.optInt("version") != 1
        || !"PBKDF2-HMAC-SHA256".equals(data.optString("kdf"))
        || !"AES-256-GCM".equals(data.optString("cipher")))
      throw new IllegalArgumentException("Formato de cifrado incompatible");
    int rounds=data.getInt("iterations");
    byte[] salt=decode(data.getString("salt"),16), nonce=decode(data.getString("nonce"),12);
    byte[] ciphertext=decode(data.getString("data"),-1);
    if (ciphertext.length > 1000020 || ciphertext.length < 16) throw new IllegalArgumentException("Archivo cifrado inválido");
    Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
    cipher.init(Cipher.DECRYPT_MODE,derive(password,salt,rounds),new GCMParameterSpec(128,nonce));
    byte[] plaintext=cipher.doFinal(ciphertext);
    if (plaintext.length > 1000000) throw new IllegalArgumentException("Respaldo demasiado grande");
    JSONObject check=new JSONObject(new String(plaintext,StandardCharsets.UTF_8));
    if (check.optJSONArray("tasks")==null || check.optJSONArray("packs")==null || check.optJSONArray("alarms")==null)
      throw new IllegalArgumentException("Contenido inválido");
    return check.toString();
  }
}
