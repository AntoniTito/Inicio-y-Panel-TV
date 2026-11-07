package local.z6panel;
import android.content.*;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;

/** Coalesced notifications only while a relevant screen is visible; no polling. */
final class WallpaperMediaObserver {
 private final Context c;private final Handler ui=new Handler(Looper.getMainLooper());private final Runnable change;private boolean attached;
 private final ContentObserver observer=new ContentObserver(ui){public void onChange(boolean self){ui.removeCallbacks(change);if(attached)ui.postDelayed(change,800);}};
 WallpaperMediaObserver(Context c,Runnable action){this.c=c;change=action;}
 void start(){if(attached)return;try{attached=true;c.getContentResolver().registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,true,observer);}catch(Exception e){attached=false;}}
 void stop(){attached=false;ui.removeCallbacksAndMessages(null);try{c.getContentResolver().unregisterContentObserver(observer);}catch(Exception ignored){}}
}
