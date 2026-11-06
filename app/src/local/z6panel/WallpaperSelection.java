package local.z6panel;

import android.content.*;
import java.util.*;

/** Membership is independent from the library order; old installations default to all. */
final class WallpaperSelection {
 static final String ALL="wallpaper_slideshow_use_all",IDS="wallpaper_slideshow_ids";
 static List<WallpaperStore.Photo> eligible(SharedPreferences prefs,List<WallpaperStore.Photo> photos){if(prefs.getBoolean(ALL,true))return photos;Set<String> ids=prefs.getStringSet(IDS,Collections.emptySet());ArrayList<WallpaperStore.Photo> result=new ArrayList<>();for(WallpaperStore.Photo p:photos)if(ids.contains(p.id))result.add(p);return result;}
 static void save(Context c,boolean all,Collection<String> selected){SharedPreferences prefs=c.getSharedPreferences("panel",0);Set<String> valid=new HashSet<>();for(WallpaperStore.Photo p:new WallpaperStore(c).photos())if(selected.contains(p.id))valid.add(p.id);SharedPreferences.Editor edit=prefs.edit().putBoolean(ALL,all);if(!all)edit.putStringSet(IDS,valid);if(!all&&valid.isEmpty()&&prefs.getString("wallpaper_mode","original").equals("slideshow"))edit.putString("wallpaper_mode","original").remove("wallpaper_slide_id");edit.apply();}
 static String summary(SharedPreferences prefs,List<WallpaperStore.Photo> photos){int count=eligible(prefs,photos).size();return (prefs.getBoolean(ALL,true)?"Todas":"Elegidas")+" · "+count+(count==1?" foto":" fotos");}
}
