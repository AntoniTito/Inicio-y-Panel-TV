package local.z6panel;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.*;
import android.media.ExifInterface;
import java.io.*;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

/** All large image allocations share this gate. No caller may wait on it on the UI thread. */
final class WallpaperImages {
 static final ReentrantLock HEAVY=new ReentrantLock(true);
 static final long MB=1024L*1024;
 static final int DARK=0xFF10151D;
 static final class Header {
  final int width,height,orientation;final String mime;
  Header(int w,int h,int o,String m){width=w;height=h;orientation=o;mime=m;}
  boolean contain(){return width<=height;}
 }
 private static final class Found extends RuntimeException {final Header header;Found(Header h){header=h;}}
 static Header header(File file)throws IOException {return header(new WallpaperSource(file));}
 static Header header(WallpaperSource file)throws IOException {
  int orientation=1;try(android.os.ParcelFileDescriptor fd=file.open()){orientation=new ExifInterface(fd.getFileDescriptor()).getAttributeInt(ExifInterface.TAG_ORIENTATION,1);}catch(IOException ignored){}
  final int exif=orientation;
  try{Bitmap unexpected=ImageDecoder.decodeBitmap(file.decoder(),(d,info,s)->{
   String mime=info.getMimeType();if(info.isAnimated()||!("image/jpeg".equals(mime)||"image/png".equals(mime)||"image/webp".equals(mime)||"image/heif".equals(mime)||"image/heic".equals(mime)))throw new IllegalArgumentException("Usa JPG, PNG o WebP estático; HEIC depende del equipo");
   int w=info.getSize().getWidth(),h=info.getSize().getHeight();if(w<=0||h<=0)throw new IllegalArgumentException("Imagen inválida");throw new Found(new Header(w,h,exif,mime));
  });if(unexpected!=null)unexpected.recycle();throw new IOException("Imagen inválida");
  }catch(Found ready){return ready.header;}catch(RuntimeException e){throw new IOException(e.getMessage()==null?"Imagen no compatible":e.getMessage(),e);}catch(IOException e){throw new IOException("No se pudo leer la imagen. Usa JPG, PNG o WebP estático; HEIC depende del equipo",e);}
 }
 static long available(){Runtime r=Runtime.getRuntime();return r.maxMemory()-(r.totalMemory()-r.freeMemory());}
 static boolean lowRam(Context c){ActivityManager am=(ActivityManager)c.getSystemService(Context.ACTIVITY_SERVICE);if(am.isLowRamDevice()||am.getMemoryClass()<128)return true;ActivityManager.MemoryInfo info=new ActivityManager.MemoryInfo();am.getMemoryInfo(info);return info.lowMemory;}
 static boolean fourK(Context c){ActivityManager am=(ActivityManager)c.getSystemService(Context.ACTIVITY_SERVICE);return !lowRam(c)&&am.getMemoryClass()>=192&&available()>=96*MB;}
 static int screenWidth(Context c){return Math.max(1,c.getResources().getDisplayMetrics().widthPixels);}
 static int screenHeight(Context c){return Math.max(1,c.getResources().getDisplayMetrics().heightPixels);}
 static int maximum(Context c){if(lowRam(c))return 1280;int w=screenWidth(c),h=screenHeight(c);if(w>=3840&&h>=2160&&fourK(c))return 3840;if(w>=1920&&h>=1080)return 1920;return 1280;}
 static String normalize(String value){return Arrays.asList("auto","light","balanced","ultra").contains(value)?value:"auto";}
 static int requested(Context c,String quality){quality=normalize(quality);return quality.equals("light")?1280:quality.equals("balanced")?1920:quality.equals("ultra")?3840:lowRam(c)?1280:1920;}
 static int usable(Context c,String quality){return Math.min(maximum(c),requested(c,quality));}
 static int[] size(Context c,int cap){int w=screenWidth(c),h=screenHeight(c);double scale=Math.min(1,Math.min((double)cap/w,(double)(cap*9/16)/h));return new int[]{Math.max(1,(int)Math.round(w*scale)),Math.max(1,(int)Math.round(h*scale))};}
 static List<String> qualities(Context c){ArrayList<String> a=new ArrayList<>(Arrays.asList("auto","light"));if(maximum(c)>=1920)a.add("balanced");if(maximum(c)>=3840)a.add("ultra");return a;}
 static String label(String q){return q.equals("light")?"720p":q.equals("balanced")?"1080p":q.equals("ultra")?"4K":"Automático";}
 static String settingLabel(Context c,String q){q=normalize(q);int cap=usable(c,q);String actual=label(cap>=3840?"ultra":cap>=1920?"balanced":"light");return label(q)+(q.equals("auto")?" · "+actual:requested(c,q)>cap?" · límite "+actual:"");}
 static String bytes(long bytes){return bytes<MB?String.format(Locale.getDefault(),"%.0f KB",bytes/1024.0):String.format(Locale.getDefault(),"%.1f MB",bytes/(double)MB);}
 static boolean insufficient(int w,int h,int tw,int th){if(w<=h){double factor=Math.min((double)tw/w,(double)th/h);return factor>1.001;}return w<tw||h<th;}
 static Matrix orientation(int o,int w,int h){float[] src={0,0,w,0,0,h},dst;switch(o){case 2:dst=new float[]{w,0,0,0,w,h};break;case 3:dst=new float[]{w,h,0,h,w,0};break;case 4:dst=new float[]{0,h,w,h,0,0};break;case 5:dst=new float[]{0,0,0,w,h,0};break;case 6:dst=new float[]{h,0,h,w,0,0};break;case 7:dst=new float[]{h,w,h,0,0,w};break;case 8:dst=new float[]{0,w,0,0,h,w};break;default:dst=src;}Matrix m=new Matrix();m.setPolyToPoly(src,0,dst,0,3);return m;}
 /** Decode only the useful source rectangle, then orient and scale down once. */
 static Bitmap frame(File file,int targetW,int targetH)throws IOException {return frame(new WallpaperSource(file),targetW,targetH);}
 static Bitmap frame(WallpaperSource file,int targetW,int targetH)throws IOException {
  HEAVY.lock();try{
   Header info=header(file);boolean contain=info.contain();BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;try(android.os.ParcelFileDescriptor fd=file.open()){BitmapFactory.decodeFileDescriptor(fd.getFileDescriptor(),null,bounds);}if(bounds.outWidth<=0||bounds.outHeight<=0)return fallback(file,targetW,targetH,contain);
   int sw=bounds.outWidth,sh=bounds.outHeight;boolean rotate=info.orientation>=5&&info.orientation<=8;int ow=rotate?sh:sw,oh=rotate?sw:sh;
   RectF crop=new RectF(0,0,ow,oh);if(!contain){double ratio=(double)targetW/targetH;if((double)ow/oh>ratio){float width=(float)(oh*ratio);crop.left=(ow-width)/2;crop.right=crop.left+width;}else{float height=(float)(ow/ratio);crop.top=(oh-height)/2;crop.bottom=crop.top+height;}}
   double scale=Math.min(1,Math.min(targetW/crop.width(),targetH/crop.height()));int outW=Math.max(1,(int)Math.round(crop.width()*scale)),outH=Math.max(1,(int)Math.round(crop.height()*scale));
   Matrix inverse=new Matrix();orientation(info.orientation,sw,sh).invert(inverse);RectF raw=new RectF(crop);inverse.mapRect(raw);Rect region=new Rect(Math.max(0,(int)Math.floor(raw.left)),Math.max(0,(int)Math.floor(raw.top)),Math.min(sw,(int)Math.ceil(raw.right)),Math.min(sh,(int)Math.ceil(raw.bottom)));
   android.os.ParcelFileDescriptor regionFd=file.open();BitmapRegionDecoder decoder;try{decoder=BitmapRegionDecoder.newInstance(regionFd.getFileDescriptor(),false);}catch(IOException|RuntimeException unsupported){regionFd.close();return fallback(file,targetW,targetH,contain);}if(decoder==null){regionFd.close();return fallback(file,targetW,targetH,contain);}
   Bitmap pixels=null;try{int sample=1;double reduction=1/scale;while(sample*2<=reduction)sample*=2;long estimate=4L*((long)Math.ceil(region.width()/(double)sample)*(long)Math.ceil(region.height()/(double)sample)+(long)outW*outH);if(estimate+16*MB>available())throw new IOException("Memoria insuficiente para esta calidad");BitmapFactory.Options options=new BitmapFactory.Options();options.inSampleSize=sample;options.inPreferredConfig=Bitmap.Config.ARGB_8888;pixels=decoder.decodeRegion(region,options);if(pixels==null)throw new IOException("No se pudo leer la imagen");Bitmap result=Bitmap.createBitmap(outW,outH,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(result);canvas.drawColor(DARK);Matrix matrix=orientation(info.orientation,pixels.getWidth(),pixels.getHeight());int pw=rotate?pixels.getHeight():pixels.getWidth(),ph=rotate?pixels.getWidth():pixels.getHeight();matrix.postScale((float)outW/pw,(float)outH/ph);canvas.drawBitmap(pixels,matrix,new Paint(Paint.FILTER_BITMAP_FLAG));return result;
   }finally{decoder.recycle();regionFd.close();if(pixels!=null)pixels.recycle();}
  }catch(OutOfMemoryError e){throw new IOException("Memoria insuficiente para esta calidad",e);}finally{HEAVY.unlock();}
 }
 private static Bitmap fallback(WallpaperSource file,int w,int h,boolean contain)throws IOException {
  return ImageDecoder.decodeBitmap(file.decoder(),(d,info,s)->{int iw=info.getSize().getWidth(),ih=info.getSize().getHeight();double scale=Math.min(1,contain?Math.min((double)w/iw,(double)h/ih):Math.max((double)w/iw,(double)h/ih));scale=Math.min(scale,Math.sqrt(Math.min(2L*w*h,Math.max(1,(available()-16*MB)/8))/(double)((long)iw*ih)));int dw=Math.max(1,(int)Math.floor(iw*scale)),dh=Math.max(1,(int)Math.floor(ih*scale));d.setTargetSize(dw,dh);d.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);if(!contain){double ratio=(double)w/h;int cw=dw,ch=dh;if((double)dw/dh>ratio)cw=Math.max(1,(int)(dh*ratio));else ch=Math.max(1,(int)(dw/ratio));d.setCrop(new Rect((dw-cw)/2,(dh-ch)/2,(dw+cw)/2,(dh+ch)/2));}});
 }
 static final class Rendered {final Bitmap bitmap;final boolean contain;final String quality,warning;Rendered(Bitmap b,boolean c,String q,String warning){bitmap=b;contain=c;quality=q;this.warning=warning;}}
 static Rendered render(Context c,WallpaperSource file,String quality,boolean reduced)throws IOException {
  int normal=usable(c,quality),cap=normal;if(reduced)cap=Math.min(cap,1280);String failure=cap<normal||(!normalize(quality).equals("auto")&&requested(c,quality)>cap&&screenWidth(c)>=requested(c,quality)&&screenHeight(c)>=requested(c,quality)*9/16)?"Calidad reducida para proteger la fluidez":"";for(int level:new int[]{3840,1920,1280}){if(level>cap)continue;int[] size=size(c,level);try{Header h=header(file);Bitmap bitmap=frame(file,size[0],size[1]);String warning=insufficient(bitmap.getWidth(),bitmap.getHeight(),size[0],size[1])?"Puede verse borrosa: resolución insuficiente":"";if(!failure.isEmpty())warning="Calidad reducida para proteger la fluidez"+(warning.isEmpty()?"":" · "+warning);return new Rendered(bitmap,h.contain(),level>=3840?"ultra":level>=1920?"balanced":"light",warning);}catch(IOException e){failure=e.getMessage();}}throw new IOException(failure.isEmpty()?"Imagen no disponible":failure);
 }
}
