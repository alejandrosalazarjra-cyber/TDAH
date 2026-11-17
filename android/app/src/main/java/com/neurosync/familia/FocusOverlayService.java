package com.neurosync.familia;

import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Vista flotante temporal iniciada explícitamente por el usuario.
 * No observa otras aplicaciones, no bloquea toques externos y desaparece sola.
 * En especial, nunca cubre diálogos de permisos ni la pantalla completa.
 */
public final class FocusOverlayService extends Service {
  private WindowManager manager;
  private View floating;
  private final Handler handler = new Handler(Looper.getMainLooper());
  private final Runnable timeout = this::stopSelf;

  private int dp(float value) {
    return Math.round(value * getResources().getDisplayMetrics().density);
  }

  @Override public IBinder onBind(Intent intent) { return null; }

  @Override public int onStartCommand(Intent intent, int flags, int startId) {
    if (!Settings.canDrawOverlays(this)) {
      stopSelf();
      return START_NOT_STICKY;
    }
    removeFloating();
    try {
      manager = (WindowManager) getSystemService(WINDOW_SERVICE);
      LinearLayout panel = new LinearLayout(this);
      panel.setOrientation(LinearLayout.HORIZONTAL);
      panel.setGravity(Gravity.CENTER_VERTICAL);
      panel.setPadding(dp(14),dp(10),dp(10),dp(10));
      GradientDrawable background = new GradientDrawable(
        GradientDrawable.Orientation.LEFT_RIGHT,
        new int[]{0xFF11223C,0xFF173F68});
      background.setCornerRadius(dp(17));
      background.setStroke(dp(1),0xFF408ED1);
      panel.setBackground(background);
      panel.setElevation(dp(9));

      TextView message = new TextView(this);
      message.setText("DrakoPlan · Vuelve a tu objetivo");
      message.setTextSize(14);
      message.setTextColor(Color.WHITE);
      message.setGravity(Gravity.CENTER_VERTICAL);
      panel.addView(message,new LinearLayout.LayoutParams(0,dp(42),1f));
      message.setOnClickListener(v->{
        Intent open=new Intent(this,MainActivity.class);
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(open);
        stopSelf();
      });

      TextView close = new TextView(this);
      close.setText("✕");
      close.setContentDescription("Cerrar ventana flotante de DrakoPlan");
      close.setTextColor(Color.WHITE);
      close.setTextSize(22);
      close.setGravity(Gravity.CENTER);
      panel.addView(close,new LinearLayout.LayoutParams(dp(44),dp(44)));
      close.setOnClickListener(v->stopSelf());

      WindowManager.LayoutParams params=new WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
          | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
          | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT);
      params.gravity=Gravity.TOP | Gravity.CENTER_HORIZONTAL;
      params.x=0;
      params.y=dp(65);
      manager.addView(panel,params);
      floating=panel;
      handler.removeCallbacks(timeout);
      handler.postDelayed(timeout,20000);
    } catch (RuntimeException e) {
      android.util.Log.w("DrakoPlan","Superposición no disponible",e);
      stopSelf();
    }
    return START_NOT_STICKY;
  }

  private void removeFloating(){
    handler.removeCallbacks(timeout);
    if (manager != null && floating != null) {
      try { manager.removeView(floating); }
      catch (RuntimeException ignored) {}
    }
    floating=null;
  }

  @Override public void onDestroy() {
    removeFloating();
    super.onDestroy();
  }
}
