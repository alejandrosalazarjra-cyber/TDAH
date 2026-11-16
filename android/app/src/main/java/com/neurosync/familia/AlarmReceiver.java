package com.neurosync.familia;

import android.app.*;
import android.content.*;
import android.os.Build;
import org.json.JSONObject;
import java.util.Map;

/** Recordatorios del sistema con repetición diaria opcional. No sustituye una alarma
 * de reloj de alta precisión: Android puede retrasarlos durante ahorro de batería.
 */
public final class AlarmReceiver extends BroadcastReceiver {
  private static final String CHANNEL="neurosync_reminders";
  private static final String PREFIX="alarm_";
  private static final String ACTION_REMIND="com.neurosync.familia.REMIND";
  public AlarmReceiver(){}
  private static PendingIntent pending(Context ctx,String id){
    Intent intent=new Intent(ctx,AlarmReceiver.class).setAction(ACTION_REMIND).putExtra("id",id);
    return PendingIntent.getBroadcast(ctx,id.hashCode(),intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  }
  public static boolean schedule(Context ctx,String id,String title,long at,boolean daily){
    try{
      if(id==null||id.length()<2||id.length()>100)return false;
      if(title==null||title.trim().isEmpty())title="Recordatorio";
      long when=at;
      if(daily)while(when<=System.currentTimeMillis())when+=86400000L;
      if(when<=System.currentTimeMillis())return false;
      JSONObject d=new JSONObject().put("id",id).put("title",title).put("when",when).put("daily",daily);
      ctx.getSharedPreferences("ns_alarms",0).edit().putString(PREFIX+id,d.toString()).apply();
      AlarmManager manager=(AlarmManager)ctx.getSystemService(Context.ALARM_SERVICE);
      manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,pending(ctx,id));
      return true;
    }catch(Exception e){return false;}
  }
  public static void cancel(Context ctx,String id){
    if(id==null)return;
    ((AlarmManager)ctx.getSystemService(Context.ALARM_SERVICE)).cancel(pending(ctx,id));
    ctx.getSharedPreferences("ns_alarms",0).edit().remove(PREFIX+id).apply();
  }
  private static void reschedule(Context ctx){
    Map<String,?> alarms=ctx.getSharedPreferences("ns_alarms",0).getAll();
    for(Map.Entry<String,?> row:alarms.entrySet()){
      if(!row.getKey().startsWith(PREFIX))continue;
      try{
        JSONObject entry=new JSONObject((String)row.getValue());
        schedule(ctx,entry.getString("id"),entry.getString("title"),entry.getLong("when"),entry.optBoolean("daily"));
      }catch(Exception ignored){}
    }
  }
  @Override public void onReceive(Context context,Intent intent){
    if(intent==null)return;
    if(Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())){reschedule(context);return;}
    if(!ACTION_REMIND.equals(intent.getAction()))return;
    String id=intent.getStringExtra("id");if(id==null)return;
    String saved=context.getSharedPreferences("ns_alarms",0).getString(PREFIX+id,null);
    if(saved==null)return;
    try{
      JSONObject info=new JSONObject(saved);
      NotificationManager manager=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
      NotificationChannel channel=new NotificationChannel(CHANNEL,"Mis alarmas",NotificationManager.IMPORTANCE_HIGH);
      channel.setDescription("Recordatorios de tareas y horarios");
      manager.createNotificationChannel(channel);
      PendingIntent open=PendingIntent.getActivity(context,0,new Intent(context,MainActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
      Notification note=new Notification.Builder(context,CHANNEL)
        .setContentTitle("⏰ NeuroSync: "+info.optString("title","Recordatorio"))
        .setContentText("Es hora de revisar tu actividad")
        .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
        .setAutoCancel(true).setContentIntent(open).build();
      manager.notify(id.hashCode(),note);
      if(info.optBoolean("daily"))schedule(context,id,info.optString("title"),info.optLong("when")+86400000L,true);
      else context.getSharedPreferences("ns_alarms",0).edit().remove(PREFIX+id).apply();
    }catch(Exception ex){android.util.Log.w("NeuroSync","No se pudo presentar la alarma");}
  }
}