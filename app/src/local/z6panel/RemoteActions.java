package local.z6panel;

import android.content.*;
import android.view.KeyEvent;
import java.util.*;
import org.json.*;

/** Device-local, bounded configuration. Cached between presses; never part of replica preferences. */
final class RemoteActions {
 static final String STORE="remote_actions_v1";
 static final class Action {
  String id=UUID.randomUUID().toString(),kind="route",label="";
  boolean enabled=true;
  TvRecoveryConfig.Settings target=new TvRecoveryConfig.Settings();
  JSONObject json()throws JSONException{return new JSONObject().put("id",id).put("kind",kind).put("label",label).put("enabled",enabled).put("target",target.json());}
  Action copy(){try{return parse(json());}catch(JSONException e){throw new IllegalStateException(e);}}
  String label(){return !label.isEmpty()?label:target.hardware()?"Función "+target.remoteCode:target.key!=0?KeyEvent.keyCodeToString(target.key):target.scan!=0?"Scan "+target.scan:"Botón sin tecla";}
  String keyDescription(){return target.hardware()?"Función del control: "+target.remoteCode:target.hasBinding()?"Android: "+KeyEvent.keyCodeToString(target.key)+" ("+target.key+") · Scan: "+target.scan:"Sin tecla asignada";}
  String description(){return kind.equals("home")?"Inicio":kind.equals("settings")?"Ajustes de Android":kind.equals("mute")?"Silenciar / activar sonido":(kind.equals("open")?"Abrir ":"Reabrir ")+target.name+(kind.equals("route")?" · "+(target.verified?target.channel:"Elegir destino"):"");}
 }
 private static List<Action> cache;
 static synchronized List<Action> get(Context c){
  if(cache!=null)return cache;
  ArrayList<Action> list=new ArrayList<>();String raw=c.getSharedPreferences(STORE,0).getString("actions",null);
  if(raw==null){TvRecoveryConfig.Settings old=TvRecoveryConfig.get(c);if(old.hasBinding()){Action a=new Action();a.target=old.copy();a.enabled=old.button;list.add(a);}}
  else try{JSONArray rows=new JSONArray(raw);if(rows.length()>32)throw new JSONException("Too many buttons");for(int i=0;i<rows.length();i++)list.add(parse(rows.getJSONObject(i)));}catch(JSONException e){android.util.Log.w("Z6Remote","Invalid local button configuration");}
  cache=Collections.unmodifiableList(list);if(raw==null){persist(c,list);TvRecoveryConfig.Settings legacy=TvRecoveryConfig.get(c).copy();if(legacy.button){legacy.button=false;TvRecoveryConfig.save(c,legacy);}}return cache;
 }
 static Action parse(JSONObject o)throws JSONException{
  Action a=new Action();a.id=o.getString("id");a.kind=o.getString("kind");a.enabled=o.optBoolean("enabled");a.target=TvRecoveryConfig.parse(o.getJSONObject("target"));
  a.label=o.optString("label",!a.target.keyName.startsWith("KEYCODE_")&&!a.target.keyName.equals("Otro botón")?a.target.keyName:"");if(a.label.length()>40||a.label.matches("(?s).*[\\p{Cntrl}].*"))throw new JSONException("Invalid button label");
  if(!a.id.matches("[A-Za-z0-9-]{1,50}")||!Arrays.asList("open","reopen","route","home","settings","mute").contains(a.kind))throw new JSONException("Invalid button");return a;
 }
 static synchronized void persist(Context c,List<Action> list){
  try{JSONArray out=new JSONArray();Set<String> ids=new HashSet<>(),keys=new HashSet<>();if(list.size()>32)throw new IllegalArgumentException("Máximo 32 botones");
   for(Action a:list){Action checked=parse(a.json());if(!ids.add(a.id))throw new IllegalArgumentException("Botón duplicado");String binding=a.target.key+":"+a.target.scan+":"+a.target.remoteCode+":"+a.target.remoteDevice;if(a.target.hasBinding()&&!keys.add(binding))throw new IllegalArgumentException("Esa tecla ya tiene una acción. Edita su botón.");for(Action other:list)if(other!=a&&!a.target.hardware()&&!other.target.hardware()&&a.target.key>0&&a.target.key==other.target.key&&(a.target.scan==0||other.target.scan==0||a.target.scan==other.target.scan))throw new IllegalArgumentException("Esa tecla ya tiene una acción. Edita su botón.");out.put(checked.json());}
   if(!c.getSharedPreferences(STORE,0).edit().putString("actions",out.toString()).commit())throw new IllegalStateException("No se pudo guardar");
   ArrayList<Action> copy=new ArrayList<>();for(Action a:list)copy.add(a.copy());cache=Collections.unmodifiableList(copy);
  }catch(JSONException e){throw new IllegalArgumentException(e);}
 }
 static void save(Context c,Action a){List<Action> list=new ArrayList<>(get(c));int at=-1;for(int i=0;i<list.size();i++)if(list.get(i).id.equals(a.id))at=i;if(at<0)list.add(a);else list.set(at,a);persist(c,list);TvRemoteKeys.changed(c);}
 static void remove(Context c,String id){List<Action> list=new ArrayList<>(get(c));list.removeIf(a->a.id.equals(id));persist(c,list);TvRemoteKeys.changed(c);}
 static Action hardware(Context c){for(Action a:get(c))if(a.enabled&&a.target.hardware())return a;return null;}
 static Action find(Context c,String id){for(Action a:get(c))if(a.id.equals(id))return a;return null;}
 static Action match(Context c,KeyEvent e){for(Action a:get(c))if(a.enabled&&a.target.matches(e))return a;return null;}
 static void nativePrepared(Context c,JSONObject result){
  android.content.SharedPreferences prefs=c.getSharedPreferences(STORE,0);if(prefs.getString("layout","{}").equals("{}"))prefs.edit().putString("layout",result.toString()).commit();
  List<Action> list=new ArrayList<>();boolean netflix=false,youtube=false;
  for(Action original:get(c)){Action a=original.copy();if(a.target.keyName.equals("Netflix")||a.target.remoteCode==88||a.target.key==189&&a.target.scan==88){a.target.key=189;a.target.scan=88;a.target.remoteCode=0;a.target.remoteDevice="";netflix=true;}if(a.target.keyName.equals("YouTube")||a.target.key==188&&a.target.scan==87){a.target.key=188;a.target.scan=87;a.target.remoteCode=0;a.target.remoteDevice="";youtube=true;}list.add(a);}
  if(!netflix){Action a=new Action();a.target=TvRecoveryConfig.get(c).copy();a.target.keyName="Netflix";a.target.key=189;a.target.scan=88;a.target.remoteCode=0;a.target.remoteDevice="";a.enabled=a.target.verified;list.add(a);}
  if(!youtube){String pkg="";for(String p:new String[]{"io.gh.reisxd.tizentube.fortaleza","com.google.android.youtube.tv"})if(TvRecoveryConfig.eligible(c,p)){pkg=p;break;}Action a=new Action();a.kind="reopen";a.target.keyName="YouTube";a.target.key=188;a.target.scan=87;a.target.pkg=pkg;a.target.name=pkg.isEmpty()?"Elegir YouTube":"YouTube";a.target.signature=TvRecoveryConfig.signature(c,pkg);a.enabled=!pkg.isEmpty();list.add(a);}
  persist(c,list);TvRecoveryConfig.Settings old=TvRecoveryConfig.get(c).copy();old.button=false;TvRecoveryConfig.save(c,old);TvRemoteKeys.changed(c);
 }
 static JSONObject export(Context c)throws JSONException{get(c);return new JSONObject().put("schema",1).put("actions",new JSONArray(c.getSharedPreferences(STORE,0).getString("actions","[]"))).put("layout",new JSONObject(c.getSharedPreferences(STORE,0).getString("layout","{}"))).put("destinations",SavedDestinations.export(c)).put("pendingDestination",SavedDestinations.pending(c)==null?JSONObject.NULL:SavedDestinations.pending(c));}
 static void restore(Context c,JSONObject value)throws JSONException{SavedDestinations.validateRestore(value);JSONArray rows=value.getJSONArray("actions");ArrayList<Action> list=new ArrayList<>();for(int i=0;i<rows.length();i++)list.add(parse(rows.getJSONObject(i)));persist(c,list);SavedDestinations.restore(c,value);c.getSharedPreferences(STORE,0).edit().putString("layout",value.optJSONObject("layout")==null?"{}":value.getJSONObject("layout").toString()).commit();}
}
