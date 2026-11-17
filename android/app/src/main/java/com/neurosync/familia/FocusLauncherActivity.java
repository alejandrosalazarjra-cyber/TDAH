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
  private int BLACK=0xFF101113, WHITE=0xFFF8F8F7, MUTED=0xFF9E9EA1, CARD=0xFF232428;
  private int themeMode=0;
  private LinearLayout root, page;
  private EditText search;
  private boolean drawer=false;
  private final Handler handler=new Handler(Looper.getMainLooper());
  private final Runnable updateClock=new Runnable(){@Override public void run(){if(!drawer)renderHome();handler.postDelayed(this,30000);}};
  private final ArrayList<AppEntry> apps=new ArrayList<>();
  private final Set<String> favorites=new HashSet<>(), focusApps=new HashSet<>();
  private android.content.SharedPreferences preferences;
  private float xStart,yStart;
  private boolean focusEnabled=true;
  private int focusStart=0,focusEnd=0;
  private int dailyBudget=120;
  private int mindfulDelay=5;
  private boolean usageWarningShown=false;

  private void applyTheme(){
    if(themeMode==1){BLACK=0xFF101C32;CARD=0xFF223653;WHITE=0xFFF6F9FF;MUTED=0xFFA7B6CD;}
    else if(themeMode==2){BLACK=0xFF10241E;CARD=0xFF243C32;WHITE=0xFFF6FCF8;MUTED=0xFFACBEB5;}
    else{BLACK=0xFF101113;CARD=0xFF232428;WHITE=0xFFF8F8F7;MUTED=0xFF9E9EA1;}
    getWindow().setStatusBarColor(BLACK);getWindow().setNavigationBarColor(BLACK);
    if(root!=null)root.setBackgroundColor(BLACK);
  }
  private void cycleTheme(){themeMode=(themeMode+1)%3;preferences.edit().putInt("theme_mode",themeMode).apply();applyTheme();showSettings();}
  private String themeName(){return themeMode==1?"Azul profundo":themeMode==2?"Bosque oscuro":"Grafito";}
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
    TextView t=new TextView(this);
    t.setText(contents);t.setTextSize(size);t.setTextColor(color);
    t.setIncludeFontPadding(true);
    t.setTypeface(Typeface.create("sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));
    t.setBreakStrategy(android.text.Layout.BREAK_STRATEGY_HIGH_QUALITY);
    return t;
  }
  private void pad(View v,int x,int y){v.setPadding(dp(x),dp(y),dp(x),dp(y));}
  private TextView action(String value,Runnable click){
    TextView t=text(value,14,WHITE,false);
    pad(t,12,11);
    android.graphics.drawable.GradientDrawable background=new android.graphics.drawable.GradientDrawable();
    background.setColor(CARD);background.setCornerRadius(dp(14));t.setBackground(background);
    t.setMinHeight(dp(46));
    t.setGravity(Gravity.CENTER);
    t.setMaxLines(2);
    t.setEllipsize(android.text.TextUtils.TruncateAt.END);
    if(click!=null)t.setOnClickListener(v->click.run());
    return t;
  }
  private View gap(int height){View v=new View(this);v.setLayoutParams(new LinearLayout.LayoutParams(1,dp(height)));return v;}
  private View line(){View v=new View(this);v.setBackgroundColor(0xFF44464C);v.setLayoutParams(new LinearLayout.LayoutParams(-1,dp(1)));return v;}

  @Override public void onCreate(Bundle state){
    super.onCreate(state);
    getWindow().getDecorView().setSystemUiVisibility(0);
    preferences=getSharedPreferences("focus_launcher",MODE_PRIVATE);
    themeMode=preferences.getInt("theme_mode",0);applyTheme();
    favorites.addAll(preferences.getStringSet("favorites",Collections.emptySet()));
    focusApps.addAll(preferences.getStringSet("focus",Collections.emptySet()));
    focusEnabled=preferences.getBoolean("focus_enabled",true);
    focusStart=preferences.getInt("focus_start",0);
    focusEnd=preferences.getInt("focus_end",0);
    dailyBudget=preferences.getInt("daily_budget",120);
    mindfulDelay=preferences.getInt("mindful_delay",5);
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
  @Override protected void onResume(){
    super.onResume();handler.removeCallbacks(updateClock);handler.post(updateClock);
    String today=new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new Date());
    if(!today.equals(preferences.getString("usage_warning_date",""))){
      usageWarningShown=false;
      preferences.edit().putString("usage_warning_date",today).apply();
    }
    if(!usageWarningShown&&hasUsagePermission()&&dailyBudget>0&&
       !today.equals(preferences.getString("last_budget_alert_date",""))&&todayUsageMillis()/60000>=dailyBudget){
      usageWarningShown=true;
      preferences.edit().putString("last_budget_alert_date",today).apply();
      new AlertDialog.Builder(this).setTitle("Recordatorio de descanso")
        .setMessage("Has alcanzado tu objetivo diario de "+dailyBudget+" minutos de uso. Puedes cerrar aplicaciones o continuar voluntariamente.")
        .setPositiveButton("Ver mis tareas",(d,w)->openNeuroSync())
        .setNegativeButton("Entendido",null).show();
    }
  }
  @Override protected void onPause(){handler.removeCallbacks(updateClock);super.onPause();}
  private void persist(){
    preferences.edit().putStringSet("favorites",new HashSet<>(favorites))
      .putStringSet("focus",new HashSet<>(focusApps))
      .putBoolean("focus_enabled",focusEnabled)
      .putInt("focus_start",focusStart).putInt("focus_end",focusEnd)
      .putInt("daily_budget",dailyBudget).putInt("mindful_delay",mindfulDelay).apply();
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
  private long focusRemaining(){
    long until=preferences.getLong("focus_session_until",0);
    return Math.max(0,until-System.currentTimeMillis());
  }
  private void chooseFocusSession(){
    if(focusRemaining()>0){
      new AlertDialog.Builder(this).setTitle("Sesión en curso")
        .setMessage("Todavía tienes "+(focusRemaining()+59999)/60000+" minutos. ¿Quieres terminar la sesión?")
        .setPositiveButton("Terminar",(d,w)->{preferences.edit().remove("focus_session_until").apply();renderHome();})
        .setNegativeButton("Continuar",null).show();
      return;
    }
    final String[] options={"15 minutos","25 minutos","45 minutos","60 minutos"};
    final int[] minutes={15,25,45,60};
    new AlertDialog.Builder(this).setTitle("Iniciar concentración")
      .setItems(options,(d,index)->{
        long until=System.currentTimeMillis()+minutes[index]*60000L;
        preferences.edit().putLong("focus_session_until",until).apply();
        renderHome();
      }).setNegativeButton("Cancelar",null).show();
  }
  private void renderHome(){
    if(drawer)return;
    LinearLayout body=vertical();
    body.setBackgroundColor(BLACK);
    pad(body,18,16);
    LinearLayout head=horizontal();
    TextView brand=text("NEUROSYNC  ·  INICIO",11,MUTED,true);
    brand.setSingleLine(true);
    head.addView(brand,new LinearLayout.LayoutParams(0,-2,1));
    TextView settings=action("⚙",this::showSettings);
    settings.setMinWidth(dp(46));head.addView(settings);body.addView(head);
    body.addView(gap(28));
    Date now=new Date();
    TextView clock=text(new SimpleDateFormat("HH:mm",Locale.getDefault()).format(now),56,WHITE,false);
    clock.setGravity(Gravity.CENTER);clock.setSingleLine(true);
    body.addView(clock);
    TextView date=text(new SimpleDateFormat("EEEE, d 'de' MMMM",new Locale("es","CO")).format(now),14,MUTED,false);
    date.setGravity(Gravity.CENTER);date.setMaxLines(2);
    body.addView(date);
    body.addView(gap(24));
    int pending=getSharedPreferences("ns_widget",0).getInt("pending",0);
    TextView progress=action("☑  "+pending+" tareas pendientes",this::openNeuroSync);
    body.addView(progress);
    body.addView(gap(8));
    long focus=focusRemaining();
    TextView session=action(focus>0?
       "◷  Concentración: "+((focus+59999)/60000)+" min":"◷  Iniciar concentración",this::chooseFocusSession);
    body.addView(session);
    body.addView(gap(22));
    TextView favoriteTitle=text("FAVORITAS",11,MUTED,true);
    body.addView(favoriteTitle);body.addView(gap(7));
    int count=0;
    for(AppEntry app:apps){
      if(favorites.contains(app.packageName)){
        addHomeApp(body,app);
        if(++count>=6)break;
      }
    }
    if(count==0){
      TextView tip=text("Elige tus aplicaciones favoritas desde la lista.",12,MUTED,false);
      pad(tip,0,5);tip.setMaxLines(2);body.addView(tip);
    }
    body.addView(gap(18));body.addView(line());body.addView(gap(12));
    body.addView(action("⌕  Buscar aplicaciones",this::showDrawer));
    body.addView(gap(8));
    body.addView(action("✓  Agenda y tareas",this::openNeuroSync));
    body.addView(gap(16));
    LinearLayout shortcuts=horizontal();
    shortcuts.setGravity(Gravity.CENTER);
    TextView phone=action("☎  Teléfono",()->launchIntent(new Intent(Intent.ACTION_DIAL)));
    shortcuts.addView(phone,new LinearLayout.LayoutParams(0,dp(48),1));
    shortcuts.addView(new View(this),new LinearLayout.LayoutParams(dp(10),1));
    TextView camera=action("◎  Cámara",()->launchIntent(new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)));
    shortcuts.addView(camera,new LinearLayout.LayoutParams(0,dp(48),1));
    body.addView(shortcuts);
    body.addView(gap(16));
    TextView footer=text("Desliza para abrir tus aplicaciones",11,MUTED,false);
    footer.setGravity(Gravity.CENTER);body.addView(footer);
    showContent(body);
  }
  private void addHomeApp(LinearLayout container,AppEntry entry){
    TextView item=text("•  "+entry.title,18,WHITE,false);pad(item,8,10);
    item.setMaxLines(1);item.setEllipsize(android.text.TextUtils.TruncateAt.END);
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
    search.setTextSize(15);search.setBackgroundColor(CARD);pad(search,14,11);
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
      TextView name=text(star+entry.title,16,WHITE,false);pad(name,10,11);
      name.setSingleLine(true);name.setEllipsize(android.text.TextUtils.TruncateAt.END);
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
    int currentLimit=preferences.getInt("app_limit_"+app.packageName,0);
    String[] choices={
      fav?"Quitar de favoritos":"Agregar a inicio",
      focus?"Quitar pausa consciente":"Añadir pausa consciente",
      "Ajustar sus notificaciones en Android",
      "Objetivo diario: "+(currentLimit==0?"sin límite":currentLimit+" min"),
      "Abrir aplicación"
    };
    new AlertDialog.Builder(this).setTitle(app.title).setItems(choices,(dialog,index)->{
      if(index==0){
        if(fav)favorites.remove(app.packageName);else favorites.add(app.packageName);
        persist();refreshApps();
        if(drawer)renderApps(search==null?"":search.getText().toString());else renderHome();
      }
      if(index==1){
        if(focus)focusApps.remove(app.packageName);else focusApps.add(app.packageName);
        persist();if(drawer)renderApps(search==null?"":search.getText().toString());
      }
      if(index==2)openNotificationSettings(app);
      if(index==3)chooseAppLimit(app);
      if(index==4)openEntry(app);
    }).setNegativeButton("Cancelar",null).show();
  }
  private void openNotificationSettings(AppEntry app){
    // El usuario gestiona la interrupción desde los ajustes oficiales de Android.
    openNotificationSettingsFor(app.packageName);
  }
  private void openNotificationSettingsFor(String pkg){
    try{
      Intent intent=new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
      intent.putExtra(Settings.EXTRA_APP_PACKAGE,pkg);
      startActivity(intent);
    }catch(Exception error){
      launchIntent(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        android.net.Uri.parse("package:"+pkg)));
    }
  }
  private void showNotificationControls(){
    drawer=true;refreshApps();
    LinearLayout view=vertical();pad(view,16,16);
    view.addView(action("‹  Volver a ajustes",this::showSettings));view.addView(gap(20));
    view.addView(text("Notificaciones",25,WHITE,true));view.addView(gap(10));
    view.addView(text("Reduce interrupciones desde los controles oficiales de Android. NeuroSync no lee mensajes privados ni modifica los avisos sin tu autorización.",13,MUTED,false));
    view.addView(gap(14));
    view.addView(action("Notificaciones de NeuroSync",()->openNotificationSettingsFor(getPackageName())));
    view.addView(gap(14));
    HashSet<String> added=new HashSet<>();
    ArrayList<AppEntry> ordered=new ArrayList<>(apps);
    ordered.sort(Comparator.comparing(a->a.title.toLowerCase(Locale.getDefault())));
    for(AppEntry app:ordered){
      if(!added.add(app.packageName))continue;
      view.addView(action("◌  "+app.title,()->openNotificationSettings(app)));
      view.addView(gap(7));
    }
    showContent(view);
  }
  private void chooseAppLimit(AppEntry app){
    String[] labels={"Sin límite","10 minutos","20 minutos","30 minutos","45 minutos","60 minutos","90 minutos"};
    int[] values={0,10,20,30,45,60,90};
    new AlertDialog.Builder(this).setTitle("Tiempo diario: "+app.title)
      .setItems(labels,(d,index)->{
        preferences.edit().putInt("app_limit_"+app.packageName,values[index]).apply();
        Toast.makeText(this,values[index]==0?"Objetivo desactivado":"Aviso a los "+values[index]+" minutos",Toast.LENGTH_SHORT).show();
      }).setNegativeButton("Cancelar",null).show();
  }
  private long appUsageMinutes(String packageName){
    if(!hasUsagePermission())return -1;
    UsageStatsManager manager=(UsageStatsManager)getSystemService(USAGE_STATS_SERVICE);
    Calendar d=Calendar.getInstance();d.set(Calendar.HOUR_OF_DAY,0);d.set(Calendar.MINUTE,0);
    d.set(Calendar.SECOND,0);d.set(Calendar.MILLISECOND,0);
    Map<String,UsageStats> usage=manager.queryAndAggregateUsageStats(d.getTimeInMillis(),System.currentTimeMillis());
    UsageStats info=usage==null?null:usage.get(packageName);
    return info==null?0:info.getTotalTimeInForeground()/60000;
  }
  private boolean focusIsActive(){
    if(focusRemaining()>0)return true;
    if(!focusEnabled)return false;
    if(focusStart==focusEnd)return true;
    Calendar now=Calendar.getInstance();int minute=now.get(Calendar.HOUR_OF_DAY)*60+now.get(Calendar.MINUTE);
    return focusStart<focusEnd?minute>=focusStart&&minute<focusEnd:minute>=focusStart||minute<focusEnd;
  }
  private void openEntry(AppEntry app){
    int limit=preferences.getInt("app_limit_"+app.packageName,0);
    if(limit>0&&!hasUsagePermission()){
      // Un permiso especial denegado nunca debe impedir abrir una app.
      // No insistir con Ajustes ni mostrar el mismo aviso en cada lanzamiento.
      openWithFocus(app);
      return;
    }
    long minutes=limit>0?appUsageMinutes(app.packageName):-1;
    if(limit>0&&minutes>=limit){
      new AlertDialog.Builder(this).setTitle("Objetivo diario alcanzado")
        .setMessage("Ya has usado "+app.title+" aproximadamente "+minutes+" minutos hoy, superando tu objetivo de "+limit+". Puedes volver a tu agenda o abrir la aplicación igualmente.")
        .setPositiveButton("Abrir igualmente",(d,w)->openWithFocus(app))
        .setNegativeButton("Volver a mis tareas",(d,w)->openNeuroSync())
        .setNeutralButton("Cancelar",null).show();
      return;
    }
    openWithFocus(app);
  }
  private void openWithFocus(AppEntry app){
    if(focusIsActive()&&focusApps.contains(app.packageName)){
      new AlertDialog.Builder(this)
        .setTitle("Antes de abrir "+app.title)
        .setMessage("¿Para qué quieres usar esta aplicación? Puedes continuar o volver a tus tareas. Si continúas, habrá una pausa de "+mindfulDelay+" segundos.")
        .setPositiveButton("Continuar",(d,w)->delayLaunch(app))
        .setNegativeButton("Volver a mis tareas",(d,w)->openNeuroSync())
        .setNeutralButton("Cancelar",null).show();
    }else launchEntry(app);
  }
  private void delayLaunch(AppEntry app){
    if(mindfulDelay<=0){launchEntry(app);return;}
    LinearLayout content=vertical();pad(content,22,14);
    TextView countdown=text(String.valueOf(mindfulDelay),35,BLACK,true);
    countdown.setGravity(Gravity.CENTER);content.addView(countdown);
    TextView message=text("Puedes cambiar de opinión antes de abrir "+app.title+".",14,BLACK,false);
    content.addView(message);
    final AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Pausa consciente")
      .setView(content).setNegativeButton("Cancelar",null).create();
    final long end=SystemClock.elapsedRealtime()+mindfulDelay*1000L;
    final Runnable[] ticker=new Runnable[1];
    ticker[0]=()->{
      if(!dialog.isShowing())return;
      long remaining=Math.max(0,(end-SystemClock.elapsedRealtime()+999)/1000);
      countdown.setText(String.valueOf(remaining));
      if(remaining<=0){dialog.dismiss();launchEntry(app);}
      else handler.postDelayed(ticker[0],200);
    };
    dialog.setOnDismissListener(d->handler.removeCallbacks(ticker[0]));
    dialog.show();handler.post(ticker[0]);
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
  private void requestUsageAccess(){
    if(hasUsagePermission()){
      Toast.makeText(this,"El acceso al tiempo de uso ya está activado",Toast.LENGTH_SHORT).show();
      return;
    }
    new AlertDialog.Builder(this)
      .setTitle("Permiso opcional: tiempo de uso")
      .setMessage("Este permiso permite calcular cuánto tiempo utilizas otras aplicaciones. No hace falta para tus tareas, alarmas, dragón o modo concentración. Si Android bloquea el acceso por una instalación manual, puedes seguir usando NeuroSync sin concederlo. Solo activa este permiso si confías en la aplicación y comprendes qué datos permite consultar.")
      .setPositiveButton("Abrir ajustes de Android",(d,w)->launchIntent(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)))
      .setNegativeButton("Continuar sin permiso",null).show();
  }
  private boolean hasUsagePermission(){
    android.app.AppOpsManager ops=(android.app.AppOpsManager)getSystemService(APP_OPS_SERVICE);
    return ops!=null&&ops.checkOpNoThrow(android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
      android.os.Process.myUid(),getPackageName())==android.app.AppOpsManager.MODE_ALLOWED;
  }
  private void showUsageReport(){
    if(!hasUsagePermission()){
      new AlertDialog.Builder(this).setTitle("Tiempo de uso")
        .setMessage("Android no ha concedido el permiso opcional de tiempo de uso. El reporte de otras aplicaciones no está disponible, pero tus tareas, alarmas, hábitos y concentración siguen funcionando. En algunas APK instaladas manualmente Android restringe este acceso.")
        .setNegativeButton("Cancelar",null)
        .setPositiveButton("Más información",(d,w)->requestUsageAccess()).show();
      return;
    }
    UsageStatsManager stats=(UsageStatsManager)getSystemService(USAGE_STATS_SERVICE);
    Calendar d=Calendar.getInstance();d.set(Calendar.HOUR_OF_DAY,0);d.set(Calendar.MINUTE,0);
    d.set(Calendar.SECOND,0);d.set(Calendar.MILLISECOND,0);
    Map<String,UsageStats> rows=stats.queryAndAggregateUsageStats(d.getTimeInMillis(),System.currentTimeMillis());
    ArrayList<UsageStats> ordered=new ArrayList<>(rows==null?Collections.emptyList():rows.values());
    ordered.sort((a,b)->Long.compare(b.getTotalTimeInForeground(),a.getTotalTimeInForeground()));
    StringBuilder summary=new StringBuilder();int displayed=0;
    for(UsageStats entry:ordered){
      long minutes=entry.getTotalTimeInForeground()/60000;
      if(minutes<1||entry.getPackageName().equals(getPackageName()))continue;
      String label=entry.getPackageName();
      try{label=String.valueOf(getPackageManager().getApplicationLabel(
        getPackageManager().getApplicationInfo(label,0)));}catch(Exception ignored){}
      summary.append(label).append(": ").append(minutes).append(" min\n");
      if(++displayed>=10)break;
    }
    if(displayed==0)summary.append("Todavía no hay información de uso para hoy.");
    new AlertDialog.Builder(this).setTitle("Uso de aplicaciones hoy").setMessage(summary.toString())
      .setPositiveButton("Cerrar",null).show();
  }
  private void chooseDailyBudget(){
    final String[] choices={"Desactivado","30 minutos","60 minutos","90 minutos","120 minutos","180 minutos","240 minutos"};
    final int[] values={0,30,60,90,120,180,240};
    int selected=0;for(int i=0;i<values.length;i++)if(values[i]==dailyBudget)selected=i;
    new AlertDialog.Builder(this).setTitle("Recordatorio de tiempo diario")
      .setSingleChoiceItems(choices,selected,(d,index)->{dailyBudget=values[index];usageWarningShown=false;persist();d.dismiss();showSettings();})
      .setNegativeButton("Cancelar",null).show();
  }
  private void chooseMindfulDelay(){
    final String[] choices={"Sin espera","3 segundos","5 segundos","10 segundos","20 segundos"};
    final int[] values={0,3,5,10,20};
    new AlertDialog.Builder(this).setTitle("Duración de la pausa")
      .setItems(choices,(d,index)->{mindfulDelay=values[index];persist();showSettings();}).show();
  }
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
    LinearLayout view=vertical();pad(view,16,17);
    TextView back=action("‹  Volver a inicio",()->{drawer=false;renderHome();});view.addView(back);
    view.addView(gap(30));view.addView(text("Mi inicio minimalista",25,WHITE,true));view.addView(gap(14));
    view.addView(action("Tema visual: "+themeName()+"  ·  Cambiar",this::cycleTheme));view.addView(gap(12));
    TextView explain=text("Puedes usar NeuroSync como pantalla de inicio sin perder el acceso a otras aplicaciones ni las llamadas de emergencia. Android te permitirá regresar a tu launcher anterior desde Ajustes.",14,MUTED,false);
    view.addView(explain);view.addView(gap(24));
    view.addView(action("✓  Elegir NeuroSync como pantalla de inicio",this::chooseDefaultHome));view.addView(gap(13));
    view.addView(action("⌂  Abrir agenda y organización",this::openNeuroSync));view.addView(gap(13));
    view.addView(action("◷  Gestionar sesión de concentración",this::chooseFocusSession));view.addView(gap(13));
    TextView pause=action(focusEnabled?"Pausa consciente: activada":"Pausa consciente: desactivada",()->{
      focusEnabled=!focusEnabled;persist();showSettings();
    });
    view.addView(pause);view.addView(gap(12));
    view.addView(action("Duración de la pausa: "+mindfulDelay+" segundos",this::chooseMindfulDelay));
    view.addView(gap(12));
    TextView schedule=action("Horario de concentración: "+formatMinutes(focusStart)+" – "+formatMinutes(focusEnd),
      this::chooseFocusHours);
    view.addView(schedule);view.addView(gap(9));
    TextView allDay=text("Si la hora inicial y final son iguales, la pausa se aplica todo el día.",12,MUTED,false);
    view.addView(allDay);view.addView(gap(12));
    TextView explanation=text("La pausa se aplica únicamente a las aplicaciones que marques al abrirlas desde NeuroSync. No bloquea aplicaciones desde otros accesos.",12,MUTED,false);
    view.addView(explanation);view.addView(gap(22));
    view.addView(text("Bienestar digital",18,WHITE,true));view.addView(gap(10));
    view.addView(action("Administrar notificaciones por aplicación",this::showNotificationControls));
    view.addView(gap(12));
    view.addView(action("Recordatorio diario: "+(dailyBudget==0?"desactivado":dailyBudget+" minutos"),this::chooseDailyBudget));
    view.addView(gap(12));
    view.addView(action("Ver uso por aplicación",this::showUsageReport));
    view.addView(gap(12));
    if(!hasUsagePermission()){
      view.addView(action("Tiempo de uso: permiso opcional (información)",this::requestUsageAccess));view.addView(gap(12));
    }
    TextView total=text(hasUsagePermission()
      ? "Tiempo aproximado registrado hoy: "+(todayUsageMillis()/60000)+" minutos"
      : "Las estadísticas de otras aplicaciones están desactivadas. Las funciones de organización y concentración siguen disponibles.",14,MUTED,false);
    view.addView(total);
    view.addView(gap(26));
    view.addView(text("Pantalla y accesibilidad",18,WHITE,true));view.addView(gap(10));
    view.addView(action("Opciones de escala de grises del sistema",()->{
      new AlertDialog.Builder(this).setTitle("Modo monocromático")
        .setMessage("NeuroSync ya usa una interfaz oscura minimalista. Para poner otras aplicaciones en escala de grises, Android ofrece opciones de corrección de color o Bienestar digital según el fabricante. Puedes activarlo voluntariamente en Ajustes.")
        .setNegativeButton("Cancelar",null)
        .setPositiveButton("Abrir Accesibilidad",(d,w)->launchIntent(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)))
        .show();
    }));view.addView(gap(14));
    view.addView(action("Cambiar pantalla de inicio desde Android",()->launchIntent(new Intent(Settings.ACTION_HOME_SETTINGS))));
    showContent(view);
  }
  @Override public void onBackPressed(){
    if(drawer){drawer=false;renderHome();}
    else super.onBackPressed();
  }
}