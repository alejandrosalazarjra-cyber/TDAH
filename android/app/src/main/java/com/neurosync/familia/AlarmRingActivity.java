package com.neurosync.familia;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Pantalla de control. El sonido lo mantiene AlarmAudioService aunque no abra esta pantalla. */
public final class AlarmRingActivity extends Activity {
 private String id;
 @Override public void onCreate(Bundle state){
   super.onCreate(state);
   if(Build.VERSION.SDK_INT>=27){setShowWhenLocked(true);setTurnScreenOn(true);}
   else getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED|WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
   getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
   id=getIntent().getStringExtra("id");
   String title=getIntent().getStringExtra("title");
   if(id==null){finish();return;}
   LinearLayout root=new LinearLayout(this);
   root.setPadding(28,36,28,36);
   root.setOrientation(LinearLayout.VERTICAL);
   root.setGravity(Gravity.CENTER);
   root.setBackgroundColor(0xFF111215);
   TextView bell=new TextView(this);bell.setText("⏰");bell.setTextSize(64);
   bell.setGravity(Gravity.CENTER);
   TextView heading=new TextView(this);heading.setText(title==null?"Alarma":title);
   heading.setTextSize(29);heading.setTextColor(0xFFFFFFFF);heading.setGravity(Gravity.CENTER);
   heading.setPadding(0,20,0,40);
   Button stop=new Button(this);stop.setText("Detener alarma");
   stop.setOnClickListener(v->{AlarmReceiver.dismiss(this,id);finish();});
   Button snooze=new Button(this);snooze.setText("Posponer 5 minutos");
   snooze.setOnClickListener(v->{AlarmReceiver.snooze(this,id,5);finish();});
   root.addView(bell);root.addView(heading);root.addView(stop);root.addView(snooze);
   setContentView(root);
 }
 @Override public void onBackPressed(){ /* Elige detener o posponer explícitamente. */ }
}