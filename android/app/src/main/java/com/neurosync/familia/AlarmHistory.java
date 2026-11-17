package com.neurosync.familia;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;

/** Registro local acotado de eventos de alarma. No contiene contraseñas ni sincroniza datos. */
public final class AlarmHistory {
 private static final String PREF="ns_alarm_history";
 private static final String KEY="events";
 private AlarmHistory(){}
 public static synchronized void record(Context ctx,String id,String action){
  if(id==null||id.length()>100)return;
  if(!"ring".equals(action)&&!"dismiss".equals(action)&&!"snooze".equals(action))return;
  try{
   String old=ctx.getSharedPreferences(PREF,0).getString(KEY,"[]");
   JSONArray prior=new JSONArray(old),next=new JSONArray();
   int start=Math.max(0,prior.length()-149);
   for(int i=start;i<prior.length();i++)next.put(prior.getJSONObject(i));
   JSONObject event=new JSONObject();
   event.put("id",id);event.put("action",action);event.put("at",System.currentTimeMillis());
   next.put(event);
   ctx.getSharedPreferences(PREF,0).edit().putString(KEY,next.toString()).apply();
  }catch(Exception e){android.util.Log.w("NeuroSync","No se guardó el evento de alarma",e);}
 }
 public static synchronized String read(Context ctx){
  return ctx.getSharedPreferences(PREF,0).getString(KEY,"[]");
 }
}
