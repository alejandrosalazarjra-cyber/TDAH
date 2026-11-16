package com.neurosync.familia;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.role.RoleManager;
import android.content.*;
import android.content.pm.*;
import android.graphics.Color;
import android.os.*;
import android.provider.Settings;
import android.text.*;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Pantalla de inicio alternativa de NeuroSync. No suplanta la pantalla de bloqueo.
 * La persona debe elegirla voluntariamente como launcher predeterminado.
 * No utiliza servicios de accesibilidad ni recopila las notificaciones de otras apps.
 */
public final class HomeActivity extends Activity {
  private static final String PREFS="ns_launcher";
  private final Handler handler=new Handler(Looper.getMainLooper());
  private LinearLayout page, appList, favoritesList, listArea;
  private TextView clock, date, pending, heading, hint;
  private EditText search;
  private boolean drawer=false;
  private final ArrayList<AppEntry> installed=new ArrayList<>();
  private final Runnable tick=new Runnable(){@Override public void run(){updateClock();handler.postDelayed(this,15000);}};
  private static final class AppEntry{
    final String title,pkg,activity;
    AppEntry(String title,String pkg,String activity){this.title=title;this.pkg=pkg;this.activity=activity;}
    String key(){return pkg+"/"+activity;}
  }
  private android.content.SharedPreferences prefs(){return getSharedPreferences(PREFS,MODE_PRIVATE);}
  private int dp(float n){return (int)(getResources().getDisplayMetrics().density*n+.5f);}
  private TextView text(String content,int sp,int color){
    TextView t=new TextView(this);t.setText(content);t.setTextSize(sp);t.setTextColor(color);return t;
  }
  private LinearLayout vertical(){
    LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;
  }
  private void margin(View v,int top,int bottom){
    LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,-2);
    params.topMargin=dp(top);params.bottomMargin=dp(bottom);page.addView(v,params);
  }
  @Override protected void onCreate(Bundle state){
    super.onCreate(state);
    getWindow().setStatusBarColor(Color.BLACK);
    getWindow().setNavigationBarColor(Color.BLACK);
    getWindow().getDecorView().setSystemUiVisibility(0);
    getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    showHome();
  }
  @Override protected void onResume(){super.onResume();handler.removeCallbacks(tick);tick.run();if(!drawer)updatePending();}
  @Override protected void onPause(){handler.removeCallbacks(tick);super.onPause();}
  private void base(){
    ScrollView scroll=new ScrollView(this);
    scroll.setFillViewport(true);scroll.setBackgroundColor(Color.BLACK);
    page=vertical();page.setPadding(dp(27),dp(28),dp(27),dp(28));
    scroll.addView(page);
    setContentView(scroll);
  }
  private void updateClock(){
    if(clock!=null)clock.setText(new SimpleDateFormat("HH:mm",new Locale("es","CO")).format(new Date()));
    if(date!=null)date.setText(new SimpleDateFormat("EEEE, d 'de' MMMM",new Locale("es","CO")).format(new Date()));
  }
  private void updatePending(){
    if(pending!=null)pending.setText(getSharedPreferences("ns_widget",0).getInt("pending",0)+" pendientes hoy");
  }
  private void showHome(){
    drawer=false;clock=null;date=null;pending=null;base();
    LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);
    LinearLayout left=vertical();left.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1));
    clock=text("",43,Color.WHITE);clock.setTypeface(null,1);date=text("",14,0xFFBDBDBD);
    left.addView(clock);left.addView(date);top.addView(left);
    TextView settings=text("⚙",27,Color.WHITE);settings.setPadding(dp(12),dp(8),dp(2),dp(8));
    settings.setContentDescription("Configuración de inicio");
    settings.setOnClickListener(v->showSettings());top.addView(settings);
    margin(top,5,26);updateClock();

    TextView focus=text("Un paso a la vez",17,0xFFD1D1D1);focus.setGravity(Gravity.CENTER);
    margin(focus,42,14);
    LinearLayout taskCard=vertical();taskCard.setPadding(dp(18),dp(20),dp(18),dp(20));
    taskCard.setBackgroundColor(0xFF171717);
    TextView headline=text("NEUROSYNC  ·  MI DÍA",12,0xFFB6C3FF);headline.setLetterSpacing(.08f);
    pending=text("",25,Color.WHITE);pending.setPadding(0,dp(12),0,dp(9));
    TextView open=text("Ver tareas, mascota y horarios  ›",14,0xFFDDDDDD);
    taskCard.addView(headline);taskCard.addView(pending);taskCard.addView(open);
    taskCard.setOnClickListener(v->startActivity(new Intent(this,MainActivity.class)));
    margin(taskCard,0,30);updatePending();

    TextView fav=text("ACCESOS DIRECTOS",12,0xFF999999);fav.setLetterSpacing(.07f);
    margin(fav,0,12);favoritesList=vertical();margin(favoritesList,0,24);
    loadApps();showFavorites();

    TextView drawerButton=text("Todas las aplicaciones   ↗",19,Color.WHITE);
    drawerButton.setPadding(0,dp(15),0,dp(15));
    drawerButton.setOnClickListener(v->showDrawer());
    margin(drawerButton,12,16);
    LinearLayout shortcuts=new LinearLayout(this);
    shortcuts.setGravity(Gravity.CENTER_VERTICAL);
    TextView phone=text("☎  Teléfono",16,0xFFDDDDDD);
    phone.setPadding(0,dp(12),dp(12),dp(12));
    phone.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1));
    phone.setOnClickListener(v->openDialer());
    TextView camera=text("◎  Cámara",16,0xFFDDDDDD);
    camera.setPadding(dp(12),dp(12),0,dp(12));
    camera.setOnClickListener(v->openCamera());
    shortcuts.addView(phone);shortcuts.addView(camera);
    margin(shortcuts,28,0);
  }
  private void loadApps(){
    installed.clear();PackageManager pm=getPackageManager();
    Intent query=new Intent(Intent.ACTION_MAIN);query.addCategory(Intent.CATEGORY_LAUNCHER);
    List<ResolveInfo> infos=pm.queryIntentActivities(query,0);
    HashSet<String> used=new HashSet<>();
    for(ResolveInfo info:infos){
      if(info.activityInfo==null)continue;
      String pkg=info.activityInfo.packageName;
      String activity=info.activityInfo.name;
      String key=pkg+"/"+activity;
      if(pkg.equals(getPackageName())||!used.add(key))continue;
      installed.add(new AppEntry(String.valueOf(info.loadLabel(pm)),pkg,activity));
    }
    installed.sort((a,b)->java.text.Collator.getInstance(new Locale("es","CO")).compare(a.title,b.title));
  }
  private void showFavorites(){
    favoritesList.removeAllViews();
    Set<String> favorite=prefs().getStringSet("favorites",Collections.emptySet());
    int count=0;
    for(AppEntry app:installed)if(favorite.contains(app.key())){
      favoritesList.addView(appButton(app,true));count++;if(count>=10)break;
    }
    if(count==0){
      TextView no=text("Selecciona aplicaciones favoritas desde la lista general.",13,0xFF999999);
      favoritesList.addView(no);
    }
  }
  private void showDrawer(){
    drawer=true;clock=null;date=null;pending=null;base();
    LinearLayout bar=new LinearLayout(this);bar.setGravity(Gravity.CENTER_VERTICAL);
    TextView back=text("‹",38,Color.WHITE);back.setPadding(0,0,dp(22),0);back.setOnClickListener(v->showHome());
    bar.addView(back);
    TextView name=text("Aplicaciones",24,Color.WHITE);bar.addView(name);margin(bar,0,20);
    search=new EditText(this);search.setSingleLine(true);search.setTextColor(Color.WHITE);
    search.setHintTextColor(0xFFAAAAAA);search.setHint("Buscar aplicación");
    search.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF888888));
    margin(search,0,15);
    appList=vertical();margin(appList,0,0);
    search.addTextChangedListener(new TextWatcher(){
      public void beforeTextChanged(CharSequence s,int start,int count,int after){}
      public void onTextChanged(CharSequence s,int start,int before,int count){renderApps(s.toString());}
      public void afterTextChanged(Editable e){}
    });
    loadApps();renderApps("");
  }
  private void renderApps(String filter){
    if(appList==null)return;appList.removeAllViews();
    String q=filter.trim().toLowerCase(new Locale("es","CO"));int matched=0;
    for(AppEntry app:installed){
      if(!app.title.toLowerCase(new Locale("es","CO")).contains(q))continue;
      appList.addView(appButton(app,false));matched++;
    }
    if(matched==0)appList.addView(text("No hay aplicaciones que coincidan.",15,0xFFAAAAAA));
  }
  private View appButton(AppEntry app,boolean favorite){
    LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);
    row.setPadding(0,dp(14),0,dp(14));
    TextView title=text(app.title,18,Color.WHITE);
    title.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1));
    row.addView(title);
    TextView pin=text(favorite?"★":"☆",23,favorite?0xFFE6CF78:0xFF888888);
    pin.setContentDescription(favorite?"Quitar de favoritos":"Agregar a favoritos");
    pin.setPadding(dp(9),0,0,0);
    pin.setOnClickListener(v->{
      HashSet<String> set=new HashSet<>(prefs().getStringSet("favorites",Collections.emptySet()));
      if(!set.add(app.key()))set.remove(app.key());
      prefs().edit().putStringSet("favorites",set).apply();
      if(drawer)renderApps(search.getText().toString());else showFavorites();
    });
    row.addView(pin);
    row.setOnClickListener(v->openApp(app));
    return row;
  }
  private void openApp(AppEntry app){
    int delay=prefs().getInt("mindful_delay",0);
    int from=prefs().getInt("quiet_start",-1),to=prefs().getInt("quiet_end",-1);
    Calendar now=Calendar.getInstance();int hour=now.get(Calendar.HOUR_OF_DAY);
    boolean quiet=from>=0&&to>=0&&(from<=to?(hour>=from&&hour<to):(hour>=from||hour<to));
    if(quiet)delay=Math.max(delay,5);
    if(delay<=0){launch(app);return;}
    final int waitSeconds=delay;
    new AlertDialog.Builder(this).setTitle("¿Abrir "+app.title+"?")
      .setMessage((quiet?"Tu horario de concentración está activo. ":"")+"¿Es lo que quieres hacer ahora?")
      .setNegativeButton("Volver",null)
      .setPositiveButton("Abrir", (dialog,which)->{
        Toast.makeText(this,"Pausa consciente: "+waitSeconds+" segundos",Toast.LENGTH_LONG).show();
        handler.postDelayed(()->launch(app),waitSeconds*1000L);
      }).show();
  }
  private void launch(AppEntry app){
    try{
      Intent intent=new Intent(Intent.ACTION_MAIN);
      intent.addCategory(Intent.CATEGORY_LAUNCHER);
      intent.setComponent(new ComponentName(app.pkg,app.activity));
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      startActivity(intent);
    }catch(Exception e){Toast.makeText(this,"No se pudo abrir esa aplicación",Toast.LENGTH_SHORT).show();}
  }
  private void openDialer(){
    try{startActivity(new Intent(Intent.ACTION_DIAL));}
    catch(Exception e){Toast.makeText(this,"Sin marcador compatible",Toast.LENGTH_SHORT).show();}
  }
  private void openCamera(){
    try{startActivity(new Intent("android.media.action.STILL_IMAGE_CAMERA"));}
    catch(Exception e){Toast.makeText(this,"No hay cámara compatible",Toast.LENGTH_SHORT).show();}
  }
  private void showSettings(){
    final String[] options={"Cambiar pantalla de inicio predeterminada","Pausa antes de abrir apps","Horario de concentración","Abrir NeuroSync (tareas)","Restablecer inicio original"};
    new AlertDialog.Builder(this).setTitle("Inicio minimalista").setItems(options,(d,index)->{
      switch(index){
        case 0:requestHome();break;
        case 1:chooseDelay();break;
        case 2:chooseQuietHours();break;
        case 3:startActivity(new Intent(this,MainActivity.class));break;
        default:openHomeSettings();
      }
    }).show();
  }
  private void chooseDelay(){
    final String[] labels={"Sin pausa","3 segundos","5 segundos","10 segundos"};
    final int[] delays={0,3,5,10};
    new AlertDialog.Builder(this).setTitle("Pausa consciente al abrir apps")
      .setSingleChoiceItems(labels,0,(d,i)->{prefs().edit().putInt("mindful_delay",delays[i]).apply();d.dismiss();}).show();
  }
  private void chooseQuietHours(){
    final String[] choices={"Desactivado","Estudio 08:00–12:00","Trabajo 14:00–18:00","Descanso 21:00–07:00"};
    new AlertDialog.Builder(this).setTitle("Horario de concentración")
      .setItems(choices,(d,i)->{
        int[] start={-1,8,14,21},end={-1,12,18,7};
        prefs().edit().putInt("quiet_start",start[i]).putInt("quiet_end",end[i]).apply();
      }).show();
  }
  private void requestHome(){
    if(Build.VERSION.SDK_INT>=29){
      RoleManager role=getSystemService(RoleManager.class);
      if(role!=null&&role.isRoleAvailable(RoleManager.ROLE_HOME)){
        if(role.isRoleHeld(RoleManager.ROLE_HOME)){
          Toast.makeText(this,"NeuroSync ya es tu pantalla de inicio",Toast.LENGTH_SHORT).show();return;
        }
        startActivityForResult(role.createRequestRoleIntent(RoleManager.ROLE_HOME),300);return;
      }
    }
    openHomeSettings();
  }
  private void openHomeSettings(){
    try{startActivity(new Intent(Settings.ACTION_HOME_SETTINGS));}
    catch(Exception e){new AlertDialog.Builder(this).setMessage("En Ajustes del teléfono, busca Apps predeterminadas → Aplicación de inicio.").setPositiveButton("Entendido",null).show();}
  }
  @Override public void onBackPressed(){if(drawer)showHome();else super.onBackPressed();}
}