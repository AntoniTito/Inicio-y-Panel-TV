package local.z6panel;
import android.content.*;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import org.json.*;
import java.io.*;

/** Fault injection restricted to the AOSP emulator and the already shell-authenticated provider. */
final class WallpaperEmulatorFixtures {
 static JSONObject run(Context c,String action)throws Exception{
  if(!("ranchu".equals(Build.HARDWARE)||"goldfish".equals(Build.HARDWARE)))throw new SecurityException("Emulator only");
  if(action.equals("legacy-daily")){WallpaperMedia media=new WallpaperMedia(c);JSONObject all=media.read(),credit=WallpaperOnline.credit(c);String id=credit.getString("id");JSONObject entry=all.getJSONObject(id);if(!WallpaperMedia.DAILY.equals(entry.optString("album")))throw new IllegalStateException("Temporary landscape required");String alias=WallpaperMedia.id("emulator-legacy:"+id);all.remove(id);entry.getJSONObject("meta").put("id",alias);all.put(alias,entry);media.write(all);credit.put("id",alias);WallpaperStore.writeJson(new File(new WallpaperStore(c).dir,"daily.json"),credit.toString());return new JSONObject().put("legacyUri",credit.getString("uri")).put("legacyId",alias);}
  if(action.equals("yesterday")){
   WallpaperMedia media=new WallpaperMedia(c);JSONObject all=media.read();for(java.util.Iterator<String> it=all.keys();it.hasNext();){String id=it.next();JSONObject e=all.getJSONObject(id);if(WallpaperMedia.DAILY.equals(e.optString("album"))&&e.has("meta"))e.getJSONObject("meta").put("date","2000-01-01");}media.write(all);
   File catalog=new File(c.getFilesDir(),"landscape-catalog.json");JSONObject j=WallpaperOnline.snapshot(c);j.put("date","2000-01-01");WallpaperStore.writeJson(catalog,j.toString());JSONObject credit=WallpaperOnline.credit(c);credit.put("date","2000-01-01");WallpaperStore.writeJson(new File(new WallpaperStore(c).dir,"daily.json"),credit.toString());c.getSharedPreferences("panel",0).edit().remove("wallpaper_catalog_attempt").apply();return new JSONObject().put("simulatedOldDate",true);
  }
  if(action.startsWith("interrupt:")){
   if(!c.getSharedPreferences("panel",0).getBoolean("wallpaper_publish_hold",false))throw new IllegalStateException("Migration hold required");String id=action.substring(10);WallpaperStore store=new WallpaperStore(c);WallpaperStore.Photo photo=store.find(id);if(photo==null||!photo.name.startsWith("Z6-0160-"))throw new SecurityException("Synthetic fixture required");File original=new File(store.dir,id+".jpg");try(InputStream in=new android.os.ParcelFileDescriptor.AutoCloseInputStream(photo.file.open());OutputStream out=new FileOutputStream(original)){byte[] b=new byte[16384];int n;while((n=in.read(b))!=-1)out.write(b,0,n);}
   WallpaperMedia media=new WallpaperMedia(c);JSONObject entries=media.read(),entry=entries.getJSONObject(id);Uri uri=Uri.parse(entry.getString("uri"));ContentValues pending=new ContentValues();pending.put(MediaStore.Images.Media.IS_PENDING,1);c.getContentResolver().update(uri,pending,null,null);try(InputStream in=new FileInputStream(original);OutputStream out=c.getContentResolver().openOutputStream(uri,"wt")){byte[] b=new byte[257];int n=in.read(b);if(n>0)out.write(b,0,n);}entry.put("committed",false);media.write(entries);JSONArray library=store.readLibrary();for(int i=0;i<library.length();i++){JSONObject p=library.getJSONObject(i);if(id.equals(p.optString("id")))p.remove("uri");}store.writeLibrary(library);return new JSONObject().put("pendingUri",uri.toString()).put("privateOriginalPreserved",original.isFile());
  }
  throw new IllegalArgumentException("Unknown fixture");
 }
}
