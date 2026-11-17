package com.neurosync.familia;

import android.content.Context;

/** Configuración local del reto. No contiene contraseñas ni datos sensibles. */
public final class AlarmChallengeStore {
 private AlarmChallengeStore(){}
 /** Guarda solo una huella SHA-256; el texto QR no se sincroniza entre familiares. */
 public static void setQr(Context ctx,String id,String contents){
  if(id==null||id.length()>100||contents==null||contents.isEmpty()||contents.length()>2048)return;
  ctx.getSharedPreferences("ns_alarm_challenges",0).edit().putString("qr_"+id,digest(contents)).apply();
 }
 public static boolean matchesQr(Context ctx,String id,String contents){
  if(id==null||contents==null)return false;
  String expected=ctx.getSharedPreferences("ns_alarm_challenges",0).getString("qr_"+id,null);
  if(expected==null)return false;
  return java.security.MessageDigest.isEqual(expected.getBytes(java.nio.charset.StandardCharsets.UTF_8),
       digest(contents).getBytes(java.nio.charset.StandardCharsets.UTF_8));
 }
 public static boolean hasQr(Context ctx,String id){
  return id!=null&&ctx.getSharedPreferences("ns_alarm_challenges",0).contains("qr_"+id);
 }
 private static String digest(String contents){
  try{
   byte[] hash=java.security.MessageDigest.getInstance("SHA-256").digest(contents.getBytes(java.nio.charset.StandardCharsets.UTF_8));
   StringBuilder result=new StringBuilder();
   for(byte b:hash)result.append(String.format(java.util.Locale.ROOT,"%02x",b&255));
   return result.toString();
  }catch(java.security.NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}
 }
 public static void set(Context ctx,String id,String type){
  if(id==null||id.length()>100)return;
  if(!"math".equals(type)&&!"taps".equals(type)&&!"memory".equals(type)&&!"typing".equals(type)&&!"shake".equals(type)&&!"math_hard".equals(type)&&!"qr".equals(type)&&!"routine_focus".equals(type)&&!"routine_active".equals(type))type="none";
  ctx.getSharedPreferences("ns_alarm_challenges",0).edit().putString(id,type).apply();
 }
 public static String get(Context ctx,String id){
  if(id==null)return "none";
  return ctx.getSharedPreferences("ns_alarm_challenges",0).getString(id,"none");
 }
 public static void remove(Context ctx,String id){
  if(id!=null)ctx.getSharedPreferences("ns_alarm_challenges",0).edit().remove(id).remove("qr_"+id).apply();
 }
}