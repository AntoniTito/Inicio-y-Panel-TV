package local.z6panel;

import android.accessibilityservice.AccessibilityService;
import android.content.*;
import android.content.pm.*;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.net.*;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.view.accessibility.AccessibilityEvent;
import android.widget.*;
import java.util.*;
import java.util.concurrent.*;

/** Reuses the accessibility overlay: the foreground activity keeps its window/audio focus. */
public class PanelService extends AccessibilityService {
 public static PanelService active;
 private WindowManager wm;
 private LinearLayout volumeHud;
 private TextView volumeLabel;
 private ProgressBar volumeBar;
 private WindowManager.LayoutParams volumeParams;
 private final Runnable dismissVolume=()->hideVolumeHud();
 private LinearLayout panel,row,quickRow,sideRows;
 private HorizontalScrollView horizontal,quickHorizontal;
 private ScrollView vertical;
 private TextView title,subtitle,editHint;
 private LinearLayout clockPanel;
 private TextClock panelClock,panelDate;
 private View addCard;
 private SharedPreferences prefs;
 private AppCatalog catalog;
 private BluetoothControls bluetooth;
 private final Handler ui=new Handler(Looper.getMainLooper());
 private final Runnable recoverLostBridge=()->recoverBridgeIfLost();
 private final ExecutorService loader=Executors.newSingleThreadExecutor();
 private final ExecutorService catalogWorker=Executors.newSingleThreadExecutor(),bootstrapWorker=Executors.newSingleThreadExecutor();
 private final ExecutorService volumeWorker=Executors.newSingleThreadExecutor();
 private final ArrayDeque<int[]> volumeRequests=new ArrayDeque<>();
 private boolean volumeDraining;
 private boolean audioLoaded,audioRefreshPending,cachedMuted,cachedVolumeFixed;
 private int cachedVolume,cachedVolumeMax;
 private long lastAudioCheck;
 private final BroadcastReceiver audioChanges=new BroadcastReceiver(){public void onReceive(Context c,Intent i){refreshAudioState();}};
 private boolean catalogDirty=true,catalogScanning=false;
 private final ArrayList<TextView> quickDetails=new ArrayList<>();
 private int lastPanelColor=Integer.MIN_VALUE;
 static class StableText extends TextView {
  StableText(Context c){super(c);}
  @Override public void setText(CharSequence value,BufferType type){if(value==null||getText()==null||!android.text.TextUtils.equals(getText(),value))super.setText(value,type);}
 }
 private static class Screen {
  final ArrayList<View> children=new ArrayList<>(),rows=new ArrayList<>();
  final ArrayList<TextView> labels=new ArrayList<>(),details=new ArrayList<>();
  final ArrayList<String> ids=new ArrayList<>();
  TextView title,subtitle;ScrollView scroll;LinearLayout items;
 }
 private final LinkedHashMap<String,Screen> screens=new LinkedHashMap<String,Screen>(8,.75f,true){protected boolean removeEldestEntry(Map.Entry<String,Screen> e){return size()>8;}};
 private Screen currentScreen;
 private final Runnable trimRecentTasks=()->loader.execute(()->SystemBridge.call("RECENTS_TRIM"));
 private final PanelKeys keys=new PanelKeys();
 private final ArrayList<View> cards=new ArrayList<>(),sideViews=new ArrayList<>();
 private final ArrayList<Action> actions=new ArrayList<>();
 private final ArrayList<Runnable> quickActions=new ArrayList<>();
 private List<AppCatalog.Entry> visible=new ArrayList<>();
 private final Deque<Page> pages=new ArrayDeque<>();
 private int selected=0,generation=0,scanVersion=0,scrollGeneration=0;
 private boolean restoringAppScroll=false;
 private boolean suppressAutoScroll=false;
 private static class AppScroll extends HorizontalScrollView {
  boolean frozen;
  boolean holdLayout;
  int heldScroll;
  AppScroll(Context context){super(context);}
  public void computeScroll(){if(frozen)return;super.computeScroll();}
  void holdCurrentPosition(){heldScroll=getScrollX();holdLayout=true;scrollTo(heldScroll,0);}
  void releasePosition(){holdLayout=false;}
  protected void onLayout(boolean changed,int left,int top,int right,int bottom){
   super.onLayout(changed,left,top,right,bottom);
   if(holdLayout)scrollTo(heldScroll,0);
  }
 }
 private boolean editMode=false,addSlot=false;
 private String editOrderSnapshot;private Set<String> editHiddenSnapshot,editShownSnapshot;
 private boolean localStarting=false;private int localAttempts=0,localGeneration=0;
 private boolean destroyed=false,loading=true,wifiScanPending=false;
 private boolean btShowPaired=true,btShowFound=false;
 private String managerFilter="Todas";
 private String foregroundPackage;
 private volatile boolean bridgeReady=false;
 private boolean bridgeChecking=false;
 private long lastHealthCheck=0;
 private int foregroundGeneration=0;
 private boolean foregroundInFlight,storageInFlight;
 private long lastForegroundCheck,lastStorageCheck;
 private String cachedStorage="Consultando espacio";
 private int cachedStoragePercent=-1;
 private boolean commandPending=false;
 private String bridgeStatus="Control desconectado";
 private final Runnable tickSettings=new Runnable(){public void run(){if(destroyed||panel==null)return;if(!pages.isEmpty()&&(pages.peek().name.equals("timer")||pages.peek().name.equals("system")))renderSide();else scheduleTimerTick();}};
 private static class Page {
  final String name,pkg;int selected=0;String selectedId;boolean interacted;Intent intent;String description;
  Page(String name,String pkg){this.name=name;this.pkg=pkg;}
 }
 private static class Action {
  final String id,label,detail;final Runnable run;final boolean device;Runnable less,more;
  Action(String label,String detail,Runnable run){this(label,detail,run,false);}
  Action(String label,String detail,Runnable run,boolean isDevice){this(label,label,detail,run,isDevice);}
  Action(String id,String label,String detail,Runnable run,boolean isDevice){this.id=id;this.label=label;this.detail=detail;this.run=run;this.device=isDevice;}
 }
 private final Runnable networkRefresh=()->{if(panel!=null&&!pages.isEmpty()&&pages.peek().name.equals("network"))render();};
 private final BroadcastReceiver networkChanges=new BroadcastReceiver(){public void onReceive(Context c,Intent i){if(android.net.wifi.WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(i.getAction())){wifiScanPending=false;if(panel!=null&&!pages.isEmpty()&&pages.peek().name.equals("wifiScan"))renderSide();return;}ui.removeCallbacks(networkRefresh);ui.postDelayed(networkRefresh,250);}};
 private final BroadcastReceiver changes=new BroadcastReceiver(){
  public void onReceive(Context c,Intent i){catalog.invalidate(i.getData()==null?null:i.getData().getSchemeSpecificPart());catalogDirty=true;refreshCatalog();}
 };
 protected void onServiceConnected(){
  active=this;wm=(WindowManager)getSystemService(WINDOW_SERVICE);prefs=getSharedPreferences("panel",0);catalog=new AppCatalog(this);
  PanelBackup.capture(this);
  try{DebuggingRecovery.restorePending(this);}catch(RuntimeException e){android.util.Log.w("Z6LocalAdb","Pending restore",e);}
  if(SystemBridge.remote!=null&&SystemBridge.remote.isBinderAlive())bridgeAttached();
  if(prefs.getInt("visual_revision",0)<4)prefs.edit().putBoolean("compact",true).putInt("opacity",80).putInt("visual_revision",4).apply();
  IntentFilter f=new IntentFilter();f.addAction(Intent.ACTION_PACKAGE_ADDED);f.addAction(Intent.ACTION_PACKAGE_REMOVED);f.addAction(Intent.ACTION_PACKAGE_CHANGED);f.addDataScheme("package");
  if(Build.VERSION.SDK_INT>=33)registerReceiver(changes,f,Context.RECEIVER_NOT_EXPORTED);else registerReceiver(changes,f);
  bluetooth=new BluetoothControls(this,()->{if(panel!=null&&!pages.isEmpty()&&(pages.peek().name.equals("bluetooth")||pages.peek().name.equals("btDevice")))renderSide();},this::onBluetoothPairingRequest);
  IntentFilter net=new IntentFilter(android.net.wifi.WifiManager.WIFI_STATE_CHANGED_ACTION);net.addAction(android.net.wifi.WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);net.addAction(ConnectivityManager.CONNECTIVITY_ACTION);net.addAction(android.net.wifi.WifiManager.NETWORK_STATE_CHANGED_ACTION);
  if(Build.VERSION.SDK_INT>=33)registerReceiver(networkChanges,net,Context.RECEIVER_EXPORTED);else registerReceiver(networkChanges,net);
  IntentFilter audioEvents=new IntentFilter("android.media.VOLUME_CHANGED_ACTION");audioEvents.addAction("android.media.STREAM_MUTE_CHANGED_ACTION");audioEvents.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
  if(Build.VERSION.SDK_INT>=33)registerReceiver(audioChanges,audioEvents,Context.RECEIVER_EXPORTED);else registerReceiver(audioChanges,audioEvents);
  refreshAudioState();
  refreshCatalog();
  storageSummary();
  prefs.edit().putBoolean("custom_volume",true).apply();
  ui.postDelayed(()->finishLocalControl(false,0),1000);
  if(prefs.getBoolean("local_adb_enabled",false))ui.postDelayed(()->startLocalControl(false),5000);
 }
 private void refreshCatalog(){
  if(destroyed)return;final int version=++scanVersion;loading=true;
  if(catalogScanning)return;if(!catalogDirty&&catalog.ready()){loading=false;return;}catalogScanning=true;catalogDirty=false;
  catalogWorker.execute(()->{
   List<AppCatalog.Entry> result;try{result=catalog.scan();catalog.preloadIcons(result);}catch(RuntimeException e){ui.post(()->{catalogScanning=false;catalogDirty=true;if(!destroyed){loading=false;message("No se pudo actualizar la lista de apps");}});return;}
   ui.post(()->{catalogScanning=false;if(destroyed)return;if(catalogDirty){refreshCatalog();return;}catalog.accept(result);loading=false;HomeActivity.catalogChanged();if(panel!=null)render();});
  });
 }
 public void onAccessibilityEvent(AccessibilityEvent e){
  if(destroyed||e==null||e.getEventType()!=AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED)return;
  CharSequence packageName=e.getPackageName();
  if(packageName==null)return;
  String pkg=packageName.toString();
  if(pkg.equals(getPackageName())||pkg.equals("android")||pkg.equals("com.android.systemui"))return;
  foregroundGeneration++;foregroundPackage=catalog.find(pkg)==null?null:pkg;if(foregroundPackage!=null)lastForegroundCheck=SystemClock.elapsedRealtime();
 }
 public void onInterrupt(){hide();}
 public void onDestroy(){
  destroyed=true;scanVersion++;ui.removeCallbacksAndMessages(null);loader.shutdownNow();catalogWorker.shutdownNow();bootstrapWorker.shutdownNow();volumeWorker.shutdownNow();synchronized(volumeRequests){volumeRequests.clear();}screens.clear();
  try{unregisterReceiver(changes);}catch(IllegalArgumentException ignored){}
  try{unregisterReceiver(networkChanges);}catch(IllegalArgumentException ignored){}
  try{unregisterReceiver(audioChanges);}catch(IllegalArgumentException ignored){}
  if(bluetooth!=null)bluetooth.close();hideVolumeHud();hide();keys.clear();active=null;super.onDestroy();
 }
 private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
 private GradientDrawable background(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
 private int panelColor(){int alpha=Math.max(60,Math.min(90,prefs.getInt("opacity",80)))*255/100;return alpha<<24;}
 private GradientDrawable focusBackground(boolean focused){GradientDrawable d=background(focused?0xBB242424:0x44000000,7);if(focused)d.setStroke(dp(1),0xFFEEEEEE);return d;}
 private TextView text(String value,int size,int color){
  TextView t=new StableText(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setGravity(Gravity.CENTER_VERTICAL);return t;
 }
 private void message(String msg){if(subtitle!=null&&panel!=null){subtitle.setVisibility(View.VISIBLE);subtitle.setText(msg);}else Toast.makeText(this,msg,Toast.LENGTH_SHORT).show();}
 private void onBluetoothPairingRequest(android.bluetooth.BluetoothDevice device){
  if(panel!=null)hide();
 }
 public void openPage(String name){if(panel==null)show();if(name!=null)push(new Page(name,null));refreshForeground();}
 public void onHomeVisible(){if(panel!=null&&pages.isEmpty())hide();}
 public void toggle(){if(panel!=null)hide();else if(!HomeActivity.visible)show();}
 private void show(){
  pages.clear();generation++;lastPanelColor=Integer.MIN_VALUE;panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);
  WindowManager.LayoutParams p=params(false);
  try{wm.addView(panel,p);renderBottom();refreshCatalog();refreshForeground();refreshAudioState();scheduleTimerTick();if(prefs.getBoolean("animate",false)){panel.setTranslationY(dp(30));panel.animate().translationY(0).setDuration(120).start();}}
  catch(RuntimeException e){hide();Toast.makeText(this,"No se pudo abrir el panel",Toast.LENGTH_SHORT).show();}
 }
 private WindowManager.LayoutParams params(boolean side){
  int width=side?Math.min(dp(360),getResources().getDisplayMetrics().widthPixels*38/100):WindowManager.LayoutParams.MATCH_PARENT;
  int chrome=104;if(side&&subtitle!=null&&subtitle.getText().length()>0)chrome=128;
  WindowManager.LayoutParams p=new WindowManager.LayoutParams(width,side?Math.min(getResources().getDisplayMetrics().heightPixels*90/100,dp(chrome+Math.min(8,Math.max(2,actions.size()-2))*44)):WindowManager.LayoutParams.WRAP_CONTENT,
   WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,PixelFormat.TRANSLUCENT);
  p.gravity=side?(Gravity.RIGHT|Gravity.CENTER_VERTICAL):Gravity.BOTTOM;p.x=side?dp(14):0;return p;
 }
 private void hide(){
  hideClockPanel();
  generation++;ui.removeCallbacks(tickSettings);pages.clear();cancelEditMode();if(bluetooth!=null)bluetooth.stopScan();
  if(panel!=null){panel.animate().cancel();try{wm.removeView(panel);}catch(IllegalArgumentException ignored){}panel.removeAllViews();panel=null;}
  currentScreen=null;cards.clear();sideViews.clear();actions.clear();quickDetails.clear();title=null;subtitle=null;panelClock=null;
 }
 private void render(){if(panel==null)return;if(pages.isEmpty())renderBottom();else renderSide();}
 private void base(boolean side){
  panel.removeAllViews();panel.setTranslationY(0);panel.setPadding(dp(side?16:22),dp(side?14:4),dp(side?16:22),dp(side?8:4));
  panel.setBackground(background(panelColor(),14));lastPanelColor=panelColor();if(!side)updatePanelLayout(params(false));
  title=text("",side?20:17,Color.WHITE);title.setSingleLine(true);title.setEllipsize(android.text.TextUtils.TruncateAt.END);panel.addView(title,new LinearLayout.LayoutParams(-1,dp(side?32:28)));
  subtitle=text("",11,0xFFBDC7CE);subtitle.setMaxLines(2);
  panel.addView(subtitle,new LinearLayout.LayoutParams(-1,side?-2:dp(24)));
 }
 private void renderBottom(){
  if(panel==null)return;generation++;currentScreen=null;PerfStats.fullRenders.incrementAndGet();
  int previousAppScroll=horizontal==null?0:horizontal.getScrollX();
  base(false);cards.clear();
  quickActions.clear();quickDetails.clear();visible=catalog.visible(0);selected=Math.max(0,selected);
  title.setVisibility(View.GONE);subtitle.setVisibility(View.GONE);subtitle.setText("");
  if(loading&&visible.isEmpty()){subtitle.setVisibility(View.VISIBLE);subtitle.setText("Cargando…");}
  boolean compact=true;
  LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);panel.addView(body,new LinearLayout.LayoutParams(-1,0,1));
  TextView quickTitle=text("ACCIONES",11,0xFFBFC5C8);quickTitle.setGravity(Gravity.CENTER_VERTICAL);quickTitle.setPadding(dp(4),0,0,0);body.addView(quickTitle,new LinearLayout.LayoutParams(-1,dp(16)));
  quickHorizontal=new HorizontalScrollView(this);quickHorizontal.setHorizontalScrollBarEnabled(false);quickHorizontal.setClipToPadding(false);quickRow=new LinearLayout(this);quickRow.setGravity(Gravity.CENTER_VERTICAL);quickRow.setPadding(0,0,dp(3),0);row=quickRow;quickHorizontal.addView(quickRow);body.addView(quickHorizontal,new LinearLayout.LayoutParams(-1,dp(38)));
  addQuickSection();
  LinearLayout appsHeader=new LinearLayout(this);appsHeader.setGravity(Gravity.CENTER_VERTICAL);body.addView(appsHeader,new LinearLayout.LayoutParams(-1,dp(17)));
  TextView appsTitle=text("APPS",11,0xFFBFC5C8);appsTitle.setGravity(Gravity.CENTER_VERTICAL);appsTitle.setPadding(dp(4),0,0,0);appsHeader.addView(appsTitle,new LinearLayout.LayoutParams(-2,-1));
  editHint=text("  ·  ← → mover  ·  ↑ ocultar  ·  OK guardar  ·  ATRÁS cancelar",9,0xFFBFC5C8);editHint.setGravity(Gravity.CENTER_VERTICAL);editHint.setSingleLine(true);editHint.setVisibility(editMode?View.VISIBLE:View.GONE);appsHeader.addView(editHint,new LinearLayout.LayoutParams(0,-1,1));
  AppScroll appScroll=new AppScroll(this);appScroll.setHorizontalScrollBarEnabled(false);appScroll.setFillViewport(false);appScroll.setClipToPadding(false);appScroll.setPadding(0,dp(3),0,dp(3));LinearLayout appRow=new LinearLayout(this);appRow.setGravity(Gravity.CENTER_VERTICAL);appRow.setClipChildren(false);appRow.setClipToPadding(false);appRow.setPadding(0,0,dp(8),0);appScroll.addView(appRow,new HorizontalScrollView.LayoutParams(-2,-1));body.addView(appScroll,new LinearLayout.LayoutParams(-1,dp(compact?72:92)));
  horizontal=appScroll;row=appRow;restoringAppScroll=previousAppScroll>0;
  final int restoreAppScroll=previousAppScroll;
  if(restoringAppScroll)appScroll.setVisibility(View.INVISIBLE);
  appScroll.post(()->{
   if(panel==null||horizontal!=appScroll)return;
   int max=Math.max(0,appScroll.getChildAt(0).getWidth()-appScroll.getWidth());
   appScroll.scrollTo(Math.min(restoreAppScroll,max),0);
   restoringAppScroll=false;
   appScroll.setVisibility(View.VISIBLE);
   paintBottom();
  });
  if(visible.isEmpty()){TextView empty=text(loading?"Cargando…":"Sin aplicaciones",14,Color.LTGRAY);appRow.addView(empty,new LinearLayout.LayoutParams(-2,dp(64)));}
  for(int i=0;i<visible.size();i++){
   final int n=i;AppCatalog.Entry e=visible.get(i);LinearLayout card=new LinearLayout(this);card.setOrientation(1);card.setGravity(Gravity.CENTER);card.setPadding(dp(8),dp(3),dp(8),dp(3));card.setClipChildren(false);card.setClipToPadding(false);
   LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(dp(120),dp(compact?62:88));cp.setMargins(0,dp(2),dp(8),0);appRow.addView(card,cp);
   ImageView icon=new ImageView(this);icon.setImageDrawable(catalog.icon(e));icon.setScaleType(ImageView.ScaleType.FIT_CENTER);card.addView(icon,new LinearLayout.LayoutParams(dp(compact?26:40),dp(compact?26:40)));
   TextView label=text(e.title,10,Color.WHITE);label.setGravity(Gravity.CENTER);label.setMaxLines(2);card.addView(label,new LinearLayout.LayoutParams(-1,-2));
   card.setContentDescription(e.title);card.setOnClickListener(v->{selected=quickActions.size()+n;if(!editMode)launch(e);});cards.add(card);
  }
  LinearLayout add=new LinearLayout(this);add.setGravity(Gravity.CENTER);add.setPadding(dp(8),dp(3),dp(8),dp(3));add.setContentDescription("Añadir aplicación");add.setVisibility(editMode?View.VISIBLE:View.GONE);
  TextView addLabel=text("+ Añadir",10,Color.WHITE);addLabel.setGravity(Gravity.CENTER);add.addView(addLabel,new LinearLayout.LayoutParams(-1,-1));
  LinearLayout.LayoutParams addParams=new LinearLayout.LayoutParams(dp(120),dp(62));addParams.setMargins(0,dp(2),dp(8),0);appRow.addView(add,addParams);addCard=add;
  add.setOnClickListener(v->{selected=cards.size()-1;push(new Page("panelAddApp",null));});
  if(editMode)cards.add(add);
  if(selected<0||selected>=cards.size())selected=quickActions.size()<cards.size()?quickActions.size():0;
  paintBottom();
  showClockPanel();
 }
 private TextClock clockText(int size,int color,String format){
  TextClock clock=new TextClock(this);clock.setTextSize(size);clock.setTextColor(color);clock.setTextLocale(new Locale("es","ES"));clock.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);clock.setSingleLine(true);clock.setFocusable(false);clock.setClickable(false);clock.setFormat12Hour(format);clock.setFormat24Hour(format);return clock;
 }
 private void showClockPanel(){
  if(panel==null||!pages.isEmpty()||!prefs.getBoolean("overlay_clock",true)){hideClockPanel();return;}
  if(clockPanel!=null)return;
  clockPanel=new LinearLayout(this);clockPanel.setOrientation(LinearLayout.VERTICAL);clockPanel.setGravity(Gravity.RIGHT);clockPanel.setPadding(dp(14),dp(8),dp(14),dp(9));clockPanel.setBackground(background(panelColor(),14));
  // Native minute/time-zone updates affect only these two text views.
  panelClock=clockText(18,0xFFB8BFC5,prefs.getBoolean("time_24",false)?"HH:mm":"h:mm a");
  panelDate=clockText(12,0xFFA3ACB4,"EEEE d 'de' MMMM 'de' yyyy");
  clockPanel.addView(panelClock,new LinearLayout.LayoutParams(-1,dp(25)));
  clockPanel.addView(panelDate,new LinearLayout.LayoutParams(-1,dp(19)));
  WindowManager.LayoutParams p=new WindowManager.LayoutParams(-2,-2,WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,PixelFormat.TRANSLUCENT);
  p.gravity=Gravity.TOP|Gravity.RIGHT;p.x=dp(14);p.y=dp(14);
  try{wm.addView(clockPanel,p);}catch(RuntimeException e){hideClockPanel();android.util.Log.w("Z6Panel","Clock overlay unavailable",e);}
 }
 private void hideClockPanel(){
  if(clockPanel!=null){try{wm.removeView(clockPanel);}catch(IllegalArgumentException ignored){}clockPanel.removeAllViews();clockPanel=null;}
  panelClock=null;panelDate=null;
 }
 private void paintBottom(){
  for(int i=0;i<cards.size();i++){View card=cards.get(i);if(!(card.getBackground() instanceof android.graphics.drawable.StateListDrawable))card.setBackground(focusSelector());if(card.isSelected()!=(i==selected)){card.setSelected(i==selected);PerfStats.focusUpdates.incrementAndGet();}}
  if(restoringAppScroll||suppressAutoScroll)return;
  if(selected<cards.size()){
   View v=cards.get(selected);HorizontalScrollView scroll=v.getParent()==quickRow?quickHorizontal:horizontal;
   final int scrollToken=++scrollGeneration;
   scroll.post(()->{
    if(panel==null||!pages.isEmpty()||scrollToken!=scrollGeneration)return;
    int viewport=scroll.getWidth(), left=scroll.getScrollX(), right=left+viewport;
    int target=left;
    if(v.getLeft()<left)target=v.getLeft()-dp(8);
    else if(v.getRight()>right)target=v.getRight()-viewport+dp(8);
    int max=Math.max(0,scroll.getChildAt(0).getWidth()-viewport);
    target=Math.min(target,max);
    target=Math.max(0,target);
    if(target!=left){
     if(scroll==horizontal)scroll.scrollTo(target,0);
     else scroll.smoothScrollTo(target,0);
    }
   });
  }
 }
 private void revealSelectedAppInstant(){
  if(horizontal==null||selected<quickActions.size()||selected>=cards.size())return;
  View selectedView=cards.get(selected);
  if(selectedView.getParent()!=row)return;
  int left=horizontal.getScrollX(),right=left+horizontal.getWidth(),target=left;
  if(selectedView.getLeft()<left)target=selectedView.getLeft()-dp(8);
  else if(selectedView.getRight()>right)target=selectedView.getRight()-horizontal.getWidth()+dp(8);
  int max=Math.max(0,horizontal.getChildAt(0).getWidth()-horizontal.getWidth());
  target=Math.max(0,Math.min(target,max));
  horizontal.scrollTo(target,0);
  if(horizontal instanceof AppScroll)((AppScroll)horizontal).heldScroll=target;
 }
 private void addQuickSection(){
  boolean muted=cachedMuted;
  addQuickCard(-1,"Audio",muted?"Activar sonido":"Silenciar",()->volume(AudioManager.ADJUST_TOGGLE_MUTE));
  addQuickCard(-1,"Wi‑Fi",wifiSummary(),()->push(new Page("network",null)));
  addQuickCard(-1,"Bluetooth",bluetooth.adapter!=null&&bluetooth.adapter.isEnabled()?"Activo":"Apagado",()->push(new Page("bluetooth",null)));
  addQuickCard(-1,"Ajustes","",()->push(new Page("settings",null)));
  addQuickCard(-1,"Inicio","",()->goHome());
 }
 private void beginEditMode(){
  if(editMode)return;editOrderSnapshot=catalog.editOrder();editHiddenSnapshot=catalog.editHidden();editShownSnapshot=catalog.editShown();editMode=true;addSlot=false;selected=Math.max(quickActions.size(),selected);
  scrollGeneration++;
  suppressAutoScroll=true;
  if(horizontal instanceof AppScroll)((AppScroll)horizontal).holdCurrentPosition();
  if(editHint!=null)editHint.setVisibility(View.VISIBLE);
  if(addCard!=null){addCard.setVisibility(View.VISIBLE);if(!cards.contains(addCard))cards.add(addCard);}
  paintBottom();
  if(horizontal instanceof AppScroll){
   AppScroll scroll=(AppScroll)horizontal;
   scroll.post(()->{if(horizontal==scroll){revealSelectedAppInstant();scroll.releasePosition();suppressAutoScroll=false;}});
  }
 }
 private void endEditMode(){
  scrollGeneration++;
  suppressAutoScroll=false;
  if(horizontal instanceof AppScroll)((AppScroll)horizontal).releasePosition();
  editMode=false;addSlot=false;
  if(editHint!=null)editHint.setVisibility(View.GONE);
  if(addCard!=null){addCard.setVisibility(View.INVISIBLE);}
  if(selected>=cards.size())selected=Math.max(0,cards.size()-1);
  for(int i=0;i<cards.size();i++)cards.get(i).setBackground(focusBackground(i==selected));
  cards.remove(addCard);
  if(horizontal!=null)horizontal.scrollTo(horizontal.getScrollX(),0);
 }
 private void saveEditMode(){editOrderSnapshot=null;editHiddenSnapshot=null;editShownSnapshot=null;endEditMode();}
 private void cancelEditMode(){if(!editMode)return;catalog.restoreEditState(editOrderSnapshot,editHiddenSnapshot,editShownSnapshot);editOrderSnapshot=null;editHiddenSnapshot=null;editShownSnapshot=null;endEditMode();}
 private void moveEdit(int delta){
  if(!editMode)return;
  if(horizontal instanceof AppScroll)((AppScroll)horizontal).frozen=false;
  if(addSlot){if(delta<0){addSlot=false;visible=catalog.visible(0);selected=quickActions.size()+Math.max(0,visible.size()-1);paintBottom();}return;}
  int appIndex=selected-quickActions.size();if(appIndex<0||appIndex>=visible.size())return;
  AppCatalog.Entry e=visible.get(appIndex);
  if(delta>0&&appIndex==visible.size()-1){addSlot=true;selected=quickActions.size()+visible.size();paintBottom();return;}
  if(delta<0&&appIndex==0)return;
  if(!catalog.moveVisible(e,delta))return;
  int nextIndex=appIndex+delta;
  int currentCardIndex=quickActions.size()+appIndex,nextCardIndex=quickActions.size()+nextIndex;
  if(horizontal!=null&&row!=null&&currentCardIndex>=0&&nextCardIndex>=0&&nextCardIndex<cards.size()){
   View current=cards.get(currentCardIndex),target=cards.get(nextCardIndex);
   int targetPosition=row.indexOfChild(target);
   row.removeView(current);row.addView(current,targetPosition);
   cards.remove(currentCardIndex);cards.add(nextCardIndex,current);
  }
  Collections.swap(visible,appIndex,nextIndex);
  selected=quickActions.size()+nextIndex;
  paintBottom();
 }
 private void hideEdit(){
  if(!editMode||addSlot)return;
  int appIndex=selected-quickActions.size();if(appIndex<0||appIndex>=visible.size())return;
  catalog.toggleVisible(visible.get(appIndex));visible=catalog.visible(0);
  selected=visible.isEmpty()?Math.max(0,quickActions.size()-1):quickActions.size()+Math.min(appIndex,visible.size()-1);
  renderBottom();
 }
 private void addAppToPanel(AppCatalog.Entry entry){
  if(entry==null)return;
  catalog.setVisible(entry,true);catalog.moveVisibleToEnd(entry);
  visible=catalog.visible(0);
  selected=quickActions.size()+Math.max(0,visible.size()-1);
  addSlot=false;
  pages.pop();
  renderBottom();
 }
 private void addQuickCard(int iconRes,String label,String detail,Runnable action){
  final int index=quickActions.size();quickActions.add(action);LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.HORIZONTAL);card.setGravity(Gravity.CENTER_VERTICAL);card.setPadding(dp(7),0,dp(7),0);LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(dp(108),dp(34));cp.rightMargin=dp(4);row.addView(card,cp);
  TextView title=text(label,9,Color.WHITE);title.setGravity(Gravity.CENTER_VERTICAL);title.setSingleLine(true);card.addView(title,new LinearLayout.LayoutParams(-2,-1));
  if(iconRes!= -1){ImageView glyph=new ImageView(this);glyph.setImageResource(iconRes);glyph.setColorFilter(0xFFD0D4D5);glyph.setAlpha(.82f);glyph.setScaleType(ImageView.ScaleType.CENTER_INSIDE);LinearLayout.LayoutParams iconParams=new LinearLayout.LayoutParams(dp(15),dp(18));iconParams.leftMargin=dp(2);card.addView(glyph,iconParams);}
  TextView value=null;if(!detail.isEmpty()){value=text(" · "+detail,8,0xFFAAAAAA);value.setGravity(Gravity.CENTER_VERTICAL);value.setSingleLine(true);card.addView(value,new LinearLayout.LayoutParams(0,-1,1));}quickDetails.add(value);
  card.setOnClickListener(v->{selected=index;quickActions.get(index).run();});cards.add(card);
 }
 private String wifiSummary(){android.net.wifi.WifiManager wifi=(android.net.wifi.WifiManager)getApplicationContext().getSystemService(WIFI_SERVICE);if(!wifi.isWifiEnabled())return "Desactivado";ConnectivityManager cm=(ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);NetworkCapabilities caps=cm.getNetworkCapabilities(cm.getActiveNetwork());return caps!=null&&caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)?"Conectado":"Sin conexión Wi-Fi";}
 private void startWifiScan(){
  android.net.wifi.WifiManager wifi=(android.net.wifi.WifiManager)getApplicationContext().getSystemService(WIFI_SERVICE);
  if(!wifi.isWifiEnabled()){message("Activa Wi-Fi para buscar redes");return;}
  if(checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){message("Permiso de ubicación necesario para ver redes");return;}
  wifiScanPending=true;try{if(!wifi.startScan()){wifiScanPending=false;message("No se pudo iniciar la búsqueda");}}catch(SecurityException e){wifiScanPending=false;message("Android rechazó la búsqueda Wi-Fi");}
  renderSide();
 }
 private void connectWifi(android.net.wifi.ScanResult result){
  android.net.wifi.WifiManager wifi=(android.net.wifi.WifiManager)getApplicationContext().getSystemService(WIFI_SERVICE);
  String ssid=result.SSID;if(ssid==null||ssid.isEmpty()){message("Red sin nombre");return;}
  try{
   java.util.List<android.net.wifi.WifiConfiguration> saved=wifi.getConfiguredNetworks();
   if(saved!=null)for(android.net.wifi.WifiConfiguration c:saved)if(("\""+ssid+"\"").equals(c.SSID)){wifi.enableNetwork(c.networkId,true);wifi.reconnect();message("Conectando a "+ssid);return;}
   if(result.capabilities!=null&&!result.capabilities.contains("WEP")&&!result.capabilities.contains("WPA")){
    android.net.wifi.WifiConfiguration c=new android.net.wifi.WifiConfiguration();c.SSID="\""+ssid+"\"";c.allowedKeyManagement.set(android.net.wifi.WifiConfiguration.KeyMgmt.NONE);
    int id=wifi.addNetwork(c);if(id>=0){wifi.enableNetwork(id,true);wifi.reconnect();message("Conectando a "+ssid);return;}
   }
   message("Red protegida: primero guárdala en Android para conectarla desde aquí");
  }catch(SecurityException e){message("El firmware no permite conectar esta red desde el panel");}
 }
 private void setScreenProfile(int profile){try{if(Build.VERSION.SDK_INT>=23&&!Settings.System.canWrite(this)){message("Permiso de brillo pendiente");return;}int brightness=profile==0?180:profile==1?255:70;if(!Settings.System.putInt(getContentResolver(),Settings.System.SCREEN_BRIGHTNESS,brightness)){message("No se pudo aplicar el perfil");return;}prefs.edit().putInt("screen_profile",profile).apply();renderSide();}catch(SecurityException e){message("El firmware no permite cambiar el brillo");}}
 private void push(Page p){pages.push(p);generation++;renderSide();}
 private void back(){
  if(editMode){cancelEditMode();renderBottom();return;}
  if(pages.isEmpty()){hide();return;}
  pages.pop();generation++;if(pages.isEmpty()&&HomeActivity.visible)hide();else render();
 }
 private void info(String label){add(label,"",null);}
 private void add(String label,String detail,Runnable run){actions.add(new Action(label,detail,run));}
 private void addKey(String id,String label,String detail,Runnable run){actions.add(new Action(id,label,detail,run,false));}
 private void adjustable(String id,String label,String detail,Runnable run,Runnable less,Runnable more){Action a=new Action(id,label,detail,run,false);a.less=less;a.more=more;actions.add(a);}
 private void addDevice(String label,String detail,Runnable run){actions.add(new Action(label,detail,run,true));}
 private void addDevice(String id,String label,String detail,Runnable run){actions.add(new Action(id,label,detail,run,true));}
 private AudioManager audio(){return (AudioManager)getSystemService(AUDIO_SERVICE);}
 private void acceptAudio(int value,int maximum,boolean muted,boolean fixed){cachedVolume=value;cachedVolumeMax=maximum;cachedMuted=muted;cachedVolumeFixed=fixed;audioLoaded=true;lastAudioCheck=SystemClock.elapsedRealtime();}
 private void refreshAudioState(){
  if(destroyed||audioRefreshPending)return;
  synchronized(volumeRequests){if(volumeDraining)return;}
  audioRefreshPending=true;
  volumeWorker.execute(()->{try{AudioManager a=audio();final int value=a.getStreamVolume(3),maximum=a.getStreamMaxVolume(3);final boolean muted=a.isStreamMute(3),fixed=a.isVolumeFixed();ui.post(()->{audioRefreshPending=false;if(destroyed)return;boolean changed=!audioLoaded||value!=cachedVolume||maximum!=cachedVolumeMax||muted!=cachedMuted||fixed!=cachedVolumeFixed;acceptAudio(value,maximum,muted,fixed);if(changed&&panel!=null){if(!pages.isEmpty()&&(pages.peek().name.equals("sound")||pages.peek().name.equals("settings")))renderSide();else if(pages.isEmpty()&&!quickDetails.isEmpty()&&quickDetails.get(0)!=null)quickDetails.get(0).setText(" · "+(muted?"Activar sonido":"Silenciar"));}});}catch(RuntimeException e){ui.post(()->audioRefreshPending=false);}});
 }
 private String audioSummary(){if(SystemClock.elapsedRealtime()-lastAudioCheck>1000)refreshAudioState();return !audioLoaded?"Consultando audio…":cachedMuted?"Silenciado":"Volumen "+cachedVolume+" / "+cachedVolumeMax;}
 private void hideVolumeHud(){ui.removeCallbacks(dismissVolume);if(volumeHud!=null){try{wm.removeView(volumeHud);}catch(IllegalArgumentException ignored){}volumeHud=null;volumeLabel=null;volumeBar=null;volumeParams=null;}}
 private void showVolumeHud(int value,int maximum,boolean muted){
  if(volumeHud==null){
   volumeHud=new LinearLayout(this);volumeHud.setOrientation(1);volumeHud.setPadding(dp(18),dp(12),dp(18),dp(12));volumeHud.setBackground(background(0xDD000000,12));
   volumeLabel=text("",16,Color.WHITE);volumeHud.addView(volumeLabel);
   volumeBar=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);volumeBar.setProgressTintList(android.content.res.ColorStateList.valueOf(Color.WHITE));LinearLayout.LayoutParams b=new LinearLayout.LayoutParams(-1,dp(8));b.topMargin=dp(8);volumeHud.addView(volumeBar,b);
   volumeParams=new WindowManager.LayoutParams(dp(240),-2,WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,PixelFormat.TRANSLUCENT);volumeParams.gravity=Gravity.BOTTOM|Gravity.RIGHT;volumeParams.x=dp(18);volumeParams.y=dp(18);
   try{wm.addView(volumeHud,volumeParams);}catch(RuntimeException ex){hideVolumeHud();return;}
  }
  volumeLabel.setText(muted?"Silencio":"Volumen  "+value+" / "+maximum);
  volumeBar.setMax(maximum);volumeBar.setProgress(muted?0:value);
  ui.removeCallbacks(dismissVolume);ui.postDelayed(dismissVolume,1200);
 }
 private void volume(int direction){
  synchronized(volumeRequests){
   int[] last=volumeRequests.peekLast();
   if((direction==AudioManager.ADJUST_RAISE||direction==AudioManager.ADJUST_LOWER)&&last!=null&&last[0]==direction)last[1]=Math.min(100,last[1]+1);
   else volumeRequests.addLast(new int[]{direction,1});
   if(volumeDraining)return;volumeDraining=true;
  }
  volumeWorker.execute(()->{
   while(!Thread.currentThread().isInterrupted()){
    int[] request;synchronized(volumeRequests){request=volumeRequests.pollFirst();if(request==null){volumeDraining=false;return;}}
    try{
     AudioManager a=audio();if(a.isVolumeFixed()){ui.post(()->{if(!destroyed)message("El volumen se controla en el televisor o receptor.");});continue;}
     long start=SystemClock.elapsedRealtimeNanos();
     a.adjustStreamVolume(AudioManager.STREAM_MUSIC,request[0],0);
     int maximum=a.getStreamMaxVolume(3);
     if(request[1]>1){int value=a.getStreamVolume(3),target=Math.max(0,Math.min(maximum,value+request[0]*(request[1]-1)));if(value!=target)a.setStreamVolume(3,target,0);}
     final int value=a.getStreamVolume(3);final boolean muted=a.isStreamMute(3);
     PerfStats.volumeNanos.addAndGet(SystemClock.elapsedRealtimeNanos()-start);PerfStats.volumeCalls.incrementAndGet();
     ui.post(()->{if(destroyed)return;acceptAudio(value,maximum,muted,false);long hudStart=SystemClock.elapsedRealtimeNanos();showVolumeHud(value,maximum,muted);PerfStats.volumeHudNanos.addAndGet(SystemClock.elapsedRealtimeNanos()-hudStart);if(panel!=null){if(pages.isEmpty()){if(!quickDetails.isEmpty()&&quickDetails.get(0)!=null)quickDetails.get(0).setText(" · "+(muted?"Activar sonido":"Silenciar"));if(!quickActions.isEmpty())quickActions.set(0,()->volume(AudioManager.ADJUST_TOGGLE_MUTE));}else if(pages.peek().name.equals("sound")||pages.peek().name.equals("settings"))renderSide();}});
    }catch(RuntimeException e){ui.post(()->{if(!destroyed)message("Android no permitió cambiar el volumen");});}
   }
  });
 }
 private void startLocalControl(boolean authorize){
  if(localStarting||!authorize&&!prefs.getBoolean("local_adb_enabled",false))return;localStarting=true;bridgeStatus="Iniciando control…";final int token=localGeneration;if(authorize){if(!prefs.getBoolean("local_adb_enabled",false))hide();message("Esperando autorización en la Z6…");}
  bootstrapWorker.execute(()->{String result;try{result=LocalAdb.start(this,authorize);}catch(java.net.SocketTimeoutException e){result="Autorización o conexión pendiente";}catch(Exception e){android.util.Log.w("Z6LocalAdb",e.toString());result="Inicio local no disponible: "+e.getClass().getSimpleName();}final String status=result;
   ui.post(()->{localStarting=false;if(destroyed||token!=localGeneration)return;if(status.equals("OK")){localAttempts=0;prefs.edit().putBoolean("local_adb_enabled",true).apply();ui.postDelayed(()->finishLocalControl(authorize,0),1200);}else{bridgeStatus=status;if(panel!=null&&!pages.isEmpty())renderSide();if(authorize)message(status);else if(++localAttempts<3)ui.postDelayed(()->startLocalControl(false),30000);}});});
 }
 private void finishLocalControl(boolean authorize,int attempt){if(destroyed)return;final int token=localGeneration;loader.execute(()->{String result=SystemBridge.read("PING",1200);ui.post(()->{if(destroyed||token!=localGeneration)return;bridgeReady="OK".equals(result);bridgeStatus=bridgeReady?"Conectado":result;if(panel!=null&&!pages.isEmpty())renderSide();if(!bridgeReady&&attempt<5)ui.postDelayed(()->{if(token==localGeneration)finishLocalControl(authorize,attempt+1);},5000);else if(authorize)message(bridgeReady?"Control autónomo activo":result);});});}
 private void setTimezone(String zone){
  if(!canSecure()){message("Permiso de sistema pendiente");return;}int old=Settings.Global.getInt(getContentResolver(),Settings.Global.AUTO_TIME_ZONE,0);
  if(!Settings.Global.putInt(getContentResolver(),Settings.Global.AUTO_TIME_ZONE,0)){message("No se pudo desactivar la zona automática");return;}
  message("Aplicando…");loader.execute(()->{String result=SystemBridge.call("TIMEZONE:"+zone);ui.post(()->{if(!result.equals("OK")){Settings.Global.putInt(getContentResolver(),Settings.Global.AUTO_TIME_ZONE,old);if(!destroyed)message(result);return;}if(!destroyed&&panel!=null){if(!pages.isEmpty()&&pages.peek().name.equals("timezone"))pages.pop();render();}});});
 }
 private void systemCommand(String op){
  if(commandPending)return;
  if(!op.equals("PING")&&!ready()){message("Control no disponible; abre Diagnóstico y permisos");return;}
  commandPending=true;message(op.equals("PING")?"Comprobando…":"Aplicando…");
  loader.execute(()->{String result=SystemBridge.call(op);ui.post(()->{
   commandPending=false;if(destroyed)return;
   if(op.equals("PING")){bridgeReady="OK".equals(result);bridgeStatus=bridgeReady?"Conectado":result;if(panel!=null&&!pages.isEmpty())renderSide();message(bridgeStatus);return;}
   if(!"OK".equals(result)){invalidateControl(result);if(panel!=null&&!pages.isEmpty())renderSide();message(result);return;}
   if(op.startsWith("UNINSTALL:")){pages.clear();pages.push(new Page("manage",null));refreshCatalog();return;}
   if(op.equals("SLEEP")){hide();return;}
   if(op.equals("WIFI_OFF")&&!pages.isEmpty()&&pages.peek().name.equals("wifiConfirm"))pages.pop();
   ui.postDelayed(()->{if(panel==null)return;render();if(op.startsWith("WIFI_")){boolean enabled=((android.net.wifi.WifiManager)getApplicationContext().getSystemService(WIFI_SERVICE)).isWifiEnabled();boolean expected=op.equals("WIFI_ON");message(enabled==expected?(enabled?"Wi-Fi activado":"Wi-Fi desactivado"):"Android todavía no confirmó el cambio de Wi-Fi");}},1500);
  });});
 }
 private boolean canSecure(){return checkSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS)==PackageManager.PERMISSION_GRANTED;}
 private void toggleGlobal(String key){if(!canSecure()){message("Permiso de sistema pendiente");return;}int value=Settings.Global.getInt(getContentResolver(),key,0);if(Settings.Global.putInt(getContentResolver(),key,value==0?1:0))renderSide();else message("No se pudo guardar");}
 private void nativePage(String name,Intent intent){
  try{startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));hide();}
  catch(RuntimeException e){Page p=new Page("restricted",null);p.description=name;push(p);}
 }
 private void nativeSettings(String name,String action){nativePage(name,new Intent(action));}
 private void launch(AppCatalog.Entry entry){
  if(entry==null){message("La aplicación ya no está disponible");refreshCatalog();return;}
  launchIntent(new Intent(Intent.ACTION_MAIN).setComponent(entry.component).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
 }
 private void launchIntent(Intent i){
  try{startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));hide();}
  catch(RuntimeException e){message("Android no permite abrir esta opción o ya no está instalada.");refreshCatalog();}
 }
 private void goHome(){
  pages.clear();
  hide();
  try{startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));}
  catch(RuntimeException e){message("No se pudo volver al inicio");}
 }
 private boolean ready(){return bridgeReady&&SystemBridge.remote!=null&&SystemBridge.remote.isBinderAlive()&&!commandPending;}
 private void recoverBridgeIfLost(){if(!destroyed&&prefs.getBoolean("local_adb_enabled",false)&&(SystemBridge.remote==null||!SystemBridge.remote.isBinderAlive()))startLocalControl(false);}
 private String appLabel(String pkg){AppCatalog.Entry entry=catalog.find(pkg);if(entry!=null)return entry.title;try{return getPackageManager().getApplicationInfo(pkg,0).loadLabel(getPackageManager()).toString();}catch(Exception e){return pkg==null?"Sin aplicación activa":pkg;}}
 private boolean restartable(String pkg){try{if(pkg==null||pkg.equals(getPackageName())||pkg.startsWith("com.android.")||pkg.startsWith("com.google.android.")||pkg.startsWith("android."))return false;PackageManager pm=getPackageManager();ApplicationInfo a=pm.getApplicationInfo(pkg,0);String[] peers=pm.getPackagesForUid(a.uid);return a.enabled&&a.uid>=10000&&(a.flags&(ApplicationInfo.FLAG_SYSTEM|ApplicationInfo.FLAG_UPDATED_SYSTEM_APP|ApplicationInfo.FLAG_PERSISTENT|ApplicationInfo.FLAG_SUSPENDED))==0&&catalog.find(pkg)!=null&&peers!=null&&peers.length==1&&pm.queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).setPackage(pkg),0).isEmpty();}catch(Exception e){return false;}}
 private void refreshForeground(){
  if(!ready()||foregroundInFlight)return;if(HomeActivity.visible){foregroundPackage=null;return;}if(foregroundPackage!=null&&SystemClock.elapsedRealtime()-lastForegroundCheck<3000)return;
  final int token=++foregroundGeneration;
  foregroundInFlight=true;PerfStats.foregroundQueries.incrementAndGet();catalogWorker.execute(()->{String pkg=SystemBridge.read("FOREGROUND_APP",1500);ui.post(()->{foregroundInFlight=false;if(destroyed||token!=foregroundGeneration)return;lastForegroundCheck=SystemClock.elapsedRealtime();foregroundPackage=pkg!=null&&catalog.find(pkg)!=null?pkg:null;if(panel!=null&&!pages.isEmpty()&&pages.peek().name.equals("settings")){if(foregroundPackage!=null&&!HomeActivity.visible&&!pages.peek().interacted)pages.peek().selectedId="current";renderSide();}});});
 }
 void bridgeAttached(){ui.post(()->{if(destroyed||bridgeReady||bridgeChecking||SystemClock.elapsedRealtime()-lastHealthCheck<2500)return;bridgeChecking=true;lastHealthCheck=SystemClock.elapsedRealtime();final IBinder binder=SystemBridge.remote;loader.execute(()->{String status=SystemBridge.read("PING",1200);ui.post(()->{bridgeChecking=false;if(destroyed||SystemBridge.remote!=binder)return;bridgeReady="OK".equals(status);bridgeStatus=bridgeReady?"Conectado":"Control pendiente de recuperación";if(bridgeReady)refreshForeground();if(panel!=null&&!pages.isEmpty())renderSide();});});});}
 void bridgeLost(){ui.post(()->{if(destroyed)return;bridgeReady=false;bridgeStatus="Control desconectado";if(panel!=null&&!pages.isEmpty())renderSide();ui.removeCallbacks(recoverLostBridge);if(prefs.getBoolean("local_adb_enabled",false))ui.postDelayed(recoverLostBridge,1000);});}
 private void invalidateControl(String result){if(result!=null&&(result.contains("TIMEOUT")||result.contains("RECOVERY_REQUIRED")||result.contains("desconectado"))){bridgeReady=false;bridgeStatus="Recupera el control en Diagnóstico";}}
 private void togglePreference(String key,boolean defaultValue){prefs.edit().putBoolean(key,!prefs.getBoolean(key,defaultValue)).apply();HomeActivity.refreshPreferences();renderSide();}
 private String storageSummary(){if(!storageInFlight&&SystemClock.elapsedRealtime()-lastStorageCheck>5000){storageInFlight=true;catalogWorker.execute(()->{String value=cachedStorage;int percent=cachedStoragePercent;try{StatFs f=new StatFs(Environment.getDataDirectory().getPath());value=String.format(Locale.ROOT,"%.1f GB libres de %.1f GB",f.getAvailableBytes()/1073741824.0,f.getTotalBytes()/1073741824.0);percent=(int)((f.getTotalBytes()-f.getAvailableBytes())*100/f.getTotalBytes());}catch(RuntimeException ignored){}final String summary=value;final int used=percent;ui.post(()->{storageInFlight=false;if(destroyed)return;boolean changed=!cachedStorage.equals(summary);cachedStorage=summary;cachedStoragePercent=used;lastStorageCheck=SystemClock.elapsedRealtime();if(changed&&panel!=null&&!pages.isEmpty()&&Arrays.asList("settings","system","storage","status").contains(pages.peek().name))renderSide();});});}return cachedStorage;}
 private Intent appSettings(String pkg){return new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+pkg));}
 private void restartApp(String pkg){
  if(!ready()||!restartable(pkg)){message("Reiniciar no está disponible; revisa Diagnóstico");return;}
  commandPending=true;message("Reiniciando aplicación…");
  loader.execute(()->{String result=SystemBridge.call("RESTART_APP:"+pkg);ui.post(()->{commandPending=false;if(destroyed)return;if(!"OK".equals(result)){invalidateControl(result);message(result);return;}AppCatalog.Entry entry=catalog.find(pkg);if(entry!=null)launch(entry);else message("La aplicación ya no está disponible");});});
 }
 private void scheduleTimer(int minutes){String result=SleepTimer.schedule(this,minutes);if("OK".equals(result))renderSide();else message(result);}
 private boolean buildModern(Page p){
  switch(p.name){
   case "settings":{
    title.setText("Ajustes");subtitle.setText("Aplicaciones y equipo, a un paso");
    final String current=HomeActivity.visible?null:foregroundPackage;
    addKey("current","App actual",current==null?"Abre una aplicación para ver sus opciones":appLabel(current),current==null?null:()->push(new Page("current",current)));
    addKey("applications","Aplicaciones","Mostrar, ocultar y organizar",()->push(new Page("manage",null)));
    addKey("equipment","Equipo",storageSummary(),()->push(new Page("system",null)));
    addKey("sound","Sonido",audioSummary(),()->push(new Page("sound",null)));
    addKey("wifi","Wi-Fi",wifiSummary(),()->push(new Page("network",null)));
    addKey("bluetooth","Bluetooth",bluetooth.adapter!=null&&bluetooth.adapter.isEnabled()?"Activado":"Desactivado",()->push(new Page("bluetooth",null)));
    addKey("personalize","Panel e inicio","Aspecto, saludo y reloj",()->push(new Page("appearance",null)));
    addKey("android","Ajustes de Android","Configuración completa · abre otra pantalla",()->nativeSettings("Ajustes de Android",Settings.ACTION_SETTINGS));
    if(p.selectedId==null)p.selectedId=current==null?"applications":"current";
    return true;
   }
   case "current":
    title.setText("App actual");subtitle.setText(appLabel(p.pkg));
    addKey("resume","Volver al video","Cerrar este panel",this::hide);
    addKey("restart","Reiniciar aplicación",ready()?"Cierra y vuelve a abrir la app":"Requiere recuperar el control en Diagnóstico",ready()&&restartable(p.pkg)?()->push(new Page("restartConfirm",p.pkg)):null);
    addKey("permissions","Información y permisos","Abre la configuración de esta app en Android",()->nativePage("Información y permisos",appSettings(p.pkg)));
    return true;
   case "restartConfirm":
    title.setText("Reiniciar aplicación");subtitle.setText("Se cerrará "+appLabel(p.pkg)+". La posición del video puede perderse.");
    addKey("confirmRestart","Reiniciar ahora","",ready()&&restartable(p.pkg)?()->restartApp(p.pkg):null);
    addKey("cancel","Cancelar","",this::back);p.selectedId=p.selectedId==null?"cancel":p.selectedId;return true;
   case "system":
    title.setText("Equipo");subtitle.setText(ready()?"Control disponible":bridgeStatus+" · revisa Diagnóstico");
    addKey("storage","Almacenamiento",storageSummary(),()->push(new Page("storage",null)));
    addKey("timer","Temporizador",SleepTimer.summary(this),()->push(new Page("timer",null)));
    addKey("sleep","Suspender ahora","Pausa la reproducción y pone el TV Box en reposo",ready()?()->push(new Page("sleepConfirm",null)):null);
    addKey("reboot","Reiniciar equipo","Cierra las aplicaciones y reinicia Android",ready()?()->push(new Page("rebootConfirm",null)):null);
    addKey("regional","Fecha e idioma","Hora, zona horaria e idioma de Android",()->push(new Page("regional",null)));
    addKey("about","Información del equipo","Android, pantalla y almacenamiento",()->push(new Page("status",null)));
    addKey("diagnostics","Diagnóstico y permisos",bridgeStatus,()->push(new Page("diagnostics",null)));
    return true;
   case "timer":
    title.setText("Temporizador");subtitle.setText(SleepTimer.summary(this));
    for(int min:new int[]{30,60,90}){final int value=min;addKey("timer"+min,min+" minutos","Pausar y suspender el TV Box",ready()?()->scheduleTimer(value):null);}
    addKey("timerCancel","Cancelar temporizador","Mantener el equipo encendido",SleepTimer.remainingMillis(this)>0?()->{SleepTimer.cancel(this);renderSide();}:null);
    if(!ready())addKey("repair","Recuperar control","Necesario para programar la suspensión",()->push(new Page("diagnostics",null)));
    return true;
   case "sleepConfirm":
    title.setText("Suspender TV Box");subtitle.setText("Se pausará el video. El apagado del televisor depende de HDMI-CEC.");
    addKey("confirmSleep","Suspender","",ready()?()->systemCommand("SLEEP"):null);addKey("cancel","Cancelar","",this::back);p.selectedId=p.selectedId==null?"cancel":p.selectedId;return true;
   case "rebootConfirm":
    title.setText("Reiniciar equipo");subtitle.setText("Se cerrarán las aplicaciones y se cancelará el temporizador.");
    addKey("confirmReboot","Reiniciar","",ready()?()->{SleepTimer.cancel(this);systemCommand("REBOOT");}:null);addKey("cancel","Cancelar","",this::back);p.selectedId=p.selectedId==null?"cancel":p.selectedId;return true;
   case "diagnostics":case "localControl":
    title.setText("Diagnóstico y permisos");subtitle.setText(bridgeStatus);
    info("Control del equipo: "+(ready()?"conectado":"no disponible"));
    info("Ajustes de sistema: "+(canSecure()?"permitidos":"pendientes"));
    info("Bluetooth: "+(checkSelfPermission(android.Manifest.permission.BLUETOOTH)==PackageManager.PERMISSION_GRANTED?"permitido":"pendiente"));
    info("Ubicación para buscar: "+(checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED?"permitida":"pendiente"));
    addKey("repair","Recuperar control",localStarting?"Iniciando…":"Autoriza la clave local si Android lo solicita",localStarting?null:()->startLocalControl(true));
    addKey("check","Comprobar conexión","Consultar el servicio auxiliar",()->systemCommand("PING"));
    addKey("autonomous","Inicio autónomo: "+(prefs.getBoolean("local_adb_enabled",false)?"activado":"desactivado"),"Recuperar la depuración al encender, sin PC",()->{if(prefs.getBoolean("local_adb_enabled",false)){localGeneration++;prefs.edit().putBoolean("local_adb_enabled",false).apply();renderSide();}else startLocalControl(true);});
    addKey("access","Permisos del panel en Android","Abre otra pantalla",()->nativePage("Permisos del panel",appSettings(getPackageName())));
    return true;
   case "network":{
    title.setText("Wi-Fi");android.net.wifi.WifiManager w=(android.net.wifi.WifiManager)getApplicationContext().getSystemService(WIFI_SERVICE);
    ConnectivityManager cm=(ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);Network n=cm.getActiveNetwork();NetworkCapabilities caps=cm.getNetworkCapabilities(n);
    boolean on=w.isWifiEnabled(),connected=caps!=null&&caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
    String ssid=w.getConnectionInfo()==null?null:w.getConnectionInfo().getSSID();subtitle.setText(!on?"Desactivado":connected?ssid==null||ssid.equals("<unknown ssid>")?"Conectado":ssid.replace("\"",""):"Activado · sin conexión");
    addKey("configure","Configurar redes en Android","Conectar, introducir contraseña u olvidar una red",()->nativeSettings("Redes Wi-Fi",Settings.ACTION_WIFI_SETTINGS));
    addKey("wifiToggle",on?"Desactivar Wi-Fi":"Activar Wi-Fi",ready()?"Cambiar la conexión interrumpe el video":"Requiere recuperar el control en Diagnóstico",ready()?()->{if(w.isWifiEnabled())push(new Page("wifiConfirm",null));else systemCommand("WIFI_ON");}:null);
    if(connected){info(caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)?"Internet verificado":"Internet sin verificar");info("Señal: "+w.getConnectionInfo().getRssi()+" dBm");}
    LinkProperties links=cm.getLinkProperties(n);if(links!=null)for(LinkAddress a:links.getLinkAddresses())if(a.getAddress() instanceof java.net.Inet4Address)info("IP: "+a.getAddress().getHostAddress());
    if(!ready())addKey("repair","Recuperar control","",()->push(new Page("diagnostics",null)));
    return true;
   }
   case "wifiConfirm":
    title.setText("Desactivar Wi-Fi");subtitle.setText("Se interrumpirán Internet y el video en línea.");addKey("confirmWifiOff","Desactivar","",ready()?()->systemCommand("WIFI_OFF"):null);addKey("cancel","Cancelar","",this::back);p.selectedId=p.selectedId==null?"cancel":p.selectedId;return true;
   case "sound":{
    audioSummary();title.setText("Sonido");subtitle.setText(cachedVolumeFixed?"El volumen se controla en el televisor":"← → ajustar · OK subir volumen");
    adjustable("volume",audioLoaded?"Volumen: "+cachedVolume+" / "+cachedVolumeMax:"Consultando audio…",cachedMuted?"Silenciado":"Audio multimedia",!audioLoaded||cachedVolumeFixed?null:()->volume(AudioManager.ADJUST_RAISE),!audioLoaded||cachedVolumeFixed?null:()->volume(AudioManager.ADJUST_LOWER),!audioLoaded||cachedVolumeFixed?null:()->volume(AudioManager.ADJUST_RAISE));
    addKey("mute",cachedMuted?"Activar sonido":"Silenciar","",!audioLoaded||cachedVolumeFixed?null:()->volume(AudioManager.ADJUST_TOGGLE_MUTE));
    addKey("audioDevices","Salida de audio y Bluetooth","Seleccionar o conectar un parlante",()->push(new Page("bluetooth",null)));
    addKey("nativeSound","Sonido avanzado en Android","Abre otra pantalla",()->nativeSettings("Sonido avanzado",Settings.ACTION_SOUND_SETTINGS));return true;
   }
   case "appearance":
    title.setText("Panel e inicio");subtitle.setText("Personalización guardada en este TV Box");
    addKey("overlayClock","Hora y fecha en el panel: "+(prefs.getBoolean("overlay_clock",true)?"sí":"no"),"Al pulsar Home · incluye el día de la semana",()->togglePreference("overlay_clock",true));
    adjustable("opacity","Opacidad: "+Math.min(90,prefs.getInt("opacity",80))+"%","← → ajustar · 70 a 90%",()->setOpacity(10),()->setOpacity(-10),()->setOpacity(10));
    addKey("animate","Animación del panel: "+(prefs.getBoolean("animate",false)?"sí":"no"),"Transición al abrir",()->togglePreference("animate",false));
    addKey("greeting","Mostrar saludo: "+(prefs.getBoolean("show_greeting",true)?"sí":"no"),"En la pantalla de inicio",()->togglePreference("show_greeting",true));
    addKey("date","Mostrar fecha: "+(prefs.getBoolean("show_date",true)?"sí":"no"),"En la pantalla de inicio",()->togglePreference("show_date",true));
    addKey("clock","Reloj de 24 horas: "+(prefs.getBoolean("time_24",false)?"sí":"no"),"Formato de hora del inicio y del panel",()->togglePreference("time_24",false));
    addKey("editHome","Nombre y elementos del inicio","Abre la pantalla de personalización",()->nativePage("Personalizar inicio",new Intent(this,PersonalizeActivity.class)));
    addKey("systemAnimations","Animaciones de Android","Velocidad de las transiciones del sistema",()->push(new Page("animations",null)));return true;
   case "regional":
    title.setText("Fecha e idioma");subtitle.setText(new java.text.SimpleDateFormat("dd MMM yyyy · HH:mm",new Locale("es")).format(new Date()));
    addKey("autoTime","Hora automática: "+(Settings.Global.getInt(getContentResolver(),Settings.Global.AUTO_TIME,0)==1?"sí":"no"),"",canSecure()?()->toggleGlobal(Settings.Global.AUTO_TIME):null);
    addKey("autoZone","Zona automática: "+(Settings.Global.getInt(getContentResolver(),Settings.Global.AUTO_TIME_ZONE,0)==1?"sí":"no"),TimeZone.getDefault().getID(),canSecure()?()->toggleGlobal(Settings.Global.AUTO_TIME_ZONE):null);
    addKey("timezone","Elegir zona horaria","Desactiva la zona horaria automática",ready()&&canSecure()?()->push(new Page("timezone",null)):null);
    addKey("language","Idioma: "+Locale.getDefault().getDisplayLanguage(new Locale("es")),"Cambiar en Android · abre otra pantalla",()->nativeSettings("Idioma",Settings.ACTION_LOCALE_SETTINGS));
    addKey("nativeDate","Fecha y hora avanzadas","Abre la configuración de Android",()->nativeSettings("Fecha y hora",Settings.ACTION_DATE_SETTINGS));return true;
   case "storage":{
    title.setText("Almacenamiento");subtitle.setText(storageSummary());info(cachedStoragePercent<0?"Consultando espacio":cachedStoragePercent+"% utilizado");
    addKey("manage","Administrar aplicaciones","Organizar las apps del panel",()->push(new Page("manage",null)));
    addKey("nativeStorage","Almacenamiento en Android","Consultar y administrar el espacio del equipo",()->nativeSettings("Almacenamiento",Settings.ACTION_INTERNAL_STORAGE_SETTINGS));return true;
   }
   case "status":
    title.setText("Información del equipo");subtitle.setText("Datos consultados a Android");info("Android "+Build.VERSION.RELEASE+" · API "+Build.VERSION.SDK_INT);info("Modelo: "+Build.MODEL);info(storageSummary());
    addKey("display","Pantalla","Resolución y frecuencia declaradas",()->push(new Page("display",null)));addKey("diagnostics","Diagnóstico y permisos",bridgeStatus,()->push(new Page("diagnostics",null)));return true;
   case "manage":
    title.setText("Aplicaciones");subtitle.setText("Apps que pueden abrirse · "+managerFilter);
    adjustable("filter","Mostrar: "+managerFilter,"← → cambiar filtro",()->cycleFilter(1),()->cycleFilter(-1),()->cycleFilter(1));
    for(AppCatalog.Entry e:catalog.all()){if(managerFilter.equals("Usuario")&&e.system||managerFilter.equals("Sistema")&&!e.system||managerFilter.equals("Ocultas")&&catalog.visible(e))continue;addKey("app:"+e.pkg,e.title,(catalog.visible(e)?"Visible en el panel":"Oculta del panel")+(e.system?" · sistema":""),()->push(new Page("app",e.pkg)));}
    addKey("install","Instalar aplicaciones","Google Play o Downloader",()->push(new Page("install",null)));
    addKey("allAndroid","Todas las apps en Android","Incluye servicios y aplicaciones sin icono",()->nativeSettings("Aplicaciones",Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS));
    addKey("resetOrder","Restaurar orden original","Solicita confirmación",()->push(new Page("resetOrder",null)));return true;
   case "install":
    title.setText("Instalar aplicaciones");subtitle.setText("Se abrirá la aplicación elegida");for(String pkg:new String[]{"com.android.vending","com.esaba.downloader"}){final AppCatalog.Entry e=catalog.find(pkg);addKey(pkg,pkg.equals("com.android.vending")?"Google Play":"Downloader",e==null?"No disponible":"",e==null?null:()->launch(e));}return true;
   case "resetOrder":
    title.setText("Restaurar orden");subtitle.setText("Se restablecerá el orden de las apps del panel.");addKey("confirmReset","Restaurar","",()->{catalog.resetOrder();pages.pop();renderSide();});addKey("cancel","Cancelar","",this::back);p.selectedId=p.selectedId==null?"cancel":p.selectedId;return true;
   case "app":{
    AppCatalog.Entry e=catalog.find(p.pkg);title.setText(e==null?"App no disponible":e.title);subtitle.setText("Opciones de esta aplicación");if(e==null)return true;
    addKey("open","Abrir","Se abrirá la aplicación",()->launch(e));
    addKey("visibility",catalog.visible(e)?"Ocultar del panel":"Mostrar en el panel","La aplicación permanece instalada",()->{catalog.toggleVisible(e);renderSide();});
    adjustable("move","Orden en el panel",catalog.visible(e)?"← antes · después →":"Primero muestra esta app en el panel",catalog.visible(e)?()->moveApp(e,1):null,catalog.visible(e)?()->moveApp(e,-1):null,catalog.visible(e)?()->moveApp(e,1):null);
    addKey("permissions","Información y permisos","Abre la configuración de esta app en Android",()->nativePage("Información y permisos",appSettings(e.pkg)));
    if(restartable(e.pkg))addKey("restart","Reiniciar aplicación",ready()?"Se cerrará y volverá a abrir":"Requiere recuperar el control",ready()?()->push(new Page("restartConfirm",e.pkg)):null);
    if(!e.system&&!Arrays.asList(AppCatalog.PRIORITY).contains(e.pkg))addKey("uninstall","Desinstalar","Eliminar la aplicación y sus datos",ready()?()->push(new Page("uninstall",e.pkg)):null);
    return true;
   }
   case "btForget":{
    title.setText("Olvidar dispositivo");subtitle.setText("Tendrás que volver a emparejarlo para usarlo.");android.bluetooth.BluetoothDevice d=bluetooth.adapter.getRemoteDevice(p.pkg);
    addKey("confirmForget","Olvidar "+bluetooth.label(d),"",()->{if(bluetooth.forget(d)){pages.pop();if(!pages.isEmpty()&&pages.peek().name.equals("btDevice"))pages.pop();render();}else message("No se pudo olvidar el dispositivo");});addKey("cancel","Cancelar","",this::back);p.selectedId=p.selectedId==null?"cancel":p.selectedId;return true;
   }
   case "uninstall":{
    AppCatalog.Entry e=catalog.find(p.pkg);title.setText("Desinstalar aplicación");subtitle.setText(e==null?"Aplicación no disponible":e.title+" · se eliminarán la app y sus datos.");
    addKey("confirmUninstall","Desinstalar","Esta acción no conserva los datos",e!=null&&!e.system&&!Arrays.asList(AppCatalog.PRIORITY).contains(e.pkg)&&ready()?()->systemCommand("UNINSTALL:"+e.pkg):null);addKey("cancel","Cancelar","",this::back);p.selectedId=p.selectedId==null?"cancel":p.selectedId;return true;
   }
   case "btOff":
    title.setText("Desactivar Bluetooth");subtitle.setText("Se desconectarán el parlante y los controles Bluetooth.");addKey("confirmBluetoothOff","Desactivar","",()->{if(bluetooth.adapter.disable()){pages.pop();render();}else message("Android no aceptó desactivar Bluetooth");});addKey("cancel","Cancelar","",this::back);p.selectedId=p.selectedId==null?"cancel":p.selectedId;return true;
   default:return false;
  }
 }
 private void setOpacity(int delta){int old=prefs.getInt("opacity",80);prefs.edit().putInt("opacity",Math.max(70,Math.min(90,old+delta))).apply();renderSide();}
 private void cycleFilter(int delta){String[] filters={"Todas","Usuario","Sistema","Ocultas"};int index=Arrays.asList(filters).indexOf(managerFilter);managerFilter=filters[(index+delta+4)%4];renderSide();}
 private void moveApp(AppCatalog.Entry e,int direction){if(catalog.moveVisible(e,direction)){renderSide();message("Orden guardado");}else message(direction<0?"Ya es la primera aplicación":"Ya es la última aplicación");}
 String inspectForShell()throws org.json.JSONException{
  boolean clockVisible=panel!=null&&pages.isEmpty()&&clockPanel!=null&&panelClock!=null&&panelDate!=null&&clockPanel.isShown();
  org.json.JSONObject result=new org.json.JSONObject();result.put("visible",panel!=null);result.put("page",pages.isEmpty()?"bottom":pages.peek().name);result.put("title",title==null?"":title.getText());result.put("subtitle",subtitle==null?"":subtitle.getText());result.put("bridgeReady",ready());result.put("foreground",foregroundPackage);result.put("homeVisible",HomeActivity.visible);result.put("timer",SleepTimer.summary(this));result.put("timerRemaining",SleepTimer.remainingMillis(this));
  result.put("panelClockEnabled",prefs.getBoolean("overlay_clock",true));result.put("panelClockVisible",clockVisible);result.put("panelClockText",clockVisible?panelClock.getText()+" · "+panelDate.getText():"");
  if(clockVisible){int[] xy=new int[2];clockPanel.getLocationOnScreen(xy);org.json.JSONObject bounds=new org.json.JSONObject();bounds.put("x",xy[0]);bounds.put("y",xy[1]);bounds.put("width",clockPanel.getWidth());bounds.put("height",clockPanel.getHeight());result.put("panelClockBounds",bounds);result.put("panelClockTextColor",String.format(Locale.ROOT,"#%06X",panelClock.getCurrentTextColor()&0xFFFFFF));result.put("panelDateTextColor",String.format(Locale.ROOT,"#%06X",panelDate.getCurrentTextColor()&0xFFFFFF));}
  org.json.JSONArray rows=new org.json.JSONArray();for(int i=0;i<actions.size();i++){Action a=actions.get(i);org.json.JSONObject row=new org.json.JSONObject();row.put("id",a.id);row.put("label",a.label);row.put("detail",a.detail);row.put("enabled",a.run!=null);row.put("selected",!pages.isEmpty()&&pages.peek().selected==i);if(i<sideViews.size()){View v=sideViews.get(i);int[] xy=new int[2];v.getLocationOnScreen(xy);row.put("x",xy[0]);row.put("y",xy[1]);row.put("width",v.getWidth());row.put("height",v.getHeight());}rows.put(row);}result.put("actions",rows);return result.toString();
 }
 void openForShell(String name){
  if(name==null)name="settings";if(!Arrays.asList("settings","sound","network","bluetooth","appearance","manage","system","timer","diagnostics","regional","storage","status","animations","timezone","display").contains(name))throw new IllegalArgumentException("Unknown page");
  if(panel!=null)hide();openPage(name);
 }
 String actionForShell(String id){for(int i=0;i<actions.size();i++)if(actions.get(i).id.equals(id)){if(actions.get(i).run==null)return "DISABLED";if(!pages.isEmpty()){pages.peek().selected=i;pages.peek().selectedId=id;pages.peek().interacted=true;}runAction(i);return "OK";}return "NOT_FOUND";}
 void keyForShell(int key){if(!Arrays.asList(3,4,19,20,21,22,23,176).contains(key))throw new IllegalArgumentException("Unsupported key");long now=SystemClock.uptimeMillis();onKeyEvent(new KeyEvent(now,now,KeyEvent.ACTION_DOWN,key,0));onKeyEvent(new KeyEvent(now,now+30,KeyEvent.ACTION_UP,key,0));}
 void prepareForRestore(){localGeneration++;foregroundGeneration++;localStarting=false;localAttempts=0;hide();}
 private android.graphics.drawable.StateListDrawable focusSelector(){android.graphics.drawable.StateListDrawable d=new android.graphics.drawable.StateListDrawable();d.addState(new int[]{android.R.attr.state_selected},focusBackground(true));d.addState(new int[0],focusBackground(false));return d;}
 private void updatePanelLayout(WindowManager.LayoutParams desired){android.view.ViewGroup.LayoutParams p=panel.getLayoutParams();if(p instanceof WindowManager.LayoutParams){WindowManager.LayoutParams old=(WindowManager.LayoutParams)p;if(old.width==desired.width&&old.height==desired.height&&old.gravity==desired.gravity&&old.x==desired.x&&old.y==desired.y)return;}wm.updateViewLayout(panel,desired);}
 private void scheduleTimerTick(){ui.removeCallbacks(tickSettings);long left=SleepTimer.remainingMillis(this);if(panel!=null&&left>0)ui.postDelayed(tickSettings,Math.max(250,left%60000+50));}
 private void renderSide(){
  if(panel==null||pages.isEmpty())return;Page page=pages.peek();String screenKey=page.name+"|"+page.pkg;Screen screen=screens.get(screenKey);
  hideClockPanel();
  if(screen!=null){title=screen.title;subtitle=screen.subtitle;}else{title=text("",20,Color.WHITE);subtitle=text("",11,0xFFBDC7CE);}
  actions.clear();
  if(buildModern(page)){
  }else if(page.name.equals("settings")){
   title.setText("Ajustes");subtitle.setText("");
   add("Ajustes de Android","Abrir la configuración nativa del TV Box",()->launchIntent(new Intent(Settings.ACTION_SETTINGS)));
   add("Personalizar inicio","Nombre, reloj y elementos visibles",()->launchIntent(new Intent(this,PersonalizeActivity.class)));
   add("Sonido","",()->push(new Page("sound",null)));
   add("Protección visual","Perfiles de brillo para reducir la fatiga visual",()->push(new Page("screenProfiles",null)));
   add("Apariencia","",()->push(new Page("appearance",null)));
   add("Aplicaciones","",()->push(new Page("manage",null)));
   add("Conexiones","",()->push(new Page("connections",null)));
   add("Equipo","",()->push(new Page("system",null)));
  }else if(page.name.equals("connections")){
   title.setText("Conexiones");subtitle.setText("");
   add("Red","",()->push(new Page("network",null)));
   add("Bluetooth","",()->push(new Page("bluetooth",null)));
  }else if(page.name.equals("bluetooth")){
   title.setText("Bluetooth");android.bluetooth.BluetoothAdapter adapter=bluetooth.adapter;
   if(adapter==null){subtitle.setText("No disponible");}
   else if(Build.VERSION.SDK_INT>30){add("Bluetooth","",()->nativeSettings("Bluetooth",Settings.ACTION_BLUETOOTH_SETTINGS));}
   else{
    java.util.List<android.bluetooth.BluetoothDevice> bluetoothDevices=bluetooth.devices();
    java.util.ArrayList<android.bluetooth.BluetoothDevice> pairedDevices=new java.util.ArrayList<>();
    java.util.ArrayList<android.bluetooth.BluetoothDevice> foundDevices=new java.util.ArrayList<>();
    for(android.bluetooth.BluetoothDevice d:bluetoothDevices){if(d.getBondState()==android.bluetooth.BluetoothDevice.BOND_BONDED)pairedDevices.add(d);else foundDevices.add(d);}
    android.bluetooth.BluetoothDevice connectedDevice=null;
    for(android.bluetooth.BluetoothDevice d:pairedDevices){
     int audioState=bluetooth.audioState(d);
     if(audioState==android.bluetooth.BluetoothProfile.STATE_CONNECTED&&connectedDevice==null)connectedDevice=d;
    }
    subtitle.setText(adapter.isDiscovering()?"Buscando dispositivos…":adapter.isEnabled()?"Activado":"Desactivado");
    if(adapter.isEnabled()){
     info(connectedDevice==null?"Conectado: Ningún dispositivo":"Conectado: "+bluetooth.label(connectedDevice));
     if(connectedDevice!=null&&bluetooth.audioControlAvailable()){
      final android.bluetooth.BluetoothDevice device=connectedDevice;
      addKey("btDisconnect:"+device.getAddress(),"Desconectar dispositivo","Libera el parlante para conectar otro dispositivo",()->{
       message(bluetooth.audioConnection(device,false));
       ui.postDelayed(()->{if(panel!=null&&!pages.isEmpty()&&pages.peek().name.equals("bluetooth"))renderSide();},500);
      });
     }
    }
    if(!adapter.isEnabled())addKey("btEnable","Activar Bluetooth","Enciende Bluetooth sin salir del video",()->{if(!adapter.enable())message("No se pudo activar Bluetooth");});
    else{
     addKey("btSearch",adapter.isDiscovering()?"Detener búsqueda":"Buscar dispositivos",adapter.isDiscovering()?"Buscando equipos cercanos…":"Busca equipos cercanos",()->{
      if(adapter.isDiscovering()){bluetooth.stopScan();renderSide();return;}
      btShowFound=true;if(!bluetooth.scan())message("Búsqueda no disponible. Comprueba Ubicación en Android.");
     });
     if(btShowFound&&(!foundDevices.isEmpty()||adapter.isDiscovering())){
      for(android.bluetooth.BluetoothDevice d:foundDevices){
       final android.bluetooth.BluetoothDevice device=d;
       addDevice("btFound:"+d.getAddress(),bluetooth.label(d),bluetooth.state(d),()->{
        push(new Page("btPairConfirm",device.getAddress()));
       });
      }
     }
     info("Dispositivos guardados: "+pairedDevices.size());
     if(btShowPaired){
      for(android.bluetooth.BluetoothDevice d:pairedDevices){
       final android.bluetooth.BluetoothDevice device=d;
       addDevice("btDevice:"+d.getAddress(),bluetooth.label(d),bluetooth.state(d),()->push(new Page("btDevice",device.getAddress())));
      }
     }
     addKey("btOff","Desactivar Bluetooth","Desconecta temporalmente los dispositivos",()->push(new Page("btOff",null)));
    }

   }
  }else if(page.name.equals("btPairConfirm")){
   android.bluetooth.BluetoothDevice device=bluetooth.adapter.getRemoteDevice(page.pkg);
   title.setText("Vincular dispositivo");subtitle.setText("¿Quieres vincular este dispositivo?");
   info(bluetooth.label(device));info("Asegúrate de que esté encendido y en modo pairing.");
   add("Vincular ahora","Iniciar el emparejamiento",()->{
    if(device.getBondState()==android.bluetooth.BluetoothDevice.BOND_BONDED){pages.pop();render();return;}
    if(!bluetooth.pair(device,null)){message("Android no permite iniciar el emparejamiento");return;}
    title.setText("Vinculación Bluetooth");
    subtitle.setText("Esperando notificación de vinculación…");
    while(!actions.isEmpty())actions.remove(actions.size()-1);
    add("Cancelar","Detener la solicitud",()->{bluetooth.cancelPairing(device);pages.clear();hide();});
    renderSide();
    ui.postDelayed(()->{if(panel!=null&&!pages.isEmpty()&&pages.peek().name.equals("btPairConfirm"))hide();},1200);
   });
   add("Cancelar","Volver a la lista",this::back);
  }else if(page.name.equals("btOff")){
   title.setText("Desactivar Bluetooth");subtitle.setText("Se desconectarán parlantes y controles Bluetooth");add("Desactivar","",()->{if(bluetooth.adapter.disable()){pages.pop();render();}else message("No se pudo desactivar");});
  }else if(page.name.equals("btLocation")){
   title.setText("Búsqueda Bluetooth");subtitle.setText("Android puede pedir confirmación para activar Ubicación");boolean enabled=Settings.Secure.getInt(getContentResolver(),Settings.Secure.LOCATION_MODE,0)!=0;
   add(enabled?"Desactivar Ubicación":"Activar Ubicación","",()->{if(!canSecure()){message("Permiso de sistema pendiente");return;}bluetooth.stopScan();if(!enabled)hide();if(Settings.Secure.putInt(getContentResolver(),Settings.Secure.LOCATION_MODE,enabled?0:1)){if(enabled)renderSide();}else message("No se pudo cambiar Ubicación");});
   if(enabled)add("Buscar dispositivos","",()->{pages.pop();if(!bluetooth.scan())message("No se pudo iniciar la búsqueda");else render();});
  }else if(page.name.equals("btDevice")){
   android.bluetooth.BluetoothDevice device=bluetooth.adapter.getRemoteDevice(page.pkg);title.setText(bluetooth.label(device));subtitle.setText(bluetooth.state(device));
   info(bluetooth.supportsAudio(device)?"Dispositivo de audio":"Dispositivo Bluetooth");
   if(bluetooth.supportsAudio(device)&&bluetooth.audioControlAvailable()){
    int state=bluetooth.audioState(device);
    if(state==android.bluetooth.BluetoothProfile.STATE_CONNECTED){
     addKey("useAudio",bluetooth.isActive(device)?"Salida activa":"Usar como salida","Envía el audio del TV Box a este dispositivo",()->message(bluetooth.useAudio(device)));
     addKey("disconnectAudio","Desconectar audio","Finaliza la conexión con este dispositivo",()->message(bluetooth.audioConnection(device,false)));
    }else if(state==android.bluetooth.BluetoothProfile.STATE_CONNECTING)info("Conexión en curso…");
    else addKey("connectAudio","Conectar audio","Usar este dispositivo como salida de sonido",()->message(bluetooth.audioConnection(device,true)));
   }else if(bluetooth.supportsAudio(device))info("El firmware no permite controlar esta conexión");
   else info("Este dispositivo no ofrece salida de audio");
   if(device.getBondState()==android.bluetooth.BluetoothDevice.BOND_BONDED)addKey("forget","Olvidar dispositivo","Tendrás que volver a emparejarlo",()->push(new Page("btForget",device.getAddress())));
  }else if(page.name.equals("system")){
   title.setText("Equipo");subtitle.setText("");
   add("Pantalla · información","",()->push(new Page("display",null)));
   add("Fecha e idioma","",()->push(new Page("regional",null)));
   add("Almacenamiento","",()->push(new Page("storage",null)));
   add("Animaciones del sistema","",()->push(new Page("animations",null)));
   add("Acerca del equipo","",()->push(new Page("status",null)));
   add("Inicio del control avanzado","",()->push(new Page("localControl",null)));
   add("Reiniciar","",()->push(new Page("rebootConfirm",null)));
  }else if(page.name.equals("localControl")){
   title.setText("Control avanzado");subtitle.setText(prefs.getBoolean("local_adb_enabled",false)?"Inicio local activado":"Requiere autorizar una clave local una vez");
   add(prefs.getBoolean("local_adb_enabled",false)?"Reactivar ahora":"Autorizar inicio autónomo","",()->startLocalControl(true));
   if(prefs.getBoolean("local_adb_enabled",false))add("Desactivar inicio automático","",()->{localGeneration++;prefs.edit().putBoolean("local_adb_enabled",false).apply();renderSide();});
   add("Comprobar conexión","",()->systemCommand("PING"));
  }else if(page.name.equals("animations")){
   title.setText("Animaciones");subtitle.setText(canSecure()?"":"Permiso de sistema pendiente");
   for(float scale:new float[]{0f,0.5f,1f}){final float value=scale;add(scale==0?"Desactivadas":scale==0.5f?"Rápidas":"Normales","",()->{if(!canSecure()){message("Permiso de sistema pendiente");return;}boolean saved=true;for(String key:new String[]{Settings.Global.WINDOW_ANIMATION_SCALE,Settings.Global.TRANSITION_ANIMATION_SCALE,Settings.Global.ANIMATOR_DURATION_SCALE})saved&=Settings.Global.putFloat(getContentResolver(),key,value);message(saved?"Guardado":"No se pudo guardar");});}
  }else if(page.name.equals("network")){
   title.setText("Wi-Fi");subtitle.setText("Conecta, desconecta o busca redes");
   android.net.wifi.WifiManager wifi=(android.net.wifi.WifiManager)getApplicationContext().getSystemService(WIFI_SERVICE);
   boolean wifiEnabled=wifi.isWifiEnabled();
   add(wifiEnabled?"Desconectar Wi-Fi":"Activar Wi-Fi",wifiEnabled?"Apaga la conexión inalámbrica":"Enciende la radio Wi-Fi",()->{if(wifi.isWifiEnabled())push(new Page("wifiConfirm",null));else systemCommand("WIFI_ON");});
   add("Buscar redes Wi-Fi","Buscar dentro de este panel sin cerrar el video",()->{push(new Page("wifiScan",null));startWifiScan();});
   ConnectivityManager cm=(ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);Network n=cm.getActiveNetwork();NetworkCapabilities caps=cm.getNetworkCapabilities(n);
   boolean wifiConnected=caps!=null&&caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
   info(!wifiEnabled?"Wi-Fi apagado":wifiConnected?"Wi-Fi conectado":"Wi-Fi encendido · sin red");
   if(wifiConnected){
    String ssid=wifi.getConnectionInfo()==null?null:wifi.getConnectionInfo().getSSID();
    if(ssid!=null&&!ssid.equals("<unknown ssid>"))info("Red  "+ssid.replace("\"",""));
    info(caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)?"Internet verificado":"Internet sin verificar");
   }
   LinkProperties links=cm.getLinkProperties(n);if(links!=null)for(LinkAddress addr:links.getLinkAddresses())if(addr.getAddress() instanceof java.net.Inet4Address)info("IP  "+addr.getAddress().getHostAddress());
  }else if(page.name.equals("wifiScan")){
   title.setText("Buscar redes Wi-Fi");subtitle.setText(wifiScanPending?"Buscando redes…":"Selecciona una red para conectar");
   add("Actualizar búsqueda","",()->startWifiScan());
   android.net.wifi.WifiManager wifi=(android.net.wifi.WifiManager)getApplicationContext().getSystemService(WIFI_SERVICE);
   if(wifi.isWifiEnabled()&&!wifiScanPending){
    java.util.List<android.net.wifi.ScanResult> results=new java.util.ArrayList<>(wifi.getScanResults());
    java.util.Collections.sort(results,(a,b)->Integer.compare(b.level,a.level));
    java.util.HashSet<String> seen=new java.util.HashSet<>();
    for(android.net.wifi.ScanResult result:results){
     if(result.SSID==null||result.SSID.isEmpty()||!seen.add(result.SSID))continue;
     final android.net.wifi.ScanResult chosen=result;
     boolean open=result.capabilities==null||(!result.capabilities.contains("WEP")&&!result.capabilities.contains("WPA"));
     add(result.SSID,(open?"Abierta":"Protegida")+" · señal "+result.level+" dBm",()->connectWifi(chosen));
    }
    if(seen.isEmpty())info("No se encontraron redes");
   }else if(!wifi.isWifiEnabled())info("Activa Wi-Fi para buscar redes");
  }else if(page.name.equals("wifiConfirm")){
   title.setText("Desconectar Wi-Fi");subtitle.setText("Se interrumpirá la conexión a Internet");add("Desconectar","",()->systemCommand("WIFI_OFF"));
  }else if(page.name.equals("rebootConfirm")){
   title.setText("Reiniciar equipo");subtitle.setText("Se cerrarán las aplicaciones");add("Reiniciar","",()->systemCommand("REBOOT"));
  }else if(page.name.equals("display")){
   title.setText("Pantalla");subtitle.setText("Solo lectura");Display display=wm.getDefaultDisplay();android.graphics.Point size=new android.graphics.Point();display.getRealSize(size);
   info(size.x+" × "+size.y);info(Math.round(display.getRefreshRate())+" Hz");info("Escala  "+getResources().getDisplayMetrics().densityDpi+" dpi");
  }else if(page.name.equals("screenProfiles")){
   title.setText("Protección visual");subtitle.setText("Perfiles de brillo reversibles; no cambian el color real de las apps");
   add("Normal",prefs.getInt("screen_profile",0)==0?"Activo":"",()->setScreenProfile(0));
   add("Luz alta",prefs.getInt("screen_profile",0)==1?"Activo":"",()->setScreenProfile(1));
   add("Noche",prefs.getInt("screen_profile",0)==2?"Activo":"",()->setScreenProfile(2));
  }else if(page.name.equals("regional")){
   title.setText("Fecha e idioma");subtitle.setText(canSecure()?"":"Permiso de sistema pendiente");
   if(canSecure()){add("Hora automática: "+(Settings.Global.getInt(getContentResolver(),Settings.Global.AUTO_TIME,0)==1?"sí":"no"),"",()->toggleGlobal(Settings.Global.AUTO_TIME));add("Zona automática: "+(Settings.Global.getInt(getContentResolver(),Settings.Global.AUTO_TIME_ZONE,0)==1?"sí":"no"),"",()->toggleGlobal(Settings.Global.AUTO_TIME_ZONE));}
   add("Elegir zona horaria","",()->push(new Page("timezone",null)));
   info(new java.text.SimpleDateFormat("dd MMM yyyy · HH:mm",new Locale("es")).format(new Date()));info(TimeZone.getDefault().getID());info(Locale.getDefault().getDisplayLanguage(new Locale("es")));
  }else if(page.name.equals("timezone")){
   title.setText("Zona horaria");subtitle.setText("La selección desactiva la zona automática");
   String[] labels={"Lima","Bogotá","Ciudad de México","Buenos Aires","Santiago","Madrid","UTC"};String[] zones={"America/Lima","America/Bogota","America/Mexico_City","America/Argentina/Buenos_Aires","America/Santiago","Europe/Madrid","UTC"};
   for(int i=0;i<labels.length;i++){final String zone=zones[i];add(labels[i],"",()->setTimezone(zone));}
  }else if(page.name.equals("storage")){
   title.setText("Almacenamiento");subtitle.setText("Solo lectura");StatFs fs=new StatFs(Environment.getDataDirectory().getPath());
   info((fs.getAvailableBytes()/1048576)+" MB libres");info((fs.getTotalBytes()/1048576)+" MB totales");info(((fs.getTotalBytes()-fs.getAvailableBytes())*100/fs.getTotalBytes())+"% utilizado");
   add("Administrar aplicaciones","",()->push(new Page("manage",null)));
  }else if(page.name.equals("sound")){
   AudioManager a=audio();title.setText("Sonido");subtitle.setText(a.isVolumeFixed()?"Volumen fijo: usa el televisor":"Volumen "+a.getStreamVolume(3)+" / "+a.getStreamMaxVolume(3)+(a.isStreamMute(3)?" · Silenciado":""));
   add("Volumen −","",()->volume(AudioManager.ADJUST_LOWER));
   add("Volumen +","",()->volume(AudioManager.ADJUST_RAISE));
   add(a.isStreamMute(3)?"Activar sonido":"Silenciar","",()->volume(AudioManager.ADJUST_TOGGLE_MUTE));
   add("Dispositivos Bluetooth","",()->push(new Page("bluetooth",null)));
  }else if(page.name.equals("appearance")){
   title.setText("Apariencia");subtitle.setText("");
   add("Opacidad: "+Math.min(90,prefs.getInt("opacity",80))+"%","70 / 80 / 90%",()->{int n=prefs.getInt("opacity",80);prefs.edit().putInt("opacity",n>=90?70:n+10).apply();renderSide();});
   add(prefs.getBoolean("compact",true)?"Tarjetas: compactas":"Tarjetas: amplias","Tamaño del panel inferior",()->{prefs.edit().putBoolean("compact",!prefs.getBoolean("compact",true)).apply();renderSide();});
   add(prefs.getBoolean("animate",false)?"Animación: activada":"Animación: desactivada","Transición breve al abrir el panel",()->{prefs.edit().putBoolean("animate",!prefs.getBoolean("animate",false)).apply();renderSide();});
   add("Volumen: directo","Indicador del sistema desactivado",()->{prefs.edit().putBoolean("custom_volume",true).apply();hideVolumeHud();renderSide();finishLocalControl(true,0);});
  }else if(page.name.equals("status")){
   title.setText("Estado del equipo");
   add("Comprobar control avanzado","",()->systemCommand("PING"));
   ConnectivityManager c=(ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);NetworkCapabilities n=c.getNetworkCapabilities(c.getActiveNetwork());StatFs fs=new StatFs(Environment.getDataDirectory().getPath());
   subtitle.setText("Información consultada directamente a Android");
   add("Android "+Build.VERSION.RELEASE+" · API "+Build.VERSION.SDK_INT,"Modelo declarado: "+Build.MODEL,()->message("Los datos de modelo son los declarados por el firmware."));
   add(n==null?"Sin red activa":n.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)?"Wi-Fi conectado":n.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)?"Ethernet conectado":"Red activa","No es una prueba de velocidad de Internet",()->message("La conexión se conserva bajo la gestión de Android."));
   add((fs.getAvailableBytes()/1048576)+" MB disponibles","Almacenamiento de datos",()->message("Ocultar o deshabilitar apps no equivale a liberar sus datos."));
  }else if(page.name.equals("manage")){
   title.setText("Aplicaciones");subtitle.setText("");
   add("Filtro: "+managerFilter,"Todas / Usuario / Sistema / Ocultas",()->{String[] filters={"Todas","Usuario","Sistema","Ocultas"};managerFilter=filters[(Arrays.asList(filters).indexOf(managerFilter)+1)%4];page.selected=0;renderSide();});
   add("Abrir Google Play","Instalar aplicaciones desde la tienda",()->launch(catalog.find("com.android.vending")));
   add("Abrir Downloader","Descargar APK de una fuente que autorices",()->launch(catalog.find("com.esaba.downloader")));
   add("Restaurar orden original","Devuelve la fila a la organización inicial",()->{catalog.resetOrder();page.selected=0;message("Orden original restaurado");renderSide();});
   for(AppCatalog.Entry e:catalog.all()){
    if(managerFilter.equals("Usuario")&&e.system||managerFilter.equals("Sistema")&&!e.system||managerFilter.equals("Ocultas")&&catalog.visible(e))continue;
    add(e.title,(e.system?"Sistema":"Usuario")+" · "+(catalog.visible(e)?"Visible":"Oculta")+" · "+catalog.category(e),()->push(new Page("app",e.pkg)));
   }

  }else if(page.name.equals("panelAddApp")){
   title.setText("Añadir aplicación");subtitle.setText("Selecciona una aplicación para mostrarla en Apps");
   for(AppCatalog.Entry e:catalog.all())if(!catalog.visible(e)){final AppCatalog.Entry entry=e;add(e.title,"",()->addAppToPanel(entry));}

  }else if(page.name.equals("app")){
   AppCatalog.Entry e=catalog.find(page.pkg);title.setText(e==null?"App no disponible":e.title);subtitle.setText(page.pkg);
   if(e!=null){
    add("Abrir",e.system?"Aplicación de sistema":"Aplicación del usuario",()->launch(e));
    add(catalog.favorite(e)?"Quitar de favoritos":"Añadir a favoritos",catalog.visible(e)?"Acceso en la fila Favoritos":"Está oculta; muéstrala para verla en Favoritos",()->{catalog.toggleFavorite(e);renderSide();});
    add(catalog.visible(e)?"Ocultar del panel":"Mostrar en el panel","Solo cambia nuestra interfaz; la app permanece instalada",()->{catalog.toggleVisible(e);renderSide();});
    add("Categoría: "+catalog.category(e),"OK alterna TV y video / Herramientas / Otras",()->{catalog.cycleCategory(e);renderSide();});
    add("Mover antes","Un puesto en el orden global del catálogo",()->{if(!catalog.move(e,-1))message("Ya es la primera aplicación");else message("Orden guardado");});
    add("Mover después","Un puesto en el orden global del catálogo",()->{if(!catalog.move(e,1))message("Ya es la última aplicación");else message("Orden guardado");});
    add("Información y permisos","",()->push(new Page("appinfo",e.pkg)));
    if(!e.system&&!Arrays.asList(AppCatalog.PRIORITY).contains(e.pkg))add("Desinstalar","",()->push(new Page("uninstall",e.pkg)));

   }
  }else if(page.name.equals("uninstall")){
   AppCatalog.Entry e=catalog.find(page.pkg);title.setText("Desinstalar app");subtitle.setText("Se eliminarán la app y sus datos del usuario actual");if(e!=null)info(e.title);
   if(e!=null&&!e.system&&!Arrays.asList(AppCatalog.PRIORITY).contains(e.pkg))add("Desinstalar","",()->systemCommand("UNINSTALL:"+e.pkg));
  }else if(page.name.equals("appinfo")){
   title.setText("Información de la app");subtitle.setText("Solo lectura");
   try{PackageInfo pi=getPackageManager().getPackageInfo(page.pkg,PackageManager.GET_PERMISSIONS);info(pi.applicationInfo.loadLabel(getPackageManager()).toString());info("Versión "+pi.versionName);
    if(pi.requestedPermissions!=null)for(int i=0;i<pi.requestedPermissions.length;i++){String label=pi.requestedPermissions[i].replace("android.permission.","");try{label=getPackageManager().getPermissionInfo(pi.requestedPermissions[i],0).loadLabel(getPackageManager()).toString();}catch(Exception ignored){}
     info(label+((pi.requestedPermissionsFlags[i]&PackageInfo.REQUESTED_PERMISSION_GRANTED)!=0?" · permitido":" · sin conceder"));}
   }catch(Exception ex){info("App no disponible");}
  }else if(page.name.equals("restricted")){
   title.setText(page.description);subtitle.setText("Control no disponible con los permisos actuales");
  }
  addKey("back","Volver","",this::back);
  addKey("close","Cerrar","",this::hide);
  ArrayList<String> ids=new ArrayList<>();for(Action action:actions)ids.add(action.id);
  boolean full=screen==null||!screen.ids.equals(ids);
  if(full){
  generation++;PerfStats.fullRenders.incrementAndGet();String heading=title.getText().toString(),hint=subtitle.getText().toString();base(true);title.setText(heading);subtitle.setText(hint);screen=new Screen();screen.title=title;screen.subtitle=subtitle;screen.ids.addAll(ids);screens.put(screenKey,screen);
  vertical=new ScrollView(this);vertical.setFillViewport(false);sideRows=new LinearLayout(this);sideRows.setOrientation(1);vertical.addView(sideRows);
  LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,0,1);lp.topMargin=dp(8);panel.addView(vertical,lp);
  LinearLayout footer=new LinearLayout(this);LinearLayout.LayoutParams fp=new LinearLayout.LayoutParams(-1,dp(36));fp.topMargin=dp(8);panel.addView(footer,fp);
  for(int i=0;i<actions.size();i++){
   final int index=i;Action a=actions.get(i);LinearLayout cell=new LinearLayout(this);cell.setOrientation(1);cell.setGravity(Gravity.CENTER_VERTICAL);cell.setPadding(dp(10),dp(4),dp(10),dp(4));cell.setMinimumHeight(dp(40));
   TextView label=text(a.label,14,a.run==null?0xFF89939B:Color.WHITE);label.setSingleLine(true);label.setEllipsize(android.text.TextUtils.TruncateAt.END);cell.addView(label);screen.labels.add(label);
   TextView desc=text(a.detail,9,a.run==null?0xFF77818A:0xFFB9C2CB);desc.setMaxLines(page.name.equals("settings")?1:2);desc.setEllipsize(android.text.TextUtils.TruncateAt.END);cell.addView(desc);screen.details.add(desc);cell.setBackground(focusSelector());
   LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,page.name.equals("settings")?dp(42):-2);cp.bottomMargin=dp(2);if(i<actions.size()-2)sideRows.addView(cell,cp);else{LinearLayout.LayoutParams foot=new LinearLayout.LayoutParams(0,-1,1);foot.rightMargin=dp(4);footer.addView(cell,foot);}screen.rows.add(cell);
   final Screen target=screen;cell.setOnClickListener(v->{if(currentScreen!=target||pages.isEmpty()||index>=actions.size())return;Page activePage=pages.peek();Action action=actions.get(index);if(action.run==null)return;activePage.selected=index;activePage.selectedId=action.id;activePage.interacted=true;runAction(index);});
  }
  screen.scroll=vertical;screen.items=sideRows;for(int i=0;i<panel.getChildCount();i++)screen.children.add(panel.getChildAt(i));
  }else{
   PerfStats.binds.incrementAndGet();if(currentScreen!=screen){generation++;panel.removeAllViews();panel.setTranslationY(0);panel.setPadding(dp(16),dp(14),dp(16),dp(8));for(View child:screen.children){if(child.getParent()!=null)((android.view.ViewGroup)child.getParent()).removeView(child);panel.addView(child);}}
   vertical=screen.scroll;sideRows=screen.items;
  }
  currentScreen=screen;title=screen.title;subtitle=screen.subtitle;sideViews.clear();sideViews.addAll(screen.rows);
  if(lastPanelColor!=panelColor()){panel.setBackground(background(panelColor(),14));lastPanelColor=panelColor();}
  for(int i=0;i<actions.size();i++){Action a=actions.get(i);TextView label=screen.labels.get(i),desc=screen.details.get(i);label.setText(a.label);int color=a.run==null?0xFF89939B:Color.WHITE;if(label.getCurrentTextColor()!=color)label.setTextColor(color);desc.setText(a.detail);int detailColor=a.run==null?0xFF77818A:0xFFB9C2CB;if(desc.getCurrentTextColor()!=detailColor)desc.setTextColor(detailColor);desc.setVisibility(a.detail.isEmpty()?View.GONE:View.VISIBLE);screen.rows.get(i).setClickable(a.run!=null);screen.rows.get(i).setContentDescription(a.label);}
  subtitle.setVisibility(subtitle.getText().length()==0?View.GONE:View.VISIBLE);
  if(page.selectedId!=null)for(int i=0;i<actions.size();i++)if(actions.get(i).id.equals(page.selectedId)){page.selected=i;break;}
  page.selected=Math.max(0,Math.min(page.selected,actions.size()-1));while(page.selected<actions.size()-1&&actions.get(page.selected).run==null)page.selected++;updatePanelLayout(params(true));paintSide();scheduleTimerTick();
 }
 private void paintSide(){
  if(pages.isEmpty())return;int s=pages.peek().selected;if(s>=0&&s<actions.size())pages.peek().selectedId=actions.get(s).id;
  for(int i=0;i<sideViews.size();i++)if(sideViews.get(i).isSelected()!=(i==s)){sideViews.get(i).setSelected(i==s);PerfStats.focusUpdates.incrementAndGet();}
  if(s<sideViews.size()-2){View v=sideViews.get(s);ScrollView current=vertical;current.post(()->{if(panel!=null&&!pages.isEmpty()&&vertical==current){int top=v.getTop(),bottom=v.getBottom();if(top<current.getScrollY())current.smoothScrollTo(0,top);else if(bottom>current.getScrollY()+current.getHeight())current.smoothScrollTo(0,bottom-current.getHeight());}});}
 }
 private void runAction(int n){if(n>=0&&n<actions.size()&&actions.get(n).run!=null){long start=SystemClock.elapsedRealtimeNanos();try{actions.get(n).run.run();}catch(RuntimeException e){message("Android no permite esta acción: "+e.getClass().getSimpleName());}finally{PerfStats.action(SystemClock.elapsedRealtimeNanos()-start);}}}
 private void navigate(int key){
  if(!pages.isEmpty()){
   Page p=pages.peek();int footer=actions.size()-2;
   p.interacted=true;
   Action focused=p.selected>=0&&p.selected<actions.size()?actions.get(p.selected):null;
   if(focused!=null&&(key==KeyEvent.KEYCODE_DPAD_LEFT||key==KeyEvent.KEYCODE_DPAD_RIGHT)){Runnable adjust=key==KeyEvent.KEYCODE_DPAD_LEFT?focused.less:focused.more;if(adjust!=null){adjust.run();return;}}
   if(key==KeyEvent.KEYCODE_DPAD_LEFT){if(p.selected==actions.size()-1)p.selected=footer;else{back();return;}}
   else if(key==KeyEvent.KEYCODE_DPAD_RIGHT){if(p.selected>=footer)p.selected=actions.size()-1;}
   else if(key==KeyEvent.KEYCODE_DPAD_UP||key==KeyEvent.KEYCODE_DPAD_DOWN){int delta=key==KeyEvent.KEYCODE_DPAD_UP?-1:1;int start=p.selected>=footer&&delta<0?footer:p.selected;for(int i=start+delta;i>=0&&i<actions.size();i+=delta)if(actions.get(i).run!=null){p.selected=i;break;}}
   paintSide();return;
  }
  if(editMode){
   if(key==KeyEvent.KEYCODE_DPAD_LEFT){moveEdit(-1);return;}
   if(key==KeyEvent.KEYCODE_DPAD_RIGHT){moveEdit(1);return;}
   if(key==KeyEvent.KEYCODE_DPAD_UP){hideEdit();return;}
   if(key==KeyEvent.KEYCODE_DPAD_DOWN&&addSlot){return;}
  }
  if(key==KeyEvent.KEYCODE_DPAD_UP||key==KeyEvent.KEYCODE_DPAD_DOWN){
   if(key==KeyEvent.KEYCODE_DPAD_DOWN){
    if(selected<quickActions.size()&&!visible.isEmpty()){selected=quickActions.size()+Math.min(selected,visible.size()-1);paintBottom();return;}
    return;
   }
   if(selected>=quickActions.size()){selected=Math.min(quickActions.size()-1,selected-quickActions.size());paintBottom();return;}
   return;
  }
  if(key==KeyEvent.KEYCODE_DPAD_LEFT||key==KeyEvent.KEYCODE_DPAD_RIGHT){
   int delta=key==KeyEvent.KEYCODE_DPAD_RIGHT?1:-1;
   selected=Math.max(0,Math.min(cards.size()-1,selected+delta));
  }
  paintBottom();
 }
 private void confirm(boolean longer){
  if(!pages.isEmpty()){runAction(pages.peek().selected);return;}
  if(editMode){
   if(addSlot){push(new Page("panelAddApp",null));return;}
   saveEditMode();return;
  }
  if(selected>=0&&selected<quickActions.size()){quickActions.get(selected).run();return;}
  int appIndex=selected-quickActions.size();if(appIndex<0||appIndex>=visible.size())return;
  AppCatalog.Entry e=visible.get(appIndex);if(longer)beginEditMode();else launch(e);
 }
 protected boolean onKeyEvent(KeyEvent e){
  if(HomeActivity.isApplicationDialogOpen())return false;
  int k=e.getKeyCode();boolean down=e.getAction()==KeyEvent.ACTION_DOWN;
  if(k==KeyEvent.KEYCODE_VOLUME_UP||k==KeyEvent.KEYCODE_VOLUME_DOWN||k==KeyEvent.KEYCODE_VOLUME_MUTE){
   if(down&&(e.getRepeatCount()==0||k!=KeyEvent.KEYCODE_VOLUME_MUTE)){int direction=k==KeyEvent.KEYCODE_VOLUME_UP?AudioManager.ADJUST_RAISE:k==KeyEvent.KEYCODE_VOLUME_DOWN?AudioManager.ADJUST_LOWER:AudioManager.ADJUST_TOGGLE_MUTE;volume(direction);}
   return true;
  }
  if(!down){
   Integer started=keys.up(k);if(started==null)return false;
   if(e.isCanceled())return true;
   if(panel==null){if(!HomeActivity.visible)return false;if((k==KeyEvent.KEYCODE_DPAD_CENTER||k==KeyEvent.KEYCODE_ENTER)&&e.getEventTime()-e.getDownTime()>=650)HomeActivity.enterEditMode();return HomeActivity.handleHomeKey(e);}
   if(k==KeyEvent.KEYCODE_BACK||k==KeyEvent.KEYCODE_ESCAPE){if(e.getEventTime()-e.getDownTime()>=650)hide();else back();}
   else if((k==KeyEvent.KEYCODE_DPAD_CENTER||k==KeyEvent.KEYCODE_ENTER)&&started==generation)confirm(e.getEventTime()-e.getDownTime()>=650);
   return true;
  }
  boolean trigger=k==KeyEvent.KEYCODE_SETTINGS||k==KeyEvent.KEYCODE_MENU||k==KeyEvent.KEYCODE_HOME&&prefs.getBoolean("home",true);
  if(trigger){keys.down(k,generation);if(e.getRepeatCount()==0){if(k==KeyEvent.KEYCODE_SETTINGS)openPage("settings");else if(k==KeyEvent.KEYCODE_HOME){if(HomeActivity.visible){if(panel!=null)hide();}else if(panel==null)show();else{pages.clear();selected=quickActions.size();renderBottom();}}else toggle();}return true;}
  if(panel==null){if(!HomeActivity.visible)return false;if(k==KeyEvent.KEYCODE_DPAD_CENTER||k==KeyEvent.KEYCODE_ENTER){keys.down(k,generation);return true;}return HomeActivity.handleHomeKey(e);}
  boolean relevant=k==KeyEvent.KEYCODE_BACK||k==KeyEvent.KEYCODE_ESCAPE||k==KeyEvent.KEYCODE_DPAD_UP||k==KeyEvent.KEYCODE_DPAD_DOWN||k==KeyEvent.KEYCODE_DPAD_LEFT||k==KeyEvent.KEYCODE_DPAD_RIGHT||k==KeyEvent.KEYCODE_DPAD_CENTER||k==KeyEvent.KEYCODE_ENTER;
  if(!relevant)return false;
  keys.down(k,generation);
  if(k>=KeyEvent.KEYCODE_DPAD_UP&&k<=KeyEvent.KEYCODE_DPAD_RIGHT)navigate(k);
  return true;
 }
}
