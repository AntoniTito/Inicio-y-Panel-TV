package local.z6panel;

import android.content.*;
import android.content.pm.*;
import android.os.Build;
import android.view.KeyEvent;
import java.security.MessageDigest;
import java.util.*;
import org.json.*;

/** Device-local choices. Deliberately separate from transferable panel preferences. */
final class TvRecoveryConfig {
 static final String STORE="tv_recovery_v1";
 static final class Settings {
  String pkg="",name="",signature="",channel="",keyName="",remoteDevice="";
  boolean boot,wake,button,verified;
  int key,scan,remoteCode;
  boolean hardware(){return remoteCode!=0;}
  boolean hasBinding(){return hardware()||key!=0||scan!=0;}
  boolean matches(KeyEvent e){return !hardware()&&hasBinding()&&key==e.getKeyCode()&&(scan==0||scan==e.getScanCode());}
  JSONArray steps=new JSONArray();JSONObject endpoint=new JSONObject();
  JSONObject json() throws JSONException {
   return new JSONObject().put("schema",1).put("package",pkg).put("name",name)
    .put("signature",signature).put("channel",channel).put("boot",boot).put("wake",wake)
    .put("button",button).put("key",key).put("scan",scan).put("keyName",keyName).put("remoteDevice",remoteDevice).put("remoteCode",remoteCode).put("verified",verified).put("steps",steps).put("endpoint",endpoint);
  }
  Settings copy(){try{return parse(new JSONObject(json().toString()));}catch(JSONException e){throw new IllegalStateException(e);}}
 }
 private static volatile Settings cached;
 static synchronized Settings get(Context c){
  if(cached==null)try{cached=parse(new JSONObject(c.getSharedPreferences(STORE,0).getString("settings","{}")));}
  catch(Exception invalid){cached=new Settings();}
  return cached;
 }
 static Settings parse(JSONObject o)throws JSONException {
  if(o.optInt("schema",1)!=1)throw new JSONException("Unsupported TV settings");
  Settings s=new Settings();s.pkg=o.optString("package","");s.name=o.optString("name","");
  s.signature=o.optString("signature","");s.channel=o.optString("channel","");
  s.boot=o.optBoolean("boot");s.wake=o.optBoolean("wake");s.button=o.optBoolean("button");
  s.verified=o.optBoolean("verified");s.key=o.optInt("key");s.scan=o.optInt("scan");s.keyName=o.optString("keyName");
  s.remoteDevice=o.optString("remoteDevice");s.remoteCode=o.optInt("remoteCode");
  s.steps=o.optJSONArray("steps");if(s.steps==null)s.steps=new JSONArray();
  s.endpoint=o.optJSONObject("endpoint");if(s.endpoint==null)s.endpoint=new JSONObject();
  if(s.pkg.length()>200||s.name.length()>160||s.channel.length()>160||s.signature.length()>256||s.steps.length()>24)
   throw new JSONException("TV settings too large");
  if(!s.pkg.isEmpty()&&!s.pkg.matches("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+"))throw new JSONException("Invalid app");
  for(int i=0;i<s.steps.length();i++){
   JSONObject step=s.steps.getJSONObject(i);String op=step.optString("op","click");
   if(!op.equals("click")&&!op.equals("back"))throw new JSONException("Invalid route action");
   if(op.equals("click"))TvNavigationService.Selector.parse(step.getJSONObject("target"));
  }
  if(s.scan<0||s.scan>65535||s.keyName.length()>40||!s.hardware()&&s.hasBinding()&&!allowedBinding(s.key,s.scan))throw new JSONException("Reserved remote key");
  if(s.remoteDevice.length()>256||s.hardware()&&(!local.z6bridge.RemoteKeyFormat.allowed(s.remoteCode)||s.remoteDevice.isEmpty()||s.key!=0||s.scan!=0))throw new JSONException("Invalid hardware button");
  if(s.verified&&(s.steps.length()==0||s.endpoint.optString("activity").isEmpty()&&s.endpoint.optJSONObject("marker")==null&&s.endpoint.optJSONObject("confirmedControl")==null))
   throw new JSONException("Unverifiable route");
  if(s.endpoint.optJSONObject("marker")!=null)TvNavigationService.Selector.parse(s.endpoint.getJSONObject("marker"));
  if(s.endpoint.optJSONObject("confirmedControl")!=null)TvNavigationService.Selector.parse(s.endpoint.getJSONObject("confirmedControl"));
  if(s.button&&!s.hasBinding())throw new JSONException("No remote key");
  return s;
 }
 static synchronized void save(Context c,Settings s){
  try{
   Settings checked=parse(s.json());
   if(!c.getSharedPreferences(STORE,0).edit().putString("settings",checked.json().toString()).commit())
    throw new IllegalStateException("No se pudo guardar la configuración de TV");
   cached=checked;
  }catch(JSONException e){throw new IllegalArgumentException(e);}
 }
 static boolean allowedKey(int key){
  if(key<=0||key>4095)return false;
  return !protectedKey(key);
 }
 static boolean protectedKey(int key){
  if(key>=KeyEvent.KEYCODE_DPAD_UP&&key<=KeyEvent.KEYCODE_DPAD_CENTER||key>=KeyEvent.KEYCODE_DPAD_UP_LEFT&&key<=KeyEvent.KEYCODE_DPAD_DOWN_RIGHT)return true;
  switch(key){
   case KeyEvent.KEYCODE_HOME:case KeyEvent.KEYCODE_BACK:case KeyEvent.KEYCODE_ESCAPE:
   case KeyEvent.KEYCODE_ENTER:case KeyEvent.KEYCODE_NUMPAD_ENTER:
   case KeyEvent.KEYCODE_POWER:case KeyEvent.KEYCODE_TV_POWER:
   case KeyEvent.KEYCODE_SLEEP:case KeyEvent.KEYCODE_WAKEUP:case KeyEvent.KEYCODE_SOFT_SLEEP:
   case KeyEvent.KEYCODE_VOLUME_UP:case KeyEvent.KEYCODE_VOLUME_DOWN:case KeyEvent.KEYCODE_VOLUME_MUTE:
   case KeyEvent.KEYCODE_MUTE:case KeyEvent.KEYCODE_AVR_POWER:return true;
   default:return false;
  }
 }
 static boolean allowedBinding(int key,int scan){return scan>=0&&scan<=65535&&(key==0?scan>0:allowedKey(key));}
 static Intent launchIntent(Context c,String pkg){
  PackageManager pm=c.getPackageManager();
  Intent i=pm.getLeanbackLaunchIntentForPackage(pkg);if(i==null)i=pm.getLaunchIntentForPackage(pkg);
  if(i==null||i.getComponent()==null||!pkg.equals(i.getComponent().getPackageName()))return null;
  try{ActivityInfo a=pm.getActivityInfo(i.getComponent(),0);if(!a.enabled||!a.exported||a.permission!=null)return null;}
  catch(PackageManager.NameNotFoundException e){return null;}
  return i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
 }
 static boolean eligible(Context c,String pkg){
  try{
   if(pkg==null||pkg.equals(c.getPackageName())||pkg.startsWith("com.android.")||pkg.startsWith("com.google.android.")||pkg.startsWith("android."))return false;
   PackageManager pm=c.getPackageManager();ApplicationInfo a=pm.getApplicationInfo(pkg,0);String[] peers=pm.getPackagesForUid(a.uid);
   return a.enabled&&a.uid>=10000&&(a.flags&(ApplicationInfo.FLAG_SYSTEM|ApplicationInfo.FLAG_UPDATED_SYSTEM_APP|ApplicationInfo.FLAG_PERSISTENT|ApplicationInfo.FLAG_SUSPENDED))==0
    &&peers!=null&&peers.length==1&&pm.queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).setPackage(pkg),0).isEmpty()
    &&launchIntent(c,pkg)!=null;
  }catch(Exception unavailable){return false;}
 }
 static String signature(Context c,String pkg){
  try{
   PackageInfo info=c.getPackageManager().getPackageInfo(pkg,Build.VERSION.SDK_INT>=28?PackageManager.GET_SIGNING_CERTIFICATES:PackageManager.GET_SIGNATURES);
   Signature[] signers=Build.VERSION.SDK_INT>=28?info.signingInfo.getApkContentsSigners():info.signatures;
   ArrayList<String> hashes=new ArrayList<>();for(Signature signer:signers){byte[] digest=MessageDigest.getInstance("SHA-256").digest(signer.toByteArray());StringBuilder b=new StringBuilder();for(byte v:digest)b.append(String.format(Locale.ROOT,"%02x",v&255));hashes.add(b.toString());}
   Collections.sort(hashes);return String.join(":",hashes);
  }catch(Exception unavailable){return "";}
 }
 static boolean sameApp(Context c,Settings s){return eligible(c,s.pkg)&&!s.signature.isEmpty()&&s.signature.equals(signature(c,s.pkg));}
}
