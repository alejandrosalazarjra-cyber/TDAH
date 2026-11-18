package com.neurosync.familia;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.LinkedHashMap;
import java.util.Map;

/** Réplica central. El servidor valida cada objeto recibido y filtra por usuario al responder. */
public final class SyncStore {
  private static final Object LOCK=new Object();
  private static final String KEY="family_v2";
  private SyncStore(){}
  private static JSONObject read(Context c)throws Exception{
    String raw=c.getSharedPreferences("ns_family",0).getString(KEY,"{\"tasks\":[],\"packs\":[],\"alarms\":[]}");
    return new JSONObject(raw);
  }
  private static Map<String,JSONObject> map(JSONArray input){
    Map<String,JSONObject> out=new LinkedHashMap<>();
    if(input==null)return out;
    for(int i=0;i<Math.min(3000,input.length());i++){
      JSONObject row=input.optJSONObject(i);
      if(row==null)continue;
      String id=row.optString("id");
      if(id.length()>0&&id.length()<100)out.put(id,row);
    }return out;
  }
  private static boolean visible(JSONObject row,JSONObject actor){
    String owner=row.optString("owner","");
    return "admin".equals(actor.optString("role")) || actor.optString("username").equals(owner) || "todos".equals(owner);
  }
  /** Entrega una copia a un destinatario, con identidad del remitente verificada.
   * La clave idempotente evita duplicados si la red reintenta la misma solicitud.
   */
  public static JSONObject sendTask(Context c,JSONObject request,JSONObject actor){
    if(actor==null)return AccountStore.fail("Inicia sesión");
    String from=actor.optString("username",""),to=request.optString("recipient","").trim().toLowerCase(java.util.Locale.ROOT);
    String key=request.optString("requestId","");
    JSONObject input=request.optJSONObject("task");
    if(!to.matches("[a-z0-9_\\-]{3,30}")||to.equals(from)||
       !AccountStore.isActive(c,to))return AccountStore.fail("Destinatario no disponible");
    if(!key.matches("[a-zA-Z0-9_\\-]{8,100}")||input==null)
      return AccountStore.fail("Solicitud de tarea inválida");
    String title=input.optString("title","").trim();
    if(title.isEmpty()||title.length()>100)return AccountStore.fail("Título inválido");
    try{
      synchronized(LOCK){
        JSONObject database=read(c);
        Map<String,JSONObject> tasks=map(database.optJSONArray("tasks"));
        String basis="neurosync-task-v1:"+from+":"+to+":"+key;
        String id="sent_"+java.util.UUID.nameUUIDFromBytes(basis.getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();
        if(tasks.containsKey(id)){
          JSONObject existing=tasks.get(id);
          if(to.equals(existing.optString("owner"))&&from.equals(existing.optString("sentBy")))
            return new JSONObject().put("ok",true).put("id",id).put("duplicate",true);
          return AccountStore.fail("Conflicto de identificador");
        }
        if(tasks.size()>=2900)return AccountStore.fail("Límite de tareas alcanzado");
        JSONArray steps=new JSONArray(),source=input.optJSONArray("steps");
        if(source!=null){
          for(int i=0;i<Math.min(20,source.length());i++){
            String step=source.optString(i,"").trim();
            if(!step.isEmpty())steps.put(step.substring(0,Math.min(140,step.length())));
          }
        }
        String repeat=input.optString("repeat","none");
        if(!("none".equals(repeat)||"daily".equals(repeat)||"weekly".equals(repeat)))repeat="none";
        String date=input.optString("date","");
        if(!date.matches("\\d{4}-\\d{2}-\\d{2}"))date=
          new java.text.SimpleDateFormat("yyyy-MM-dd",java.util.Locale.ROOT).format(new java.util.Date());
        JSONObject task=new JSONObject().put("id",id).put("owner",to)
          .put("sentBy",from).put("title",title).put("date",date)
          .put("repeat",repeat).put("category",input.optString("category","Personal").substring(0,
             Math.min(50,input.optString("category","Personal").length())))
          .put("steps",steps).put("doneDates",new JSONArray()).put("stepDates",new JSONObject())
          .put("priority","normal").put("updatedAt",System.currentTimeMillis());
        tasks.put(id,task);
        JSONArray all=new JSONArray();for(JSONObject row:tasks.values())all.put(row);
        database.put("tasks",all);
        if(!c.getSharedPreferences("ns_family",0).edit().putString(KEY,database.toString()).commit())
          return AccountStore.fail("No se pudo entregar la tarea. Vuelve a intentarlo.");
        return new JSONObject().put("ok",true).put("id",id).put("recipient",to);
      }
    }catch(Exception e){return AccountStore.fail("No se pudo enviar la tarea");}
  }
  public static JSONObject merge(Context c,JSONObject incoming,JSONObject actor){
    if(actor==null)return AccountStore.fail("Inicia sesión");
    try{
      synchronized(LOCK){
        JSONObject database=read(c),reply=new JSONObject().put("ok",true),view=new JSONObject();
        for(String collection:new String[]{"tasks","packs","alarms"}){
          Map<String,JSONObject> rows=map(database.optJSONArray(collection));
          JSONArray posted=incoming.optJSONArray(collection);
          if(posted!=null&&posted.length()>2500)return AccountStore.fail("Demasiadas actividades");
          if(posted!=null)for(int i=0;i<posted.length();i++){
            JSONObject change=posted.optJSONObject(i);
            if(change==null)continue;
            String id=change.optString("id"),owner=change.optString("owner","");
            if(id.length()<3||id.length()>99||change.toString().length()>12000)continue;
            JSONObject old=rows.get(id);
            boolean admin="admin".equals(actor.optString("role"));
            boolean own=actor.optString("username").equals(owner);
            boolean shared="todos".equals(owner);
            if(!admin&&!own&&!shared)continue;
            if(old!=null&&!visible(old,actor))continue;
            long updated=change.optLong("updatedAt",0);
            // Evitar timestamps futuros que impidan aceptar ediciones posteriores.
            // Se admiten relojes ligeramente desajustados (hasta cinco minutos).
            long now=System.currentTimeMillis();
            if(updated<1||updated>now+300000L)continue;
            if(!admin&&shared){
              // Miembros: las tareas compartidas solo permiten confirmar pasos, no cambiar datos ni borrarlas.
              if(old==null||change.optBoolean("deleted"))continue;
              JSONObject safe=new JSONObject(old.toString());
              if(change.has("doneDates"))safe.put("doneDates",change.optJSONArray("doneDates"));
              if(change.has("stepDates"))safe.put("stepDates",change.optJSONObject("stepDates"));
              safe.put("updatedAt",updated);
              change=safe;
            }
            if(old==null||updated>old.optLong("updatedAt",0))rows.put(id,change);
          }
          JSONArray all=new JSONArray(),subset=new JSONArray();
          for(JSONObject row:rows.values()){
            all.put(row);if(visible(row,actor))subset.put(row);
          }
          database.put(collection,all);view.put(collection,subset);
        }
        // Nunca comunicar éxito si la réplica familiar no quedó persistida.
        if(!c.getSharedPreferences("ns_family",0).edit()
            .putString(KEY,database.toString()).commit())
          return AccountStore.fail("No se guardaron los cambios familiares. Intenta sincronizar de nuevo.");
        return reply.put("data",view);
      }
    }catch(Exception e){return AccountStore.fail("Error sincronizando datos");}
  }
}