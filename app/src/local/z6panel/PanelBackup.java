package local.z6panel;

import android.content.*;
import android.util.Base64;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Shell-only export is provided by BridgeProvider; snapshots remain app-private. */
final class PanelBackup {
 static synchronized void capture(Context c){
  android.util.AtomicFile file=new android.util.AtomicFile(new File(c.getFilesDir(),"panel-before-0.10.0.json"));
  try(FileInputStream existing=file.openRead()){new JSONObject(read(existing));return;}catch(FileNotFoundException missing){}catch(Exception invalid){android.util.Log.e("Z6Backup","Existing backup is not valid; keeping it for recovery",invalid);return;}
  FileOutputStream stream=null;try{byte[] bytes=export(c).getBytes(StandardCharsets.UTF_8);stream=file.startWrite();stream.write(bytes);file.finishWrite(stream);}catch(Exception e){if(stream!=null)file.failWrite(stream);android.util.Log.e("Z6Backup","Unable to preserve preferences",e);}
 }
 private static String read(InputStream stream)throws IOException{ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[] buffer=new byte[4096];int n;while((n=stream.read(buffer))!=-1){if(bytes.size()+n>262144)throw new IOException("Backup too large");bytes.write(buffer,0,n);}return bytes.toString("UTF-8");}
 static String export(Context c)throws JSONException{
  JSONObject out=new JSONObject();
  for(Map.Entry<String,?> entry:c.getSharedPreferences("panel",0).getAll().entrySet()){
   Object value=entry.getValue();JSONObject v=new JSONObject();String type;
   if(value instanceof Boolean)type="boolean";else if(value instanceof Integer)type="int";else if(value instanceof Long)type="long";else if(value instanceof Float)type="float";else if(value instanceof Set){type="set";value=new JSONArray((Set<?>)value);}else type="string";
   v.put("type",type);v.put("value",value);out.put(entry.getKey(),v);
  }return out.toString();
 }
 static String encoded(Context c,boolean original)throws Exception{
  String value;
  if(original){android.util.AtomicFile file=new android.util.AtomicFile(new File(c.getFilesDir(),"panel-before-0.10.0.json"));try(FileInputStream stream=file.openRead()){value=read(stream);new JSONObject(value);}}
  else value=export(c);
  return Base64.encodeToString(value.getBytes(StandardCharsets.UTF_8),Base64.NO_WRAP);
 }
 static void restore(Context c,String encoded)throws Exception{
  if(encoded==null||encoded.length()>262144)throw new IllegalArgumentException("Invalid backup");
  JSONObject data=new JSONObject(new String(Base64.decode(encoded,Base64.DEFAULT),StandardCharsets.UTF_8));
  // Validate every entry before clearing or writing anything.
  Map<String,Object> values=new LinkedHashMap<>();Iterator<String> keys=data.keys();
  while(keys.hasNext()){String key=keys.next();JSONObject v=data.getJSONObject(key);String type=v.getString("type");Object result;
   switch(type){case "boolean":result=v.getBoolean("value");break;case "int":result=v.getInt("value");break;case "long":result=v.getLong("value");break;case "float":result=(float)v.getDouble("value");break;case "string":result=v.getString("value");break;case "set":Set<String> set=new HashSet<>();JSONArray a=v.getJSONArray("value");for(int i=0;i<a.length();i++)set.add(a.getString(i));result=set;break;default:throw new IllegalArgumentException("Unknown backup type");}values.put(key,result);
  }
  SharedPreferences.Editor edit=c.getSharedPreferences("panel",0).edit().clear();
  for(Map.Entry<String,Object> entry:values.entrySet()){String key=entry.getKey();Object value=entry.getValue();if(value instanceof Boolean)edit.putBoolean(key,(Boolean)value);else if(value instanceof Integer)edit.putInt(key,(Integer)value);else if(value instanceof Long)edit.putLong(key,(Long)value);else if(value instanceof Float)edit.putFloat(key,(Float)value);else if(value instanceof Set)edit.putStringSet(key,(Set<String>)value);else edit.putString(key,(String)value);}
  if(!edit.commit())throw new IOException("Preferences not saved");
 }
}
