package com.neurosync.familia;

import android.content.Context;

/** Opciones por alarma, almacenadas localmente sin permisos especiales. */
final class AlarmOptionsStore {
 private static final String PREFS="ns_alarm_options_v1";
 private AlarmOptionsStore(){}
 static void set(Context ctx,String id,int minutes,int max){
  if(id==null||id.isEmpty()||id.length()>100)return;
  if(minutes!=1&&minutes!=3&&minutes!=5&&minutes!=10&&minutes!=15)minutes=5;
  if(max!=0&&max!=1&&max!=2&&max!=3&&max!=5&&max!=10)max=3;
  ctx.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit()
   .putInt("minutes_"+id,minutes).putInt("max_"+id,max).apply();
 }
 static int minutes(Context ctx,String id){
  if(id==null)return 5;
  return ctx.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getInt("minutes_"+id,5);
 }
 static int max(Context ctx,String id){
  if(id==null)return 3;
  return ctx.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getInt("max_"+id,3);
 }
 static int used(Context ctx,String id){
  if(id==null)return 0;
  return ctx.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getInt("used_"+id,0);
 }
 static void increment(Context ctx,String id){
  if(id==null)return;
  ctx.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit()
   .putInt("used_"+id,used(ctx,id)+1).apply();
 }
 static void reset(Context ctx,String id){
  if(id==null)return;
  ctx.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putInt("used_"+id,0).apply();
 }
 static void clear(Context ctx,String id){
  if(id==null)return;
  ctx.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().remove("minutes_"+id)
   .remove("max_"+id).remove("used_"+id).apply();
 }
}