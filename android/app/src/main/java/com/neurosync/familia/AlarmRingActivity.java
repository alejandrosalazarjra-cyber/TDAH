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
  String custom=getSharedPreferences("ns_sound",0).getString("uri","");
  Uri local=custom.isEmpty()?null:Uri.parse(custom);
  if(!startSound(local)){
    Uri system=RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
    if(!startSound(system))startSound(Settings.System.DEFAULT_NOTIFICATION_URI);
  }
 }
 private boolean startSound(Uri uri){
   if(uri==null)return false;
   try{
     stopSound();
     player=new MediaPlayer();
     player.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM)
       .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build());
     player.setDataSource(this,uri);player.setLooping(true);player.prepare();player.start();return true;
   }catch(Exception e){android.util.Log.w("NeuroSync","Intentando otro tono de alarma",e);stopSound();return false;}
 }
 @Override protected void onDestroy(){stopSound();super.onDestroy();}
 @Override public void onBackPressed(){ /* Evita cerrar accidentalmente sin detener o posponer. */ }
}