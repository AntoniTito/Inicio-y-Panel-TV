package local.z6panel;

import android.app.*;
import android.content.*;
import android.os.*;
import java.lang.reflect.Method;

/** One explicit operation, not usage-history monitoring. Must confirm the old UID is gone. */
final class NativeAppRestart {
 static volatile String lastState="";
 static boolean available(Context c){try{AppOpsManager ops=(AppOpsManager)c.getSystemService(Context.APP_OPS_SERVICE);return Build.VERSION.SDK_INT<34&&ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,android.os.Process.myUid(),c.getPackageName())==AppOpsManager.MODE_ALLOWED&&ActivityManager.class.getMethod("getUidImportance",int.class)!=null;}catch(Exception e){return false;}}
 static String close(Context c,TvRecoveryConfig.Settings s,java.util.function.BooleanSupplier valid){
  if(!available(c))return "NATIVE_UNAVAILABLE";
  try{
   if(!valid.getAsBoolean()||!TvRecoveryConfig.sameApp(c,s))return "CANCELLED";
   ActivityManager am=(ActivityManager)c.getSystemService(Context.ACTIVITY_SERVICE);Method importance=ActivityManager.class.getMethod("getUidImportance",int.class);
   int uid=c.getPackageManager().getApplicationInfo(s.pkg,0).uid;
   long until=SystemClock.elapsedRealtime()+4000,nextAttempt=0;int attempts=0;
   while(valid.getAsBoolean()&&SystemClock.elapsedRealtime()<until){int level=(Integer)importance.invoke(am,uid);lastState="importance="+level+", attempts="+attempts;if(level>=1000)return "OK";
    // Wait for our own activity to cover the player before requesting a background kill.
    if(level>ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND&&attempts<4&&SystemClock.elapsedRealtime()>=nextAttempt){am.killBackgroundProcesses(s.pkg);attempts++;nextAttempt=SystemClock.elapsedRealtime()+400;}
    Thread.sleep(100);
   }
   return valid.getAsBoolean()?"NATIVE_NOT_CLOSED":"CANCELLED";
  }catch(InterruptedException e){Thread.currentThread().interrupt();return "CANCELLED";}catch(Exception e){return "NATIVE_UNAVAILABLE";}
 }
}
