package com.neurosync.familia;

import android.app.Activity;
import android.app.NotificationManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.view.*;
import android.widget.*;
import org.json.*;

/** Pantalla Android autónoma de comprobación antes de salir, incluso con WebView cerrada. */
public final class DepartureChecklistActivity extends Activity {
 private String id;
 private JSONObject entry;
 private LinearLayout root,checks;
 private TextView progress;
 private Button finishButton;
 private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
 private GradientDrawable bg(int color,int radius){
  GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;
 }
 private TextView text(String value,int size,int color,boolean bold){
  TextView v=new TextView(this);v.setText(value);v.setTextColor(color);v.setTextSize(size);
  if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return v;
 }
 private void add(LinearLayout view,View child,int top){
  LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(top);view.addView(child,p);
 }
 @Override public void onCreate(Bundle saved){
  super.onCreate(saved);
  if(Build.VERSION.SDK_INT>=27){setShowWhenLocked(true);setTurnScreenOn(true);}
  else getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED|WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
  getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
  getWindow().setStatusBarColor(0xFF0B1830);
  getWindow().setNavigationBarColor(0xFF0B1830);
  id=getIntent().getStringExtra("id");
  if(id==null){finish();return;}
  entry=DepartureReminderReceiver.get(this,id);
  if(!id.equals(entry.optString("id"))){finish();return;}
  ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);
  root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setGravity(Gravity.CENTER_HORIZONTAL);
  root.setPadding(dp(22),dp(35),dp(22),dp(35));root.setBackgroundColor(0xFF0B1830);
  scroll.addView(root);setContentView(scroll);
  add(root,text("NEUROSYNC  /  ANTES DE SALIR",12,0xFF81BDFF,true),5);
  TextView title=text(entry.optString("title","Mi lista"),30,Color.WHITE,true);
  title.setGravity(Gravity.CENTER);add(root,title,18);
  TextView subtitle=text("Revisa cada objeto antes de salir",15,0xFFADBED5,false);
  subtitle.setGravity(Gravity.CENTER);add(root,subtitle,6);
  progress=text("",16,0xFF82C6FF,true);progress.setGravity(Gravity.CENTER);
  add(root,progress,22);
  checks=new LinearLayout(this);checks.setOrientation(LinearLayout.VERTICAL);add(root,checks,13);
  finishButton=new Button(this);finishButton.setAllCaps(false);finishButton.setTextSize(16);
  finishButton.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
  finishButton.setTextColor(Color.WHITE);finishButton.setBackground(bg(0xFF117DEB,17));
  finishButton.setMinHeight(dp(58));add(root,finishButton,20);
  finishButton.setOnClickListener(v->{
   if(allDone()){
    NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
    if(nm!=null)nm.cancel(id.hashCode()^0x310088);
    finish();
   }else Toast.makeText(this,"Marca todos los objetos para confirmar la salida",Toast.LENGTH_LONG).show();
  });
  Button safe=new Button(this);safe.setAllCaps(false);safe.setText("Cerrar sin completar");
  safe.setTextColor(0xFFB9C9DD);safe.setBackground(bg(0xFF1B2D46,15));safe.setMinHeight(dp(48));
  add(root,safe,10);
  safe.setOnClickListener(v->new android.app.AlertDialog.Builder(this)
   .setTitle("Salir sin revisar todo")
   .setMessage("Hay objetos pendientes. Puedes volver a abrir la lista desde NeuroSync.")
   .setNegativeButton("Seguir revisando",null)
   .setPositiveButton("Cerrar de todos modos",(d,w)->finish()).show());
  showItems();
 }
 private boolean allDone(){
  JSONArray items=entry.optJSONArray("items");JSONObject marked=entry.optJSONObject("checked");
  if(items==null||items.length()==0)return false;
  for(int n=0;n<items.length();n++)if(marked==null||!marked.optBoolean(String.valueOf(n),false))return false;
  return true;
 }
 private void showItems(){
  entry=DepartureReminderReceiver.get(this,id);
  JSONArray items=entry.optJSONArray("items");JSONObject checked=entry.optJSONObject("checked");
  if(items==null){finish();return;}
  checks.removeAllViews();int done=0;
  for(int i=0;i<items.length();i++){
   final int position=i;
   boolean selected=checked!=null&&checked.optBoolean(String.valueOf(i),false);if(selected)done++;
   CheckBox item=new CheckBox(this);
   item.setText(items.optString(i));item.setTextSize(18);item.setTextColor(Color.WHITE);
   item.setButtonTintList(android.content.res.ColorStateList.valueOf(0xFF59ADFF));
   item.setPadding(dp(16),dp(12),dp(16),dp(12));
   item.setMinHeight(dp(62));item.setBackground(bg(0xFF1E314C,13));
   item.setChecked(selected);
   item.setOnCheckedChangeListener((button,active)->{
    if(!DepartureReminderReceiver.mark(this,id,position,active)){
     button.setOnCheckedChangeListener(null);button.setChecked(!active);
     Toast.makeText(this,"No se pudo guardar. Intenta otra vez.",Toast.LENGTH_SHORT).show();
     showItems();return;
    }
    showItems();
   });
   add(checks,item,9);
  }
  progress.setText(done+" de "+items.length()+" objetos listos");
  finishButton.setText(done==items.length()?"✓ Todo listo, salir":"Faltan "+(items.length()-done)+" objetos");
  finishButton.setAlpha(done==items.length()?1f:.65f);
 }
 @Override public void onBackPressed(){
  if(allDone()){finish();return;}
  new android.app.AlertDialog.Builder(this).setTitle("Aún faltan objetos")
   .setMessage("Puedes volver a revisar la lista cuando quieras.")
   .setPositiveButton("Seguir revisando",null)
   .setNegativeButton("Salir de todos modos",(d,w)->finish()).show();
 }
}
