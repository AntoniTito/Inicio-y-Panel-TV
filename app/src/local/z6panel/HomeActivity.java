package local.z6panel;
import android.app.*;
import android.content.*;
import android.os.*;
import android.provider.Settings;
import android.bluetooth.BluetoothAdapter;
import android.net.*;
import android.graphics.Color;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

/** Real HOME, lightweight and independent of the vendor launcher. */
public class HomeActivity extends Activity {
 public static boolean visible=false;
 private static HomeActivity current;
 private final Handler ui=new Handler(Looper.getMainLooper());
 private final ExecutorService worker=Executors.newSingleThreadExecutor();
 private SharedPreferences prefs;private AppCatalog catalog;private FrameLayout root;private LinearLayout content,appsRow,controlsView;private TextView clockHour,clockMeridiem,date,greeting,wifiState,bluetoothState,notice;private Dialog applicationDialog;private boolean resumed,editMode,addSlot,longCenterHandled,scanInProgress;private String editOrderSnapshot;private Set<String> editHiddenSnapshot,editShownSnapshot;private String editFocusPackage;private final Handler editUi=new Handler(Looper.getMainLooper());private final Runnable clearNotice=()->{if(notice==null)return;if(editMode)setEditHelp();else{notice.setText("");notice.setVisibility(View.GONE);}};private final Runnable longCenter=()->{if(isAppCard(getCurrentFocus())){beginEditMode();}};
 private final BroadcastReceiver networkChanges=new BroadcastReceiver(){public void onReceive(Context c,Intent i){updateStatus();}};
 private int category=0;private boolean catalogLoaded=false;
 private String lastAppsSignature;
 private final Runnable tick=new Runnable(){public void run(){updateStatus();if(resumed)ui.postDelayed(this,30000);}};
 public void onCreate(Bundle b){super.onCreate(b);current=this;getWindow().setFlags(1024,1024);prefs=getSharedPreferences("panel",0);catalog=new AppCatalog(this);catalogLoaded=catalog.restoreSnapshot();
  IntentFilter net=new IntentFilter(android.net.wifi.WifiManager.WIFI_STATE_CHANGED_ACTION);net.addAction(ConnectivityManager.CONNECTIVITY_ACTION);net.addAction(android.net.wifi.WifiManager.NETWORK_STATE_CHANGED_ACTION);if(Build.VERSION.SDK_INT>=33)registerReceiver(networkChanges,net,Context.RECEIVER_EXPORTED);else registerReceiver(networkChanges,net);
  render();
 }
 protected void onResume(){super.onResume();boolean redirected=!visible&&PanelService.active!=null&&PanelService.active.restoreOverlayAfterHome(getIntent());visible=true;resumed=true;if(PanelService.active!=null&&!redirected)PanelService.active.onHomeVisible();if(greeting!=null){greeting.setVisibility(prefs.getBoolean("show_greeting",true)?View.VISIBLE:View.GONE);date.setVisibility(prefs.getBoolean("show_date",true)?View.VISIBLE:View.GONE);controlsView.setVisibility(prefs.getBoolean("show_controls",true)?View.VISIBLE:View.GONE);}ui.removeCallbacks(tick);ui.post(tick);if(catalogLoaded){renderApps();return;}if(scanInProgress)return;scanInProgress=true;worker.execute(()->{try{java.util.List<AppCatalog.Entry> list=catalog.scan();ui.post(()->{if(!isDestroyed()){catalog.accept(list);catalogLoaded=true;renderApps();}scanInProgress=false;});}catch(Exception ignored){ui.post(()->scanInProgress=false);}});
 }
 protected void onPause(){visible=false;resumed=false;ui.removeCallbacks(tick);super.onPause();}
 protected void onDestroy(){if(applicationDialog!=null&&applicationDialog.isShowing())applicationDialog.dismiss();if(current==this)current=null;unregisterReceiver(networkChanges);ui.removeCallbacksAndMessages(null);worker.shutdownNow();super.onDestroy();}
 protected void onNewIntent(Intent i){super.onNewIntent(i);setIntent(i);}
 @Override public void onBackPressed(){if(applicationDialog!=null&&applicationDialog.isShowing()){applicationDialog.cancel();return;}if(editMode){cancelEdit();showNotice("EDICIÓN CANCELADA");return;}if(PanelService.active!=null)PanelService.active.onHomeVisible();}
 public static void enterEditMode(){if(current!=null)current.beginEditMode();}
 public static boolean handleHomeKey(KeyEvent e){return current!=null&&current.handleEditKey(e);}
 public static boolean isApplicationDialogOpen(){return current!=null&&current.applicationDialog!=null&&current.applicationDialog.isShowing();}
 public static void catalogChanged(){if(current!=null&&visible)current.ui.post(()->{if(current!=null&&!current.isDestroyed()){current.catalogLoaded=true;current.renderApps();}});}
 public static void refreshPreferences(){if(current!=null)current.ui.post(()->{if(current==null||current.isDestroyed())return;current.greeting.setVisibility(current.prefs.getBoolean("show_greeting",true)?View.VISIBLE:View.GONE);current.date.setVisibility(current.prefs.getBoolean("show_date",true)?View.VISIBLE:View.GONE);current.controlsView.setVisibility(current.prefs.getBoolean("show_controls",true)?View.VISIBLE:View.GONE);current.updateStatus();});}
 private void beginEditMode(){
  if(editMode)return;
  View focused=getCurrentFocus();if(!isAppCard(focused))return;
  editFocusPackage=((AppCatalog.Entry)focused.getTag()).pkg;
  editOrderSnapshot=catalog.editOrder();editHiddenSnapshot=catalog.editHidden();editShownSnapshot=catalog.editShown();
  editMode=true;addSlot=false;longCenterHandled=true;renderApps();
  if(editFocusPackage!=null)focusPackage(editFocusPackage);
 }
 private void cancelEdit(){catalog.restoreEditState(editOrderSnapshot,editHiddenSnapshot,editShownSnapshot);editOrderSnapshot=null;editHiddenSnapshot=null;editShownSnapshot=null;editMode=false;addSlot=false;String pkg=editFocusPackage;editFocusPackage=null;renderApps();if(pkg!=null)focusPackage(pkg);}
 private void saveEdit(){editOrderSnapshot=null;editHiddenSnapshot=null;editShownSnapshot=null;editMode=false;addSlot=false;String pkg=editFocusPackage;editFocusPackage=null;renderApps();if(pkg!=null)focusPackage(pkg);}
 private void activateFocused(){
  View focused=getCurrentFocus();
  if(focused==null)return;
  if(isAppCard(focused)||isControl(focused)||"add_application".equals(focused.getTag()))focused.performClick();
 }
 private boolean handleEditKey(KeyEvent e){
  if(applicationDialog!=null&&applicationDialog.isShowing())return false;
  if(!editMode)return false;
  int k=e.getKeyCode();
  if(k==KeyEvent.KEYCODE_DPAD_CENTER||k==KeyEvent.KEYCODE_ENTER){
   if(e.getAction()!=KeyEvent.ACTION_UP)return true;
   if(e.getEventTime()-e.getDownTime()>=650)return true;
   if(addSlot){addApplication();return true;}
   saveEdit();showNotice("ORDEN GUARDADO");return true;
  }
  if(e.getAction()!=KeyEvent.ACTION_DOWN||e.getRepeatCount()!=0)return true;
  if(k==KeyEvent.KEYCODE_DPAD_LEFT||k==KeyEvent.KEYCODE_DPAD_RIGHT){moveFocused(k==KeyEvent.KEYCODE_DPAD_LEFT?-1:1);return true;}
  if(k==KeyEvent.KEYCODE_DPAD_UP){hideFocused();return true;}
  if(k==KeyEvent.KEYCODE_BACK){cancelEdit();showNotice("EDICIÓN CANCELADA");return true;}
  return false;
 }
 @Override public boolean dispatchKeyEvent(KeyEvent e){if(isApplicationDialogOpen())return super.dispatchKeyEvent(e);int k=e.getKeyCode();if(k==KeyEvent.KEYCODE_DPAD_CENTER||k==KeyEvent.KEYCODE_ENTER){if(e.getAction()==KeyEvent.ACTION_DOWN&&e.getRepeatCount()==0){longCenterHandled=false;editUi.postDelayed(longCenter,650);return true;}if(e.getAction()==KeyEvent.ACTION_UP){editUi.removeCallbacks(longCenter);if(editMode){if(longCenterHandled){longCenterHandled=false;return true;}if(addSlot){addApplication();return true;}saveEdit();showNotice("ORDEN GUARDADO");return true;}if(!longCenterHandled)activateFocused();longCenterHandled=false;return true;}}if(e.getAction()==KeyEvent.ACTION_DOWN&&e.getRepeatCount()==0){if(editMode){if(k==KeyEvent.KEYCODE_DPAD_LEFT||k==KeyEvent.KEYCODE_DPAD_RIGHT){moveFocused(k==KeyEvent.KEYCODE_DPAD_LEFT?-1:1);return true;}if(k==KeyEvent.KEYCODE_DPAD_UP){hideFocused();return true;}if(k==KeyEvent.KEYCODE_BACK){cancelEdit();showNotice("EDICIÓN CANCELADA");return true;}}else if(handleNormalNavigation(k))return true;}return super.dispatchKeyEvent(e);}
 private boolean handleNormalNavigation(int key){View focused=getCurrentFocus();if(focused==null)return false;if(isAppCard(focused)){java.util.List<AppCatalog.Entry> visible=catalog.visible(0);int index=appIndex(focused,visible);if(key==KeyEvent.KEYCODE_DPAD_LEFT||key==KeyEvent.KEYCODE_DPAD_RIGHT){if(index<0||visible.isEmpty())return false;int next=index+(key==KeyEvent.KEYCODE_DPAD_RIGHT?1:-1);if(next<0)next=visible.size()-1;if(next>=visible.size())next=0;focusPackage(visible.get(next).pkg);return true;}if(key==KeyEvent.KEYCODE_DPAD_UP){return focusControl(controlsView.getChildCount()-1);}if(key==KeyEvent.KEYCODE_DPAD_DOWN)return true;}if(isControl(focused)){int index=controlsView.indexOfChild(focused);if(key==KeyEvent.KEYCODE_DPAD_LEFT||key==KeyEvent.KEYCODE_DPAD_RIGHT){int next=index+(key==KeyEvent.KEYCODE_DPAD_RIGHT?1:-1);if(next<0)next=controlsView.getChildCount()-1;if(next>=controlsView.getChildCount())next=0;return focusControl(next);}if(key==KeyEvent.KEYCODE_DPAD_DOWN){java.util.List<AppCatalog.Entry> visible=catalog.visible(0);if(!visible.isEmpty()){focusPackage(visible.get(0).pkg);return true;}}if(key==KeyEvent.KEYCODE_DPAD_UP)return true;}return false;}
 private boolean isAppCard(View v){return v!=null&&v.getTag() instanceof AppCatalog.Entry;}
 private boolean isControl(View v){return controlsView!=null&&v!=null&&v.getParent()==controlsView;}
 private int appIndex(View v,java.util.List<AppCatalog.Entry> visible){if(!(v.getTag() instanceof AppCatalog.Entry))return -1;String pkg=((AppCatalog.Entry)v.getTag()).pkg;for(int i=0;i<visible.size();i++)if(pkg.equals(visible.get(i).pkg))return i;return -1;}
 private boolean focusControl(int index){if(controlsView==null||controlsView.getChildCount()==0)return false;int safe=Math.max(0,Math.min(index,controlsView.getChildCount()-1));controlsView.getChildAt(safe).requestFocus();return true;}
 private AppCatalog.Entry focusedEntry(){if(appsRow==null)return null;for(int i=0;i<appsRow.getChildCount();i++){View v=appsRow.getChildAt(i);if(v.hasFocus()&&v.getTag() instanceof AppCatalog.Entry)return (AppCatalog.Entry)v.getTag();}return null;}
 private void focusPackage(String pkg){for(int i=0;i<appsRow.getChildCount();i++){View v=appsRow.getChildAt(i);if(v.getTag() instanceof AppCatalog.Entry&&pkg.equals(((AppCatalog.Entry)v.getTag()).pkg)){v.requestFocus();return;}}}
 private void focusAdd(){for(int i=0;i<appsRow.getChildCount();i++){View v=appsRow.getChildAt(i);if("add_application".equals(v.getTag())){v.requestFocus();return;}}}
 private void moveFocused(int delta){
  if(addSlot){
   if(delta<0){addSlot=false;renderApps();java.util.List<AppCatalog.Entry> visible=catalog.visible(0);if(!visible.isEmpty())focusPackage(visible.get(visible.size()-1).pkg);}
   return;
  }
  AppCatalog.Entry e=focusedEntry();if(e==null&&editFocusPackage!=null)e=catalog.find(editFocusPackage);if(e==null||appsRow==null)return;
  java.util.List<AppCatalog.Entry> visible=catalog.visible(0);int index=appIndexForPackage(e.pkg,visible);
  if(delta>0&&index==visible.size()-1){addSlot=true;editFocusPackage=null;renderApps();focusAdd();return;}
  int target=index+delta;if(index<0||target<0||target>=visible.size()||!catalog.moveVisible(e,delta))return;
  View moving=appsRow.getChildAt(index);if(moving==null)return;
  appsRow.removeViewAt(index);appsRow.addView(moving,target);editFocusPackage=e.pkg;moving.requestFocus();
 }
 private int appIndexForPackage(String pkg,List<AppCatalog.Entry> list){for(int i=0;i<list.size();i++)if(pkg.equals(list.get(i).pkg))return i;return -1;}
 private void hideFocused(){AppCatalog.Entry e=focusedEntry();if(e==null&&editFocusPackage!=null)e=catalog.find(editFocusPackage);if(e==null)return;int oldIndex=appIndexForPackage(e.pkg,catalog.visible(0));catalog.toggleVisible(e);java.util.List<AppCatalog.Entry> remaining=catalog.visible(0);editFocusPackage=remaining.isEmpty()?null:remaining.get(Math.min(Math.max(0,oldIndex),remaining.size()-1)).pkg;renderApps();if(editFocusPackage!=null)focusPackage(editFocusPackage);else focusAdd();showNotice("APLICACIÓN OCULTA");}
 private void addApplication(){
  java.util.List<AppCatalog.Entry> all= catalog.all();java.util.List<AppCatalog.Entry> visibleNow=catalog.visible(0);java.util.Set<String> visiblePackages=new HashSet<>();for(AppCatalog.Entry e:visibleNow)visiblePackages.add(e.pkg);
  java.util.List<AppCatalog.Entry> choices=new java.util.ArrayList<>();
  for(AppCatalog.Entry e:all)if(!visiblePackages.contains(e.pkg))choices.add(e);
  if(choices.isEmpty()){showNotice("NO HAY MÁS APLICACIONES DISPONIBLES");return;}
  final Dialog dialog=new Dialog(this);
  applicationDialog=dialog;
  dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
  LinearLayout panel=new LinearLayout(this);
  panel.setOrientation(LinearLayout.VERTICAL);
  panel.setPadding(dp(22),dp(18),dp(22),dp(14));
  panel.setBackground(bg(0xF21A2228));
  TextView title=text("Añadir aplicación",22);
  title.setTypeface(android.graphics.Typeface.create("sans-serif-light",android.graphics.Typeface.NORMAL));
  title.setTextColor(0xFFE7EAE7);
  panel.addView(title,new LinearLayout.LayoutParams(-1,dp(34)));
  TextView hint=text("Selecciona una aplicación para mostrarla en el inicio",12);
  hint.setTextColor(0xFF9FA8A8);
  panel.addView(hint,new LinearLayout.LayoutParams(-1,dp(28)));
  ScrollView scroll=new ScrollView(this);
  scroll.setVerticalScrollBarEnabled(false);
  LinearLayout list=new LinearLayout(this);
  list.setOrientation(LinearLayout.VERTICAL);
  scroll.addView(list,new ScrollView.LayoutParams(-1,-2));
  for(AppCatalog.Entry item:choices){
   final AppCatalog.Entry e=item;
   LinearLayout row=new LinearLayout(this);
   row.setGravity(Gravity.CENTER_VERTICAL);
   row.setPadding(dp(10),0,dp(10),0);
   row.setFocusable(true);
   row.setFocusableInTouchMode(true);
   row.setBackground(bg(0x662B353A));
   ImageView icon=new ImageView(this);
   icon.setImageDrawable(catalog.icon(e));
   row.addView(icon,new LinearLayout.LayoutParams(dp(34),dp(34)));
   TextView label=text(e.title,15);
   label.setTextColor(0xFFE7EAE7);
   label.setGravity(Gravity.CENTER_VERTICAL);
   LinearLayout.LayoutParams textParams=new LinearLayout.LayoutParams(0,dp(50),1);
   textParams.leftMargin=dp(12);
   row.addView(label,textParams);
   row.setOnFocusChangeListener((v,focused)->{
    v.setBackground(bg(focused?0xEE3B4A50:0x662B353A));
    if(focused)((View)v).setPadding(dp(10),0,dp(10),0);
   });
   row.setOnKeyListener((v,key,event)->{
    if((key==KeyEvent.KEYCODE_DPAD_CENTER||key==KeyEvent.KEYCODE_ENTER)&&event.getAction()==KeyEvent.ACTION_UP){v.performClick();return true;}
    return false;
   });
   row.setOnClickListener(v->{catalog.setVisible(e,true);catalog.moveVisibleToEnd(e);addSlot=false;editFocusPackage=e.pkg;dialog.dismiss();renderApps();focusPackage(e.pkg);});
   LinearLayout.LayoutParams rowParams=new LinearLayout.LayoutParams(-1,dp(50));
   rowParams.bottomMargin=dp(5);
   list.addView(row,rowParams);
  }
  panel.addView(scroll,new LinearLayout.LayoutParams(-1,dp(250)));
  TextView cancel=text("ATRÁS · CANCELAR",11);
  cancel.setGravity(Gravity.CENTER);
  cancel.setTextColor(0xFFB7BBB8);
  panel.addView(cancel,new LinearLayout.LayoutParams(-1,dp(38)));
  cancel.setOnClickListener(v->dialog.cancel());
  dialog.setContentView(panel);
  dialog.setOnCancelListener(d->{addSlot=false;renderApps();if(applicationDialog==dialog)applicationDialog=null;});
  dialog.setOnDismissListener(d->{if(applicationDialog==dialog)applicationDialog=null;});
  dialog.setOnShowListener(d->{
   Window window=dialog.getWindow();
   if(window!=null){
    window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
    WindowManager.LayoutParams lp=window.getAttributes();
    lp.dimAmount=.58f;
    window.setAttributes(lp);
    window.setLayout(dp(520),WindowManager.LayoutParams.WRAP_CONTENT);
   }
   if(list.getChildCount()>0)list.getChildAt(0).requestFocus();
  });
  dialog.show();
  Window window=dialog.getWindow();
  if(window!=null)window.setLayout(dp(520),WindowManager.LayoutParams.WRAP_CONTENT);
 }
 private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
 private TextView text(String s,int size){TextView t=new PanelService.StableText(this);t.setText(s);t.setTextColor(Color.WHITE);t.setTextSize(size);return t;}
 private GradientDrawable bg(int c){GradientDrawable d=new GradientDrawable();d.setColor(c);d.setCornerRadius(dp(8));return d;}
 private void focus(View v){v.setFocusable(true);v.setFocusableInTouchMode(true);v.setOnTouchListener((view,event)->{if(event.getAction()==MotionEvent.ACTION_DOWN)view.requestFocus();return false;});v.setBackground(bg(0x55202020));v.setOnFocusChangeListener((x,f)->{GradientDrawable d=bg(f?0xFF3A3A3A:0x55202020);if(f)d.setStroke(dp(2),0xFFB8B8B8);x.setBackground(d);});}
 private void render(){
  root=new FrameLayout(this);root.setPadding(dp(58),dp(28),dp(58),dp(24));root.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{0xFF102E3A,0xFF121722,0xFF07090D}));setContentView(root);
  LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.TOP);root.addView(header,new FrameLayout.LayoutParams(-1,dp(52)));
  LinearLayout identity=new LinearLayout(this);identity.setOrientation(1);header.addView(identity,new LinearLayout.LayoutParams(0,-1,1));
  String name=prefs.getString("display_name","").trim();greeting=text("",12);greeting.setTypeface(android.graphics.Typeface.create("sans-serif-light",android.graphics.Typeface.NORMAL));greeting.setLetterSpacing(.18f);greeting.setTextColor(0xFF707574);greeting.setVisibility(prefs.getBoolean("show_greeting",true)?View.VISIBLE:View.GONE);identity.addView(greeting);
  date=text("",18);date.setTypeface(android.graphics.Typeface.create("sans-serif",android.graphics.Typeface.NORMAL));date.setTextColor(0xFFB7BBB8);date.setVisibility(prefs.getBoolean("show_date",true)?View.VISIBLE:View.GONE);identity.addView(date);
  LinearLayout controls=new LinearLayout(this);controls.setGravity(Gravity.RIGHT|Gravity.TOP);controls.setVisibility(prefs.getBoolean("show_controls",true)?View.VISIBLE:View.GONE);controlsView=controls;header.addView(controls,new LinearLayout.LayoutParams(dp(430),-1));
  LinearLayout wifi=controlButton("Wi‑Fi",()->openPanel("network"),92);wifiState=(TextView)wifi.getChildAt(1);controls.addView(wifi);
  LinearLayout bluetooth=controlButton("Bluetooth",()->openPanel("bluetooth"),105);bluetoothState=(TextView)bluetooth.getChildAt(1);controls.addView(bluetooth);
  LinearLayout settings=controlButton("⚙ Ajustes",()->openPanel("settings"),94);controls.addView(settings);
  content=new LinearLayout(this);content.setOrientation(1);content.setGravity(Gravity.BOTTOM);FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(-1,-1);lp.topMargin=dp(52);root.addView(content,lp);
  notice=text("",11);notice.setGravity(Gravity.CENTER);notice.setTypeface(android.graphics.Typeface.create("sans-serif-light",android.graphics.Typeface.NORMAL));notice.setLetterSpacing(.04f);notice.setTextColor(Color.WHITE);notice.setAlpha(.92f);notice.setSingleLine(true);notice.setPadding(dp(12),0,dp(12),0);notice.setBackground(bg(0x66333333));FrameLayout.LayoutParams noticeParams=new FrameLayout.LayoutParams(-2,dp(28),Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);noticeParams.bottomMargin=0;root.addView(notice,noticeParams);
  renderApps();
 }
 private void renderApps(){
  if(content==null)return;StringBuilder signature=new StringBuilder().append(catalog.revision()).append('|').append(editMode).append('|').append(addSlot).append('|').append(catalogLoaded);for(AppCatalog.Entry entry:catalog.visible(0))signature.append('|').append(entry.pkg);if(signature.toString().equals(lastAppsSignature)&&appsRow!=null)return;lastAppsSignature=signature.toString();View currentFocus=getCurrentFocus();String focusedPackage=currentFocus!=null&&currentFocus.getTag() instanceof AppCatalog.Entry?((AppCatalog.Entry)currentFocus.getTag()).pkg:null;content.removeAllViews();
  LinearLayout hero=new LinearLayout(this);hero.setOrientation(1);hero.setGravity(Gravity.LEFT|Gravity.TOP);LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(-1,0,1);content.addView(hero,hp);
  LinearLayout clockRow=new LinearLayout(this);clockRow.setGravity(Gravity.CENTER_VERTICAL);LinearLayout.LayoutParams crp=new LinearLayout.LayoutParams(-1,-2);crp.topMargin=0;hero.addView(clockRow,crp);clockHour=text("",90);clockHour.setTypeface(android.graphics.Typeface.create("sans-serif-thin",android.graphics.Typeface.NORMAL));clockHour.setTextColor(0xFFB7BBB8);clockRow.addView(clockHour);clockMeridiem=text("",24);clockMeridiem.setTypeface(android.graphics.Typeface.create("sans-serif-light",android.graphics.Typeface.NORMAL));clockMeridiem.setTextColor(0xFF707574);LinearLayout.LayoutParams mp=new LinearLayout.LayoutParams(-2,-2);mp.leftMargin=dp(7);clockRow.addView(clockMeridiem,mp);
  HorizontalScrollView scroll=new HorizontalScrollView(this);scroll.setHorizontalScrollBarEnabled(false);scroll.setClipToPadding(false);scroll.setPadding(dp(3),0,dp(3),0);appsRow=new LinearLayout(this);appsRow.setGravity(Gravity.CENTER_HORIZONTAL);appsRow.setClipChildren(false);appsRow.setClipToPadding(false);appsRow.setMinimumWidth(Math.max(0,getResources().getDisplayMetrics().widthPixels-dp(122)));scroll.addView(appsRow,new HorizontalScrollView.LayoutParams(-1,-1));LinearLayout.LayoutParams sl=new LinearLayout.LayoutParams(-1,dp(142));sl.topMargin=dp(8);content.addView(scroll,sl);
  if(notice!=null){if(editMode)setEditHelp();else if(notice.getText().length()==0)notice.setVisibility(View.GONE);}
  View first=null;if(catalog.visible(0).isEmpty()){TextView empty=text(!catalogLoaded?"…":"Sin aplicaciones",15);empty.setPadding(dp(12),dp(24),0,0);appsRow.addView(empty);}
  for(AppCatalog.Entry e:catalog.visible(0)){LinearLayout card=new LinearLayout(this);card.setOrientation(1);card.setGravity(Gravity.CENTER);card.setTag(e);focus(card);LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(dp(160),dp(108));cp.rightMargin=dp(8);appsRow.addView(card,cp);ImageView icon=new ImageView(this);icon.setImageDrawable(catalog.icon(e));card.addView(icon,new LinearLayout.LayoutParams(dp(52),dp(52)));TextView label=text(e.title,13);label.setGravity(Gravity.CENTER);label.setMaxLines(2);card.addView(label);card.setOnClickListener(v->{if(editMode)return;try{startActivity(new Intent(Intent.ACTION_MAIN).setComponent(e.component));}catch(Exception ex){showNotice("APLICACIÓN NO DISPONIBLE");}});if(first==null)first=card;}
  if(editMode){LinearLayout add=new LinearLayout(this);add.setOrientation(1);add.setGravity(Gravity.CENTER);add.setTag("add_application");focus(add);LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(dp(160),dp(108));cp.rightMargin=dp(8);appsRow.addView(add,cp);TextView plus=text("+",42);plus.setGravity(Gravity.CENTER);add.addView(plus,new LinearLayout.LayoutParams(-1,dp(58)));TextView label=text("Añadir aplicación",13);label.setGravity(Gravity.CENTER);add.addView(label);add.setOnClickListener(v->{addSlot=true;addApplication();});}
  if(focusedPackage!=null){focusPackage(focusedPackage);}else if(first!=null)first.requestFocus();
  if(clockHour.getText().length()==0)updateStatus();
 }
  private void setEditHelp(){if(notice==null)return;notice.removeCallbacks(clearNotice);notice.setText("← →  MOVER    ·    ↑  OCULTAR    ·    OK  GUARDAR    ·    ATRÁS  CANCELAR");notice.setVisibility(View.VISIBLE);}
  private void showNotice(String message){if(notice==null)return;notice.removeCallbacks(clearNotice);notice.setText(message);notice.setVisibility(View.VISIBLE);notice.postDelayed(clearNotice,1800);}
 private LinearLayout controlButton(String label,Runnable action,int width){LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setGravity(Gravity.CENTER);box.setPadding(0,0,0,0);box.setFocusable(true);box.setFocusableInTouchMode(true);TextView title=text(label,13);title.setGravity(Gravity.CENTER);title.setTypeface(android.graphics.Typeface.create("sans-serif-light",android.graphics.Typeface.NORMAL));title.setTextColor(0xFFC6C9C6);TextView state=text("",8);state.setGravity(Gravity.CENTER);state.setTypeface(android.graphics.Typeface.create("sans-serif-light",android.graphics.Typeface.NORMAL));state.setTextColor(0xFF969C9D);box.addView(title);box.addView(state);box.setOnFocusChangeListener((v,focused)->{title.setTextColor(focused?0xFFE0E2DE:0xFFC6C9C6);state.setTextColor(focused?0xFFB8BDBB:0xFF969C9D);GradientDrawable d=new GradientDrawable();d.setColor(Color.TRANSPARENT);d.setCornerRadius(dp(3));if(focused)d.setStroke(dp(1),0x99C6C9C6);v.setBackground(d);});box.setOnClickListener(v->action.run());LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(width),dp(46));lp.rightMargin=0;controlsPlaceholder(box,lp);return box;}
 private void controlsPlaceholder(View v,LinearLayout.LayoutParams lp){v.setLayoutParams(lp);}
 private void openPanel(String page){if(PanelService.active!=null)PanelService.active.openPage(page);else startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));}
 private String bluetoothStatus(BluetoothAdapter adapter){try{return adapter==null?"No disponible":adapter.isEnabled()?"Activo":"Apagado";}catch(SecurityException denied){return "Permiso pendiente";}}
 private void updateStatus(){if(clockHour==null)return;Date now=new Date();Locale es=new Locale("es","ES");int hour=Integer.parseInt(new SimpleDateFormat("H",es).format(now));String greetingText=hour<12?"BUENOS DÍAS":hour<19?"BUENAS TARDES":"BUENAS NOCHES";String name=prefs.getString("display_name","").trim().replaceFirst("(?i)^(hola|buenos\\s+d[ií]as|buenas\\s+tardes|buenas\\s+noches)\\s+","");greeting.setText(greetingText+" "+(name.isEmpty()?"ANTONI":name.toUpperCase(es)));date.setText(new SimpleDateFormat("EEEE d MMM yyyy",es).format(now).replace(".","").toUpperCase(es));boolean use24=prefs.getBoolean("time_24",false);String time=new SimpleDateFormat(use24?"HH:mm":"h:mm a",Locale.US).format(now);if(use24){clockHour.setText(time);clockMeridiem.setText("");}else{boolean pm=time.endsWith("PM");time=time.substring(0,time.length()-3).trim();clockHour.setText(time);clockMeridiem.setText(pm?"P. M.":"A. M.");}ConnectivityManager cm=(ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);NetworkCapabilities n=cm.getNetworkCapabilities(cm.getActiveNetwork());String net=n==null?"Sin conexión":n.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)?"Conectado":n.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)?"Ethernet":"No conectado";wifiState.setText(net);BluetoothAdapter a=BluetoothAdapter.getDefaultAdapter();bluetoothState.setText(bluetoothStatus(a));}
}
