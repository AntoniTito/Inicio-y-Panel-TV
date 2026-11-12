package local.z6panel;

import android.content.*;
import java.util.*;
import org.json.*;

/** Device-local route snapshots and one durable test draft. No background work. */
final class SavedDestinations {
 static final String ROUTES="destinations",PENDING="destination_draft";
 static final class Entry {
  String id;TvRecoveryConfig.Settings target;
  String label(){return target.name+" · "+target.channel;}
  JSONObject json()throws JSONException{return new JSONObject().put("id",id).put("target",target.json());}
 }
 private static List<Entry> cache;
 private static SharedPreferences prefs(Context c){return c.getSharedPreferences(RemoteActions.STORE,0);}
 static TvRecoveryConfig.Settings route(TvRecoveryConfig.Settings source){
  TvRecoveryConfig.Settings s=source.copy();s.boot=s.wake=s.button=false;s.key=s.scan=s.remoteCode=0;s.keyName=s.remoteDevice="";return s;
 }
 static TvRecoveryConfig.Settings apply(TvRecoveryConfig.Settings binding,TvRecoveryConfig.Settings source){
  TvRecoveryConfig.Settings s=route(source);s.key=binding.key;s.scan=binding.scan;s.remoteCode=binding.remoteCode;s.keyName=binding.keyName;s.remoteDevice=binding.remoteDevice;s.boot=binding.boot;s.wake=binding.wake;s.button=binding.button;return s;
 }
 static TvRecoveryConfig.Settings withoutRoute(TvRecoveryConfig.Settings source){TvRecoveryConfig.Settings s=source.copy();s.verified=false;s.channel="";s.steps=new JSONArray();s.endpoint=new JSONObject();return s;}
 static boolean sameRoute(TvRecoveryConfig.Settings a,TvRecoveryConfig.Settings b){return a.verified&&b.verified&&fingerprint(a).equals(fingerprint(b));}
 static JSONObject usage(Context c,String id){Entry e=find(c,id);JSONArray names=new JSONArray();boolean startup=false;if(e!=null){for(RemoteActions.Action a:RemoteActions.get(c))if(a.kind.equals("route")&&sameRoute(a.target,e.target))names.put(a.label());startup=sameRoute(TvRecoveryConfig.get(c),e.target);}try{return new JSONObject().put("buttons",names).put("startup",startup);}catch(JSONException failure){throw new IllegalStateException(failure);}}
 static void erase(Context c,String id,boolean assignments){
  Entry e=find(c,id);if(e==null)throw new IllegalArgumentException("El destino ya no existe");JSONObject before;try{before=RemoteActions.export(c);}catch(JSONException failure){throw new IllegalStateException("No se pudo preparar el borrado",failure);}TvRecoveryConfig.Settings startup=TvRecoveryConfig.get(c).copy();
  try{if(assignments){List<RemoteActions.Action> actions=new ArrayList<>();for(RemoteActions.Action a:RemoteActions.get(c)){a=a.copy();if(a.kind.equals("route")&&sameRoute(a.target,e.target)){a.kind="reopen";a.target=withoutRoute(a.target);}actions.add(a);}RemoteActions.persist(c,actions);if(sameRoute(startup,e.target)){TvRecoveryConfig.Settings cleared=withoutRoute(startup);cleared.boot=cleared.wake=cleared.button=false;TvRecoveryConfig.save(c,cleared);}JSONObject p=pending(c);if(p!=null&&sameRoute(TvRecoveryConfig.parse(p.getJSONObject("target")),e.target))discard(c);}remove(c,id);TvRecovery.get(c).changed();}
  catch(Exception failure){try{RemoteActions.restore(c,before);TvRecoveryConfig.save(c,startup);}catch(Exception rollback){failure.addSuppressed(rollback);}throw new IllegalStateException("No se pudo completar el borrado. Revisa el almacenamiento.",failure);}
 }
 private static String fingerprint(TvRecoveryConfig.Settings s){try{return route(s).json().toString();}catch(JSONException e){throw new IllegalArgumentException(e);}}
 static synchronized List<Entry> all(Context c){
  if(cache!=null)return new ArrayList<>(cache);
  String raw=prefs(c).getString(ROUTES,null);ArrayList<Entry> list=new ArrayList<>();
  if(raw!=null)try{list.addAll(parse(new JSONArray(raw)));}catch(JSONException e){throw new IllegalStateException("No se pudo leer Destinos guardados; se conserva su archivo",e);}
  cache=list;
  if(raw==null){try{TvRecoveryConfig.Settings startup=TvRecoveryConfig.get(c);if(startup.verified)put(list,startup);for(RemoteActions.Action a:RemoteActions.get(c))if(a.target.verified)put(list,a.target);persist(c,list);}catch(RuntimeException e){cache=null;throw e;}}
  return new ArrayList<>(cache);
 }
 private static Entry put(List<Entry> list,TvRecoveryConfig.Settings target){
  if(!target.verified)throw new IllegalArgumentException("Primero prueba y confirma el destino");
  String key=fingerprint(target);for(Entry e:list)if(fingerprint(e.target).equals(key))return e;
  if(list.size()>=64)throw new IllegalArgumentException("Máximo 64 destinos guardados. Elimina uno que ya no uses.");
  Entry e=new Entry();e.id=UUID.randomUUID().toString();e.target=route(target);list.add(e);return e;
 }
 static synchronized Entry save(Context c,TvRecoveryConfig.Settings target){List<Entry> list=all(c);Entry e=put(list,target);persist(c,list);return e;}
 static Entry find(Context c,String id){for(Entry e:all(c))if(e.id.equals(id))return e;return null;}
 static synchronized void remove(Context c,String id){List<Entry> list=all(c);list.removeIf(e->e.id.equals(id));persist(c,list);}
 private static void persist(Context c,List<Entry> list){try{JSONArray a=new JSONArray();for(Entry e:list)a.put(e.json());if(!prefs(c).edit().putString(ROUTES,a.toString()).commit())throw new IllegalStateException("No se pudieron guardar los destinos");cache=new ArrayList<>(list);}catch(JSONException e){throw new IllegalArgumentException(e);}}
 static JSONArray export(Context c)throws JSONException{JSONArray out=new JSONArray();for(Entry e:all(c))out.put(e.json());return out;}
 private static List<Entry> parse(JSONArray rows)throws JSONException{
  if(rows.length()>64)throw new JSONException("Too many destinations");ArrayList<Entry> list=new ArrayList<>();Set<String> ids=new HashSet<>();
  for(int i=0;i<rows.length();i++){JSONObject o=rows.getJSONObject(i);Entry e=new Entry();e.id=o.getString("id");e.target=route(TvRecoveryConfig.parse(o.getJSONObject("target")));if(!e.id.matches("[A-Za-z0-9-]{1,50}")||!ids.add(e.id)||!e.target.verified)throw new JSONException("Invalid destination");list.add(e);}return list;
 }
 static JSONObject pending(Context c){String raw=prefs(c).getString(PENDING,"");if(raw.isEmpty())return null;try{return validatePending(new JSONObject(raw));}catch(JSONException e){throw new IllegalStateException("No se pudo leer el destino pendiente; se conserva",e);}}
 private static JSONObject validatePending(JSONObject o)throws JSONException{TvRecoveryConfig.Settings target=TvRecoveryConfig.parse(o.getJSONObject("target"));if(!target.verified||o.optString("profileId").length()>50)throw new JSONException("Invalid destination draft");return o;}
 static void stage(Context c,TvRecoveryConfig.Settings target,String profile,boolean personal){try{writePending(c,new JSONObject().put("target",target.json()).put("profileId",profile).put("personal",personal).put("passed",false));}catch(JSONException e){throw new IllegalArgumentException(e);}}
 static void passed(Context c,TvRecoveryConfig.Settings target,String profile,boolean personal){try{JSONObject p=pending(c);if(p==null||!fingerprint(TvRecoveryConfig.parse(p.getJSONObject("target"))).equals(fingerprint(target))||!p.optString("profileId").equals(profile)||p.optBoolean("personal")!=personal){stage(c,target,profile,personal);p=pending(c);}p.put("passed",true);writePending(c,p);}catch(JSONException e){throw new IllegalArgumentException(e);}}
 static void writePending(Context c,JSONObject o){if(!prefs(c).edit().putString(PENDING,o.toString()).commit())throw new IllegalStateException("No se pudo conservar el destino pendiente");}
 static void discard(Context c){if(!prefs(c).edit().remove(PENDING).commit())throw new IllegalStateException("No se pudo quitar el borrador");}
 static void validateRestore(JSONObject o)throws JSONException{if(o.has(ROUTES))parse(o.getJSONArray(ROUTES));if(o.optJSONObject("pendingDestination")!=null)validatePending(o.getJSONObject("pendingDestination"));}
 static synchronized void restore(Context c,JSONObject o)throws JSONException{validateRestore(o);SharedPreferences.Editor edit=prefs(c).edit();if(o.has(ROUTES))edit.putString(ROUTES,o.getJSONArray(ROUTES).toString());if(o.has("pendingDestination")){JSONObject p=o.optJSONObject("pendingDestination");if(p==null)edit.remove(PENDING);else edit.putString(PENDING,p.toString());}if(!edit.commit())throw new IllegalStateException("No se pudieron restaurar los destinos");cache=null;}
}
