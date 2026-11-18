package com.neurosync.familia;

import android.app.*;
import android.content.*;
import android.os.*;
import org.json.*;
import java.util.Map;

/** Aviso local independiente de la WebView: funciona tras cerrar la interfaz. */
public final class DepartureReminderReceiver extends android.content.BroadcastReceiver {
 static final String PREF="ns_departure_reminders_v1",ACTION="com.neurosync.familia.DEPARTURE";
 private static String key(String id){return "pack_"+id;}
 private static PendingIntent pending(Context ctx,String id){
  Intent intent=new Intent(ctx,DepartureReminderReceiver.class).setAction(ACTION).putExtra("id",id);
  return PendingIntent.getBroadcast(ctx,id.hashCode()^0x416177, intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
 }
 static JSONObject get(Context ctx,String id){
  try{return new JSONObject(ctx.getSharedPreferences(PREF,0).getString(key(id),"{}"));}catch(Exception e){return new JSONObject();}
 }
 static boolean schedule(Context ctx,String id,String title,String itemsJson,long when,String repeat){
  if(id==null||id.length()<2||id.length()>100||title==null||title.length()>100
   ||when<=0||itemsJson==null||itemsJson.length()>16000)return false;
  try{
   JSONArray items=new JSONArray(itemsJson);
   if(items.length()<1||items.length()>80)return false;
   JSONArray safe=new JSONArray();
   for(int i=0;i<items.length();i++){
    String item=items.optString(i,"").trim();
    if(item.length()<1||item.length()>100)return false;
    safe.put(item);
   }
   if(!("none".equals(repeat)||"daily".equals(repeat)||"weekly".equals(repeat)||"weekdays".equals(repeat)||"weekends".equals(repeat)))return false;
   long next="none".equals(repeat)?when:AlarmReceiver.nextRepeat(when,repeat,System.currentTimeMillis());
   if(next<=System.currentTimeMillis())return false;
   JSONObject original=get(ctx,id),entry=new JSONObject();
   entry.put("id",id).put("title",title).put("items",safe).put("when",next).put("repeat",repeat);
   // Al editar mantener los checks solo cuando la lista de objetos no cambia.
   boolean same=original.optJSONArray("items")!=null&&original.optJSONArray("items").toString().equals(safe.toString());
   entry.put("checked",same?original.optJSONObject("checked"):new JSONObject());
   android.content.SharedPreferences store=ctx.getSharedPreferences(PREF,0);
   String prior=store.getString(key(id),null);
   if(!store.edit().putString(key(id),entry.toString()).commit())return false;
   if(!program(ctx,id,next)){
    android.content.SharedPreferences.Editor rollback=store.edit();
    if(prior==null)rollback.remove(key(id));else rollback.putString(key(id),prior);
    rollback.commit();return false;
   }
   return true;
  }catch(Exception e){return false;}
 }
 private static boolean program(Context ctx,String id,long when){
  try{
   AlarmManager manager=(AlarmManager)ctx.getSystemService(Context.ALARM_SERVICE);
   if(manager==null)return false;
   if(Build.VERSION.SDK_INT<31||manager.canScheduleExactAlarms())
    manager.setAlarmClock(new AlarmManager.AlarmClockInfo(when,PendingIntent.getActivity(ctx,id.hashCode()^0x11213,
      new Intent(ctx,DepartureChecklistActivity.class).putExtra("id",id),
      PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE)),pending(ctx,id));
   else manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,pending(ctx,id));
   return true;
  }catch(Exception e){android.util.Log.e("NeuroSync","No se pudo programar lista de salida",e);return false;}
 }
 static void cancel(Context ctx,String id){
  if(id==null)return;
  AlarmManager manager=(AlarmManager)ctx.getSystemService(Context.ALARM_SERVICE);
  if(manager!=null)manager.cancel(pending(ctx,id));
  ctx.getSharedPreferences(PREF,0).edit().remove(key(id)).apply();
  android.app.NotificationManager nm=(android.app.NotificationManager)ctx.getSystemService(Context.NOTIFICATION_SERVICE);
  if(nm!=null)nm.cancel(id.hashCode()^0x310088);
 }
 static boolean mark(Context ctx,String id,int index,boolean done){
  JSONObject entry=get(ctx,id);JSONArray items=entry.optJSONArray("items");
  if(items==null||index<0||index>=items.length())return false;
  try{
   JSONObject checked=entry.optJSONObject("checked");if(checked==null)checked=new JSONObject();
   checked.put(Integer.toString(index),done);entry.put("checked",checked);
   return ctx.getSharedPreferences(PREF,0).edit().putString(key(id),entry.toString()).commit();
  }catch(Exception e){return false;}
 }
 static String state(Context ctx,String id){return get(ctx,id).toString();}
 private static void restore(Context ctx){
  long now=System.currentTimeMillis();
  for(Map.Entry<String,?> x:ctx.getSharedPreferences(PREF,0).getAll().entrySet()){
   if(!x.getKey().startsWith("pack_"))continue;
   try{
    JSONObject entry=new JSONObject(String.valueOf(x.getValue()));
    String id=entry.optString("id",""),repeat=entry.optString("repeat","none");
    long when=entry.optLong("when");
    if(when<=now&&!"none".equals(repeat)){
     when=AlarmReceiver.nextRepeat(when,repeat,now);
     entry.put("when",when);entry.put("checked",new JSONObject());
     ctx.getSharedPreferences(PREF,0).edit().putString(key(id),entry.toString()).apply();
    }
    if(when>now)program(ctx,id,when);
   }catch(Exception ignored){}
  }
 }
 @Override public void onReceive(Context ctx,Intent intent){
  if(intent==null)return;
  if(Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())||
     Intent.ACTION_TIME_CHANGED.equals(intent.getAction())||
     Intent.ACTION_TIMEZONE_CHANGED.equals(intent.getAction())||
     AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED.equals(intent.getAction())){
    restore(ctx);return;
  }
  if(!ACTION.equals(intent.getAction()))return;
  String id=intent.getStringExtra("id");
  if(id==null)return;
  JSONObject entry=get(ctx,id);if(!id.equals(entry.optString("id")))return;
  try{
   String repeat=entry.optString("repeat","none");
   if(!"none".equals(repeat)){
    long next=AlarmReceiver.nextRepeat(entry.optLong("when"),repeat,System.currentTimeMillis());
    if(next>System.currentTimeMillis()&&program(ctx,id,next)){
     entry.put("when",next);entry.put("checked",new JSONObject());
     ctx.getSharedPreferences(PREF,0).edit().putString(key(id),entry.toString()).apply();
    }
   }
   String channel="neurosync_departure_v1";
   NotificationManager nm=(NotificationManager)ctx.getSystemService(Context.NOTIFICATION_SERVICE);
   if(nm==null)return;
   NotificationChannel ch=new NotificationChannel(channel,"Listas para salir",NotificationManager.IMPORTANCE_HIGH);
   ch.setDescription("Revisa los objetos antes de salir");
   nm.createNotificationChannel(ch);
   Intent open=new Intent(ctx,DepartureChecklistActivity.class).putExtra("id",id);
   open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
   PendingIntent fullscreen=PendingIntent.getActivity(ctx,id.hashCode()^0x11213,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
   Notification.Builder notice=new Notification.Builder(ctx,channel)
    .setSmallIcon(android.R.drawable.ic_menu_agenda)
    .setContentTitle("Revisa tu lista antes de salir")
    .setContentText(entry.optString("title","Lista de salida"))
    .setCategory(Notification.CATEGORY_REMINDER).setPriority(Notification.PRIORITY_HIGH)
    .setVisibility(Notification.VISIBILITY_PRIVATE)
    .setContentIntent(fullscreen).setFullScreenIntent(fullscreen,true).setAutoCancel(false);
   nm.notify(id.hashCode()^0x310088,notice.build());
  }catch(Exception e){android.util.Log.e("NeuroSync","Falló aviso de salida",e);}
 }
}
