package com.neurosync.familia;

import android.app.*;
import android.content.*;
import android.os.Build;
import android.os.IBinder;
import org.json.JSONObject;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

/** Servidor autoritativo local en el móvil principal. Acceso solo con secreto compartido.
 * Necesita red privada (p.ej., Tailscale) para conectar fuera de casa.
 */
public final class SyncService extends Service {
  public static final int PORT=8765;
  private ServerSocket server;
  private final ExecutorService workers=Executors.newFixedThreadPool(3);
  private volatile boolean running=false;
  @Override public void onCreate(){
    super.onCreate();
    NotificationManager nm=getSystemService(NotificationManager.class);
    nm.createNotificationChannel(new NotificationChannel("sync","Sincronización familiar",NotificationManager.IMPORTANCE_LOW));
    Notification n=new Notification.Builder(this,"sync").setContentTitle("NeuroSync: servidor familiar")
      .setContentText("Tu celular comparte las tareas en la red privada").setSmallIcon(android.R.drawable.stat_notify_sync).setOngoing(true).build();
    if(Build.VERSION.SDK_INT>=34)startForeground(101,n,android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
    else startForeground(101,n);
    running=true;
    new Thread(this::serve,"family-server").start();
  }
  @Override public int onStartCommand(Intent intent,int flags,int id){return START_NOT_STICKY;}
  private void serve(){
    try(ServerSocket listener=new ServerSocket(PORT)){
      server=listener;
      while(running){
        Socket socket=listener.accept();
        workers.execute(()->handle(socket));
      }
    }catch(Exception e){android.util.Log.e("NeuroSync","Servidor detenido",e);}
  }
  private void handle(Socket socket){
    try(Socket s=socket){
      s.setSoTimeout(10000);
      InputStream in=new BufferedInputStream(s.getInputStream());
      ByteArrayOutputStream rawHeader=new ByteArrayOutputStream();
      int b,state=0;
      while((b=in.read())!=-1 && rawHeader.size()<8192){
        rawHeader.write(b);
        if((state==0||state==2)&&b==13)state++;
        else if((state==1||state==3)&&b==10)state++;
        else state=0;
        if(state==4)break;
      }
      if(state!=4){reply(s,400,"{\"error\":\"Encabezado inválido\"}");return;}
      String header=rawHeader.toString("UTF-8");
      String[] lines=header.split("\r\n");
      if(!lines[0].startsWith("POST /sync ")){reply(s,404,"{}");return;}
      String secret="",lengthRaw="";
      for(String line:lines){
        int colon=line.indexOf(':');if(colon<0)continue;
        String key=line.substring(0,colon).trim().toLowerCase(Locale.ROOT);
        if(key.equals("x-family-key"))secret=line.substring(colon+1).trim();
        if(key.equals("content-length"))lengthRaw=line.substring(colon+1).trim();
      }
      String key=getSharedPreferences("neurosync_sync",0).getString("secret","");
      if(key.length()<32 || !java.security.MessageDigest.isEqual(key.getBytes(StandardCharsets.UTF_8),secret.getBytes(StandardCharsets.UTF_8))){
        reply(s,401,"{\"error\":\"Código de familia incorrecto\"}");return;
      }
      int length;try{length=Integer.parseInt(lengthRaw);}catch(Exception e){length=-1;}
      if(length<0||length>500000){reply(s,413,"{\"error\":\"Contenido demasiado grande\"}");return;}
      byte[] content=in.readNBytes(length);
      if(content.length!=length){reply(s,400,"{}");return;}
      JSONObject incoming=new JSONObject(new String(content,StandardCharsets.UTF_8));
      JSONObject result=SyncStore.merge(this,incoming);
      reply(s,200,result.toString());
    }catch(Exception e){android.util.Log.w("NeuroSync","Petición rechazada: "+e.getClass().getSimpleName());}
  }
  private void reply(Socket s,int status,String content)throws IOException{
    byte[] bytes=content.getBytes(StandardCharsets.UTF_8);
    String name=status==200?"OK":status==401?"Unauthorized":"Bad Request";
    String headers="HTTP/1.1 "+status+" "+name+"\r\nContent-Type: application/json; charset=utf-8\r\nCache-Control: no-store\r\nConnection: close\r\nContent-Length: "+bytes.length+"\r\n\r\n";
    OutputStream out=s.getOutputStream();out.write(headers.getBytes(StandardCharsets.US_ASCII));out.write(bytes);out.flush();
  }
  @Override public void onDestroy(){running=false;try{if(server!=null)server.close();}catch(Exception ignored){}workers.shutdownNow();super.onDestroy();}
  @Override public IBinder onBind(Intent i){return null;}
}
