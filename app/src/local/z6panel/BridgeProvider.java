package local.z6panel;
import android.content.*;import android.database.Cursor;import android.net.Uri;import android.os.*;
/** Only the authenticated ADB shell can supply the privileged Binder. No data access. */
public final class BridgeProvider extends ContentProvider {
 private static final IBinder CLIENT_LIFE=new Binder();
 public boolean onCreate(){return true;}
 public Bundle call(String method,String arg,Bundle extras){
  if(Binder.getCallingUid()!=2000)throw new SecurityException("ADB shell required");
  Bundle result=new Bundle();
  try{
   if("attach".equals(method)){
    if(extras==null)throw new IllegalArgumentException();IBinder binder=extras.getBinder("bridge");if(binder==null)throw new IllegalArgumentException();
    boolean changed=SystemBridge.remote==null||!SystemBridge.remote.equals(binder);
    if(changed){SystemBridge.remote=binder;binder.linkToDeath(()->{if(SystemBridge.remote==binder){SystemBridge.remote=null;if(PanelService.active!=null)PanelService.active.bridgeLost();}},0);}
    if(changed&&PanelService.active!=null)PanelService.active.bridgeAttached();
    result.putBinder("client_life",CLIENT_LIFE);
    result.putBoolean("attached",true);return result;
   }
   if("prefs_export".equals(method)||"prefs_original".equals(method)){result.putString("base64",PanelBackup.encoded(getContext(),"prefs_original".equals(method)));return result;}
   if("wallpaper_emulator_fixture".equals(method)){result.putString("json",WallpaperEmulatorFixtures.run(getContext(),arg).toString());return result;}
   if("wallpaper_media_status".equals(method)){result.putString("json",WallpaperOnline.inspect(getContext()).toString());return result;}
   if("wallpaper_backup_manifest".equals(method)){result.putString("json",WallpaperArchive.manifest(getContext()).toString());return result;}
   if("wallpaper_backup_chunk".equals(method)){result.putString("json",WallpaperArchive.chunk(getContext(),arg).toString());return result;}
   if("wallpaper_network_status".equals(method)){result.putString("json",WallpaperOnline.lastFailure(getContext()).toString());return result;}
   if("metrics".equals(method)){org.json.JSONObject m=PerfStats.snapshot();String bridge=SystemBridge.read("STATS",1200);try{m.put("helper",new org.json.JSONObject(bridge));}catch(Exception unavailable){m.put("helperStatus",bridge);}result.putString("json",m.toString());return result;}
   if("prefs_restore".equals(method)&&PanelService.active==null){PanelBackup.restore(getContext(),arg);result.putString("status","OK");return result;}
   if("bridge".equals(method)){if(!"PING".equals(arg)&&!"VERSION".equals(arg)&&!"FOREGROUND_APP".equals(arg))throw new IllegalArgumentException("Read-only bridge query required");result.putString("status",SystemBridge.call(arg));return result;}
   PanelService service=PanelService.active;if(service==null){result.putString("error","Panel service is not active");return result;}
   java.util.concurrent.CountDownLatch latch=new java.util.concurrent.CountDownLatch(1);
   new Handler(Looper.getMainLooper()).post(()->{try{
    if("prefs_restore".equals(method)){service.prepareForRestore();PanelBackup.restore(getContext(),arg);HomeActivity.refreshPreferences();result.putString("status","OK");}
    else if("inspect".equals(method))result.putString("json",service.inspectForShell());
    else if("home_inspect".equals(method))result.putString("json",HomeActivity.inspectHome());
    else if("wallpaper_inspect".equals(method))result.putString("json",WallpaperActivity.inspect());
    else if("wallpaper_open".equals(method)){getContext().startActivity(new Intent(getContext(),WallpaperActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP));result.putString("status","OK");}
    else if("wallpaper_action".equals(method))result.putString("status",WallpaperActivity.shellAction(arg));
    else if("wallpaper_next".equals(method)){HomeActivity.advanceWallpaperForShell();result.putString("status","OK");}
    else if("open".equals(method)){service.openForShell(arg);result.putString("status","OK");}
    else if("action".equals(method))result.putString("status",service.actionForShell(arg));
    else if("key".equals(method)){service.keyForShell(Integer.parseInt(arg));result.putString("status","OK");}
    else if("timer_test".equals(method))result.putString("status",SleepTimer.scheduleSeconds(getContext(),Integer.parseInt(arg)));
    else if("timer_cancel".equals(method)){SleepTimer.cancel(getContext());result.putString("status","OK");}
    else throw new IllegalArgumentException("Unsupported method");
   }catch(Exception e){result.putString("error",e.toString());}finally{latch.countDown();}});
   if(!latch.await(5,java.util.concurrent.TimeUnit.SECONDS))throw new IllegalStateException("Panel response timed out");return result;
  }catch(Exception e){result.putString("error",e.toString());return result;}
 }
 public Cursor query(Uri u,String[] p,String s,String[] a,String o){return null;}
 public String getType(Uri u){return null;}
 public Uri insert(Uri u,ContentValues v){throw new UnsupportedOperationException();}
 public int delete(Uri u,String s,String[] a){throw new UnsupportedOperationException();}
 public int update(Uri u,ContentValues v,String s,String[] a){throw new UnsupportedOperationException();}
}
