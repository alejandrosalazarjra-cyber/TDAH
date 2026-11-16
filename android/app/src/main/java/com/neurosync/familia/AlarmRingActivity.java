package com.neurosync.familia;

import android.app.Activity;
import android.os.*;
import android.graphics.Color;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.*;
import java.util.Random;

/** Alarma con un desafío opcional. Siempre tiene salida segura sin reto. */
public final class AlarmRingActivity extends Activity {
 private String id;
 private String challenge;
 private int remaining=3, correct=0, tapped=0;
 private int answer;
 private TextView prompt, progress;
 private EditText input;
 private LinearLayout root;
 private long openedAt;
 private final Random random=new Random();
 private int dp(int px){return Math.round(getResources().getDisplayMetrics().density*px);}
 private TextView label(String text,int size){
   TextView view=new TextView(this);view.setText(text);view.setTextColor(Color.WHITE);
   view.setTextSize(size);view.setGravity(Gravity.CENTER);view.setPadding(dp(8),dp(12),dp(8),dp(12));
   return view;
 }
 private Button button(String text){
   Button result=new Button(this);result.setText(text);result.setAllCaps(false);
   result.setMinHeight(dp(52));return result;
 }
 private void finishAlarm(){
   AlarmReceiver.dismiss(this,id);finish();
 }
 @Override public void onCreate(Bundle state){
   super.onCreate(state);
   if(Build.VERSION.SDK_INT>=27){setShowWhenLocked(true);setTurnScreenOn(true);}
   else getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED|WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
   getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
   id=getIntent().getStringExtra("id");
   if(id==null){finish();return;}
   openedAt=SystemClock.elapsedRealtime();
   challenge=AlarmChallengeStore.get(this,id);
   String title=getIntent().getStringExtra("title");
   root=new LinearLayout(this);
   root.setOrientation(LinearLayout.VERTICAL);
   root.setGravity(Gravity.CENTER);
   root.setPadding(dp(22),dp(24),dp(22),dp(24));
   root.setBackgroundColor(0xFF111215);
   ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.addView(root);
   setContentView(scroll);
   root.addView(label("⏰",54));
   root.addView(label(title==null?"Alarma":title,27));
   progress=label("",15);root.addView(progress);
   prompt=label("",22);root.addView(prompt);
   if("math".equals(challenge)){
     input=new EditText(this);input.setSingleLine(true);input.setTextColor(Color.WHITE);
     input.setHintTextColor(0xFFB0B0B0);input.setHint("Tu respuesta");
     input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER|android.text.InputType.TYPE_NUMBER_FLAG_SIGNED);
     root.addView(input);
     Button check=button("Comprobar respuesta");root.addView(check);
     check.setOnClickListener(v->{
       int value;
       try{value=Integer.parseInt(input.getText().toString().trim());}
       catch(Exception ex){input.setError("Escribe el resultado");return;}
       if(value!=answer){input.setError("Inténtalo de nuevo");return;}
       if(++correct>=remaining)finishAlarm();
       else nextQuestion();
     });
     nextQuestion();
   }else if("taps".equals(challenge)){
     progress.setText("Toca el botón 12 veces para desactivar la alarma");
     Button tap=button("Tocar · 0 / 12");root.addView(tap);
     tap.setOnClickListener(v->{
       tapped++;tap.setText("Tocar · "+tapped+" / 12");
       if(tapped>=12)finishAlarm();
     });
   }else{
     Button stop=button("Detener alarma");root.addView(stop);
     stop.setOnClickListener(v->finishAlarm());
   }
   Button snooze=button("Posponer 5 minutos");root.addView(snooze);
   snooze.setOnClickListener(v->{AlarmReceiver.snooze(this,id,5);finish();});
   if(!"none".equals(challenge)){
     Button emergency=button("Detener sin desafío");
     root.addView(emergency);
     emergency.setOnClickListener(v->new android.app.AlertDialog.Builder(this)
       .setTitle("¿Detener sin completar?")
       .setMessage("Puedes detener la alarma sin el desafío si lo necesitas. Esta opción siempre estará disponible.")
       .setNegativeButton("Volver al desafío",null)
       .setPositiveButton("Detener ahora",(d,w)->finishAlarm()).show());
   }
 }
 private void nextQuestion(){
   int x=3+random.nextInt(16),y=2+random.nextInt(14);
   answer=x+y;
   progress.setText("Desafío "+(correct+1)+" de "+remaining);
   prompt.setText(x+" + "+y+" = ?");
   input.setText("");
 }
 @Override public void onBackPressed(){
   // Si necesita detenerse, el botón seguro está siempre visible.
   if("none".equals(challenge))finishAlarm();
   else new android.app.AlertDialog.Builder(this).setTitle("Alarma activa")
     .setMessage("Elige Posponer o Detener sin desafío si no puedes continuar.")
     .setPositiveButton("Entendido",null).show();
 }
}