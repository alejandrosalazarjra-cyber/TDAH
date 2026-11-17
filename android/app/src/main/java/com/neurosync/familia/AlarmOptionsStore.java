package com.neurosync.familia;

import android.content.Context;
import android.content.SharedPreferences;

/** Opciones locales por alarma. No requiere accesos invasivos ni permisos extra. */
final class AlarmOptionsStore {
 private static final String NAME="ns_alarm_options_v1";
 private AlarmOptionsStore(){}
 private static SharedPreferences pref(Context c){return c.getSharedPreferences(NAME,0);}
 static boolean configure(Context c,String id,int minutes,int max){
  if(id==null||id.length()<2||id.length()>100)return false;
  if(minutes!=1&&minutes!=3&&minutes!=5&&minutes!=10&&minutes!=15)return false;
  if(max<0||max>5)return false;
  return pref(c).edit().putInt(id+"_minutes",minutes).putInt(id+"_max",max).commit();
 }
 static boolean setAudio(Context c,String id,int volumePercent,int fadeSeconds){
  if(id==null||id.length()<2||id.length()>100)return false;
  if(volumePercent<20||volumePercent>100)return false;
  if(fadeSeconds!=0&&fadeSeconds!=30&&fadeSeconds!=60)return false;
  return pref(c).edit().putInt(id+"_volume",volumePercent)
    .putInt(id+"_fade",fadeSeconds).commit();
 }
 static int volume(Context c,String id){return pref(c).getInt(id+"_volume",100);}
 static int fadeSeconds(Context c,String id){return pref(c).getInt(id+"_fade",0);}
 static int minutes(Context c,String id){return pref(c).getInt(id+"_minutes",5);}
 static int max(Context c,String id){return pref(c).getInt(id+"_max",3);}
 static int used(Context c,String id){return pref(c).getInt(id+"_used",0);}
 static boolean canSnooze(Context c,String id){return used(c,id)<max(c,id);}
 static void consumed(Context c,String id){pref(c).edit().putInt(id+"_used",used(c,id)+1).apply();}
 static void reset(Context c,String id){pref(c).edit().remove(id+"_used").apply();}
 static void delete(Context c,String id){
  pref(c).edit().remove(id+"_minutes").remove(id+"_max").remove(id+"_used")
   .remove(id+"_volume").remove(id+"_fade").apply();
 }
}
