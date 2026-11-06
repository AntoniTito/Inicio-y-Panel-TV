package local.z6panel;

import android.app.*;import android.content.*;import android.graphics.*;import android.os.*;import android.view.*;import android.widget.*;
import java.io.*;import java.util.*;import java.util.concurrent.*;

/** Only HOME owns a renderer; the accessibility overlay never loads photos. */
final class HomeWallpaper {
 private final Activity activity;private final ImageView image;private final View shade;private final Handler ui=new Handler(Looper.getMainLooper());
 private final ThreadPoolExecutor worker=new ThreadPoolExecutor(1,1,10,TimeUnit.SECONDS,new ArrayBlockingQueue<>(2),r->new Thread(()->{android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);r.run();},"Z6-home-wallpaper"),new ThreadPoolExecutor.DiscardPolicy());
 private final SharedPreferences prefs;private Bitmap bitmap;private volatile WallpaperOnline online;private volatile boolean active;private volatile int generation;private volatile String loaded="";private boolean reduced;
 HomeWallpaper(Activity a,ImageView v,View s){activity=a;image=v;shade=s;prefs=a.getSharedPreferences("panel",0);worker.allowCoreThreadTimeOut(true);}
 private final Runnable advance=()->load(true);
 void nextForShell(){if(active)load(true);}
 void resume(){active=true;generation++;load(false);}
 void pause(){active=false;generation++;ui.removeCallbacksAndMessages(null);worker.getQueue().clear();if(online!=null)online.cancel();clear();}
 void destroy(){pause();worker.shutdownNow();}
 void trim(){reduced=true;generation++;worker.getQueue().clear();clear();if(active)load(false);}
 private void clear(){image.setImageDrawable(null);shade.setVisibility(View.GONE);if(bitmap!=null){bitmap.recycle();bitmap=null;}loaded="";}
 private void load(boolean next){
  ui.removeCallbacks(advance);if(!active)return;String mode=prefs.getString("wallpaper_mode","original");if(mode.equals("original")){clear();return;}
  final int token=generation;worker.execute(()->{File file=null;String key="";try{
   WallpaperStore store=new WallpaperStore(activity);
   if(!active||token!=generation)return;
   if(mode.equals("daily")){file=WallpaperOnline.file(activity);if(file==null||!WallpaperOnline.today().equals(prefs.getString("wallpaper_daily_date",""))){online=new WallpaperOnline();if(!active||token!=generation){online.cancel();return;}try{online.update(activity,false);}catch(Exception ignored){}if(!active||token!=generation)return;file=WallpaperOnline.file(activity);}}
   else{List<WallpaperStore.Photo> list=store.photos();if(!list.isEmpty()){String id=prefs.getString(mode.equals("slideshow")?"wallpaper_slide_id":"wallpaper_photo","");int index=-1;for(int i=0;i<list.size();i++)if(list.get(i).id.equals(id))index=i;if(index<0&&mode.equals("photo"))list=Collections.emptyList();else if(index<0)index=0;if(!list.isEmpty()){if(next&&mode.equals("slideshow"))index=(index+1)%list.size();WallpaperStore.Photo photo=list.get(index);file=photo.file;if(mode.equals("slideshow"))prefs.edit().putString("wallpaper_slide_id",photo.id).apply();}}}
   if(!active||token!=generation)return;
   int max=prefs.getString("wallpaper_quality","light").equals("balanced")&&!reduced?1920:1280;
   int width=Math.min(max,activity.getResources().getDisplayMetrics().widthPixels),height=Math.min(max*9/16,activity.getResources().getDisplayMetrics().heightPixels);
   final String signature=file==null?"":file.getName()+"|"+file.lastModified()+"|"+width;
   Bitmap fresh=file==null||signature.equals(loaded)?null:WallpaperStore.decode(file,width,height);final Bitmap result=fresh;
   ui.post(()->{if(!active||token!=generation){if(result!=null)result.recycle();return;}if(result!=null){Bitmap old=bitmap;bitmap=result;image.setImageBitmap(result);shade.setVisibility(View.VISIBLE);loaded=signature;if(old!=null)old.recycle();}else if(signature.isEmpty())clear();if(mode.equals("slideshow"))ui.postDelayed(advance,Math.max(5,prefs.getInt("wallpaper_minutes",30))*60000L);});
  }catch(Exception failure){ui.post(()->{if(active&&token==generation&&mode.equals("slideshow"))ui.postDelayed(advance,Math.max(5,prefs.getInt("wallpaper_minutes",30))*60000L);});}});
 }
 String inspect(){return "{\"active\":"+active+",\"loaded\":"+(bitmap!=null)+",\"bytes\":"+(bitmap==null?0:bitmap.getAllocationByteCount())+",\"width\":"+(bitmap==null?0:bitmap.getWidth())+",\"height\":"+(bitmap==null?0:bitmap.getHeight())+",\"key\":\""+loaded+"\",\"scheduled\":"+(active&&prefs.getString("wallpaper_mode","original").equals("slideshow"))+"}";}
}
