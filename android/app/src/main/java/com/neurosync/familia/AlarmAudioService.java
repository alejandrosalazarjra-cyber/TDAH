package com.neurosync.familia;

import android.app.*;
import android.content.*;
import android.media.*;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;

/** Reproduce alarmas sin esperar a que el usuario abra la notificación. */
public final class AlarmAudioService extends Service {
  private static final String CHANNEL="neurosync_audio_alarma_v1";
  public static final String START="com.neurosync.familia.audio.START";
  public static final String STOP="com.neurosync.familia.audio.STOP";
  private MediaPlayer player;
  private ToneGenerator backupTone;
  private final Handler handler=new Handler(Looper.getMainLooper());
  private final Runnable stopLater=()->stopSelf();

  public static void start(Context context,String id,String title){
    Intent intent=new Intent(context,AlarmAudioService.class).setAction(START)
      .putExtra("id",id).putExtra("title",title);
    if(Build.VERSION.SDK_INT>=26)context.startForegroundService(intent);
    else context.startService(intent);
  }
  public static void stop(Context context){
    context.stopService(new Intent(context,AlarmAudioService.class));
  }
  @Override public int onStartCommand(Intent intent,int flags,int startId){
    if(intent==null||STOP.equals(intent.getAction())){stopSelf();return START_NOT_STICKY;}
    if(!START.equals(intent.getAction()))return START_NOT_STICKY;
    String id=intent.getStringExtra("id");
    String title=intent.getStringExtra("title");
    if(id==null){stopSelf();return START_NOT_STICKY;}
    NotificationManager nm=getSystemService(NotificationManager.class);
    NotificationChannel channel=new NotificationChannel(CHANNEL,"Sonido de alarmas",NotificationManager.IMPORTANCE_LOW);
    channel.setSound(null,null);
    nm.createNotificationChannel(channel);
    Intent open=new Intent(this,AlarmRingActivity.class).putExtra("id",id).putExtra("title",title);
    PendingIntent pending=PendingIntent.getActivity(this,id.hashCode(),open,
      PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    Notification notification=new Notification.Builder(this,CHANNEL)
      .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
      .setContentTitle("Alarma: "+(title==null?"Recordatorio":title))
      .setContentText("Sonando · toca para detener o posponer")
      .setCategory(Notification.CATEGORY_ALARM).setContentIntent(pending).setOngoing(true).build();
    try{
      if(Build.VERSION.SDK_INT>=29)
        startForeground(701,notification,android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
      else startForeground(701,notification);
      playAudio();
      handler.removeCallbacks(stopLater);
      handler.postDelayed(stopLater,180000); // Límite de seguridad: no sonar indefinidamente.
    }catch(Exception error){
      android.util.Log.e("NeuroSync","No se pudo iniciar reproducción de alarma",error);
      stopSelf();
    }
    return START_NOT_STICKY;
  }
  private void playAudio(){
    release();
    String custom=getSharedPreferences("ns_sound",0).getString("uri","");
    Uri[] candidates=new Uri[]{
      custom.isEmpty()?null:Uri.parse(custom),
      RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
      Settings.System.DEFAULT_NOTIFICATION_URI
    };
    for(Uri uri:candidates){
      if(uri==null)continue;
      try{
        MediaPlayer attempt=new MediaPlayer();
        attempt.setAudioAttributes(new AudioAttributes.Builder()
          .setUsage(AudioAttributes.USAGE_ALARM)
          .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build());
        attempt.setDataSource(this,uri);
        attempt.setLooping(true);attempt.prepare();attempt.start();
        player=attempt;return;
      }catch(Exception error){android.util.Log.w("NeuroSync","Tono no disponible, probando siguiente");}
    }
    try{
      backupTone=new ToneGenerator(AudioManager.STREAM_ALARM,90);
      backupTone.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD,180000);
    }catch(Exception e){android.util.Log.e("NeuroSync","Sin reproductor de emergencia",e);}
  }
  private void release(){
    if(player!=null){try{player.stop();}catch(Exception ignored){}player.release();player=null;}
    if(backupTone!=null){backupTone.stopTone();backupTone.release();backupTone=null;}
  }
  @Override public void onDestroy(){handler.removeCallbacks(stopLater);release();super.onDestroy();}
  @Override public IBinder onBind(Intent intent){return null;}
}