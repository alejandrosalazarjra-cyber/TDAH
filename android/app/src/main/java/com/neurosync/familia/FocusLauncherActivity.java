package com.neurosync.familia;

import android.app.*;
import android.app.role.RoleManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.*;
import android.content.pm.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.*;
import android.provider.Settings;
import android.provider.MediaStore;
import android.text.*;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * NeuroSync Inicio: launcher alternativo voluntario, original, sin bloquear Android.
 * La lista de aplicaciones usa ACTION_MAIN/CATEGORY_LAUNCHER (no requiere QUERY_ALL_PACKAGES).
 * Los límites de concentración se aplican solo al abrir apps desde este launcher.
 */
public final class FocusLauncherActivity extends Activity {
  private static final int BLACK=0xFF101113, WHITE=0xFFF8F8F7, MUTED=0xFF9E9EA1, CARD=0xFF232428;
  private LinearLayout root, page;
  private EditText search;
  private boolean drawer=false;
  private final Handler handler=new Handler(Looper.getMainLooper());
  private final Runnable updateClock=new Runnable(){@Override public void run(){if(!drawer)renderHome();handler.postDelayed(this,30000);}};
  private final ArrayList<AppEntry> apps=new ArrayList<>();
  private final Set<String> favorites=new HashSet<>(), focusApps=new HashSet<>(), mutedApps=new HashSet<>();
  private android.content.SharedPreferences preferences;
  private float xStart,yStart;
  private boolean focusEnabled=true;
  private int focusStart=0,focusEnd=0;

  private static class AppEntry {
    String title,packageName,activity;
    AppEntry(String title,String pkg,String cls){this.title=title;this.packageName=pkg;this.activity=cls;}
  }
  private int dp(float amount){return Math.round(getResources().getDisplayMetrics().density*amount);}
  private LinearLayout vertical(){
    LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;
  }
  private LinearLayout horizontal(){
    LinearLayout l=new LinearLayout(this);l.setGravity(Gravity.CENTER_VERTICAL);l.setOrientation(LinearLayout.HORIZONTAL);return l;
  }
  private TextView text(String contents,float size,int color,boolean bold){
    TextView t=new TextView(this);t.setText(contents);t.setTextSize(size);t.setTextColor(color);
    t.setIncludeFontPadding(true);
    if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
    return t;
  }
  private void pad(View v,int x,int y){v.setPadding(dp(x),dp(y),dp(x),dp(y));}
  private TextView action(String value,Runnable click){
    TextView t=text(value,16,WHITE,false);
    pad(t,12,13);t.setBackgroundColor(CARD);t.setGravity(Gravity.CENTER);
    if(click!=null)t.setOnClickListener(v->click.run());return t;
  }
  private View gap(int height){View v=new View(this);v.setLayoutParams(new LinearLayout.LayoutParams(1,dp(height)));return v;}
  private View line(){View v=new View(this);v.setBackgroundColor(0xFF44464C);v.setLayoutParams(new LinearLayout.LayoutParams(-1,dp(1)));return v;}

