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
  if(!"math".equals(type)&&!"taps".equals(type)&&!"memory".equals(type)&&!"typing".equals(type)&&!"shake".equals(type)&&!"math_hard".equals(type)&&!"qr".equals(type)&&!"routine_focus".equals(type)&&!"routine_active".equals(type)&&!"routine_five".equals(type)
    &&!"walk".equals(type)&&!"photo".equals(type)&&!"routine_custom".equals(type))type="none";
  ctx.getSharedPreferences("ns_alarm_challenges",0).edit().putString(id,type).apply();
 }
 public static String get(Context ctx,String id){
  if(id==null)return "none";
  return ctx.getSharedPreferences("ns_alarm_challenges",0).getString(id,"none");
 }
 private static boolean permittedStage(String stage){
   return "math".equals(stage)||"math_hard".equals(stage)||"memory".equals(stage)||
     "typing".equals(stage)||"taps".equals(stage)||"shake".equals(stage)||
     "walk".equals(stage)||"photo".equals(stage);
 }
 /** El usuario puede ordenar entre dos y cinco retos disponibles sin reconstruir la APK. */
 public static boolean setSequence(Context ctx,String id,String raw){
   if(id==null||id.isEmpty()||id.length()>100||raw==null||raw.length()>350)return false;
   try{
     org.json.JSONArray input=new org.json.JSONArray(raw);
     if(input.length()<2||input.length()>5)return false;
     for(int i=0;i<input.length();i++)if(!permittedStage(input.getString(i)))return false;
     return ctx.getSharedPreferences("ns_alarm_challenges",0).edit()
       .putString("sequence_"+id,input.toString()).commit();
   }catch(Exception ignored){return false;}
 }
 public static String[] getSequence(Context ctx,String id){
   String raw=ctx.getSharedPreferences("ns_alarm_challenges",0).getString("sequence_"+id,"");
   try{
     org.json.JSONArray arr=new org.json.JSONArray(raw);
     if(arr.length()<2||arr.length()>5)throw new IllegalArgumentException();
     String[] result=new String[arr.length()];
     for(int i=0;i<arr.length();i++){
       result[i]=arr.getString(i);
       if(!permittedStage(result[i]))throw new IllegalArgumentException();
     }
     return result;
   }catch(Exception ignored){return new String[]{"math","typing"};}
 }
 public static void remove(Context ctx,String id){
  if(id!=null)ctx.getSharedPreferences("ns_alarm_challenges",0).edit().remove(id).remove("qr_"+id).remove("sequence_"+id).apply();
 }
}