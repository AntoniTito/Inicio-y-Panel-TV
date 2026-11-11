package local.z6panel;

import android.content.*;

/** Records only real Android boots. Package updates never trigger TV recovery. */
public final class TvBootReceiver extends BroadcastReceiver {
 @Override public void onReceive(Context c,Intent i){
  if(i!=null&&Intent.ACTION_BOOT_COMPLETED.equals(i.getAction()))TvRecovery.boot(c);
 }
}
