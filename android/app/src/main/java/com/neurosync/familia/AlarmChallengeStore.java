package com.neurosync.familia;

import android.content.Context;

/** Configuración local del reto. No contiene contraseñas ni datos sensibles. */
public final class AlarmChallengeStore {
 private AlarmChallengeStore(){}
 public static void set(Context ctx,String id,String type){
  if(id==null||id.length()>100)return;
  if(!"math".equals(type)&&!"taps".equals(type))type="none";
  ctx.getSharedPreferences("ns_alarm_challenges",0).edit().putString(id,type).apply();
 }
 public static String get(Context ctx,String id){
  if(id==null)return "none";
  return ctx.getSharedPreferences("ns_alarm_challenges",0).getString(id,"none");
 }
 public static void remove(Context ctx,String id){
  if(id!=null)ctx.getSharedPreferences("ns_alarm_challenges",0).edit().remove(id).apply();
 }
}