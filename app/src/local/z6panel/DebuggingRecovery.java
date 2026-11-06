package local.z6panel;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.PowerManager;
import android.provider.Settings;

/** Mirrors this firmware's developer-setting switch from the app process, not adbd's group. */
final class DebuggingRecovery {
 private static SharedPreferences state(Context c){return c.getSharedPreferences("z6panel_adb_recovery",0);}
 static synchronized void restorePending(Context c){
  if(!state(c).getBoolean("restore_pending",false))return;
  if(Settings.Global.putInt(c.getContentResolver(),Settings.Global.ADB_ENABLED,1))state(c).edit().remove("restore_pending").commit();
 }
 static synchronized String cycle(Context c,boolean manual){
  if(c.checkSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS)!=PackageManager.PERMISSION_GRANTED)return "Falta permiso para recuperar la depuración";
  if(Thread.currentThread().isInterrupted())return "Recuperación de depuración interrumpida";
  boolean bootRecovery=android.os.SystemClock.elapsedRealtime()<120000&&c.getSharedPreferences("panel",0).getBoolean("local_adb_enabled",false);
  if(!manual&&!bootRecovery&&Settings.Global.getInt(c.getContentResolver(),Settings.Global.ADB_ENABLED,0)!=1)return "Depuración desactivada en Android; actívala para recuperar el control";
  SharedPreferences s=state(c);int boot=Settings.Global.getInt(c.getContentResolver(),Settings.Global.BOOT_COUNT,-1);
  if(boot<0)return "No se pudo verificar el arranque para recuperar la depuración";
  if(!manual&&s.getInt("attempt_boot",-1)==boot)return "Depuración local pendiente; usa Recuperar control";
  if(!s.edit().putInt("attempt_boot",boot).putBoolean("restore_pending",true).commit())return "No se pudo guardar la recuperación de depuración";
  PowerManager.WakeLock wake=null;boolean restored=false,disabled=false,separated=false;String result="No se pudo recuperar la depuración";
  try{
   PowerManager power=(PowerManager)c.getSystemService(Context.POWER_SERVICE);wake=power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"Z6Panel:AdbRecovery");wake.acquire(8000);
   if(!Settings.Global.putInt(c.getContentResolver(),Settings.Global.ADB_ENABLED,0))return "Android no aceptó reiniciar la depuración";
   disabled=true;
   // Let the observer handle OFF separately; consecutive writes may be coalesced.
   try{Thread.sleep(1800);separated=true;}catch(InterruptedException interrupted){Thread.currentThread().interrupt();result="Recuperación de depuración interrumpida";}
  }catch(RuntimeException e){result="Recuperación de depuración: "+e.getClass().getSimpleName();}
  finally{
   try{restored=Settings.Global.putInt(c.getContentResolver(),Settings.Global.ADB_ENABLED,1);if(restored)s.edit().remove("restore_pending").commit();}catch(RuntimeException ignored){}
   if(wake!=null&&wake.isHeld())wake.release();
  }
  if(restored&&disabled&&separated){android.util.Log.i("Z6LocalAdb","Restored developer ADB setting from app process");return "OK";}
  return result;
 }
}
