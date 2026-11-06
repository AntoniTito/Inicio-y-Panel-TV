package local.z6panel;

import android.content.*;
import android.content.pm.*;
import android.graphics.drawable.Drawable;
import android.util.LruCache;
import java.util.*;

/** Launcher-capable applications, independent of panel navigation and layout. */
public final class AppCatalog {
 public static final String[] PRIORITY={"com.xuper.netxxus","io.gh.reisxd.tizentube.fortaleza","com.esaba.downloader","com.android.vending"};
 public static final class Entry {
  public final String pkg,title; public final ComponentName component; public final boolean system;
  Entry(ResolveInfo r,PackageManager pm){pkg=r.activityInfo.packageName;title=r.loadLabel(pm).toString();component=new ComponentName(pkg,r.activityInfo.name);system=(r.activityInfo.applicationInfo.flags&(ApplicationInfo.FLAG_SYSTEM|ApplicationInfo.FLAG_UPDATED_SYSTEM_APP))!=0;}
  Entry(String p,String t,ComponentName c,boolean s){pkg=p;title=t;component=c;system=s;}
 }
 private final Context context; private final SharedPreferences prefs;
 private static volatile List<Entry> entries=Collections.emptyList();
 private static volatile boolean loaded;
 private static final Object SCAN_LOCK=new Object();
 private static final java.util.concurrent.atomic.AtomicLong invalidation=new java.util.concurrent.atomic.AtomicLong(),revision=new java.util.concurrent.atomic.AtomicLong();
 private static volatile long pendingGeneration=-1;
 private static volatile List<Entry> pendingScan;
 private static final LruCache<String,Drawable.ConstantState> icons=new LruCache<>(64);
 public AppCatalog(Context c){context=c.getApplicationContext();prefs=context.getSharedPreferences("panel",0);}
 public boolean ready(){return loaded;}
 public long revision(){return revision.get();}
 public void invalidate(String pkg){invalidation.incrementAndGet();revision.incrementAndGet();synchronized(icons){if(pkg==null)icons.evictAll();else icons.remove(pkg);}}
 /** Run off the UI thread. Leanback components take precedence over mobile launchers. */
 public List<Entry> scan(){
  synchronized(SCAN_LOCK){
  long generation=invalidation.get();
  if(pendingScan!=null&&pendingGeneration==generation)return pendingScan;
  PerfStats.scans.incrementAndGet();
  PackageManager pm=context.getPackageManager();Map<String,Entry> found=new HashMap<>();
  for(String cat:new String[]{Intent.CATEGORY_LAUNCHER,Intent.CATEGORY_LEANBACK_LAUNCHER}){
   for(ResolveInfo r:pm.queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(cat),0)){
    if(r.activityInfo!=null&&!r.activityInfo.packageName.equals(context.getPackageName())){
     try{found.put(r.activityInfo.packageName,new Entry(r,pm));}catch(RuntimeException ignored){}
    }
   }
  }
  List<Entry> result=new ArrayList<>(found.values());result.sort((a,b)->a.pkg.compareTo(b.pkg));
  pendingGeneration=generation;pendingScan=result;return result;
  }
 }
 public boolean accept(List<Entry> fresh){
  if(fresh==pendingScan&&pendingGeneration!=invalidation.get())return false;
  Map<String,String> old=new HashMap<>();for(Entry e:entries)old.put(e.pkg,e.title+"|"+e.component.flattenToString()+"|"+e.system);
  boolean changed=fresh.size()!=entries.size();for(Entry e:fresh)if(!(e.title+"|"+e.component.flattenToString()+"|"+e.system).equals(old.get(e.pkg)))changed=true;
  if(changed||!loaded){entries=Collections.unmodifiableList(new ArrayList<>(fresh));revision.incrementAndGet();saveSnapshot();}
  loaded=true;return changed;
 }
 public boolean restoreSnapshot(){
  if(loaded)return true;
  String raw=prefs.getString("catalog_snapshot","");
  if(raw.isEmpty())return false;
  ArrayList<Entry> restored=new ArrayList<>();PackageManager pm=context.getPackageManager();
  for(String item:raw.split("\\|")){
   String[] parts=item.split(";",-1);
   if(parts.length!=4)continue;
   try{ApplicationInfo info=pm.getApplicationInfo(parts[0],0);restored.add(new Entry(parts[0],parts[2],new ComponentName(parts[0],parts[1]),(info.flags&(ApplicationInfo.FLAG_SYSTEM|ApplicationInfo.FLAG_UPDATED_SYSTEM_APP))!=0));}catch(Exception ignored){}
  }
  entries=Collections.unmodifiableList(restored);loaded=!restored.isEmpty();revision.incrementAndGet();
  return !restored.isEmpty();
 }
 private void saveSnapshot(){
  StringBuilder raw=new StringBuilder();
  for(Entry e:entries){
   if(raw.length()>0)raw.append('|');
   raw.append(e.pkg).append(';').append(e.component.getClassName()).append(';').append(e.title.replace(";"," ")).append(';').append(e.system?'1':'0');
  }
  if(!raw.toString().equals(prefs.getString("catalog_snapshot","")))prefs.edit().putString("catalog_snapshot",raw.toString()).apply();
 }
 public List<Entry> all(){
  ArrayList<Entry> out=new ArrayList<>(entries);List<String> order=Arrays.asList(prefs.getString("app_order","").split("\\|"));
  out.sort((a,b)->{int x=rank(a.pkg,order),y=rank(b.pkg,order);return x==y?a.title.compareToIgnoreCase(b.title):Integer.compare(x,y);});return out;
 }
 private int rank(String p,List<String> order){int i=order.indexOf(p);if(i>=0)return i;int d=Arrays.asList(PRIORITY).indexOf(p);return order.size()+(d>=0?d:10);}
 public Entry find(String pkg){for(Entry e:entries)if(e.pkg.equals(pkg))return e;return null;}
 public boolean visible(Entry e){if(prefs.getStringSet("hidden_apps",Collections.emptySet()).contains(e.pkg))return false;return !e.system||Arrays.asList(PRIORITY).contains(e.pkg)||prefs.getStringSet("shown_apps",Collections.emptySet()).contains(e.pkg);}
 public boolean favorite(Entry e){return prefs.getStringSet("favorites",Collections.emptySet()).contains(e.pkg);}
 public String category(Entry e){return prefs.getString("category_"+e.pkg,e.pkg.equals(PRIORITY[0])||e.pkg.equals(PRIORITY[1])?"TV y video":e.pkg.equals(PRIORITY[2])||e.pkg.equals(PRIORITY[3])?"Herramientas":"Otras");}
 public void cycleCategory(Entry e){String[] cats={"TV y video","Herramientas","Otras"};int i=Arrays.asList(cats).indexOf(category(e));prefs.edit().putString("category_"+e.pkg,cats[(i+1)%cats.length]).apply();}
 private void setMember(String key,String p,boolean value){Set<String> s=new HashSet<>(prefs.getStringSet(key,Collections.emptySet()));if(value)s.add(p);else s.remove(p);prefs.edit().putStringSet(key,s).apply();}
 public void toggleFavorite(Entry e){setMember("favorites",e.pkg,!favorite(e));}
 public void toggleVisible(Entry e){boolean show=!visible(e);setMember("hidden_apps",e.pkg,!show);setMember("shown_apps",e.pkg,show);}
 public void setVisible(Entry e,boolean show){setMember("hidden_apps",e.pkg,!show);setMember("shown_apps",e.pkg,show);}
 public List<Entry> visible(int category){List<Entry> result=new ArrayList<>();for(Entry e:all())if(visible(e)&&(category==0||category==1&&favorite(e)||category==2&&category(e).equals("TV y video")||category==3&&category(e).equals("Herramientas")))result.add(e);return result;}
 public boolean move(Entry e,int delta){List<Entry> ordered=all();int i=-1;for(int n=0;n<ordered.size();n++)if(ordered.get(n).pkg.equals(e.pkg))i=n;int target=i+delta;if(i<0||target<0||target>=ordered.size())return false;Collections.swap(ordered,i,target);StringBuilder s=new StringBuilder();for(Entry a:ordered){if(s.length()>0)s.append('|');s.append(a.pkg);}prefs.edit().putString("app_order",s.toString()).apply();return true;}
 public boolean moveVisible(Entry e,int delta){
  List<Entry> ordered=all();List<Integer> positions=new ArrayList<>();
  for(int i=0;i<ordered.size();i++)if(visible(ordered.get(i)))positions.add(i);
  int visibleIndex=-1;
  for(int i=0;i<positions.size();i++)if(ordered.get(positions.get(i)).pkg.equals(e.pkg)){visibleIndex=i;break;}
  int targetIndex=visibleIndex+delta;
  if(visibleIndex<0||targetIndex<0||targetIndex>=positions.size())return false;
  Collections.swap(ordered,positions.get(visibleIndex),positions.get(targetIndex));
  StringBuilder s=new StringBuilder();for(Entry a:ordered){if(s.length()>0)s.append('|');s.append(a.pkg);}
  prefs.edit().putString("app_order",s.toString()).apply();return true;
 }
 public boolean moveVisibleToEnd(Entry e){
  List<Entry> ordered=all();int source=-1,lastVisible=-1;
  for(int i=0;i<ordered.size();i++){if(ordered.get(i).pkg.equals(e.pkg))source=i;if(visible(ordered.get(i)))lastVisible=i;}
  if(source<0||lastVisible<0||source==lastVisible)return false;
  Entry moved=ordered.remove(source);if(source<lastVisible)lastVisible--;ordered.add(lastVisible+1,moved);
  StringBuilder s=new StringBuilder();for(Entry a:ordered){if(s.length()>0)s.append('|');s.append(a.pkg);}
  prefs.edit().putString("app_order",s.toString()).apply();return true;
 }
 public void resetOrder(){prefs.edit().remove("app_order").apply();}
 public String editOrder(){return prefs.getString("app_order","");}
 public Set<String> editHidden(){return new HashSet<>(prefs.getStringSet("hidden_apps",Collections.emptySet()));}
 public Set<String> editShown(){return new HashSet<>(prefs.getStringSet("shown_apps",Collections.emptySet()));}
 public void restoreEditState(String order,Set<String> hidden,Set<String> shown){
  SharedPreferences.Editor e=prefs.edit();
  if(order==null||order.isEmpty())e.remove("app_order");else e.putString("app_order",order);
  if(hidden==null||hidden.isEmpty())e.remove("hidden_apps");else e.putStringSet("hidden_apps",new HashSet<>(hidden));
  if(shown==null||shown.isEmpty())e.remove("shown_apps");else e.putStringSet("shown_apps",new HashSet<>(shown));
  e.apply();
 }
 public void preloadIcons(List<Entry> list){for(Entry e:list)icon(e);}
 public Drawable icon(Entry e){Drawable.ConstantState state;synchronized(icons){state=icons.get(e.pkg);}if(state!=null)return state.newDrawable(context.getResources()).mutate();try{Drawable d=context.getPackageManager().getApplicationIcon(e.pkg);state=d.getConstantState();if(state!=null)synchronized(icons){icons.put(e.pkg,state);}return d;}catch(PackageManager.NameNotFoundException ignored){return null;}}
}
