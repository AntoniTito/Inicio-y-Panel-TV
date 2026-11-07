package local.z6panel;
import android.content.Context;
import android.util.Base64;
import org.json.*;
import java.io.*;

/** Bounded, read-only chunks, reachable only through the shell/DUMP protected provider. */
final class WallpaperArchive {
 static WallpaperSource source(Context c,String id)throws Exception{if(id.equals("daily")){WallpaperSource f=WallpaperOnline.file(c);if(f==null)throw new FileNotFoundException();return f;}if(!id.matches("[a-f0-9]{32}"))throw new IllegalArgumentException("Invalid image ID");WallpaperStore.Photo p=new WallpaperStore(c).find(id);if(p==null)throw new FileNotFoundException();return p.file;}
 static JSONObject manifest(Context c)throws Exception{JSONArray images=new JSONArray();for(WallpaperStore.Photo p:new WallpaperStore(c).photos())images.put(describe(p.id,p.file));WallpaperSource daily=WallpaperOnline.file(c);if(daily!=null)images.put(describe("daily",daily));return new JSONObject().put("images",images);}
 private static JSONObject describe(String id,WallpaperSource source)throws Exception{try(InputStream in=new android.os.ParcelFileDescriptor.AutoCloseInputStream(source.open())){return new JSONObject().put("id",id).put("size",source.length()).put("sha256",WallpaperMedia.hash(in));}}
 static JSONObject chunk(Context c,String arg)throws Exception{String[] request=arg.split(":",-1);if(request.length!=2)throw new IllegalArgumentException("Invalid request");WallpaperSource source=source(c,request[0]);long offset=Long.parseLong(request[1]);if(offset<0||offset>source.length())throw new IllegalArgumentException("Invalid offset");try(FileInputStream in=new android.os.ParcelFileDescriptor.AutoCloseInputStream(source.open())){in.getChannel().position(offset);byte[] bytes=new byte[49152];int n=in.read(bytes);return new JSONObject().put("base64",n<0?"":Base64.encodeToString(bytes,0,n,Base64.NO_WRAP));}}
}
