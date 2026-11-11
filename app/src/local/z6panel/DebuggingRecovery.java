package local.z6panel;
import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;

/** Completes a legacy interrupted restoration; new recovery never cycles debugging. */
final class DebuggingRecovery {
 static synchronized void restorePending(Context c){
  SharedPreferences state=c.getSharedPreferences("z6panel_adb_recovery",0);
  if(state.getBoolean("restore_pending",false)&&Settings.Global.putInt(c.getContentResolver(),Settings.Global.ADB_ENABLED,1))state.edit().remove("restore_pending").commit();
 }
}
