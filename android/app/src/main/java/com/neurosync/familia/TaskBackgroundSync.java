package com.neurosync.familia;

import android.Manifest;
import android.app.*;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.PersistableBundle;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

/** Sondeo limitado por Android (mínimo ~15 minutos, sin garantía de puntualidad).
 * Solo consulta identificadores y estados; nunca descarga tareas privadas completas.
 * No inicia servicios de primer plano ni usa alarmas exactas.
 */
public final class TaskBackgroundSync extends JobService {
  private static final int JOB_ID=17026;
  private static final String CHANNEL="ns_family_tasks_v1";
  private final ExecutorService executor=Executors.newSingleThreadExecutor();
  private Future<?> running;

  static void schedule(Context context) {
    try {
      JobScheduler scheduler=(JobScheduler)context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
      if(scheduler==null)return;
      JobInfo job=new JobInfo.Builder(JOB_ID,new ComponentName(context,TaskBackgroundSync.class))
        .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
        .setPersisted(false)
        .setPeriodic(15*60*1000L).build();
      scheduler.schedule(job);
    }catch(Exception e){android.util.Log.w("NeuroSync","No se pudo programar consulta de tareas",e);}
  }
  static void cancel(Context context){
    JobScheduler scheduler=(JobScheduler)context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
    if(scheduler!=null)scheduler.cancel(JOB_ID);
  }
  @Override public boolean onStartJob(JobParameters params){
    running=executor.submit(()->{
      try{poll();}catch(Exception e){
        android.util.Log.w("NeuroSync","Consulta de tareas pospuesta",e);
      }finally{jobFinished(params,false);}
    });
    return true;
  }
  @Override public boolean onStopJob(JobParameters params){
    if(running!=null)running.cancel(true);
    return true;
  }
  private static boolean privateIp(String ip){
    if(ip==null||!ip.matches("(?:\\d{1,3}\\.){3}\\d{1,3}"))return false;
    String[] parts=ip.split("\\.");
    int[] n=new int[4];
    try{
      for(int i=0;i<4;i++){n[i]=Integer.parseInt(parts[i]);if(n[i]>255)return false;}
    }catch(Exception e){return false;}
    return n[0]==10||(n[0]==192&&n[1]==168)||
      (n[0]==172&&n[1]>=16&&n[1]<=31)||(n[0]==100&&n[1]>=64&&n[1]<=127);
  }
  private JSONObject request(String mode,String location,String token)throws Exception{
    if("host".equals(mode)){
      JSONObject actor=AccountStore.identity(this,token);
      return actor==null?null:SyncStore.notificationDigest(this,actor);
    }
    URL url;
    if("client".equals(mode)&&privateIp(location)){
      url=new URL("http://"+location+":"+SyncService.PORT+"/notification-digest");
    }else if("cloud".equals(mode)){
      URI base=new URI(location);
      if(!"https".equalsIgnoreCase(base.getScheme())||base.getHost()==null||
        base.getUserInfo()!=null||base.getQuery()!=null||base.getFragment()!=null||
        !(base.getPath()==null||base.getPath().isEmpty()||"/".equals(base.getPath())))
        return null;
      url=new URL(location.replaceAll("/$","")+"/notification-digest");
    }else{return null;}
    HttpURLConnection c=(HttpURLConnection)url.openConnection();
    try{
      c.setRequestMethod("POST");
      c.setConnectTimeout(4000);c.setReadTimeout(5000);c.setUseCaches(false);
      c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json");
      c.setRequestProperty("Authorization","Bearer "+token);
      byte[] bytes="{}".getBytes(StandardCharsets.UTF_8);
      c.setFixedLengthStreamingMode(bytes.length);
      try(OutputStream out=c.getOutputStream()){out.write(bytes);}
      if(c.getResponseCode()!=200)return null;
      ByteArrayOutputStream result=new ByteArrayOutputStream();
      try(InputStream in=c.getInputStream()){
        byte[] block=new byte[4096];int n;
        while((n=in.read(block))!=-1){
          result.write(block,0,n);
          if(result.size()>160000)throw new IOException("Respuesta demasiado grande");
        }
      }
      return new JSONObject(result.toString("UTF-8"));
    }finally{c.disconnect();}
  }
  private void poll()throws Exception {
    String mode=getPreferences(0).getString("mode","");
    String location="host";
    if("client".equals(mode))location=getPreferences(0).getString("server","");
    else if("cloud".equals(mode))location=getPreferences(0).getString("cloud","");
    if(!("host".equals(mode)||"client".equals(mode)||"cloud".equals(mode)))return;
    JSONObject saved=SessionVault.restore(this,mode,location);
    if(saved==null)return;
    JSONObject user=saved.optJSONObject("user");
    String token=saved.optString("token","");
    if(user==null||token.length()<30)return;
    String username=user.optString("username","");
    if(username.isEmpty())return;
    JSONObject digest=request(mode,location,token);
    if(digest==null||!digest.optBoolean("ok"))return;
    // No presentar datos de una sesión que se cerró o se cambió durante la consulta.
    JSONObject current=SessionVault.restore(this,mode,location);
    if(current==null||!token.equals(current.optString("token"))||
      !username.equals(current.optJSONObject("user")==null?"":current.optJSONObject("user").optString("username")))return;
    android.content.SharedPreferences prefs=getSharedPreferences("ns_background_task_digest",MODE_PRIVATE);
    String key="state_"+username;
    JSONObject previous=null;
    String old=prefs.getString(key,"");
    if(!old.isEmpty()){try{previous=new JSONObject(old);}catch(Exception e){return;}}
    JSONObject next=new JSONObject();
    JSONArray received=digest.optJSONArray("received"),sent=digest.optJSONArray("sent");
    if(received!=null)for(int i=0;i<Math.min(received.length(),3000);i++){
      JSONObject t=received.optJSONObject(i);
      if(t==null)continue;
      String id=t.optString("id",""),from=t.optString("sentBy","");
      if(id.startsWith("sent_")&&id.length()<100&&from.length()>0)
        next.put("r:"+id,"received");
    }
    if(sent!=null)for(int i=0;i<Math.min(sent.length(),3000);i++){
      JSONObject t=sent.optJSONObject(i);
      if(t==null)continue;
      String id=t.optString("id",""),status=t.optString("deliveryStatus","");
      if(id.startsWith("sent_")&&id.length()<100&&
         ("pending".equals(status)||"accepted".equals(status)||"completed".equals(status)))
        next.put("s:"+id,status);
    }
    if(!prefs.edit().putString(key,next.toString()).commit())return;
    if(previous==null)return; // La primera consulta crea una línea base sin ruido.
    if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
      !=PackageManager.PERMISSION_GRANTED)return;
    NotificationManager manager=getSystemService(NotificationManager.class);
    if(manager==null||!manager.areNotificationsEnabled())return;
    manager.createNotificationChannel(new NotificationChannel(CHANNEL,"Tareas familiares",
      NotificationManager.IMPORTANCE_DEFAULT));
    Iterator<String> keys=next.keys();int shown=0;
    while(keys.hasNext()&&shown<5){
      String item=keys.next(),value=next.optString(item);
      String last=previous.optString(item,"");
      boolean fresh=item.startsWith("r:")&&last.isEmpty();
      boolean progress=item.startsWith("s:")&&
        (("pending".equals(last)&&("accepted".equals(value)||"completed".equals(value)))||
         ("accepted".equals(last)&&"completed".equals(value)));
      if(!fresh&&!progress)continue;
      String title=fresh?"Nueva tarea recibida":("accepted".equals(value)?"Tarea aceptada":"Tarea completada");
      int id=(username+":"+item+":"+value).hashCode()&0x7fffffff;
      Intent open=new Intent(this,MainActivity.class).putExtra("neurosync_view","tasks")
        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
      PendingIntent action=PendingIntent.getActivity(this,id,open,
        PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
      Notification notice=new Notification.Builder(this,CHANNEL)
        .setSmallIcon(android.R.drawable.stat_notify_more)
        .setContentTitle(title)
        .setContentText("Revisa tu bandeja de tareas en NeuroSync")
        .setVisibility(Notification.VISIBILITY_PRIVATE)
        .setContentIntent(action).setAutoCancel(true).build();
      manager.notify("ns_background_"+username,id,notice);
      shown++;
    }
  }
  @Override public void onDestroy(){executor.shutdownNow();super.onDestroy();}
}
