package local.z6panel;

import android.content.*;
import android.hardware.input.InputManager;
import android.os.*;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;

/** Event-driven fallback for function keys consumed by vendor policy before accessibility. */
final class TvRemoteKeys {
 private static TvRemoteKeys instance;
 private static final Object RPC=new Object();
 private final Context context;private final Handler ui=new Handler(Looper.getMainLooper());
 private final ThreadPoolExecutor worker=new ThreadPoolExecutor(1,1,20,TimeUnit.SECONDS,new ArrayBlockingQueue<Runnable>(1));
 private final String runtimeToken=UUID.randomUUID().toString();
 private String learnToken="",applied="",status="Sin detección ampliada";
 private volatile long revision;private volatile boolean blocked;
 private boolean active,busy,dirty,touched,listening,forcePending;private int transientRetries;
 private final Runnable retry=()->sync(false);
 private final Runnable deviceChanged=()->sync(true);
 private final InputManager.InputDeviceListener devices=new InputManager.InputDeviceListener(){public void onInputDeviceAdded(int id){changed();}public void onInputDeviceRemoved(int id){changed();}public void onInputDeviceChanged(int id){changed();}private void changed(){ui.removeCallbacks(deviceChanged);ui.postDelayed(deviceChanged,300);}};
 private TvRemoteKeys(Context c){context=c.getApplicationContext();worker.allowCoreThreadTimeOut(true);}
 static TvRemoteKeys get(Context c){if(instance==null)instance=new TvRemoteKeys(c);return instance;}
 static void resume(Context c){TvRemoteKeys m=get(c);m.active=true;m.blocked=false;m.configuration();}
 static void suspend(){if(instance!=null){instance.active=false;instance.blocked=true;instance.learnToken="";instance.configuration();}}
 static void changed(Context c){get(c).configuration();}
 static void ready(Context c){new Handler(Looper.getMainLooper()).post(()->{TvRemoteKeys m=get(c);m.applied="";m.configuration();});}
 static void lost(){new Handler(Looper.getMainLooper()).post(()->{TvRemoteKeys m=instance;if(m!=null){m.revision++;m.applied="";m.status="Control auxiliar desconectado";m.ui.removeCallbacks(m.retry);TvRecoveryActivity.refresh();}});}
 static String begin(Context c){TvRemoteKeys m=get(c);m.blocked=false;m.learnToken=UUID.randomUUID().toString();m.revision++;m.sync(false);return m.learnToken;}
 static void end(Context c){TvRemoteKeys m=get(c);m.learnToken="";m.revision++;m.sync(false);}
 static void disable(Context c){TvRemoteKeys m=get(c);m.blocked=true;m.learnToken="";m.revision++;m.configuration();}
 private void configuration(){
  transientRetries=0;ui.removeCallbacks(retry);
  TvRecoveryConfig.Settings s=TvRecoveryConfig.get(context);if(active&&s.button&&s.hardware())blocked=false;
  boolean needed=active&&s.button&&s.hardware();InputManager input=(InputManager)context.getSystemService(Context.INPUT_SERVICE);
  if(needed&&!listening){input.registerInputDeviceListener(devices,ui);listening=true;}else if(!needed&&listening){input.unregisterInputDeviceListener(devices);listening=false;ui.removeCallbacks(deviceChanged);}
  revision++;sync(false);
 }
 private JSONObject desired()throws JSONException{
  if(!blocked&&!learnToken.isEmpty())return new JSONObject().put("mode","learn").put("token",learnToken);
  TvRecoveryConfig.Settings s=TvRecoveryConfig.get(context);
  if(!blocked&&active&&s.button&&s.hardware()&&!TvRecoveryActivity.keySetupActive())return new JSONObject().put("mode","bind").put("token",runtimeToken).put("device",s.remoteDevice).put("code",s.remoteCode);
  return new JSONObject().put("mode","stop");
 }
 private static String encode(JSONObject value){return "REMOTE:"+Base64.encodeToString(value.toString().getBytes(StandardCharsets.UTF_8),Base64.NO_WRAP);}
 private void sync(boolean force){
  final JSONObject request;try{request=desired();}catch(JSONException e){return;}String canonical=request.toString();
  if(force)applied="";if(busy){dirty=true;forcePending|=force;return;}if(canonical.equals(applied)||request.optString("mode").equals("stop")&&!touched)return;
  final long epoch=revision;final String sent=canonical,token=request.optString("token");if(force)try{request.put("refresh",true);}catch(JSONException ignored){}
  busy=true;touched=true;
  worker.execute(()->{String reply; synchronized(RPC){reply=epoch!=revision||blocked&&!request.optString("mode").equals("stop")?"CANCELLED":SystemBridge.call(encode(request));}final String outcome=reply;
   ui.post(()->{busy=false;applied=epoch==revision&&outcome.equals("OK")?sent:"";if(epoch==revision){status=outcome;if(!token.isEmpty()&&token.equals(learnToken))TvRecoveryActivity.remoteReady(token,"OK".equals(outcome));}TvRecoveryActivity.refresh();if(dirty||epoch!=revision){boolean forceAgain=forcePending;forcePending=false;dirty=false;sync(forceAgain);}else if(outcome.contains("BUSY")&&!blocked&&++transientRetries<=3){ui.removeCallbacks(retry);ui.postDelayed(retry,750L*transientRetries);}});
  });
 }
 static void receive(String payload){new Handler(Looper.getMainLooper()).post(()->{if(instance!=null)instance.message(payload);});}
 private void message(String payload){
  try{JSONObject o=new JSONObject(payload);String type=o.optString("type"),token=o.optString("token"),device=o.optString("device");int code=o.optInt("code");
   if(type.equals("learn")&&token.equals(learnToken)&&!token.isEmpty()&&device.length()>0&&device.length()<=256&&local.z6bridge.RemoteKeyFormat.allowed(code)){
    ui.postDelayed(()->{if(token.equals(learnToken))TvRecoveryActivity.remoteDetected(token,device,code);},150);return;
   }
   if(type.equals("key")&&token.equals(runtimeToken)){TvRecoveryConfig.Settings s=TvRecoveryConfig.get(context);if(active&&s.button&&s.hardware()&&s.remoteCode==code&&s.remoteDevice.equals(device)&&!TvRecoveryActivity.keySetupActive()&&TvRecovery.instance!=null)TvRecovery.instance.hardwareShortcut();}
   if((type.equals("expired")||type.equals("unavailable"))&&token.equals(learnToken))TvRecoveryActivity.remoteReady(token,false);
   if(type.equals("unavailable")&&token.equals(runtimeToken))status="Control no disponible; vuelve a conectarlo";
  }catch(JSONException ignored){}
 }
 static String stopAndWait(){
  TvRemoteKeys m=instance;if(m!=null)m.blocked=true;
  synchronized(RPC){if(SystemBridge.remote==null||!SystemBridge.remote.isBinderAlive())return "OK";String version=SystemBridge.read("VERSION",1200);if(version.equals("0.11.0")||version.equals("0.18.0-tv1"))return "OK";try{return SystemBridge.call(encode(new JSONObject().put("mode","stop")));}catch(JSONException e){return "ERROR";}}
 }
 static JSONObject inspect()throws JSONException{TvRemoteKeys m=instance;return m==null?new JSONObject():new JSONObject().put("status",helperAlive()?m.status:"Control auxiliar desconectado").put("helperConnected",helperAlive()).put("learning",!m.learnToken.isEmpty()).put("deviceListener",m.listening).put("syncPending",m.busy);}
 private static boolean helperAlive(){return SystemBridge.remote!=null&&SystemBridge.remote.isBinderAlive();}
 static String availability(){if(!helperAlive())return "Control auxiliar desconectado";TvRemoteKeys m=instance;if(m==null||m.busy)return "Preparando botón";if(m.status.equals("OK"))return "Botón disponible";if(m.status.equals("NO_DEVICE"))return "Conecta el control asignado";if(m.status.equals("AMBIGUOUS_DEVICE"))return "Hay controles iguales; vuelve a asignar el botón";return "Botón pendiente; revisa el auxiliar o elige otra tecla";}
 static void fixture(String type,String device,int code){if(!("ranchu".equals(Build.HARDWARE)||"goldfish".equals(Build.HARDWARE)))throw new SecurityException("Emulator only");TvRemoteKeys m=instance;if(m==null)throw new IllegalStateException();try{m.message(new JSONObject().put("type",type).put("device",device).put("code",code).put("token",type.equals("learn")?m.learnToken:m.runtimeToken).toString());}catch(JSONException e){throw new IllegalArgumentException(e);}}
}
