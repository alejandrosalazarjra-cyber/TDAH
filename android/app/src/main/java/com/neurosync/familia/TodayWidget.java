package com.neurosync.familia;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

/** Widget de tareas, próxima alarma y concentración; la app solo expone resúmenes locales. */
public final class TodayWidget extends AppWidgetProvider {
 private static final String PREF="ns_widget";
 public static void refresh(Context ctx,int count){
  ctx.getSharedPreferences(PREF,0).edit().putInt("pending",Math.max(0,count)).apply();
  refreshState(ctx);
 }
 public static void setNextAlarm(Context ctx,String value){
  // Solo hora y fecha, no incluye títulos ni contenido privado.
  String safe=value==null?"":value.trim();
  if(safe.length()>45)safe=safe.substring(0,45);
  ctx.getSharedPreferences(PREF,0).edit().putString("next_alarm",safe).apply();
  refreshState(ctx);
 }
 public static void refreshState(Context ctx){
  AppWidgetManager manager=AppWidgetManager.getInstance(ctx);
  int[] ids=manager.getAppWidgetIds(new android.content.ComponentName(ctx,TodayWidget.class));
  update(ctx,manager,ids);
 }
 private static void update(Context ctx,AppWidgetManager manager,int[] ids){
  android.content.SharedPreferences p=ctx.getSharedPreferences(PREF,0);
  int pending=p.getInt("pending",0);
  String next=p.getString("next_alarm","");
  long until=ctx.getSharedPreferences("focus_launcher",0).getLong("focus_session_until",0);
  long minutes=Math.max(0,(until-System.currentTimeMillis()+59999)/60000);
  for(int id:ids){
   RemoteViews view=new RemoteViews(ctx.getPackageName(),R.layout.widget_today);
   view.setTextViewText(R.id.widget_count,String.valueOf(pending));
   view.setTextViewText(R.id.widget_next_alarm,
        next.isEmpty()?"◷ Próxima alarma: sin programar":"◷ Próxima alarma: "+next);
   view.setTextViewText(R.id.widget_focus,
        minutes>0?"◎ Concentración: "+minutes+" min restantes":"◎ Concentración: sin sesión");
   Intent open=new Intent(ctx,MainActivity.class);
   PendingIntent app=PendingIntent.getActivity(ctx,0,open,
       PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
   Intent launcher=new Intent(ctx,FocusLauncherActivity.class);
   PendingIntent home=PendingIntent.getActivity(ctx,1,launcher,
       PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
   view.setOnClickPendingIntent(R.id.widget_root,app);
   view.setOnClickPendingIntent(R.id.widget_open_tasks,app);
   view.setOnClickPendingIntent(R.id.widget_open_launcher,home);
   manager.updateAppWidget(id,view);
  }
 }
 @Override public void onUpdate(Context ctx,AppWidgetManager manager,int[] ids){update(ctx,manager,ids);}
}