package com.neurosync.familia;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

/** Widget ligero para pantalla de inicio con número de actividades pendientes. */
public final class TodayWidget extends AppWidgetProvider {
 public static void refresh(Context ctx,int count){
  ctx.getSharedPreferences("ns_widget",0).edit().putInt("pending",Math.max(0,count)).apply();
  AppWidgetManager manager=AppWidgetManager.getInstance(ctx);
  int[] ids=manager.getAppWidgetIds(new android.content.ComponentName(ctx,TodayWidget.class));
  update(ctx,manager,ids);
 }
 private static void update(Context ctx,AppWidgetManager manager,int[] ids){
  int pending=ctx.getSharedPreferences("ns_widget",0).getInt("pending",0);
  for(int id:ids){
   RemoteViews view=new RemoteViews(ctx.getPackageName(),R.layout.widget_today);
   view.setTextViewText(R.id.widget_count,String.valueOf(pending));
   Intent open=new Intent(ctx,MainActivity.class);
   PendingIntent p=PendingIntent.getActivity(ctx,0,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
   view.setOnClickPendingIntent(R.id.widget_root,p);
   manager.updateAppWidget(id,view);
  }
 }
 @Override public void onUpdate(Context ctx,AppWidgetManager manager,int[] ids){update(ctx,manager,ids);}
}
