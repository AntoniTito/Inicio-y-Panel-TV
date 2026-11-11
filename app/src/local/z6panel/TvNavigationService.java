package local.z6panel;

import android.accessibilityservice.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.*;
import android.os.*;
import android.view.*;
import android.view.accessibility.*;
import android.widget.*;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;

/** Optional UI capability, isolated from PanelService and idle unless a TV job is active. */
public final class TvNavigationService extends AccessibilityService {
 static TvNavigationService active;
 interface Result<T>{void done(T result);}
 static final class Selector {
  String id="",text="",description="",type="",hint="";boolean nativeRow;
  String label(){return !text.isEmpty()?text:!description.isEmpty()?description:!hint.isEmpty()?hint:!id.isEmpty()?id.substring(id.lastIndexOf('/')+1):type;}
  String key(){return id+"|"+text+"|"+description+"|"+type+"|"+hint;}
  JSONObject json()throws JSONException{return new JSONObject().put("id",id).put("text",text).put("description",description).put("type",type).put("hint",hint);}
  static Selector parse(JSONObject o)throws JSONException{
   Selector s=new Selector();s.id=o.optString("id");s.text=o.optString("text");s.description=o.optString("description");s.type=o.optString("type");s.hint=o.optString("hint");
   if(s.id.length()>250||s.type.length()>200||s.text.length()>160||s.description.length()>160||s.hint.length()>160||s.type.isEmpty())throw new JSONException("Invalid control");
   if(s.id.isEmpty()&&s.text.isEmpty()&&s.description.isEmpty()&&s.hint.isEmpty()&&!videoType(s.type))throw new JSONException("Control has no stable identity");
   return s;
  }
 }
 static final class Snapshot {
  final ArrayList<Selector> controls=new ArrayList<>(),markers=new ArrayList<>();
  String activity="";boolean scrollable;
  int matches(Selector s){int n=0;for(Selector c:controls)if(c.key().equals(s.key()))n++;return n;}
 }
 private final Handler ui=new Handler(Looper.getMainLooper());
 private final ThreadPoolExecutor worker=new ThreadPoolExecutor(1,1,20,TimeUnit.SECONDS,new ArrayBlockingQueue<Runnable>(1));
 private String target="",lastActivity="";private Snapshot lastSnapshot;
 private volatile boolean remoteInput;
 private volatile Selector lastConfirmed;private volatile String actionFailure="",remoteState="";
 private boolean unavailable(String reason){actionFailure=reason;return false;}
 String failureMessage(){if(actionFailure.equals("focus_mode"))return "El control auxiliar no está disponible; revísalo en Equipo → Diagnóstico";if(actionFailure.equals("foreground")||actionFailure.contains("window"))return "La app dejó de estar disponible en pantalla";if(actionFailure.startsWith("matches:"))return "El menú cambió; vuelve a elegir el control";return "La app no respondió a esa selección; puedes continuar manualmente";}
 private volatile long epoch;
 private boolean busy,retry,destroyed;private int queries;private Runnable pendingAction;
 private LinearLayout overlay,rows;private ScrollView scroll;private TextView heading,hint;
 private final ArrayList<View> rowViews=new ArrayList<>();private final ArrayList<Runnable> rowActions=new ArrayList<>();
 private final ArrayList<String> rowIds=new ArrayList<>();private int selection,painted=-1;
 private String rendered="";private final Set<Integer> overlayKeys=new HashSet<>();
 private final Runnable changed=()->{if(TvRecovery.instance!=null&&!target.isEmpty())TvRecovery.instance.navigationChanged();};
 @Override protected void onServiceConnected(){active=this;worker.allowCoreThreadTimeOut(true);end();TvRecoveryActivity.refresh();TvRecovery.readyChanged();}
 @Override public void onAccessibilityEvent(AccessibilityEvent e){
  if(target.isEmpty()||e==null||!target.contentEquals(e.getPackageName()==null?"":e.getPackageName()))return;
  if(e.getEventType()==AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED&&e.getClassName()!=null){String name=e.getClassName().toString();try{if(target.equals(getPackageManager().getActivityInfo(new ComponentName(target,name),0).packageName))lastActivity=name;}catch(android.content.pm.PackageManager.NameNotFoundException ignored){}}
  ui.removeCallbacks(changed);ui.postDelayed(changed,150);
 }
 @Override public void onInterrupt(){if(TvRecovery.instance!=null)TvRecovery.instance.cancel("Ayuda de navegación interrumpida");end();}
 @Override public void onDestroy(){destroyed=true;if(TvRecovery.instance!=null)TvRecovery.instance.cancel("La ayuda de navegación se desconectó");end();worker.shutdownNow();active=null;TvRecoveryActivity.refresh();super.onDestroy();}
 void begin(String pkg){end();target=pkg;lastActivity="";AccessibilityServiceInfo info=getServiceInfo();if(info==null)return;info.packageNames=new String[]{pkg};info.eventTypes=AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED|AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED|AccessibilityEvent.TYPE_VIEW_SCROLLED;info.notificationTimeout=120;info.flags=AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS|AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;setServiceInfo(info);}
 void end(){epoch++;lastConfirmed=null;lastSnapshot=null;pendingAction=null;retry=false;target="";lastActivity="";ui.removeCallbacks(changed);hideGuide();AccessibilityServiceInfo info=getServiceInfo();if(info!=null){info.packageNames=new String[]{"local.z6panel.navigation.idle"};info.eventTypes=0;info.flags=0;setServiceInfo(info);}}
 String activity(){return lastActivity;}
 private static String norm(CharSequence value){if(value==null)return "";String s=value.toString().trim().replaceAll("\\s+"," ");return s.length()>160?s.substring(0,160):s;}
 static boolean videoType(String type){return type.endsWith("SurfaceView")||type.endsWith("TextureView")||type.endsWith("VideoView")||type.endsWith("PlayerView");}
 private static boolean readable(AccessibilityNodeInfo n,String pkg){return n!=null&&(n.getPackageName()==null||pkg.contentEquals(n.getPackageName()))&&!n.isPassword()&&!n.isEditable()&&!String.valueOf(n.getClassName()).contains("EditText");}
 private static String childHint(AccessibilityNodeInfo node,String pkg,int depth){
  if(depth==0)return "";
  String numeric="";
  for(int i=0;i<Math.min(12,node.getChildCount());i++){
   AccessibilityNodeInfo child=node.getChild(i);if(child==null)continue;
   try{if(!readable(child,pkg))continue;String text=norm(child.getText());if(text.isEmpty())text=norm(child.getContentDescription());if(text.isEmpty())text=childHint(child,pkg,depth-1);if(!text.isEmpty()){if(!text.matches("[0-9]+"))return text;if(numeric.isEmpty())numeric=text;}}finally{child.recycle();}
  }return numeric;
 }
 private static Selector selector(AccessibilityNodeInfo n,String pkg){
  if(!readable(n,pkg)||!n.isVisibleToUser()||!n.isEnabled())return null;
  Selector s=new Selector();s.id=n.getViewIdResourceName()==null?"":n.getViewIdResourceName();s.text=norm(n.getText());s.description=norm(n.getContentDescription());s.type=norm(n.getClassName());
  if(s.text.isEmpty()&&s.description.isEmpty()&&!videoType(s.type))s.hint=childHint(n,pkg,3);
  try{return Selector.parse(s.json());}catch(JSONException e){return null;}
 }
 private AccessibilityNodeInfo root(String pkg,long token){
  if(token!=epoch||pkg.isEmpty()||destroyed)return null;
  List<AccessibilityWindowInfo> windows=getWindows();
  try{
   for(AccessibilityWindowInfo w:windows)if(w.isActive()&&w.getType()==AccessibilityWindowInfo.TYPE_SYSTEM)return null;
   for(AccessibilityWindowInfo w:windows){
    if(w.getType()!=AccessibilityWindowInfo.TYPE_APPLICATION)continue;
    AccessibilityNodeInfo r=w.getRoot();if(r==null)continue;
    if(pkg.contentEquals(r.getPackageName()==null?"":r.getPackageName()))return r;
    r.recycle();return null; // Never inspect a selected app hidden behind another application.
   }
  }finally{for(AccessibilityWindowInfo w:windows)w.recycle();}
  AccessibilityNodeInfo r=getRootInActiveWindow();
  if(r!=null&&!pkg.contentEquals(r.getPackageName()==null?"":r.getPackageName())){r.recycle();r=null;}
  return r;
 }
 private interface Visit{void node(AccessibilityNodeInfo n);}
 private void walk(AccessibilityNodeInfo root,String pkg,long token,Visit visit){
  ArrayDeque<AccessibilityNodeInfo> stack=new ArrayDeque<>();stack.push(root);int count=0;
  try{while(!stack.isEmpty()){
   AccessibilityNodeInfo n=stack.pop();try{
    if(token!=epoch||Thread.currentThread().isInterrupted())return;
    if(++count>1800)throw new IllegalStateException("Demasiados controles; usa un menú más concreto");
    if(!readable(n,pkg))continue;
    visit.node(n);
    for(int i=n.getChildCount()-1;i>=0;i--){AccessibilityNodeInfo child=n.getChild(i);if(child!=null)stack.push(child);}
   }finally{n.recycle();}
  }}finally{while(!stack.isEmpty())stack.pop().recycle();}
 }
 private <T> void query(Callable<T> call,Result<T> result){
  query(call,result,false);
 }
 private <T> void query(Callable<T> call,Result<T> result,boolean priority){
  if(target.isEmpty()||destroyed){result.done(null);return;}
  if(busy){if(priority){if(pendingAction==null)pendingAction=()->query(call,result,true);else result.done(null);}else retry=true;return;}
  busy=true;queries++;final long token=epoch;
  try{worker.execute(()->{
   T answer=null;try{if(token==epoch)answer=call.call();}catch(Exception e){android.util.Log.w("Z6TvNavigation","UI query unavailable: "+e.getClass().getSimpleName());}
   final T value=answer;ui.post(()->{busy=false;if(token==epoch&&!destroyed&&(priority||pendingAction==null))result.done(value);if(pendingAction!=null&&!busy){Runnable action=pendingAction;pendingAction=null;action.run();}else if(retry&&!busy){retry=false;ui.post(changed);}});
  });}catch(RejectedExecutionException e){busy=false;result.done(null);}
 }
 void snapshot(Result<Snapshot> done){
  final String pkg=target,activity=lastActivity;final long token=epoch;
  query(()->{
   AccessibilityNodeInfo root=root(pkg,token);if(root==null)return null;
   Snapshot snapshot=new Snapshot();snapshot.activity=activity;
   walk(root,pkg,token,n->{
    if(!n.isVisibleToUser())return;
    if(n.isScrollable())snapshot.scrollable=true;
    Selector s=selector(n,pkg);if(s==null)return;
    if(activatable(n)){s.nativeRow=isListItem(n,pkg);snapshot.controls.add(s);}
    if(videoType(s.type)||!s.text.isEmpty()||!s.description.isEmpty())snapshot.markers.add(s);
   });return snapshot;
  },snapshot->{lastSnapshot=snapshot;done.done(snapshot);});
 }
 void click(Selector wanted,Result<Boolean> done){
  final String pkg=target;final long token=epoch;
  actionFailure="";remoteState="";query(()->{
   if(!pkg.equals(SystemBridge.read("FOREGROUND_APP",1200)))return unavailable("foreground");
   if(!"OK".equals(SystemBridge.call("TV_FOCUS:"+pkg)))return unavailable("focus_mode");
   AccessibilityNodeInfo root=root(pkg,token);if(root==null)return unavailable("window");
   ArrayList<AccessibilityNodeInfo> matches=new ArrayList<>();
   try{
    walk(root,pkg,token,n->{Selector s=selector(n,pkg);if(s!=null&&s.key().equals(wanted.key())&&activatable(n))matches.add(AccessibilityNodeInfo.obtain(n));});
    if(token!=epoch||matches.size()!=1)return unavailable("matches:"+matches.size());
    AccessibilityNodeInfo node=matches.get(0);
    if(isListItem(node,pkg))return remoteList(node,wanted,pkg,token);
    return node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
   }finally{for(AccessibilityNodeInfo n:matches)n.recycle();}
  },ok->done.done(Boolean.TRUE.equals(ok)),true);
 }
 static boolean injectedKey(KeyEvent e){int k=e.getKeyCode();return active!=null&&active.remoteInput&&e.getDeviceId()==-1&&(k==19||k==20||k==23);}
 private boolean inputWindow(boolean release){
  CountDownLatch latch=new CountDownLatch(1);final boolean[] ok={false};ui.post(()->{try{if(overlay!=null){WindowManager.LayoutParams p=(WindowManager.LayoutParams)overlay.getLayoutParams();if(release){p.flags|=WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;overlay.setVisibility(View.INVISIBLE);}else{p.flags=WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL|(Build.VERSION.SDK_INT<30?WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE:0);overlay.setVisibility(View.VISIBLE);}((WindowManager)getSystemService(WINDOW_SERVICE)).updateViewLayout(overlay,p);if(!release&&Build.VERSION.SDK_INT>=30)overlay.requestFocus();}ok[0]=true;}finally{latch.countDown();}});
  try{return latch.await(1200,TimeUnit.MILLISECONDS)&&ok[0];}catch(InterruptedException e){Thread.currentThread().interrupt();return false;}
 }
 private boolean remoteList(AccessibilityNodeInfo node,Selector wanted,String pkg,long token)throws Exception{
  AccessibilityNodeInfo parent=node.getParent();if(parent==null)return false;
  final String listId=parent.getViewIdResourceName()==null?"":parent.getViewIdResourceName(),listType=String.valueOf(parent.getClassName());
  final int wantedRow=node.getCollectionItemInfo()==null?-1:node.getCollectionItemInfo().getRowIndex();
  remoteInput=true;
  try{
   if(!inputWindow(true)||token!=epoch)return unavailable("overlay");
   if(!parent.isFocused()&&!parent.performAction(AccessibilityNodeInfo.ACTION_FOCUS))return unavailable("list_focus");
   long until=SystemClock.elapsedRealtime()+10000;String previous="";int stalled=0,direction=-1;boolean reversed=false;
   for(int move=0;move<80&&SystemClock.elapsedRealtime()<until&&token==epoch;move++){
    Thread.sleep(90);AccessibilityNodeInfo r=root(pkg,token);if(r==null)return unavailable("list_window");ArrayList<AccessibilityNodeInfo> lists=new ArrayList<>();
    try{
     walk(r,pkg,token,n->{if(n.isVisibleToUser()&&listType.equals(String.valueOf(n.getClassName()))&&listId.equals(n.getViewIdResourceName()==null?"":n.getViewIdResourceName())&&hasRowType(n,wanted.type))lists.add(AccessibilityNodeInfo.obtain(n));});
     if(lists.size()!=1)return unavailable("lists:"+lists.size());
     AccessibilityNodeInfo list=lists.get(0);Selector selected=null;int selectedIndex=-1,targetIndex=-1,count=0,selectedRow=-1;
     for(int i=0;i<list.getChildCount();i++){AccessibilityNodeInfo item=list.getChild(i);if(item==null)continue;try{Selector v=selector(item,pkg);if(v==null)continue;if(item.isSelected()){selected=v;selectedIndex=i;count++;selectedRow=item.getCollectionItemInfo()==null?-1:item.getCollectionItemInfo().getRowIndex();}if(v.key().equals(wanted.key()))targetIndex=i;}finally{item.recycle();}}
     remoteState="move="+move+", selected="+(selected==null?"none":selected.label())+", count="+count+", row="+selectedRow+", wanted="+wantedRow+", focused="+list.isFocused();
     if(count!=1||selected==null){if(++stalled>6)return unavailable("no_selected");continue;}
     if(selected.key().equals(wanted.key())){
      // A compact/touch variant may expose an item click even when its key
      // listener does not activate the row. Both paths require verified native
      // focus and the exact selected row; never click an unselected list item.
      AccessibilityNodeInfo chosen=list.getChild(selectedIndex);boolean clicked=false;
      if(chosen!=null)try{Selector checked=selector(chosen,pkg);clicked=token==epoch&&checked!=null&&checked.key().equals(wanted.key())&&chosen.performAction(AccessibilityNodeInfo.ACTION_CLICK);}finally{chosen.recycle();}
      boolean ok=clicked||token==epoch&&"OK".equals(SystemBridge.call("TV_NAV:"+pkg+":23"));if(ok&&token==epoch)lastConfirmed=wanted;return ok;
     }
     if(selected.key().equals(previous)){if(++stalled>3){if(reversed)return unavailable("stalled");direction=-direction;reversed=true;stalled=0;}}else{previous=selected.key();stalled=0;}
     int key=wantedRow>=0&&selectedRow>=0?(wantedRow<selectedRow?19:20):targetIndex>=0?(targetIndex<selectedIndex?19:20):direction<0?19:20;
     if(token!=epoch||!"OK".equals(SystemBridge.call("TV_NAV:"+pkg+":"+key)))return unavailable("key");
    }finally{for(AccessibilityNodeInfo item:lists)item.recycle();}
   }
   return unavailable("list_timeout");
  }finally{parent.recycle();inputWindow(false);remoteInput=false;}
 }
 private static boolean hasRowType(AccessibilityNodeInfo list,String type){for(int i=0;i<Math.min(64,list.getChildCount());i++){AccessibilityNodeInfo n=list.getChild(i);if(n==null)continue;try{if(type.equals(String.valueOf(n.getClassName())))return true;}finally{n.recycle();}}return false;}
 private static boolean activatable(AccessibilityNodeInfo n){return n.isClickable()||(n.getActions()&AccessibilityNodeInfo.ACTION_CLICK)!=0||!n.isScrollable()&&n.isFocusable()&&(n.getActions()&AccessibilityNodeInfo.ACTION_FOCUS)!=0;}
 private static boolean isListItem(AccessibilityNodeInfo node,String pkg){
  AccessibilityNodeInfo parent=node.getParent();
  if(parent==null)return false;
  try{String type=String.valueOf(parent.getClassName());return readable(parent,pkg)&&(type.endsWith("ListView")||type.endsWith("GridView"));}finally{parent.recycle();}
 }
 void scroll(boolean forward,Result<Boolean> done){
  final String pkg=target;final long token=epoch;
  query(()->{
   if(!pkg.equals(SystemBridge.read("FOREGROUND_APP",1200)))return false;
   AccessibilityNodeInfo r=root(pkg,token);if(r==null)return false;ArrayList<AccessibilityNodeInfo> candidates=new ArrayList<>();
   try{walk(r,pkg,token,n->{if(n.isVisibleToUser()&&n.isScrollable())candidates.add(AccessibilityNodeInfo.obtain(n));});
    for(AccessibilityNodeInfo n:candidates)if(token==epoch&&n.performAction(forward?AccessibilityNodeInfo.ACTION_SCROLL_FORWARD:AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD))return true;return false;
   }finally{for(AccessibilityNodeInfo n:candidates)n.recycle();}
  },ok->done.done(Boolean.TRUE.equals(ok)),true);
 }
 void back(Result<Boolean> done){final String pkg=target;final long token=epoch;query(()->pkg.equals(SystemBridge.read("FOREGROUND_APP",1200)),ok->{if(Boolean.TRUE.equals(ok)&&token==epoch)done.done(performGlobalAction(GLOBAL_ACTION_BACK));else done.done(false);},true);}
 void isTargetForeground(Result<Boolean> done){final String pkg=target;query(()->pkg.equals(SystemBridge.read("FOREGROUND_APP",1200)),done);}
 JSONObject endpoint(Snapshot snapshot,String channel,String source){
  try{
   JSONObject endpoint=new JSONObject().put("activity",snapshot.activity);
   Selector marker=null;
   for(Selector s:snapshot.markers)if(videoType(s.type)){marker=s;break;}
   if(marker==null)for(Selector s:snapshot.markers)if(!channel.isEmpty()&&(channelLabel(s.text,channel)||channelLabel(s.description,channel))){marker=s;break;}
   if(marker!=null)endpoint.put("marker",marker.json());
   if(marker==null&&(snapshot.activity.isEmpty()||snapshot.activity.equals(source))){
    Selector confirmed=lastConfirmed;
    if(confirmed==null||!channelLabel(confirmed.label(),channel)||snapshot.matches(confirmed)!=0)return null;
    endpoint.put("activity","").put("confirmedControl",confirmed.json());
   }
   return endpoint;
  }catch(JSONException e){return null;}
 }
 private static boolean channelLabel(String text,String channel){String a=text.toLowerCase(Locale.ROOT),b=channel.toLowerCase(Locale.ROOT);return a.equals(b)||a.startsWith(b+" · ")||a.startsWith(b+" - ");}
 boolean matchesEndpoint(Snapshot s,JSONObject endpoint){
  String expected=endpoint.optString("activity");if(!expected.isEmpty()&&!expected.equals(s.activity))return false;
  JSONObject confirmed=endpoint.optJSONObject("confirmedControl");if(confirmed!=null){try{Selector control=Selector.parse(confirmed);if(lastConfirmed==null||!lastConfirmed.label().equalsIgnoreCase(control.label()))return false;for(Selector visible:s.controls)if(visible.label().equalsIgnoreCase(control.label()))return false;return true;}catch(JSONException e){return false;}}
  JSONObject marker=endpoint.optJSONObject("marker");if(marker!=null){try{String key=Selector.parse(marker).key();for(Selector m:s.markers)if(m.key().equals(key))return true;return false;}catch(JSONException e){return false;}}
  return !expected.isEmpty();
 }
 private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
 private TextView text(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);return t;}
 private android.graphics.drawable.Drawable background(boolean selected){GradientDrawable d=new GradientDrawable();d.setColor(selected?0x5029333C:0x20121A20);d.setCornerRadius(dp(7));if(selected)d.setStroke(dp(2),0xFFE5F0F3);return d;}
 private Drawable rowBackground(){StateListDrawable d=new StateListDrawable();d.addState(new int[]{android.R.attr.state_selected},background(true));d.addState(new int[]{},background(false));return d;}
 private void ensureGuide(){
  if(overlay!=null)return;
  overlay=new LinearLayout(this);overlay.setOrientation(1);overlay.setPadding(dp(12),dp(10),dp(12),dp(10));GradientDrawable bg=new GradientDrawable();bg.setColor(0xF0121720);bg.setCornerRadius(dp(12));overlay.setBackground(bg);
  heading=text("Configurar canal",18,Color.WHITE);overlay.addView(heading);
  hint=text("Elige los controles de la app hasta llegar a tu canal. Atrás cancela.",11,0xFFB8BFC5);overlay.addView(hint);
  scroll=new ScrollView(this);rows=new LinearLayout(this);rows.setOrientation(1);scroll.addView(rows);overlay.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
  if(Build.VERSION.SDK_INT>=30){overlay.setFocusableInTouchMode(true);overlay.setDescendantFocusability(android.view.ViewGroup.FOCUS_BLOCK_DESCENDANTS);overlay.setOnKeyListener((v,k,e)->TvRecovery.handleKey(e));}
  WindowManager.LayoutParams lp=new WindowManager.LayoutParams(Math.min(dp(390),getResources().getDisplayMetrics().widthPixels*42/100),getResources().getDisplayMetrics().heightPixels*90/100,WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL|(Build.VERSION.SDK_INT<30?WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE:0),PixelFormat.TRANSLUCENT);
  lp.gravity=Gravity.RIGHT|Gravity.CENTER_VERTICAL;lp.x=dp(10);
  try{((WindowManager)getSystemService(WINDOW_SERVICE)).addView(overlay,lp);if(Build.VERSION.SDK_INT>=30)overlay.requestFocus();}
  catch(RuntimeException e){hideGuide();throw e;}
 }
 private void addRow(String id,String label,Runnable action){
  final int i=rowViews.size();TextView cell=text(label,13,Color.WHITE);cell.setGravity(Gravity.CENTER_VERTICAL);cell.setMinHeight(dp(42));cell.setPadding(dp(9),dp(5),dp(9),dp(5));cell.setMaxLines(2);cell.setBackground(rowBackground());LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(4);rows.addView(cell,p);rowViews.add(cell);rowActions.add(action);rowIds.add(id);cell.setOnClickListener(v->{selection=i;paint();action.run();});
 }
 void showGuide(Snapshot snapshot){
  StringBuilder signature=new StringBuilder(snapshot.activity);for(Selector s:snapshot.controls)signature.append(s.key());signature.append(snapshot.scrollable);
  if(signature.toString().equals(rendered)&&overlay!=null)return;
  String previous=selection>=0&&selection<rowIds.size()?rowIds.get(selection):"";ensureGuide();rendered=signature.toString();rows.removeAllViews();rowViews.clear();rowActions.clear();rowIds.clear();painted=-1;
  heading.setText("Enseñar el recorrido");hint.setText("Elige Televisión, Perú y un canal. Cuando se vea, selecciona «Ya se ve el canal». Atrás cancela.");
  addRow("done","✓ Ya se ve el canal · probar desde cero",()->TvRecovery.get(this).taught());
  if(snapshot.scrollable){addRow("scroll_next","↓ Mostrar más opciones de la app",()->TvRecovery.get(this).guideScroll(true));addRow("scroll_previous","↑ Opciones anteriores",()->TvRecovery.get(this).guideScroll(false));}
  LinkedHashMap<String,Selector> unique=new LinkedHashMap<>();Set<String> ambiguous=new HashSet<>();for(Selector s:snapshot.controls){if(unique.put(s.key(),s)!=null)ambiguous.add(s.key());}
  for(Selector s:unique.values())if(!ambiguous.contains(s.key()))addRow(s.key(),s.label(),()->TvRecovery.get(this).guideClick(s));
  if(unique.isEmpty())addRow("empty","No hay controles accesibles todavía",()->TvRecovery.get(this).navigationChanged());
  addRow("back_app","Volver dentro de la app",()->TvRecovery.get(this).guideBack());
  addRow("cancel","Cancelar configuración",()->TvRecovery.get(this).cancel("Configuración cancelada; la app sigue abierta"));
  selection=Math.max(0,rowIds.indexOf(previous));paint();
 }
 void showConfirmation(String channel){
  ensureGuide();rendered="confirm";rows.removeAllViews();rowViews.clear();rowActions.clear();rowIds.clear();painted=-1;selection=0;
  heading.setText("¿Es el canal elegido?");hint.setText("Comprueba imagen y sonido de «"+channel+"». Guardar no activa todavía el inicio automático.");
  addRow("confirm_yes","Sí · guardar este recorrido",()->TvRecovery.get(this).confirmTest(true));
  addRow("confirm_no","No · dejarlo sin guardar",()->TvRecovery.get(this).confirmTest(false));paint();
 }
 private void paint(){
  if(painted>=0&&painted<rowViews.size())rowViews.get(painted).setSelected(false);
  if(selection>=0&&selection<rowViews.size()){
   View v=rowViews.get(selection);v.setSelected(true);painted=selection;
   scroll.post(()->{if(overlay!=null&&v.getParent()==rows){if(v.getTop()<scroll.getScrollY())scroll.scrollTo(0,v.getTop());else if(v.getBottom()>scroll.getScrollY()+scroll.getHeight())scroll.scrollTo(0,v.getBottom()-scroll.getHeight());}});
  }
 }

 boolean guideKey(KeyEvent event){
  if(overlay==null)return false;int key=event.getKeyCode();boolean down=event.getAction()==KeyEvent.ACTION_DOWN;
  if(!Arrays.asList(19,20,21,22,23,66).contains(key))return false;
  if(down){overlayKeys.add(key);if(key==19||key==20){selection=Math.max(0,Math.min(rowViews.size()-1,selection+(key==19?-1:1)));paint();}}
  else if(overlayKeys.remove(key)&&(key==23||key==66)&&selection>=0&&selection<rowActions.size())rowActions.get(selection).run();
  return true;
 }
 void hideGuide(){if(overlay!=null){try{((WindowManager)getSystemService(WINDOW_SERVICE)).removeView(overlay);}catch(IllegalArgumentException ignored){}overlay.removeAllViews();overlay=null;}heading=hint=null;rows=null;scroll=null;rowViews.clear();rowActions.clear();rowIds.clear();painted=-1;overlayKeys.clear();rendered="";}
 JSONObject inspect()throws JSONException{JSONObject out=new JSONObject().put("active",!target.isEmpty()).put("target",target).put("overlay",overlay!=null).put("selection",selection).put("workerBusy",busy).put("queries",queries).put("actionFailure",actionFailure).put("remoteState",remoteState);JSONArray a=new JSONArray();for(int i=0;i<rowIds.size();i++)a.put(new JSONObject().put("id",rowIds.get(i)).put("label",((TextView)rowViews.get(i)).getText()));JSONArray seen=new JSONArray();if(lastSnapshot!=null)for(Selector control:lastSnapshot.controls){if(seen.length()==80)break;seen.put(control.json());}return out.put("controls",a).put("observedControls",seen);}
 boolean shellChoose(String id){int i=rowIds.indexOf(id);if(i<0)return false;selection=i;rowActions.get(i).run();return true;}
}
