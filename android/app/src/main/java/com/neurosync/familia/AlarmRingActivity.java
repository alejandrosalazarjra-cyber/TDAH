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
 private String memoryCode="";
 private final Handler uiHandler=new Handler(Looper.getMainLooper());
 private static final String[] WAKE_PHRASES={"Hoy elijo avanzar paso a paso","Respiro y comienzo mi dia","Una accion pequeña cuenta"};
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
   result.setMinHeight(dp(54));result.setTextSize(15);
   result.setTextColor(Color.WHITE);
   android.graphics.drawable.GradientDrawable bg=new android.graphics.drawable.GradientDrawable();
   bg.setColor(0xFF465FD6);bg.setCornerRadius(dp(16));result.setBackground(bg);
   LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,dp(54));
   params.setMargins(0,dp(5),0,dp(5));result.setLayoutParams(params);
   return result;
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
   android.graphics.drawable.GradientDrawable background=new android.graphics.drawable.GradientDrawable(
     android.graphics.drawable.GradientDrawable.Orientation.TL_BR,new int[]{0xFF101A31,0xFF233963,0xFF131C34});
   root.setBackground(background);
   ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.addView(root);
   setContentView(scroll);
   root.addView(label("☀",52));
   root.addView(label("ES MOMENTO DE DESPERTAR",14));
   root.addView(label(title==null?"Mi alarma":title,29));
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
   }else if("memory".equals(challenge)){
     // El código se muestra unos segundos y se oculta; no requiere cámara ni internet.
     memoryCode=String.format(java.util.Locale.US,"%04d",1000+random.nextInt(9000));
     progress.setText("Memoriza el código durante 8 segundos");
     prompt.setText(memoryCode);
     input=new EditText(this);input.setSingleLine(true);input.setTextColor(Color.WHITE);
     input.setHintTextColor(0xFF9EAECA);input.setHint("Escribe el código");
     input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
     input.setVisibility(android.view.View.GONE);root.addView(input);
     Button check=button("Comprobar código");check.setVisibility(android.view.View.GONE);root.addView(check);
     uiHandler.postDelayed(()->{
       if(isFinishing()||isDestroyed())return;
       prompt.setText("¿Qué código viste?");progress.setText("Recuerda los cuatro números");
       input.setVisibility(android.view.View.VISIBLE);check.setVisibility(android.view.View.VISIBLE);
     },8000);
     check.setOnClickListener(v->{
       if(memoryCode.equals(input.getText().toString().trim()))finishAlarm();
       else{input.setError("Revisa el código e inténtalo de nuevo");}
     });
   }else if("typing".equals(challenge)){
     String phrase=WAKE_PHRASES[random.nextInt(WAKE_PHRASES.length)];
     progress.setText("Escribe la frase para completar el desafío");
     prompt.setText(phrase);
     input=new EditText(this);input.setSingleLine(false);input.setMinLines(2);
     input.setTextColor(Color.WHITE);input.setHintTextColor(0xFF9EAECA);
     input.setHint("Escribe aquí la frase");root.addView(input);
     Button check=button("Comprobar frase");root.addView(check);
     check.setOnClickListener(v->{
       String actual=input.getText().toString().trim().replaceAll("\\s+"," ");
       if(phrase.equalsIgnoreCase(actual))finishAlarm();
       else input.setError("Escribe la frase como aparece arriba");
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
     emergency.setOnClickListener(v->{
       emergency.setVisibility(android.view.View.GONE);
       TextView message=label("Puedes detener la alarma sin completar el reto.",15);
       root.addView(message);
       Button confirm=button("Sí, detener ahora");root.addView(confirm);
       confirm.setOnClickListener(w->finishAlarm());
       Button back=button("Volver al desafío");root.addView(back);
       back.setOnClickListener(w->{root.removeView(message);root.removeView(confirm);root.removeView(back);emergency.setVisibility(android.view.View.VISIBLE);});
     });
   }
 }
 @Override protected void onDestroy(){uiHandler.removeCallbacksAndMessages(null);super.onDestroy();}
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
   else Toast.makeText(this,"Usa Posponer o Detener sin desafío",Toast.LENGTH_LONG).show();
 }
}