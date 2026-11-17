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
 private static final String CHANNEL="neurosync_alarms_v4_visual";
 private static final String DISMISS="com.neurosync.familia.alarm.DISMISS";
 private static final String POSTPONE="com.neurosync.familia.alarm.POSTPONE";
 private static final String PREFIX="alarm_";
 private static final String SNOOZE_PREFIX="snooze_";
 private static final String FIRE="com.neurosync.familia.RING";
 private static final String SNOOZE="com.neurosync.familia.SNOOZE";
 private static PendingIntent pendingSnooze(Context ctx,String id){
  Intent intent=new Intent(ctx,AlarmReceiver.class).setAction(SNOOZE).putExtra("id",id);
  return PendingIntent.getBroadcast(ctx,id.hashCode()^0x3F3F3F3F,intent,
    PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
 }
 private static PendingIntent pending(Context ctx,String id){
  Intent intent=new Intent(ctx,AlarmReceiver.class).setAction(FIRE).putExtra("id",id);
  return PendingIntent.getBroadcast(ctx,id.hashCode(),intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
 }
 private static AlarmManager manager(Context ctx){return (AlarmManager)ctx.getSystemService(Context.ALARM_SERVICE);}
 /** Avanza calendariamente conservando la hora local (incluido horario de verano). */
 static long nextRepeat(long previous,String repeat,long now){
  if(previous<=0)return -1;
  int unit,amount;
  if("weekly".equals(repeat)){unit=java.util.Calendar.WEEK_OF_YEAR;amount=1;}
  else if("daily".equals(repeat)){unit=java.util.Calendar.DAY_OF_YEAR;amount=1;}
  else if("every2".equals(repeat)){unit=java.util.Calendar.DAY_OF_YEAR;amount=2;}
  else if("every3".equals(repeat)){unit=java.util.Calendar.DAY_OF_YEAR;amount=3;}
  else return -1;
  java.util.Calendar calendar=java.util.Calendar.getInstance();
  calendar.setTimeInMillis(previous);
  // Evita loops no acotados para relojes mal configurados o datos corruptos.
  for(int count=0;count<50000 && calendar.getTimeInMillis()<=now;count++){
   calendar.add(unit,amount);
  }
  return calendar.getTimeInMillis()>now?calendar.getTimeInMillis():-1;
 }
 public static boolean schedule(Context ctx,String id,String title,long at,boolean daily){
   return scheduleRepeat(ctx,id,title,at,daily?"daily":"none");
 }
 public static boolean scheduleRepeat(Context ctx,String id,String title,long at,String repeat){
  try{
   if(id==null||id.length()<2||id.length()>100||at<=0)return false;
   if(title==null||title.trim().isEmpty())title="Recordatorio";
   long when=at;
   if(!"none".equals(repeat)){
    long next=nextRepeat(when,repeat,System.currentTimeMillis());
    if(next>0)when=next;
   }
   if(when<=System.currentTimeMillis())return false;
   JSONObject entry=new JSONObject().put("id",id).put("title",title).put("when",when).put("repeat",repeat);
   // Guardar primero el registro para que el receptor encuentre la alarma si se dispara
   // inmediatamente. commit() verifica persistencia antes de anunciar programación correcta.
   android.content.SharedPreferences prefs=ctx.getSharedPreferences("ns_alarms",0);
   String previous=prefs.getString(PREFIX+id,null);
   if(!prefs.edit().putString(PREFIX+id,entry.toString()).commit())return false;
   if(!program(ctx,id,when)){
    android.content.SharedPreferences.Editor rollback=prefs.edit();
    if(previous==null)rollback.remove(PREFIX+id);
    else rollback.putString(PREFIX+id,previous);
    rollback.commit();
    return false;
   }
   return true;
  }catch(Exception e){return false;}
 }
 private static boolean program(Context ctx,String id,long when){
  try{
   AlarmManager alarm=manager(ctx);
   if(Build.VERSION.SDK_INT<31||alarm.canScheduleExactAlarms()){
     Intent view=new Intent(ctx,AlarmRingActivity.class).putExtra("id",id);
     PendingIntent show=PendingIntent.getActivity(ctx,id.hashCode()^0x45454545,view,
       PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
     alarm.setAlarmClock(new AlarmManager.AlarmClockInfo(when,show),pending(ctx,id));
   }else{
     alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,pending(ctx,id));
   }
   return true;
  }catch(Exception ex){android.util.Log.e("NeuroSync","No se pudo programar alarma",ex);return false;}
 }
 public static void cancel(Context ctx,String id){
  if(id==null)return;
  manager(ctx).cancel(pending(ctx,id));
  manager(ctx).cancel(pendingSnooze(ctx,id));
  ctx.getSharedPreferences("ns_alarm_snoozes",0).edit().remove(SNOOZE_PREFIX+id).apply();
  ctx.getSharedPreferences("ns_alarms",0).edit().remove(PREFIX+id).apply();
  AlarmChallengeStore.remove(ctx,id);
  ((NotificationManager)ctx.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(id.hashCode());
 }
 public static void dismiss(Context ctx,String id){
  if(id==null)return;
  manager(ctx).cancel(pendingSnooze(ctx,id));
  ctx.getSharedPreferences("ns_alarm_snoozes",0).edit().remove(SNOOZE_PREFIX+id).apply();
  AlarmHistory.record(ctx,id,"dismiss");
  AlarmAudioService.stop(ctx);
  ((NotificationManager)ctx.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(id.hashCode());
  // No borrar la siguiente repetición diaria/semanal.
  String data=ctx.getSharedPreferences("ns_alarms",0).getString(PREFIX+id,null);
  if(data!=null)try{
    JSONObject entry=new JSONObject(data);
    if("none".equals(entry.optString("repeat","none")))
      ctx.getSharedPreferences("ns_alarms",0).edit().remove(PREFIX+id).apply();
  }catch(Exception ignored){}
 }
 public static void snooze(Context ctx,String id,int minutes){
  try{
   String s=ctx.getSharedPreferences("ns_alarms",0).getString(PREFIX+id,null);if(s==null)return;
   JSONObject a=new JSONObject(s);
   AlarmHistory.record(ctx,id,"snooze");
   AlarmManager alarm=manager(ctx);
   long when=System.currentTimeMillis()+minutes*60000L;
   if(Build.VERSION.SDK_INT<31||alarm.canScheduleExactAlarms())
     alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,pendingSnooze(ctx,id));
   else alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,pendingSnooze(ctx,id));
   ctx.getSharedPreferences("ns_alarm_snoozes",0).edit().putLong(SNOOZE_PREFIX+id,when).apply();
   AlarmAudioService.stop(ctx);
   ((NotificationManager)ctx.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(id.hashCode());
  }catch(Exception ignored){}
 }
 /** Diagnóstico de los registros locales: no afirma una garantía de ejecución del SO. */
 public static JSONObject diagnostics(Context ctx){
  JSONObject result=new JSONObject();
  int count=0,invalid=0;long next=Long.MAX_VALUE;
  try{
   for(Map.Entry<String,?> row:ctx.getSharedPreferences("ns_alarms",0).getAll().entrySet()){
    if(!row.getKey().startsWith(PREFIX))continue;
    try{
     JSONObject alarm=new JSONObject((String)row.getValue());
     String id=alarm.optString("id","");
     long at=alarm.optLong("when",-1);
     if(id.length()<2||at<=0){invalid++;continue;}
     count++;
     if(at>System.currentTimeMillis()&&at<next)next=at;
    }catch(Exception malformed){invalid++;}
   }
   result.put("count",count).put("invalid",invalid).put("nextAt",next==Long.MAX_VALUE?0:next);
  }catch(Exception e){android.util.Log.w("NeuroSync","No se pudieron contar alarmas locales",e);}
  return result;
 }
 private static void restore(Context ctx){
  long now=System.currentTimeMillis();
  for(Map.Entry<String,?> row:ctx.getSharedPreferences("ns_alarms",0).getAll().entrySet()){
   if(!row.getKey().startsWith(PREFIX))continue;
   try{
    JSONObject a=new JSONObject((String)row.getValue());
    String id=a.getString("id");
    long when=a.getLong("when");
    String repeat=a.optString("repeat",a.optBoolean("daily")?"daily":"none");
    if(when<=now && !"none".equals(repeat)){
      when=nextRepeat(when,repeat,now);
      if(when>now){
       a.put("when",when);
       ctx.getSharedPreferences("ns_alarms",0).edit().putString(PREFIX+id,a.toString()).apply();
      }
    }
    if(when>now)program(ctx,id,when);
    else if("none".equals(repeat)){
      // Una alarma pasada no se repite automáticamente; conservar datos para
      // que el usuario pueda revisarla sin inventar una nueva fecha.
      android.util.Log.i("DrakoPlan","Alarma antigua sin repetición: "+id);
    }
   }catch(Exception ex){android.util.Log.w("DrakoPlan","No se pudo restaurar una alarma",ex);}
  }
 }
 private static void restoreSnoozes(Context ctx){
  long now=System.currentTimeMillis();
  for(Map.Entry<String,?> row:ctx.getSharedPreferences("ns_alarm_snoozes",0).getAll().entrySet()){
   if(!row.getKey().startsWith(SNOOZE_PREFIX))continue;
   try{
    String id=row.getKey().substring(SNOOZE_PREFIX.length());
    long when=(Long)row.getValue();
    if(!ctx.getSharedPreferences("ns_alarms",0).contains(PREFIX+id)){
      ctx.getSharedPreferences("ns_alarm_snoozes",0).edit().remove(row.getKey()).apply();
      continue;
    }
    if(when<=now){
      // Una posposición vencida durante el apagado se reprograma pronto;
      // evita perder el aviso y evita mostrarlo desde el proceso de arranque.
      when=now+10000L;
      ctx.getSharedPreferences("ns_alarm_snoozes",0).edit().putLong(row.getKey(),when).apply();
    }
    AlarmManager alarm=manager(ctx);
    if(Build.VERSION.SDK_INT<31||alarm.canScheduleExactAlarms())
      alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,pendingSnooze(ctx,id));
    else alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,pendingSnooze(ctx,id));
   }catch(Exception ex){android.util.Log.w("DrakoPlan","No se pudo restaurar posposición",ex);}
  }
 }
 private static void channel(Context ctx){
  NotificationManager manager=(NotificationManager)ctx.getSystemService(Context.NOTIFICATION_SERVICE);
  NotificationChannel c=new NotificationChannel(CHANNEL,"Alarmas de NeuroSync",NotificationManager.IMPORTANCE_HIGH);
  c.setDescription("Alarmas con sonido y visualización en pantalla de bloqueo");
  // El sonido y la vibración se gestionan en AlarmAudioService para que
  // detener o posponer silencie la alarma inmediatamente, sin audio duplicado.
  c.enableVibration(false);
  c.setSound(null,null);
  c.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
  manager.createNotificationChannel(c);
 }
 @Override public void onReceive(Context ctx,Intent intent){
  if(intent==null)return;
  if(Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())
    || Intent.ACTION_TIME_CHANGED.equals(intent.getAction())
    || Intent.ACTION_TIMEZONE_CHANGED.equals(intent.getAction())
    || AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED.equals(intent.getAction())){restore(ctx);restoreSnoozes(ctx);return;}
  if(DISMISS.equals(intent.getAction())){
    String alarmId=intent.getStringExtra("id");
    if("none".equals(AlarmChallengeStore.get(ctx,alarmId)))dismiss(ctx,alarmId);
    return;
  }
  if(POSTPONE.equals(intent.getAction())){snooze(ctx,intent.getStringExtra("id"),5);return;}
  if(!FIRE.equals(intent.getAction())&&!SNOOZE.equals(intent.getAction()))return;
  String id=intent.getStringExtra("id");if(id==null)return;
  if(SNOOZE.equals(intent.getAction()))
    ctx.getSharedPreferences("ns_alarm_snoozes",0).edit().remove(SNOOZE_PREFIX+id).apply();
  String raw=ctx.getSharedPreferences("ns_alarms",0).getString(PREFIX+id,null);if(raw==null)return;
  try{
   JSONObject a=new JSONObject(raw);String title=a.optString("title","Recordatorio");
   AlarmHistory.record(ctx,id,"ring");
   channel(ctx);
   Intent screen=new Intent(ctx,AlarmRingActivity.class).putExtra("id",id).putExtra("title",title);
   screen.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
   PendingIntent full=PendingIntent.getActivity(ctx,id.hashCode(),screen,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
   Intent stopAction=new Intent(ctx,AlarmReceiver.class).setAction(DISMISS).putExtra("id",id);
   Intent snoozeAction=new Intent(ctx,AlarmReceiver.class).setAction(POSTPONE).putExtra("id",id);
   PendingIntent stopPending=PendingIntent.getBroadcast(ctx,id.hashCode()^0x25252525,stopAction,
      PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
   PendingIntent snoozePending=PendingIntent.getBroadcast(ctx,id.hashCode()^0x35353535,snoozeAction,
      PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
   Notification.Builder builder=new Notification.Builder(ctx,CHANNEL)
    .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
    .setContentTitle("⏰ "+title)
    .setContentText("Pulsa para detener o posponer")
    .setCategory(Notification.CATEGORY_ALARM)
    .setPriority(Notification.PRIORITY_MAX)
    .setVisibility(Notification.VISIBILITY_PUBLIC)
    .setAutoCancel(false).setOngoing(true)
    .addAction(android.R.drawable.ic_menu_recent_history,"Posponer",snoozePending)
    .setContentIntent(full).setFullScreenIntent(full,true);
   if("none".equals(AlarmChallengeStore.get(ctx,id))){
     builder.addAction(android.R.drawable.ic_media_pause,"Detener",stopPending);
   }
   Notification notice=builder.build();
   ((NotificationManager)ctx.getSystemService(Context.NOTIFICATION_SERVICE)).notify(id.hashCode(),notice);
   try{AlarmAudioService.start(ctx,id,title);}
   catch(Exception e){android.util.Log.e("NeuroSync","No se pudo iniciar audio en segundo plano",e);}

   String repeat=a.optString("repeat",a.optBoolean("daily")?"daily":"none");
   if(!"none".equals(repeat) && FIRE.equals(intent.getAction())){
    long next=nextRepeat(a.optLong("when"),repeat,System.currentTimeMillis());
    if(next>System.currentTimeMillis() && program(ctx,id,next)){
      a.put("when",next);
      ctx.getSharedPreferences("ns_alarms",0).edit().putString(PREFIX+id,a.toString()).apply();
    }else{
      android.util.Log.e("DrakoPlan","No se pudo programar la siguiente repetición: "+id);
    }
   }else{
    // Se mantiene el registro hasta detenerla, para permitir posponer.
   }
  }catch(Exception e){android.util.Log.e("NeuroSync","No se pudo disparar la alarma",e);}
 }
}