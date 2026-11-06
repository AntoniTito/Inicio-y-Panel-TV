package local.z6panel;

import android.content.*;
import android.graphics.*;
import android.util.AtomicFile;
import org.json.*;
import java.io.*;
import java.util.*;

/** Private, bounded library. Image I/O is called only from image workers. */
final class WallpaperStore {
 static final long MAX_INPUT=20L*1024*1024, MAX_LIBRARY=100L*1024*1024;
 static final int MAX_IMAGES=50;
 private static final Object LOCK=new Object();
 final Context context; final File dir;
 WallpaperStore(Context c){context=c.getApplicationContext();dir=new File(context.getFilesDir(),"wallpapers");dir.mkdirs();}
 static final class Photo {
  final String id,name; final File file;
  Photo(String i,String n,File f){id=i;name=n;file=f;}
 }
 private JSONArray readLibrary(){try(FileInputStream in=new AtomicFile(new File(dir,"library.json")).openRead()){return new JSONArray(new String(readLimited(in,262144),"UTF-8"));}catch(FileNotFoundException missing){return new JSONArray();}catch(Exception e){throw new IllegalStateException("No se pudo leer la biblioteca; no se modificó",e);}}
 private void writeLibrary(JSONArray a)throws IOException {writeJson(new File(dir,"library.json"),a.toString());}
 static void writeJson(File file,String text)throws IOException {AtomicFile atom=new AtomicFile(file);FileOutputStream out=null;try{out=atom.startWrite();out.write(text.getBytes("UTF-8"));atom.finishWrite(out);}catch(IOException e){if(out!=null)atom.failWrite(out);throw e;}}
 List<Photo> photos(){synchronized(LOCK){JSONArray a=readLibrary();ArrayList<Photo> out=new ArrayList<>();for(int i=0;i<a.length();i++){JSONObject p=a.optJSONObject(i);if(p==null)continue;String id=p.optString("id");if(!id.matches("[a-f0-9]{32}"))continue;File f=new File(dir,id+".jpg");if(f.isFile())out.add(new Photo(id,p.optString("name","Imagen"),f));}return out;}}
 Photo find(String id){for(Photo p:photos())if(p.id.equals(id))return p;return null;}
 Photo importImage(File source,String label)throws Exception {
  if(source.length()==0||source.length()>MAX_INPUT)throw new IOException("La imagen debe ocupar como máximo 20 MB");
  if(photos().size()>=MAX_IMAGES)throw new IOException("Biblioteca completa: máximo 50 imágenes");
  if(dir.getUsableSpace()<MAX_INPUT+5*1024*1024)throw new IOException("Falta espacio para importar la imagen");
  String id=UUID.randomUUID().toString().replace("-","");File output=new File(dir,id+".jpg");
  Bitmap image=null;boolean committed=false;
  try{image=decode(source,1920,1080);try(FileOutputStream out=new FileOutputStream(output)){if(!image.compress(Bitmap.CompressFormat.JPEG,88,out))throw new IOException("No se pudo guardar la imagen");}
   String name=label==null?"Imagen":label.replaceAll("[\\p{Cntrl}]"," ").trim();if(name.length()>100)name=name.substring(0,100);if(name.isEmpty())name="Imagen";
   synchronized(LOCK){JSONArray all=readLibrary();long bytes=output.length();for(Photo p:photos())bytes+=p.file.length();if(all.length()>=MAX_IMAGES||bytes>MAX_LIBRARY)throw new IOException("Biblioteca completa: 50 imágenes o 100 MB");JSONObject p=new JSONObject();p.put("id",id);p.put("name",name);all.put(p);writeLibrary(all);committed=true;}
   return new Photo(id,name,output);
  }finally{if(image!=null)image.recycle();if(!committed)output.delete();}
 }
 void remove(String id)throws Exception {synchronized(LOCK){JSONArray old=readLibrary(),next=new JSONArray();for(int i=0;i<old.length();i++)if(!id.equals(old.getJSONObject(i).optString("id")))next.put(old.get(i));writeLibrary(next);if(id.matches("[a-f0-9]{32}"))new File(dir,id+".jpg").delete();}}
 void move(String id,int delta)throws Exception {synchronized(LOCK){JSONArray old=readLibrary();ArrayList<JSONObject> list=new ArrayList<>();int at=-1;for(int i=0;i<old.length();i++){JSONObject p=old.getJSONObject(i);list.add(p);if(id.equals(p.optString("id")))at=i;}int to=at+delta;if(at<0||to<0||to>=list.size())return;Collections.swap(list,at,to);JSONArray next=new JSONArray();for(JSONObject p:list)next.put(p);writeLibrary(next);}}
 static Bitmap decode(File f,int width,int height)throws IOException {
  try{return ImageDecoder.decodeBitmap(ImageDecoder.createSource(f),(decoder,info,source)->{
   String mime=info.getMimeType();if(info.isAnimated()||!("image/jpeg".equals(mime)||"image/png".equals(mime)||"image/webp".equals(mime)||"image/heif".equals(mime)||"image/heic".equals(mime)))throw new IllegalArgumentException("Usa JPG, PNG o WebP estático; HEIC depende del equipo");
   int w=info.getSize().getWidth(),h=info.getSize().getHeight();if(w<=0||h<=0)throw new IllegalArgumentException("Imagen inválida");double scale=Math.min(1,Math.min((double)width/w,(double)height/h));decoder.setTargetSize(Math.max(1,(int)(w*scale)),Math.max(1,(int)(h*scale)));decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
  });}catch(IllegalArgumentException e){throw new IOException(e.getMessage(),e);}catch(IOException e){throw new IOException("No se pudo leer la imagen. Usa JPG, PNG o WebP estático; HEIC depende del equipo",e);}catch(OutOfMemoryError e){throw new IOException("Memoria insuficiente; usa una imagen más pequeña",e);}
 }
 static byte[] readLimited(InputStream in,int max)throws IOException {ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){if(out.size()+n>max)throw new IOException("Archivo demasiado grande");out.write(b,0,n);}return out.toByteArray();}
 File receive(InputStream in,long length)throws IOException {if(length<=0||length>MAX_INPUT)throw new IOException("Máximo 20 MB por imagen");if(dir.getUsableSpace()<length+5*1024*1024)throw new IOException("Falta espacio disponible");File temp=File.createTempFile("incoming-",".tmp",context.getCacheDir());boolean ok=false;try(FileOutputStream out=new FileOutputStream(temp)){byte[] b=new byte[16384];long remaining=length;while(remaining>0){if(Thread.currentThread().isInterrupted())throw new InterruptedIOException();int n=in.read(b,0,(int)Math.min(b.length,remaining));if(n<0)throw new EOFException("Envío incompleto");out.write(b,0,n);remaining-=n;}ok=true;return temp;}finally{if(!ok)temp.delete();}}
 File copyUri(android.net.Uri uri)throws IOException {File temp=File.createTempFile("import-",".tmp",context.getCacheDir());boolean ok=false;try(InputStream in=context.getContentResolver().openInputStream(uri);FileOutputStream out=new FileOutputStream(temp)){if(in==null)throw new IOException("No se pudo abrir el archivo");byte[] b=new byte[16384];long size=0;int n;while((n=in.read(b))!=-1){size+=n;if(size>MAX_INPUT)throw new IOException("Máximo 20 MB por imagen");if(Thread.currentThread().isInterrupted())throw new InterruptedIOException();out.write(b,0,n);}ok=true;return temp;}finally{if(!ok)temp.delete();}}
}
