package local.z6panel;

import android.content.*;
import org.json.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Foreground-only, one bounded Commons attempt; no periodic background job. */
final class WallpaperOnline {
 private static final AtomicBoolean BUSY=new AtomicBoolean();
 private volatile boolean cancelled; private volatile HttpURLConnection connection;
 static String today(){return new SimpleDateFormat("yyyy-MM-dd",Locale.ROOT).format(new Date());}
 void cancel(){cancelled=true;HttpURLConnection c=connection;if(c!=null)c.disconnect();}
 private HttpURLConnection connect(String url)throws IOException {URL u=new URL(url);for(int redirect=0;redirect<3;redirect++){if(cancelled)throw new InterruptedIOException();if(!"https".equals(u.getProtocol())||!(u.getHost().equals("commons.wikimedia.org")||u.getHost().equals("upload.wikimedia.org")||u.getHost().equals("thumb.wikimedia.org")))throw new IOException("Origen de fondo no permitido");HttpURLConnection c=(HttpURLConnection)u.openConnection();connection=c;c.setConnectTimeout(10000);c.setReadTimeout(15000);c.setInstanceFollowRedirects(false);c.setRequestProperty("User-Agent","Z6Panel/0.12.0 (personal wallpaper; MediaWiki API)");int code=c.getResponseCode();if(code==200)return c;if(code>=300&&code<400){String location=c.getHeaderField("Location");c.disconnect();if(location==null)throw new IOException("Redirección incompleta");u=new URL(u,location);continue;}c.disconnect();throw new IOException("Wikimedia no está disponible ("+code+")");}throw new IOException("Demasiadas redirecciones de Wikimedia");}
 String update(Context context,boolean manual)throws Exception {
  SharedPreferences prefs=context.getSharedPreferences("panel",0);long now=System.currentTimeMillis();
  if(!manual&&today().equals(prefs.getString("wallpaper_daily_date","")))return "El paisaje de hoy ya está guardado";
  long delay=manual?60000:3600000;if(now-prefs.getLong("wallpaper_daily_attempt",0)<delay)return "Se conservará el paisaje anterior; vuelve a intentar más tarde";
  if(!BUSY.compareAndSet(false,true))return "Actualización en curso";
  WallpaperStore store=new WallpaperStore(context);File temp=null,prepared=null;
  try{prefs.edit().putLong("wallpaper_daily_attempt",now).apply();
   String api="https://commons.wikimedia.org/w/api.php?action=query&generator=categorymembers&gcmtitle=Category%3AFeatured_pictures_of_landscapes&gcmtype=file&gcmnamespace=6&gcmlimit=30&prop=imageinfo&iiprop=url%7Csize%7Cmime%7Cextmetadata&iiurlwidth=1920&format=json&formatversion=2";
   HttpURLConnection c=connect(api);JSONObject data;try(InputStream in=c.getInputStream()){data=new JSONObject(new String(WallpaperStore.readLimited(in,1024*1024),"UTF-8"));}finally{c.disconnect();}
   JSONArray pages=data.getJSONObject("query").getJSONArray("pages");ArrayList<JSONObject> candidates=new ArrayList<>();
   for(int i=0;i<pages.length();i++){JSONObject page=pages.getJSONObject(i);JSONArray infos=page.optJSONArray("imageinfo");if(infos==null||infos.length()==0)continue;JSONObject info=infos.getJSONObject(0),m=info.optJSONObject("extmetadata");if(m==null)continue;String license=value(m,"LicenseShortName");if(!license.startsWith("CC BY")&&!license.equals("CC0")&&!license.equals("Public domain"))continue;if(!info.optString("mime").equals("image/jpeg")||info.optInt("width")<info.optInt("height")||info.optString("thumburl").isEmpty()||value(m,"Artist").isEmpty())continue;info.put("title",page.optString("title"));candidates.add(info);}
   if(candidates.isEmpty())throw new IOException("No hay paisajes adecuados disponibles");
   JSONObject selected=candidates.get(Math.floorMod(today().hashCode(),candidates.size()));c=connect(selected.getString("thumburl"));
   try(InputStream in=c.getInputStream()){temp=File.createTempFile("daily-",".tmp",context.getCacheDir());try(FileOutputStream out=new FileOutputStream(temp)){byte[] bytes=new byte[16384];long count=0;int n;while((n=in.read(bytes))!=-1){if(cancelled)throw new InterruptedIOException();count+=n;if(count>5*1024*1024)throw new IOException("Paisaje demasiado grande");out.write(bytes,0,n);}}}finally{c.disconnect();}
   android.graphics.Bitmap bitmap=WallpaperStore.decode(temp,1920,1080);prepared=new File(store.dir,"daily-"+UUID.randomUUID()+".jpg");try(FileOutputStream out=new FileOutputStream(prepared)){if(!bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG,88,out))throw new IOException("No se pudo guardar el paisaje");}finally{bitmap.recycle();}
   if(cancelled)throw new InterruptedIOException();
   JSONObject metadata=selected.getJSONObject("extmetadata");JSONObject credit=new JSONObject();credit.put("file",prepared.getName());credit.put("title",selected.optString("title"));credit.put("author",value(metadata,"Artist"));credit.put("license",value(metadata,"LicenseShortName"));credit.put("licenseUrl",value(metadata,"LicenseUrl"));credit.put("source",selected.optString("descriptionurl"));credit.put("date",today());
   WallpaperStore.writeJson(new File(store.dir,"daily.json"),credit.toString());prefs.edit().putString("wallpaper_daily_date",today()).apply();String keep=prepared.getName();prepared=null;File[] old=store.dir.listFiles((d,name)->name.startsWith("daily-")&&name.endsWith(".jpg")&&!name.equals(keep));if(old!=null)for(File f:old)f.delete();return "Paisaje guardado";
  }finally{if(temp!=null)temp.delete();if(prepared!=null)prepared.delete();if(connection!=null)connection.disconnect();connection=null;BUSY.set(false);}
 }
 static String value(JSONObject metadata,String key){JSONObject v=metadata.optJSONObject(key);return v==null?"":v.optString("value");}
 static JSONObject credit(Context context){try(InputStream in=new FileInputStream(new File(new WallpaperStore(context).dir,"daily.json"))){return new JSONObject(new String(WallpaperStore.readLimited(in,32768),"UTF-8"));}catch(Exception ignored){return new JSONObject();}}
 static File file(Context context){String name=credit(context).optString("file");if(!name.matches("daily-[a-f0-9-]+\\.jpg"))return null;File f=new File(new WallpaperStore(context).dir,name);return f.isFile()?f:null;}
}
