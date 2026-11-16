package com.neurosync.familia;

import android.app.Notification;
import android.content.Context;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import java.util.Set;

/** Filtrado privado y opt-in de notificaciones de aplicaciones seleccionadas.
 * Solo lee el nombre del paquete; nunca envía ni almacena texto de notificaciones.
 * No interviene llamadas, alarmas ni avisos de servicios persistentes.
 */
public final class FocusNotificationListener extends NotificationListenerService {
  @Override public void onNotificationPosted(StatusBarNotification sbn){
    if(sbn==null)return;
    String pkg=sbn.getPackageName();
    if(pkg==null||pkg.equals(getPackageName())||pkg.equals("android")||pkg.startsWith("com.android."))return;
    Set<String> hidden=getSharedPreferences("focus_launcher",Context.MODE_PRIVATE)
      .getStringSet("muted_notifications",java.util.Collections.emptySet());
    if(!hidden.contains(pkg))return;
    Notification n=sbn.getNotification();
    if(n==null)return;
    String category=n.category;
    if(Notification.CATEGORY_CALL.equals(category)||Notification.CATEGORY_ALARM.equals(category)
      ||Notification.CATEGORY_SERVICE.equals(category)||Notification.CATEGORY_SYSTEM.equals(category))return;
    if((n.flags&Notification.FLAG_FOREGROUND_SERVICE)!=0)return;
    try{cancelNotification(sbn.getKey());}
    catch(SecurityException exception){android.util.Log.w("NeuroSync","Android denegó filtro de notificaciones");}
  }
}