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
  /** Consulta de eventos públicos con cursor por cuenta, nunca con un token del WebView.
   * JobScheduler es un respaldo no instantáneo; FCM sigue pendiente.
   */
  private boolean pollPublic()throws Exception {
    String base=BuildConfig.PUBLIC_API_BASE_URL;
    if(base==null||!base.startsWith("https://"))return false;
    JSONObject saved=PublicSessionVault.restore(this,"public",base);
    if(saved==null)return false;
    String token=saved.optString("token","");
    JSONObject user=saved.optJSONObject("user");
    if(token.length()<40||user==null)return false;
    String uid=user.optString("id","");
    if(!uid.matches("[0-9a-fA-F-]{36}"))return false;
    android.content.SharedPreferences prefs=
      getSharedPreferences("ns_public_background_events_v1",MODE_PRIVATE);
    String key="cursor_"+uid;
    String cursor=prefs.getString(key,"");
    if(cursor.isEmpty()){
      JSONObject initial=publicGet(base,"/v1/events/latest",token);
      if(initial==null||!initial.optBoolean("ok"))return true;
      String latest=initial.optString("nextCursor","0");
      if(latest.matches("[0-9]{1,18}"))prefs.edit().putString(key,latest).commit();
      return true; // Primera consulta: sin avisos de acciones históricas.
    }
    if(!cursor.matches("[0-9]{1,18}"))return true;
    JSONObject response=publicGet(base,"/v1/events?cursor="+cursor,token);
    if(response==null||!response.optBoolean("ok"))return true;
    JSONObject still=PublicSessionVault.restore(this,"public",base);
    if(still==null||!token.equals(still.optString("token","")))return true;
    JSONArray events=response.optJSONArray("events");
    if(events==null)return true;
    String next=response.optString("nextCursor",cursor);
    if(!next.matches("[0-9]{1,18}"))return true;
    if(!prefs.edit().putString(key,next).commit())return true;
    if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
      !=PackageManager.PERMISSION_GRANTED)return true;
    NotificationManager manager=getSystemService(NotificationManager.class);
    if(manager==null||!manager.areNotificationsEnabled())return true;
    final String channel="ns_public_tasks_v1";
    manager.createNotificationChannel(new NotificationChannel(channel,"Tareas de NeuroSync",
      NotificationManager.IMPORTANCE_DEFAULT));
    int shown=0;
    for(int i=0;i<events.length()&&shown<5;i++){
      JSONObject event=events.optJSONObject(i);if(event==null)continue;
      String type=event.optString("event_type","");
      String role=event.optString("task_role","");
      boolean received="task_received".equals(type)&&"recipient".equals(role);
      boolean progress="sender".equals(role)&&
        ("task_accepted".equals(type)||"task_completed".equals(type)||
         "task_declined".equals(type));
      if(!received&&!progress)continue;
      String title=received?"Nueva tarea recibida":
        "task_accepted".equals(type)?"Tu tarea fue aceptada":
        "task_completed".equals(type)?"Tu tarea fue completada":"Tu tarea fue rechazada";
      int id=(uid+":"+event.optString("sequence","")).hashCode()&0x7fffffff;
      Intent open=new Intent(this,MainActivity.class).putExtra("neurosync_view","tasks")
        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
      PendingIntent action=PendingIntent.getActivity(this,id,open,
        PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
      Notification notice=new Notification.Builder(this,channel)
        .setSmallIcon(android.R.drawable.stat_notify_more)
        .setContentTitle(title).setContentText("Abre NeuroSync para consultar los detalles")
        .setVisibility(Notification.VISIBILITY_PRIVATE).setContentIntent(action)
        .setAutoCancel(true).build();
      manager.notify("ns_public_"+uid,id,notice);
      shown++;
    }
    return true;
  }
  private JSONObject publicGet(String base,String path,String token)throws Exception {
    URL target=new URL(base+path);
    if(!"https".equalsIgnoreCase(target.getProtocol()))return null;
    HttpURLConnection c=(HttpURLConnection)target.openConnection();
    try{
      c.setRequestMethod("GET");c.setInstanceFollowRedirects(false);
      c.setConnectTimeout(4000);c.setReadTimeout(5000);
      c.setRequestProperty("Accept","application/json");
      c.setRequestProperty("Authorization","Bearer "+token);
      int status=c.getResponseCode();
      if(status!=200)return null;
      ByteArrayOutputStream output=new ByteArrayOutputStream();
      try(InputStream input=c.getInputStream()){
        byte[] buf=new byte[4096];int n;
        while((n=input.read(buf))!=-1){
          output.write(buf,0,n);
          if(output.size()>100000)throw new IOException("Respuesta pública excesiva");
        }
      }
      return new JSONObject(output.toString("UTF-8"));
    }finally{c.disconnect();}
  }

  private void poll()throws Exception {
    if(pollPublic())return;
    // El servicio comparte las preferencias privadas de MainActivity por su nombre explícito.
    android.content.SharedPreferences settings=getSharedPreferences("MainActivity",MODE_PRIVATE);
    String mode=settings.getString("mode","");
    String location="host";
    if("client".equals(mode))location=settings.getString("server","");
    else if("cloud".equals(mode))location=settings.getString("cloud","");
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
