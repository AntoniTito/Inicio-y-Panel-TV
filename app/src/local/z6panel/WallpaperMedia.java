package local.z6panel;

import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import org.json.*;
import java.io.*;
import java.security.MessageDigest;
import java.util.*;

/** Own MediaStore rows only. A persisted pending-row journal makes publication restartable. */
final class WallpaperMedia {
 static final Object LOCK=new Object();
 static final String PERSONAL="Pictures/Z6/Mis fotos/",SAVED="Pictures/Z6/Conservadas/",DAILY="Pictures/Z6/Paisajes del dia/";
 final Context c;final File index;
 WallpaperMedia(Context context){c=context.getApplicationContext();index=new File(c.getFilesDir(),"wallpaper-media.json");}
 JSONObject read(){try(InputStream in=new android.util.AtomicFile(index).openRead()){return new JSONObject(new String(WallpaperStore.readLimited(in,262144),"UTF-8"));}catch(FileNotFoundException absent){return new JSONObject();}catch(Exception e){throw new IllegalStateException("No se pudo leer el registro multimedia; no se modificó",e);}}
 void write(JSONObject j)throws IOException{WallpaperStore.writeJson(index,j.toString());}
 static String hash(InputStream in)throws Exception{MessageDigest digest=MessageDigest.getInstance("SHA-256");byte[] b=new byte[16384];int n;while((n=in.read(b))!=-1)digest.update(b,0,n);StringBuilder s=new StringBuilder();for(byte v:digest.digest())s.append(String.format(Locale.ROOT,"%02x",v&255));return s.toString();}
 static String id(String value){try{MessageDigest d=MessageDigest.getInstance("MD5");StringBuilder s=new StringBuilder();for(byte v:d.digest(value.getBytes("UTF-8")))s.append(String.format(Locale.ROOT,"%02x",v&255));return s.toString();}catch(Exception e){throw new IllegalStateException(e);}}
 private boolean row(Uri u){try(Cursor q=c.getContentResolver().query(u,new String[]{"_id"},null,null,null)){return q!=null&&q.moveToFirst();}catch(Exception e){return false;}}
 WallpaperSource publish(File original,String id,String album,JSONObject metadata)throws Exception{synchronized(LOCK){
  if(!id.matches("[a-f0-9]{32}"))throw new IOException("Identificador de imagen inválido");JSONObject all=read(),entry=all.optJSONObject(id);Uri uri=entry==null?null:Uri.parse(entry.optString("uri"));
  if(entry!=null&&entry.optBoolean("committed")&&!row(uri))throw new FileNotFoundException("Imagen eliminada desde Multimedia");
  if(uri==null||!row(uri)){ContentValues values=new ContentValues();values.put(MediaStore.Images.Media.DISPLAY_NAME,"Z6-"+id+".jpg");values.put(MediaStore.Images.Media.MIME_TYPE,"image/jpeg");values.put(MediaStore.Images.Media.RELATIVE_PATH,album);values.put(MediaStore.Images.Media.IS_PENDING,1);values.put(MediaStore.Images.Media.TITLE,metadata.optString("name","Z6"));values.put(MediaStore.Images.Media.DESCRIPTION,android.text.Html.fromHtml(metadata.optString("author"),android.text.Html.FROM_HTML_MODE_LEGACY).toString()+" · "+metadata.optString("license")+" · "+metadata.optString("licenseUrl")+" · "+metadata.optString("source"));uri=c.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values);if(uri==null)throw new IOException("Android no pudo crear la imagen en Multimedia");entry=new JSONObject().put("uri",uri.toString()).put("committed",false);all.put(id,entry);write(all);}
  if(!entry.optBoolean("committed")){String expected;try(InputStream in=new FileInputStream(original)){expected=hash(in);}try(InputStream in=new FileInputStream(original);OutputStream out=c.getContentResolver().openOutputStream(uri,"wt")){if(out==null)throw new IOException("No se pudo escribir la imagen");byte[] b=new byte[16384];int n;while((n=in.read(b))!=-1)out.write(b,0,n);}String actual;try(InputStream in=c.getContentResolver().openInputStream(uri)){if(in==null)throw new IOException("No se pudo verificar la imagen");actual=hash(in);}WallpaperSource source=new WallpaperSource(c,uri);if(!expected.equals(actual)||source.length()!=original.length())throw new IOException("La copia no pasó la verificación; se conserva la original");ContentValues done=new ContentValues();done.put(MediaStore.Images.Media.IS_PENDING,0);c.getContentResolver().update(uri,done,null,null);entry.put("hash",actual).put("bytes",original.length()).put("committed",true);}
  metadata.put("uri",uri.toString());entry.put("album",album).put("meta",metadata);all.put(id,entry);write(all);return new WallpaperSource(c,uri);
 }}
 WallpaperSource source(String id){synchronized(LOCK){JSONObject e=read().optJSONObject(id);return e==null||!e.optBoolean("committed")?null:new WallpaperSource(c,Uri.parse(e.optString("uri")));}}
 boolean deleted(String id){synchronized(LOCK){JSONObject e=read().optJSONObject(id);return e!=null&&e.optBoolean("deleted");}}
 void move(String id,String album)throws Exception{synchronized(LOCK){JSONObject all=read(),e=all.optJSONObject(id);if(e==null)throw new IOException("Imagen no disponible");ContentValues v=new ContentValues();v.put(MediaStore.Images.Media.RELATIVE_PATH,album);if(c.getContentResolver().update(Uri.parse(e.getString("uri")),v,null,null)<1)throw new IOException("No se pudo conservar la imagen");e.put("album",album);write(all);}}
 boolean remove(String id){synchronized(LOCK){try{JSONObject all=read(),e=all.optJSONObject(id);if(e==null)return true;WallpaperSource source=new WallpaperSource(c,Uri.parse(e.getString("uri")));if(!source.delete()||source.isFile())return false;e.put("deleted",true);write(all);return true;}catch(Exception failure){return false;}}}
 void updateMeta(String id,JSONObject meta)throws Exception{synchronized(LOCK){JSONObject all=read(),e=all.optJSONObject(id);if(e!=null){e.put("meta",meta);write(all);}}}
 void pruneTombstones(Set<String> keep)throws Exception{synchronized(LOCK){JSONObject all=read();for(Iterator<String> it=all.keys();it.hasNext();){String id=it.next();if(all.optJSONObject(id).optBoolean("deleted")&&!keep.contains(id))it.remove();}write(all);}}
 static WallpaperSource reference(Context c,JSONObject j,File fallback){String uri=j.optString("uri");return uri.startsWith("content://media/")?new WallpaperSource(c,Uri.parse(uri)):new WallpaperSource(fallback);}
 static void openGallery(android.app.Activity a){Intent intent=new Intent(Intent.ACTION_VIEW).setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,"vnd.android.cursor.dir/image");if(intent.resolveActivity(a.getPackageManager())==null){intent=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_GALLERY);if(intent.resolveActivity(a.getPackageManager())==null){android.widget.Toast.makeText(a,"No hay una galería disponible. Puedes gestionar las imágenes en Mis fotos.",1).show();return;}}try{a.startActivity(intent);}catch(Exception e){android.widget.Toast.makeText(a,"No se pudo abrir la galería. Usa Mis fotos.",1).show();}}
}
