package com.neurosync.familia;

import android.app.*;
import android.content.*;
import android.os.Build;
import android.os.IBinder;
import org.json.JSONObject;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.*;

/** Servidor privado para tres teléfonos. No se debe publicar el puerto en Internet.
 * Diseñado para usarse mediante una VPN privada como Tailscale.
 */
public final class SyncService extends Service {
  public static final int PORT=8765;
  private volatile boolean running=false;
  private ServerSocket server;
  private final ExecutorService workers=Executors.newFixedThreadPool(4);
  @Override public void onCreate(){
    super.onCreate();
    NotificationManager nm=getSystemService(NotificationManager.class);
    nm.createNotificationChannel(new NotificationChannel("family_service","Servidor familiar",NotificationManager.IMPORTANCE_LOW));
    Notification note=new Notification.Builder(this,"family_service")
      .setContentTitle("NeuroSync · Sincronización familiar")
      .setContentText("Tu celular está compartiendo tareas en la red privada")
      .setSmallIcon(android.R.drawable.stat_notify_sync).setOngoing(true).build();
    if(Build.VERSION.SDK_INT>=34)startForeground(101,note,android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
    else startForeground(101,note);
    running=true;
    new Thread(this::serve,"neurosync-server").start();
  }
  @Override public int onStartCommand(Intent intent,int flags,int startId){return START_NOT_STICKY;}
  private void serve(){
    try(ServerSocket socket=new ServerSocket(PORT)){
      server=socket;socket.setReuseAddress(true);
      while(running){Socket connection=socket.accept();workers.execute(()->respond(connection));}
    }catch(Exception e){android.util.Log.w("NeuroSync","Servidor no disponible: "+e.getMessage());}
  }
  private static String readUtf8(InputStream in,int size)throws IOException{
    if(size<0||size>500000)throw new IOException("Longitud inválida");
    ByteArrayOutputStream output=new ByteArrayOutputStream();
    byte[] buffer=new byte[8192];int total=0;
    while(total<size){int n=in.read(buffer,0,Math.min(buffer.length,size-total));if(n<0)throw new IOException("Cuerpo incompleto");output.write(buffer,0,n);total+=n;}
    return new String(output.toByteArray(),StandardCharsets.UTF_8);
  }
  private void respond(Socket socket){
    try(Socket s=socket){
      s.setSoTimeout(12000);
      InputStream in=new BufferedInputStream(s.getInputStream());
      ByteArrayOutputStream header=new ByteArrayOutputStream();
      int ch,matched=0;
      while((ch=in.read())!=-1 && header.size()<8192){
        header.write(ch);
        if((matched==0||matched==2)&&ch==13)matched++;
        else if((matched==1||matched==3)&&ch==10)matched++;
        else matched=0;
        if(matched==4)break;
      }
      if(matched!=4){write(s,400,AccountStore.fail("Solicitud inválida"));return;}
      String[] lines=header.toString("UTF-8").split("\r\n");
      String first=lines[0];
      if(!first.startsWith("POST ")){write(s,405,AccountStore.fail("Solo POST"));return;}
      String route=first.split(" ")[1];
      int length=-1;String token="";
      for(String line:lines){
        int pos=line.indexOf(':');if(pos<0)continue;
        String key=line.substring(0,pos).trim().toLowerCase(Locale.ROOT);
        String value=line.substring(pos+1).trim();
        if(key.equals("content-length")){try{length=Integer.parseInt(value);}catch(Exception ignored){}}
        if(key.equals("authorization")&&value.startsWith("Bearer "))token=value.substring(7);
      }
      if(length<0||length>500000){write(s,413,AccountStore.fail("Datos demasiado grandes"));return;}
      JSONObject request=new JSONObject(readUtf8(in,length)),result;
      if(route.equals("/login")){
        // La contraseña inicial solo se usa localmente para configurar al administrador.
        if("administrador".equals(request.optString("username")) && "admin1234".equals(request.optString("password"))){
          write(s,403,AccountStore.fail("El administrador debe cambiar su contraseña en su teléfono antes de conectarse remotamente"));return;
        }
        result=AccountStore.authenticate(this,request.optString("username"),request.optString("password"));
        write(s,result.optBoolean("ok")?200:401,result);return;
      }
      JSONObject actor=AccountStore.identity(this,token);
      if(actor==null){write(s,401,AccountStore.fail("Sesión caducada: inicia sesión de nuevo"));return;}
      if(route.equals("/users"))result=AccountStore.list(this,actor);
      else if(route.equals("/create-user"))result=AccountStore.create(this,actor,request.optString("username"),request.optString("password"));
      else if(route.equals("/change-password"))result=AccountStore.change(this,actor,request.optString("current"),request.optString("next"));
      else if(route.equals("/sync"))result=SyncStore.merge(this,request,actor);
      else {write(s,404,AccountStore.fail("Ruta no encontrada"));return;}
      write(s,result.optBoolean("ok")?200:400,result);
    }catch(Exception e){android.util.Log.w("NeuroSync","Petición descartada: "+e.getClass().getSimpleName());}
  }
  private static void write(Socket s,int status,JSONObject json)throws IOException{
    byte[] body=json.toString().getBytes(StandardCharsets.UTF_8);
    String title=status==200?"OK":"Request Failed";
    String h="HTTP/1.1 "+status+" "+title+"\r\nContent-Type: application/json; charset=utf-8\r\nCache-Control: no-store\r\nConnection: close\r\nContent-Length: "+body.length+"\r\n\r\n";
    OutputStream o=s.getOutputStream();o.write(h.getBytes(StandardCharsets.US_ASCII));o.write(body);o.flush();
  }
  @Override public void onDestroy(){running=false;try{if(server!=null)server.close();}catch(Exception ignored){}workers.shutdownNow();super.onDestroy();}
  @Override public IBinder onBind(Intent intent){return null;}
}