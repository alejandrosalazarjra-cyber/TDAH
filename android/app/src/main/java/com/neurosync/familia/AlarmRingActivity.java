package com.neurosync.familia;

import android.app.Activity;
import android.os.*;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.*;
import java.util.Random;

/** Alarma con un desafío opcional. Siempre tiene salida segura sin reto. */
public final class AlarmRingActivity extends Activity implements SensorEventListener {
 private String id;
 private String challenge;
 private String routineType="";
 private int routineStep=0;
 private String alarmTitle="";
 private int remaining=3, correct=0, tapped=0;
 private int answer;
 private String memoryCode="";
 private SensorManager sensors;
 private int shakes=0;
 private long lastShake=0;
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
   bg.setColor(0xFF168BFA);bg.setCornerRadius(dp(16));result.setBackground(bg);
   LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,dp(54));
   params.setMargins(0,dp(5),0,dp(5));result.setLayoutParams(params);
   return result;
 }
 private void forceDismissAlarm(){
   AlarmReceiver.dismiss(this,id);finish();
 }
 private void finishAlarm(){
   if(!routineType.isEmpty()){
     String[] stages=routineStages(routineType);
     if(routineStep+1<stages.length){
       android.content.Intent next=new android.content.Intent(this,AlarmRingActivity.class)
         .putExtra("id",id).putExtra("title",alarmTitle)
         .putExtra("routineType",routineType).putExtra("routineStep",routineStep+1);
       startActivity(next);finish();return;
     }
   }
   forceDismissAlarm();
 }
 private String[] routineStages(String kind){
   return "routine_active".equals(kind)?new String[]{"memory","shake","math"}:new String[]{"math","typing"};
 }
 @Override public void onCreate(Bundle state){
   super.onCreate(state);
   if(Build.VERSION.SDK_INT>=27){setShowWhenLocked(true);setTurnScreenOn(true);}
   else getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED|WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
   getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_FULLSCREEN);
   // Experiencia de alarma independiente de la WebView y sin barras superpuestas.
   getWindow().getDecorView().setSystemUiVisibility(
     android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
     | android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
     | android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
     | android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
     | android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
     | android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
   id=getIntent().getStringExtra("id");
   if(id==null){finish();return;}
   openedAt=SystemClock.elapsedRealtime();
   challenge=AlarmChallengeStore.get(this,id);
   String configured=challenge;
   routineType=getIntent().getStringExtra("routineType");
   if(routineType==null)routineType="";
   if(routineType.isEmpty()&&("routine_focus".equals(configured)||"routine_active".equals(configured)))
      routineType=configured;
   if(!routineType.isEmpty()){
     if(!"routine_focus".equals(routineType)&&!"routine_active".equals(routineType))
       routineType="";
     else{
       routineStep=Math.max(0,Math.min(getIntent().getIntExtra("routineStep",0),routineStages(routineType).length-1));
       challenge=routineStages(routineType)[routineStep];
     }
   }
   String title=getIntent().getStringExtra("title");
   alarmTitle=title==null?"Mi alarma":title;
   root=new LinearLayout(this);
   root.setOrientation(LinearLayout.VERTICAL);
   root.setGravity(Gravity.CENTER);
   root.setPadding(dp(24),dp(26),dp(24),dp(36));
   android.graphics.drawable.GradientDrawable background=new android.graphics.drawable.GradientDrawable(
     android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,new int[]{0xFF0C1730,0xFF101827,0xFF090D17});
   root.setBackground(background);
   ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.addView(root);
   setContentView(scroll);
   TextView wakeMark=label("✦",31);wakeMark.setTextColor(0xFF168BFA);
   root.addView(wakeMark);
   TextView eyebrow=label("NEUROSYNC · DESPERTAR",12);
   eyebrow.setTextColor(0xFF93A8C2);root.addView(eyebrow);
   TextView currentTime=label(new java.text.SimpleDateFormat("HH:mm",java.util.Locale.getDefault()).format(new java.util.Date()),70);
   currentTime.setTextColor(0xFF168BFA);
   currentTime.setTypeface(android.graphics.Typeface.DEFAULT,android.graphics.Typeface.BOLD);
   currentTime.setPadding(dp(2),dp(0),dp(2),dp(5));
   root.addView(currentTime);
   TextView subtitle=label(alarmTitle,24);subtitle.setTextColor(0xFFF1F7FF);
   root.addView(subtitle);
   TextView invitation=label("Despierta a tu ritmo",14);
   invitation.setTextColor(0xFF98B1C9);root.addView(invitation);
   Runnable updateClock=new Runnable(){
     @Override public void run(){
       if(isFinishing()||isDestroyed())return;
       currentTime.setText(new java.text.SimpleDateFormat("HH:mm",java.util.Locale.getDefault()).format(new java.util.Date()));
       uiHandler.postDelayed(this,30000);
     }
   };
   uiHandler.postDelayed(updateClock,30000);
   if(!routineType.isEmpty()){
     root.addView(label("RUTINA DE DESPERTAR · ETAPA "+(routineStep+1)+" DE "+routineStages(routineType).length,14));
   }
   progress=label("",15);progress.setTextColor(0xFF91B7D8);root.addView(progress);
   prompt=label("",22);root.addView(prompt);
   if("math".equals(challenge)||"math_hard".equals(challenge)){
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
   }else if("qr".equals(challenge)){
     if(!AlarmChallengeStore.hasQr(this,id)){
       progress.setText("No hay un QR registrado en este dispositivo.");
       prompt.setText("Puedes detener la alarma con la opción de seguridad.");
     }else{
       progress.setText("Escanea el QR que registraste cuando creaste la alarma.");
       prompt.setText("Coloca tu código QR físico frente a la cámara.");
       Button scan=button("Abrir cámara y escanear QR");root.addView(scan);
       scan.setOnClickListener(v->{
         try{
           new com.google.zxing.integration.android.IntentIntegrator(this)
             .setDesiredBarcodeFormats(com.google.zxing.integration.android.IntentIntegrator.QR_CODE)
             .setPrompt("Escanea el QR de tu alarma").setBeepEnabled(false)
             .setOrientationLocked(false).initiateScan();
         }catch(Exception ex){Toast.makeText(this,"No se pudo abrir el lector QR",Toast.LENGTH_LONG).show();}
       });
     }
   }else if("shake".equals(challenge)){
     progress.setText("Mueve suavemente tu teléfono 12 veces");
     prompt.setText("0 / 12 movimientos");
     TextView safety=label("Si no puedes moverlo, usa Detener sin desafío.",13);root.addView(safety);
     sensors=(SensorManager)getSystemService(SENSOR_SERVICE);
     Sensor acceleration=sensors==null?null:sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
     if(acceleration==null){
       prompt.setText("Este teléfono no dispone del sensor necesario");
     }else{
       sensors.registerListener(this,acceleration,SensorManager.SENSOR_DELAY_UI);
     }
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
   final int snoozeMinutes=AlarmOptionsStore.minutes(this,id);
   final int remainingSnoozes=Math.max(0,AlarmOptionsStore.max(this,id)-AlarmOptionsStore.used(this,id));
   if(remainingSnoozes>0){
     Button snooze=button("Posponer "+snoozeMinutes+" min · "+remainingSnoozes+" disponible(s)");
     android.graphics.drawable.GradientDrawable subdued=new android.graphics.drawable.GradientDrawable();
     subdued.setColor(0xFF283851);subdued.setCornerRadius(dp(16));snooze.setBackground(subdued);
     root.addView(snooze);
     snooze.setOnClickListener(v->{
       if(AlarmReceiver.snooze(this,id,snoozeMinutes))finish();
       else Toast.makeText(this,"No se pudo posponer. Puedes terminar la alarma.",Toast.LENGTH_LONG).show();
     });
   }
   if(!"none".equals(challenge)){
     Button emergency=button("Detener sin desafío");
     emergency.setTextColor(0xFFD5E4F4);
     android.graphics.drawable.GradientDrawable secondary=new android.graphics.drawable.GradientDrawable();
     secondary.setColor(0xFF29384D);secondary.setCornerRadius(dp(16));
     emergency.setBackground(secondary);
     root.addView(emergency);
     emergency.setOnClickListener(v->{
       emergency.setVisibility(android.view.View.GONE);
       TextView message=label("Puedes detener la alarma sin completar el reto.",15);
       root.addView(message);
       Button confirm=button("Sí, detener ahora");root.addView(confirm);
       confirm.setOnClickListener(w->forceDismissAlarm());
       Button back=button("Volver al desafío");root.addView(back);
       back.setOnClickListener(w->{root.removeView(message);root.removeView(confirm);root.removeView(back);emergency.setVisibility(android.view.View.VISIBLE);});
     });
   }
 }
 @Override protected void onActivityResult(int requestCode,int resultCode,android.content.Intent data){
   super.onActivityResult(requestCode,resultCode,data);
   com.google.zxing.integration.android.IntentResult result=
       com.google.zxing.integration.android.IntentIntegrator.parseActivityResult(requestCode,resultCode,data);
   if(result==null||!"qr".equals(challenge))return;
   String contents=result.getContents();
   if(contents==null){progress.setText("Lectura cancelada. Puedes intentarlo de nuevo.");return;}
   if(AlarmChallengeStore.matchesQr(this,id,contents)){finishAlarm();}
   else{progress.setText("El QR no coincide con el registrado.");prompt.setText("Vuelve a intentarlo con el QR correcto.");}
 }
 @Override protected void onDestroy(){if(sensors!=null)sensors.unregisterListener(this);uiHandler.removeCallbacksAndMessages(null);super.onDestroy();}
 @Override public void onAccuracyChanged(Sensor sensor,int accuracy){}
 @Override public void onSensorChanged(SensorEvent e){
   if(!"shake".equals(challenge)||e.sensor.getType()!=Sensor.TYPE_ACCELEROMETER)return;
   float x=e.values[0],y=e.values[1],z=e.values[2];
   float force=(float)Math.sqrt(x*x+y*y+z*z);
   long now=SystemClock.elapsedRealtime();
   // Umbral moderado; limita dobles conteos por la misma sacudida.
   if(force>17.5f&&now-lastShake>650){
     lastShake=now;shakes++;prompt.setText(shakes+" / 12 movimientos");
     if(shakes>=12){sensors.unregisterListener(this);finishAlarm();}
   }
 }
 private void nextQuestion(){
   int x,y;
   if("math_hard".equals(challenge)){
     x=7+random.nextInt(19);y=3+random.nextInt(13);
     if(random.nextBoolean()){answer=x*y;prompt.setText(x+" × "+y+" = ?");}
     else{answer=x+y+random.nextInt(16);prompt.setText("Calcula: "+x+" + "+y+" + "+(answer-x-y)+" = ?");}
   }else{x=3+random.nextInt(16);y=2+random.nextInt(14);answer=x+y;prompt.setText(x+" + "+y+" = ?");}
   progress.setText("Desafío "+(correct+1)+" de "+remaining);
   input.setText("");
 }
 @Override public void onBackPressed(){
   // Si necesita detenerse, el botón seguro está siempre visible.
   if("none".equals(challenge))forceDismissAlarm();
   else Toast.makeText(this,"Usa Posponer o Detener sin desafío",Toast.LENGTH_LONG).show();
 }
}