  @Override public void onCreate(Bundle state){
    super.onCreate(state);
    getWindow().setStatusBarColor(BLACK);getWindow().setNavigationBarColor(BLACK);
    getWindow().getDecorView().setSystemUiVisibility(0);
    preferences=getSharedPreferences("focus_launcher",MODE_PRIVATE);
    favorites.addAll(preferences.getStringSet("favorites",Collections.emptySet()));
    focusApps.addAll(preferences.getStringSet("focus",Collections.emptySet()));
    mutedApps.addAll(preferences.getStringSet("muted_notifications",Collections.emptySet()));
    focusEnabled=preferences.getBoolean("focus_enabled",true);
    focusStart=preferences.getInt("focus_start",0);
    focusEnd=preferences.getInt("focus_end",0);
    root=vertical();root.setBackgroundColor(BLACK);setContentView(root);
    refreshApps();renderHome();
    root.setOnTouchListener((v,event)->{
      if(event.getAction()==MotionEvent.ACTION_DOWN){xStart=event.getX();yStart=event.getY();return true;}
      if(event.getAction()==MotionEvent.ACTION_UP){
        float dx=event.getX()-xStart,dy=event.getY()-yStart;
        if(Math.abs(dx)>dp(65)||Math.abs(dy)>dp(85)){showDrawer();return true;}
      }
      return false;
    });
  }
  @Override protected void onResume(){super.onResume();handler.removeCallbacks(updateClock);handler.post(updateClock);}
  @Override protected void onPause(){handler.removeCallbacks(updateClock);super.onPause();}
  private void persist(){
    preferences.edit().putStringSet("favorites",new HashSet<>(favorites))
      .putStringSet("focus",new HashSet<>(focusApps))
      .putStringSet("muted_notifications",new HashSet<>(mutedApps))
      .putBoolean("focus_enabled",focusEnabled)
      .putInt("focus_start",focusStart).putInt("focus_end",focusEnd).apply();
  }
  private void refreshApps(){
    apps.clear();
    Intent intent=new Intent(Intent.ACTION_MAIN);intent.addCategory(Intent.CATEGORY_LAUNCHER);
    PackageManager pm=getPackageManager();
    List<ResolveInfo> list=pm.queryIntentActivities(intent,0);
    Set<String> unique=new HashSet<>();
    for(ResolveInfo resolved:list){
      if(resolved.activityInfo==null)continue;
      String pkg=resolved.activityInfo.packageName,cls=resolved.activityInfo.name;
      if(pkg.equals(getPackageName()))continue;
      if(!unique.add(pkg+"/"+cls))continue;
      CharSequence label=resolved.loadLabel(pm);
      apps.add(new AppEntry(label==null?pkg:label.toString(),pkg,cls));
    }
    Collections.sort(apps,(a,b)->a.title.compareToIgnoreCase(b.title));
  }
  private void showContent(LinearLayout content){
    root.removeAllViews();
    ScrollView scroll=new ScrollView(this);
    scroll.setFillViewport(true);scroll.setClipToPadding(false);
    scroll.addView(content,new ScrollView.LayoutParams(-1,-2));
    root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
  }
  private void renderHome(){
    if(drawer)return;
    LinearLayout body=vertical();body.setBackgroundColor(BLACK);pad(body,23,24);
    LinearLayout head=horizontal();
    TextView brand=text("NEUROSYNC  /  INICIO",12,MUTED,true);
    head.addView(brand,new LinearLayout.LayoutParams(0,-2,1));
    TextView settings=action("⚙",this::showSettings);head.addView(settings);body.addView(head);
    body.addView(gap(50));
    Date now=new Date();
    TextView clock=text(new SimpleDateFormat("HH:mm",Locale.getDefault()).format(now),66,WHITE,false);
    clock.setGravity(Gravity.CENTER_HORIZONTAL);
    body.addView(clock);
    TextView date=text(new SimpleDateFormat("EEEE, d 'de' MMMM",new Locale("es","CO")).format(now),17,MUTED,false);
    date.setGravity(Gravity.CENTER_HORIZONTAL);body.addView(date);
    body.addView(gap(26));
    int pending= getSharedPreferences("ns_widget",0).getInt("pending",0);
    TextView progress=text("☑  "+pending+" tareas por completar",15,WHITE,false);
    progress.setGravity(Gravity.CENTER);pad(progress,14,18);progress.setBackgroundColor(CARD);body.addView(progress);
    progress.setOnClickListener(v->openNeuroSync());
    body.addView(gap(28));
    TextView favoriteTitle=text("ACCESOS FAVORITOS",12,MUTED,true);body.addView(favoriteTitle);body.addView(gap(11));
    int count=0;
    for(AppEntry a:apps){
      if(favorites.contains(a.packageName)){addHomeApp(body,a);count++;if(count>=7)break;}
    }
    if(count==0){
      TextView tip=text("Mantén pulsada una aplicación en la lista para añadirla aquí.",14,MUTED,false);
      pad(tip,0,6);body.addView(tip);
    }
    body.addView(gap(12));body.addView(line());body.addView(gap(18));
    TextView drawerButton=action("☷  Buscar todas las aplicaciones",this::showDrawer);
    drawerButton.setGravity(Gravity.CENTER);body.addView(drawerButton);body.addView(gap(16));
    TextView tasks=action("✓  Abrir mi organización y horarios",this::openNeuroSync);body.addView(tasks);
    body.addView(gap(22));
    LinearLayout foot=horizontal();foot.setGravity(Gravity.CENTER);
    TextView phone=action("☎  Teléfono",()->launchIntent(new Intent(Intent.ACTION_DIAL)));foot.addView(phone);
    foot.addView(new View(this),new LinearLayout.LayoutParams(dp(16),1));
    TextView camera=action("◉  Cámara",()->launchIntent(new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)));foot.addView(camera);
    body.addView(foot);
    body.addView(gap(25));
    TextView footer=text("Desliza para ver aplicaciones  ·  Mantén pulsado para elegir favoritas",11,MUTED,false);
    footer.setGravity(Gravity.CENTER);body.addView(footer);
    showContent(body);
  }
  private void addHomeApp(LinearLayout container,AppEntry entry){
    TextView item=text("•  "+entry.title,22,WHITE,false);pad(item,8,13);
    item.setOnClickListener(v->openEntry(entry));
    item.setOnLongClickListener(v->{showEntryOptions(entry);return true;});
    container.addView(item);
  }
  private void showDrawer(){
    drawer=true;refreshApps();
    root.removeAllViews();
    LinearLayout wrapper=vertical();wrapper.setBackgroundColor(BLACK);pad(wrapper,18,15);
    LinearLayout heading=horizontal();
    TextView back=action("‹ Inicio",()->{drawer=false;renderHome();});
    heading.addView(back);
    TextView title=text("   Mis aplicaciones",20,WHITE,true);
    heading.addView(title);wrapper.addView(heading);wrapper.addView(gap(16));
    search=new EditText(this);search.setSingleLine(true);search.setTextColor(WHITE);
    search.setHintTextColor(MUTED);search.setHint("Buscar aplicación...");
    search.setTextSize(17);search.setBackgroundColor(CARD);pad(search,15,13);
    wrapper.addView(search);
    page=vertical();page.setPadding(0,dp(12),0,dp(80));
    ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.addView(page);
    wrapper.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
    root.addView(wrapper,new LinearLayout.LayoutParams(-1,-1));
    search.addTextChangedListener(new TextWatcher(){
      @Override public void beforeTextChanged(CharSequence s,int start,int count,int after){}
      @Override public void onTextChanged(CharSequence s,int start,int before,int count){renderApps(s.toString());}
      @Override public void afterTextChanged(Editable e){}
    });
    renderApps("");
  }
  private void renderApps(String query){
    if(page==null)return;
    page.removeAllViews();
    String q=query.trim().toLowerCase(Locale.getDefault());
    int total=0;
    for(AppEntry entry:apps){
      if(!entry.title.toLowerCase(Locale.getDefault()).contains(q))continue;
      LinearLayout row=horizontal();
      String star=favorites.contains(entry.packageName)?"★ ":"";
      TextView name=text(star+entry.title,19,WHITE,false);pad(name,12,14);
      row.addView(name,new LinearLayout.LayoutParams(0,-2,1));
      TextView controls=text("⋮",22,MUTED,true);pad(controls,15,10);
      controls.setOnClickListener(v->showEntryOptions(entry));
      row.addView(controls);
      row.setOnClickListener(v->openEntry(entry));
      row.setOnLongClickListener(v->{showEntryOptions(entry);return true;});
      page.addView(row);page.addView(line());total++;
    }
    if(total==0){TextView empty=text("No encontramos aplicaciones con ese nombre.",15,MUTED,false);pad(empty,10,24);page.addView(empty);}
  }
  private void showEntryOptions(AppEntry app){
    boolean fav=favorites.contains(app.packageName),focus=focusApps.contains(app.packageName);
    boolean muted=mutedApps.contains(app.packageName);
    String[] items={
      fav?"Quitar de favoritos":"Agregar a inicio",
      focus?"Quitar pausa consciente":"Añadir pausa consciente",
      muted?"Dejar pasar notificaciones":"Filtrar sus notificaciones",
      "Abrir aplicación"
    };
    new AlertDialog.Builder(this).setTitle(app.title).setItems(items,(dialog,which)->{
      if(which==0){if(fav)favorites.remove(app.packageName);else favorites.add(app.packageName);persist();refreshApps();if(drawer)renderApps(search==null?"":search.getText().toString());else renderHome();}
      if(which==1){if(focus)focusApps.remove(app.packageName);else focusApps.add(app.packageName);persist();if(drawer)renderApps(search==null?"":search.getText().toString());}
      if(which==2){
        if(muted)mutedApps.remove(app.packageName);else mutedApps.add(app.packageName);
        persist();if(drawer)renderApps(search==null?"":search.getText().toString());
        if(!isNotificationListenerEnabled())showNotificationPermissionHelp();
      }
      if(which==3)openEntry(app);
    }).setNegativeButton("Cancelar",null).show();
  }
  private boolean focusIsActive(){
    if(!focusEnabled)return false;
    if(focusStart==focusEnd)return true;
    Calendar now=Calendar.getInstance();int minute=now.get(Calendar.HOUR_OF_DAY)*60+now.get(Calendar.MINUTE);
    return focusStart<focusEnd?minute>=focusStart&&minute<focusEnd:minute>=focusStart||minute<focusEnd;
  }
  private void openEntry(AppEntry app){
    if(focusIsActive()&&focusApps.contains(app.packageName)){
      new AlertDialog.Builder(this)
        .setTitle("Antes de abrir "+app.title)
        .setMessage("¿Para qué quieres usar esta aplicación? Puedes continuar o volver a tus tareas.")
        .setPositiveButton("Abrir de todos modos",(d,w)->launchEntry(app))
        .setNegativeButton("Volver a mis tareas",(d,w)->openNeuroSync())
        .setNeutralButton("Cancelar",null).show();
    }else launchEntry(app);
  }
  private void launchEntry(AppEntry app){
    try{
      Intent intent=new Intent(Intent.ACTION_MAIN);
      intent.addCategory(Intent.CATEGORY_LAUNCHER);
      intent.setClassName(app.packageName,app.activity);
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
      startActivity(intent);
    }catch(Exception e){Toast.makeText(this,"No se pudo abrir la aplicación",Toast.LENGTH_SHORT).show();}
  }
  private void launchIntent(Intent intent){
    try{startActivity(intent);}catch(Exception e){Toast.makeText(this,"No se encontró una aplicación compatible",Toast.LENGTH_SHORT).show();}
  }
  private void openNeuroSync(){startActivity(new Intent(this,MainActivity.class));}
  private void chooseDefaultHome(){
    if(Build.VERSION.SDK_INT>=29){
      RoleManager roles=getSystemService(RoleManager.class);
      if(roles!=null&&roles.isRoleAvailable(RoleManager.ROLE_HOME)){
        try{startActivityForResult(roles.createRequestRoleIntent(RoleManager.ROLE_HOME),41);return;}catch(Exception ignored){}
      }
    }
    launchIntent(new Intent(Settings.ACTION_HOME_SETTINGS));
  }
  private boolean isNotificationListenerEnabled(){
    String enabled=Settings.Secure.getString(getContentResolver(),"enabled_notification_listeners");
    return enabled!=null&&enabled.contains(getPackageName());
  }
  private void showNotificationPermissionHelp(){
    new AlertDialog.Builder(this).setTitle("Permiso de notificaciones")
      .setMessage("NeuroSync puede ocultar notificaciones de las aplicaciones que tú selecciones. No guarda ni envía el texto de tus mensajes. Puedes desactivar este permiso cuando quieras.")
      .setPositiveButton("Abrir permisos",(d,w)->launchIntent(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)))
      .setNegativeButton("Ahora no",null).show();
  }
  private void requestUsageAccess(){launchIntent(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));}
  private long todayUsageMillis(){
    try{
      UsageStatsManager manager=(UsageStatsManager)getSystemService(USAGE_STATS_SERVICE);
      Calendar day=Calendar.getInstance();day.set(Calendar.HOUR_OF_DAY,0);day.set(Calendar.MINUTE,0);
      day.set(Calendar.SECOND,0);day.set(Calendar.MILLISECOND,0);
      List<UsageStats> stats=manager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY,day.getTimeInMillis(),System.currentTimeMillis());
      long total=0;
      if(stats!=null)for(UsageStats st:stats)if(!st.getPackageName().equals(getPackageName()))total+=st.getTotalTimeInForeground();
      return total;
    }catch(Exception e){return 0;}
  }
  private String formatMinutes(int value){return String.format(Locale.getDefault(),"%02d:%02d",value/60,value%60);}
  private void chooseFocusHours(){
    int hour=focusStart/60,min=focusStart%60;
    new TimePickerDialog(this,(picker,h,m)->{
      focusStart=h*60+m;
      new TimePickerDialog(this,(endPicker,h2,m2)->{
        focusEnd=h2*60+m2;persist();showSettings();
      },focusEnd/60,focusEnd%60,true).show();
    },hour,min,true).show();
  }
  private void showSettings(){
    drawer=true;
    LinearLayout view=vertical();pad(view,20,22);
    TextView back=action("‹  Volver a inicio",()->{drawer=false;renderHome();});view.addView(back);
    view.addView(gap(30));view.addView(text("Mi inicio minimalista",25,WHITE,true));view.addView(gap(14));
    TextView explain=text("Puedes usar NeuroSync como pantalla de inicio sin perder el acceso a otras aplicaciones ni las llamadas de emergencia. Android te permitirá regresar a tu launcher anterior desde Ajustes.",14,MUTED,false);
    view.addView(explain);view.addView(gap(24));
    view.addView(action("✓  Elegir NeuroSync como pantalla de inicio",this::chooseDefaultHome));view.addView(gap(13));
    view.addView(action("⌂  Abrir agenda y organización",this::openNeuroSync));view.addView(gap(13));
    TextView pause=action(focusEnabled?"Pausa consciente: activada":"Pausa consciente: desactivada",()->{
      focusEnabled=!focusEnabled;persist();showSettings();
    });
    view.addView(pause);view.addView(gap(12));
    TextView schedule=action("Horario de concentración: "+formatMinutes(focusStart)+" – "+formatMinutes(focusEnd),
      this::chooseFocusHours);
    view.addView(schedule);view.addView(gap(9));
    TextView allDay=text("Si la hora inicial y final son iguales, la pausa se aplica todo el día.",12,MUTED,false);
    view.addView(allDay);view.addView(gap(12));
    TextView explanation=text("La pausa se aplica únicamente a las aplicaciones que marques al abrirlas desde NeuroSync. No bloquea aplicaciones desde otros accesos.",12,MUTED,false);
    view.addView(explanation);view.addView(gap(22));
    view.addView(text("Bienestar digital",18,WHITE,true));view.addView(gap(10));
    view.addView(action("Configurar filtro de notificaciones",this::showNotificationPermissionHelp));
    view.addView(gap(12));
    view.addView(action("Conceder acceso a tiempo de uso",this::requestUsageAccess));view.addView(gap(12));
    TextView total=text("Tiempo aproximado registrado hoy: "+(todayUsageMillis()/60000)+" minutos",14,MUTED,false);view.addView(total);
    view.addView(gap(26));
    view.addView(action("Cambiar pantalla de inicio desde Android",()->launchIntent(new Intent(Settings.ACTION_HOME_SETTINGS))));
    showContent(view);
  }
  @Override public void onBackPressed(){
    if(drawer){drawer=false;renderHome();}
    else super.onBackPressed();
  }
}