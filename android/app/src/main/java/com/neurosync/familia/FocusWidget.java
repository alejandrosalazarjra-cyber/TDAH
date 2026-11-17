package com.neurosync.familia;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

/** Widget compacto para el temporizador de enfoque local, sin mostrar datos de tareas privadas. */
public final class FocusWidget extends AppWidgetProvider {
 public static void refresh(Context ctx){
  AppWidgetManager manager=AppWidgetManager.getInstance(ctx);
  int[] ids=manager.getAppWidgetIds(new android.content.ComponentName(ctx,FocusWidget.class));
  update(ctx,manager,ids);
 }
 private static void update(Context ctx,AppWidgetManager manager,int[] ids){
  long until=ctx.getSharedPreferences("focus_launcher",0).getLong("focus_session_until",0);
  long left=Math.max(0,(until-System.currentTimeMillis()+59999)/60000);
  for(int id:ids){
   RemoteViews view=new RemoteViews(ctx.getPackageName(),R.layout.widget_focus);
   view.setTextViewText(R.id.widget_focus_minutes,left>0?left+" min restantes":"Sin sesión activa");
   PendingIntent app=PendingIntent.getActivity(ctx,0,new Intent(ctx,MainActivity.class),
       PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
   PendingIntent home=PendingIntent.getActivity(ctx,1,new Intent(ctx,FocusLauncherActivity.class),
       PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
   view.setOnClickPendingIntent(R.id.widget_focus_root,app);
   view.setOnClickPendingIntent(R.id.widget_focus_open_app,app);
   view.setOnClickPendingIntent(R.id.widget_focus_open_home,home);
   manager.updateAppWidget(id,view);
  }
 }
 @Override public void onUpdate(Context ctx,AppWidgetManager manager,int[] ids){update(ctx,manager,ids);}
}