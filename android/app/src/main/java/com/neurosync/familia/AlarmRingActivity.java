package com.neurosync.familia;

import android.app.*;
import android.content.*;
import android.media.*;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;

/** Pantalla de alarma real: aparece sobre bloqueo cuando Android concede pantalla completa.
 * Reproduce el sonido de alarma del sistema y deja detener o posponer.
 */
public final class AlarmRingActivity extends Activity {
 private MediaPlayer player;
 private String id, title;
 private void stopSound(){
   try{if(player!=null){player.stop();player.release();player=null;}}catch(Exception ignored){}
 }
 @Override public void onCreate(Bundle saved){
  super.onCreate(saved);
  if(Build.VERSION.SDK_INT>=27){setShowWhenLocked(true);setTurnScreenOn(true);}
  else getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED|WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
  getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
  id=getIntent().getStringExtra("id");title=getIntent().getStringExtra("title");
  if(id==null){finish();return;}
  LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);
  root.setGravity(Gravity.CENTER);root.setPadding(30,40,30,40);root.setBackgroundColor(0xFF172448);
  TextView bell=new TextView(this);bell.setText("⏰");bell.setTextSize(72);bell.setGravity(Gravity.CENTER);
  TextView heading=new TextView(this);heading.setText(title==null?"Alarma":title);heading.setTextColor(0xFFFFFFFF);
  heading.setTextSize(28);heading.setGravity(Gravity.CENTER);heading.setPadding(0,22,0,36);
  Button dismiss=new Button(this);dismiss.setText("Detener alarma");
  dismiss.setOnClickListener(v->{AlarmReceiver.dismiss(this,id);stopSound();finish();});
  Button snooze=new Button(this);snooze.setText("Posponer 5 minutos");
  snooze.setOnClickListener(v->{AlarmReceiver.snooze(this,id,5);stopSound();finish();});
  root.addView(bell);root.addView(heading);root.addView(dismiss);root.addView(snooze);
  setContentView(root);
  try{
   String custom=getSharedPreferences("ns_sound",0).getString("uri","");
   Uri uri=custom.isEmpty()?null:Uri.parse(custom);
   if(uri==null)uri=android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM);
   if(uri==null)uri=Settings.System.DEFAULT_NOTIFICATION_URI;
   player=new MediaPlayer();
   player.setDataSource(this,uri);
   player.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM)
     .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build());
   player.setLooping(true);player.prepare();player.start();
  }catch(Exception error){android.util.Log.e("NeuroSync","No se pudo reproducir audio de alarma",error);}
 }
 @Override protected void onDestroy(){stopSound();super.onDestroy();}
 @Override public void onBackPressed(){ /* Evita cerrar accidentalmente sin detener o posponer. */ }
}