package com.neurosync.familia;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.LinkedHashMap;
import java.util.Map;

/** Réplica compartida de tareas y listas. Borrados mediante lápidas (tombstones).
 * Resolución determinista: gana updatedAt más reciente. No se sincronizan recordatorios privados.
 */
public final class SyncStore {
  private static final Object LOCK = new Object();
  public static final String KEY = "central_snapshot";
  private SyncStore() {}
  public static JSONObject merge(Context ctx, JSONObject incoming) throws Exception {
    synchronized (LOCK) {
      String stored = ctx.getSharedPreferences("neurosync_sync",0).getString(KEY,"{\"tasks\":[],\"packs\":[]}");
      JSONObject result = new JSONObject(stored);
      for (String kind : new String[]{"tasks","packs"}) {
        Map<String,JSONObject> rows = new LinkedHashMap<>();
        insert(rows,result.optJSONArray(kind));
        insert(rows,incoming.optJSONArray(kind));
        JSONArray values = new JSONArray();
        for (JSONObject value : rows.values()) values.put(value);
        result.put(kind,values);
      }
      ctx.getSharedPreferences("neurosync_sync",0).edit().putString(KEY,result.toString()).apply();
      return result;
    }
  }
  private static void insert(Map<String,JSONObject> rows, JSONArray list) {
    if(list==null)return;
    for(int i=0;i<Math.min(list.length(),3000);i++){
      JSONObject entry=list.optJSONObject(i);
      if(entry==null)continue;
      String id=entry.optString("id","");
      if(id.length()<1||id.length()>100)continue;
      JSONObject previous=rows.get(id);
      long time=entry.optLong("updatedAt",1);
      if(previous==null || time>previous.optLong("updatedAt",1))rows.put(id,entry);
    }
  }
}
