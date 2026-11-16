package com.neurosync.familia;

import android.app.*;
import android.content.*;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.os.*;
import android.provider.Settings;
import org.json.JSONObject;
import java.util.Map;

/** Alarmas reales, notificación visible y pantalla completa cuando está autorizado.
 * Programa alarmas precisas si hay permiso y una alternativa no exacta si no lo hay.
 */
public final class AlarmReceiver extends BroadcastReceiver {
 public AlarmReceiver(){}
 private static final String CHANNEL="neurosync_alarms_v2";
 private static final String PREFIX="alarm_";
 private static final String FIRE="com.neurosync.familia.RING";
 private static final String SNOOZE="com.neurosync.familia.SNOOZE";
 private static PendingIntent pending(Context ctx,String id){
  Intent intent=new Intent(ctx,AlarmReceiver.class).setAction(FIRE).putExtra("id",id);
  return PendingIntent.getBroadcast(ctx,id.hashCode(),intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
 }
 private static AlarmManager manager(Context ctx){return (AlarmManager)ctx.getSystemService(Context.ALARM_SERVICE);}
 public static boolean schedule(Context ctx,String id,String title,long at,boolean daily){
   return scheduleRepeat(ctx,id,title,at,daily?"daily":"none");
 }
 public static boolean scheduleRepeat(Context ctx,String id,String title,long at,String repeat){
  try{
   if(id==null||id.length()<2||id.length()>100||at<=0)return false;
   if(title==null||title.trim().isEmpty())title="Recordatorio";
   long when=at;
   long interval="weekly".equals(repeat)?604800000L:"daily".equals(repeat)?86400000L:0L;
   if(interval>0)while(when<=System.currentTimeMillis())when+=interval;
   if(when<=System.currentTimeMillis())return false;
   JSONObject entry=new JSONObject().put("id",id).put("title",title).put("when",when).put("repeat",repeat);
   ctx.getSharedPreferences("ns_alarms",0).edit().putString(PREFIX+id,entry.toString()).apply();
   return program(ctx,id,when);
  }catch(Exception e){return false;}
 }
 private static boolean program(Context ctx,String id,long when){
  try{
   AlarmManager alarm=manager(ctx);
   if(Build.VERSION.SDK_INT<31||alarm.canScheduleExactAlarms())alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,pending(ctx,id));
   else alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,pending(ctx,id));
   return true;
  }catch(Exception ex){android.util.Log.e("NeuroSync","No se pudo programar alarma",ex);return false;}
 }
 public static void cancel(Context ctx,String id){
  if(id==null)return;
  manager(ctx).cancel(pending(ctx,id));
  ctx.getSharedPreferences("ns_alarms",0).edit().remove(PREFIX+id).apply();
  ((NotificationManager)ctx.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(id.hashCode());
 }
 public static void dismiss(Context ctx,String id){
  ((NotificationManager)ctx.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(id.hashCode());
 }
 public static void snooze(Context ctx,String id,int minutes){
  try{
   String s=ctx.getSharedPreferences("ns_alarms",0).getString(PREFIX+id,null);if(s==null)return;
   JSONObject a=new JSONObject(s);
   program(ctx,id,System.currentTimeMillis()+minutes*60000L);
   ((NotificationManager)ctx.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(id.hashCode());
  }catch(Exception ignored){}
 }
 private static void restore(Context ctx){
  for(Map.Entry<String,?> row:ctx.getSharedPreferences("ns_alarms",0).getAll().entrySet()){
   if(!row.getKey().startsWith(PREFIX))continue;
   try{
    JSONObject a=new JSONObject((String)row.getValue());
    String id=a.getString("id");long when=a.getLong("when");
    String repeat=a.optString("repeat",a.optBoolean("daily")?"daily":"none");
    long interval="weekly".equals(repeat)?604800000L:"daily".equals(repeat)?86400000L:0L;
    if(interval>0)while(when<=System.currentTimeMillis())when+=interval;
    if(when>System.currentTimeMillis())program(ctx,id,when);
   }catch(Exception ignored){}
  }
 }
 private static void channel(Context ctx){
  NotificationManager manager=(NotificationManager)ctx.getSystemService(Context.NOTIFICATION_SERVICE);
  NotificationChannel c=new NotificationChannel(CHANNEL,"Alarmas de NeuroSync",NotificationManager.IMPORTANCE_HIGH);
  c.setDescription("Alarmas con sonido y visualización en pantalla de bloqueo");
  c.enableVibration(true);
  c.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
    new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build());
  c.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
  manager.createNotificationChannel(c);
 }
 @Override public void onReceive(Context ctx,Intent intent){
  if(intent==null)return;
  if(Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())){restore(ctx);return;}
  if(!FIRE.equals(intent.getAction()))return;
  String id=intent.getStringExtra("id");if(id==null)return;
  String raw=ctx.getSharedPreferences("ns_alarms",0).getString(PREFIX+id,null);if(raw==null)return;
  try{
   JSONObject a=new JSONObject(raw);String title=a.optString("title","Recordatorio");
   channel(ctx);
   Intent screen=new Intent(ctx,AlarmRingActivity.class).putExtra("id",id).putExtra("title",title);
   screen.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
   PendingIntent full=PendingIntent.getActivity(ctx,id.hashCode(),screen,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
   Notification notice=new Notification.Builder(ctx,CHANNEL)
    .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
    .setContentTitle("⏰ "+title)
    .setContentText("Pulsa para detener o posponer")
    .setCategory(Notification.CATEGORY_ALARM)
    .setPriority(Notification.PRIORITY_MAX)
    .setVisibility(Notification.VISIBILITY_PUBLIC)
    .setAutoCancel(false).setOngoing(true)
    .setContentIntent(full).setFullScreenIntent(full,true).build();
   ((NotificationManager)ctx.getSystemService(Context.NOTIFICATION_SERVICE)).notify(id.hashCode(),notice);
   String repeat=a.optString("repeat",a.optBoolean("daily")?"daily":"none");
   long interval="weekly".equals(repeat)?604800000L:"daily".equals(repeat)?86400000L:0L;
   if(interval>0){
    long next=a.optLong("when")+interval;
    while(next<=System.currentTimeMillis())next+=interval;
    a.put("when",next);
    ctx.getSharedPreferences("ns_alarms",0).edit().putString(PREFIX+id,a.toString()).apply();
    program(ctx,id,next);
   }else{
    // Se mantiene el registro hasta detenerla, para permitir posponer.
   }
  }catch(Exception e){android.util.Log.e("NeuroSync","No se pudo disparar la alarma",e);}
 }
}