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
            if(!"admin".equals(actor.optString("role")) && !actor.optString("username").equals(owner))continue;
            JSONObject old=rows.get(id);
            if(old!=null&&!visible(old,actor))continue;
            long updated=change.optLong("updatedAt",0);
            if(updated<1)continue;
            if(old==null||updated>old.optLong("updatedAt",0))rows.put(id,change);
          }
          JSONArray all=new JSONArray(),subset=new JSONArray();
          for(JSONObject row:rows.values()){
            all.put(row);if(visible(row,actor))subset.put(row);
          }
          database.put(collection,all);view.put(collection,subset);
        }
        c.getSharedPreferences("ns_family",0).edit().putString(KEY,database.toString()).commit();
        return reply.put("data",view);
      }
    }catch(Exception e){return AccountStore.fail("Error sincronizando datos");}
  }
}