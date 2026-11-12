package local.z6panel;

import android.content.*;
import android.os.*;
import android.util.Base64;
import android.view.KeyEvent;
import org.json.*;
import java.nio.charset.StandardCharsets;

/** Called only inside BridgeProvider's UID=2000 + DUMP gate and main-thread dispatch. */
final class TvRecoveryDiagnostics {
 static void call(Context c,String method,String arg,Bundle out)throws Exception {
  TvRecovery t=TvRecovery.get(c);
  if(method.equals("tv_ui_inspect")){out.putString("json",TvRecoveryActivity.inspect().toString());return;}
  if(method.equals("tv_apps_inspect")){JSONArray apps=new JSONArray();for(AppCatalog.Entry e:new AppCatalog(c).all())apps.put(new JSONObject().put("package",e.pkg).put("name",e.title).put("openable",TvRecoveryConfig.openable(c,e.pkg)).put("restartable",TvRecoveryConfig.eligible(c,e.pkg)));out.putString("json",new JSONObject().put("apps",apps).toString());return;}
  if(method.equals("tv_destinations_inspect")){out.putString("json",new JSONObject().put("destinations",SavedDestinations.export(c)).put("pending",SavedDestinations.pending(c)==null?JSONObject.NULL:SavedDestinations.pending(c)).toString());return;}
  if(method.equals("tv_buttons_export")){out.putString("base64",Base64.encodeToString(RemoteActions.export(c).toString().getBytes(StandardCharsets.UTF_8),Base64.NO_WRAP));return;}
  if(method.equals("tv_buttons_inspect")){JSONObject o=RemoteActions.export(c);o.put("nativeRestart",NativeAppRestart.available(c)).put("nativeState",NativeAppRestart.lastState);out.putString("json",o.toString());return;}
  if(method.equals("tv_buttons_restore")){if(arg==null||arg.length()>524288)throw new IllegalArgumentException();t.cancel("");RemoteActions.restore(c,new JSONObject(new String(Base64.decode(arg,Base64.DEFAULT),StandardCharsets.UTF_8)));t.changed();out.putString("status","OK");return;}
  if(method.equals("tv_native_prepared")){if(arg==null||arg.length()>4096)throw new IllegalArgumentException();JSONObject layout=new JSONObject(new String(Base64.decode(arg,Base64.DEFAULT),StandardCharsets.UTF_8));if(!layout.optString("status").equals("OK")||!layout.optString("current").equals("local.z6panel/local.z6panel.RemoteLayoutReceiver/z6remote"))throw new IllegalArgumentException();RemoteActions.nativePrepared(c,layout);out.putString("status","OK");return;}
  if(method.equals("tv_inspect")){JSONObject state=t.inspect();if(TvNavigationService.active!=null)state.put("navigation",TvNavigationService.active.inspect());out.putString("json",state.toString());return;}
  if(method.equals("tv_open")){c.startActivity(TvRecoveryActivity.openIntent(c));out.putString("status","OK");return;}
  if(method.equals("tv_export")){out.putString("base64",Base64.encodeToString(TvRecoveryConfig.get(c).json().toString().getBytes(StandardCharsets.UTF_8),Base64.NO_WRAP));return;}
  if(method.equals("tv_restore")){
   if(arg==null||arg.length()>65536)throw new IllegalArgumentException("Invalid TV backup");
   TvRecoveryConfig.Settings s=TvRecoveryConfig.parse(new JSONObject(new String(Base64.decode(arg,Base64.DEFAULT),StandardCharsets.UTF_8)));
   if(!s.pkg.isEmpty()&&!TvRecoveryConfig.sameApp(c,s))throw new IllegalArgumentException("TV backup app is unavailable or signature changed");
   t.cancel("");TvRecoveryConfig.save(c,s);t.restored();out.putString("status","OK");return;
  }
  if(method.equals("tv_cancel")){t.disable();out.putString("status","OK");return;}
  if(!method.equals("tv_control")||arg==null||arg.length()>65536)throw new IllegalArgumentException("Unsupported TV diagnostic");
  JSONObject o=new JSONObject(new String(Base64.decode(arg,Base64.DEFAULT),StandardCharsets.UTF_8));String op=o.getString("op");
  switch(op){
   case "ui_choose":if(!TvRecoveryActivity.shellChoose(o.getString("id")))throw new IllegalArgumentException("UI control unavailable");break;
   case "button_run":RemoteActions.Action action=RemoteActions.find(c,o.getString("id"));if(action==null)throw new IllegalArgumentException();t.execute(action);break;
   case "button_label":RemoteActions.Action named=RemoteActions.find(c,o.getString("id"));if(named==null)throw new IllegalArgumentException();named=named.copy();named.label=o.getString("label").trim();RemoteActions.save(c,named);TvRecoveryActivity.refresh();break;
   case "destination_use":t.useDestination(o.getString("id"),o.optString("button"));break;
   case "pending_retry":t.retryPending();break;
   case "pending_save":t.savePending();break;
   case "pending_discard":SavedDestinations.discard(c);break;
   case "button_teach":RemoteActions.Action teach=RemoteActions.find(c,o.getString("id"));if(teach==null)throw new IllegalArgumentException();t.requestProfile(teach,true,false);break;
   case "record_done":if(TvNavigationService.active!=null)TvNavigationService.active.reviewRecording();break;
   case "select":t.choose(o.getString("package"));break;
   case "teach":t.request("configuración",true,null);break;
   case "test":TvRecoveryConfig.Settings trial=TvRecoveryConfig.get(c);if(!trial.verified)throw new IllegalArgumentException("No verified route");t.request("prueba",false,trial);break;
   case "run":t.request("manual",false,null);break;
   case "activate":t.activate();break;
   case "toggle":t.toggle(o.getString("kind"));break;
   case "assign":t.assignKey(o.getInt("key"),o.optInt("scan"),o.optString("name"));break;
   case "learn_key":case "prepare_key":String label=o.optString("name","Otro botón");if(label.length()>40)throw new IllegalArgumentException();c.startActivity(TvRecoveryActivity.openIntent(c).putExtra(op.equals("prepare_key")?"prepare_remote":"learn_remote",label));break;
   case "cancel":t.cancel("Recuperación cancelada");break;
   case "remote_fixture":TvRemoteKeys.fixture(o.getString("type"),o.optString("device","0003|ffff|0001|Z6 test remote"),o.optInt("code",88));break;
   case "choose":if(TvNavigationService.active==null||!TvNavigationService.active.shellChoose(o.getString("id")))throw new IllegalArgumentException("Guide control unavailable");break;
   case "key":int key=o.getInt("key"),repeat=o.optInt("repeat"),scan=o.optInt("scan");if(repeat<0||repeat>30||key<0||key>4095||scan<0||scan>65535)throw new IllegalArgumentException("Invalid key");long now=SystemClock.uptimeMillis();TvRecovery.handleKey(new KeyEvent(now,now,o.optBoolean("up")?KeyEvent.ACTION_UP:KeyEvent.ACTION_DOWN,key,repeat,0,0,scan));break;
   case "event":t.testEvent(o.getString("event"));break;
   default:throw new IllegalArgumentException("Unknown TV operation");
  }
  out.putString("status","OK");
 }
}
