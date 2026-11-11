package local.z6panel;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.drawable.*;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.util.*;

/** Native TV setup. No image assets, UI polling, or transferable device configuration. */
public final class TvRecoveryActivity extends Activity {
 static TvRecoveryActivity current;
 private boolean resumed,learning,preparing,confirming;
 private String pendingKeyName="Otro botón";
 private String remoteSession="";private AlertDialog keyDialog;
 private int observedKey=-1,observedScan;private String keyResult="idle";
 private String page="main";
 private LinearLayout root,body,left,right,footer;
 private TextView summary,status;
 private Button app,channel,test,boot,wake,button,key,run,activate,access;
 private final Handler ui=new Handler(Looper.getMainLooper());
 private final Runnable expireKey=()->{learning=false;TvRemoteKeys.end(this);if(!keyResult.equals("reserved"))keyResult="expired";if(status!=null)status.setText(keyResult.equals("reserved")?"La tecla recibida está protegida. Elige otro botón.":"No llegó ningún botón asignable. Puedes volver a probar con otra tecla.");};
 private TvRecovery engine(){return TvRecovery.get(this);}
 private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
 @Override public void onCreate(Bundle b){super.onCreate(b);current=this;getWindow().setFlags(1024,1024);main();}
 @Override protected void onResume(){super.onResume();resumed=true;current=this;update();maybeLearn();}
 @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);if(resumed)maybeLearn();}
 private void maybeLearn(){String prepare=getIntent().getStringExtra("prepare_remote"),name=getIntent().getStringExtra("learn_remote");getIntent().removeExtra("prepare_remote");getIntent().removeExtra("learn_remote");if(prepare!=null){pendingKeyName=prepare.length()>40?"Otro botón":prepare;preparing=true;learning=false;ui.removeCallbacks(expireKey);TvRemoteKeys.end(this);AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Asignar "+pendingKeyName).setMessage("Cuando tengas el control en la mano, pulsa Comenzar y después el botón "+pendingKeyName+". La escucha empezará entonces. Si se detecta, podrás confirmar la asignación con OK.").setPositiveButton("Comenzar",(d,w)->beginLearning()).setNegativeButton("Volver",null).show();keyDialog=dialog;dialog.setOnDismissListener(d->{preparing=false;if(keyDialog==dialog)keyDialog=null;TvRemoteKeys.changed(this);});dialog.getButton(AlertDialog.BUTTON_POSITIVE).requestFocus();}else if(name!=null){pendingKeyName=name.length()>40?"Otro botón":name;beginLearning();}}
 @Override protected void onPause(){resumed=false;if(learning)keyResult="left_screen";learning=false;TvRemoteKeys.end(this);ui.removeCallbacks(expireKey);super.onPause();}
 @Override protected void onDestroy(){learning=preparing=confirming=false;if(current==this)current=null;TvRemoteKeys.end(this);ui.removeCallbacksAndMessages(null);super.onDestroy();}
 static void refresh(){TvRecoveryActivity a=current;if(a!=null&&a.resumed)a.runOnUiThread(a::update);}
 private TextView text(String label,int size,int color){TextView t=new TextView(this);t.setText(label);t.setTextSize(size);t.setTextColor(color);return t;}
 private Drawable background(int color,boolean border){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(8));if(border)d.setStroke(dp(2),0xFFE5F0F3);return d;}
 private Drawable selector(){StateListDrawable d=new StateListDrawable();d.addState(new int[]{android.R.attr.state_focused},background(0x70333F49,true));d.addState(new int[]{android.R.attr.state_pressed},background(0x70333F49,true));d.addState(new int[]{},background(0x301A2630,false));return d;}
 private void frame(String title){
  root=new LinearLayout(this);root.setOrientation(1);root.setPadding(dp(24),dp(12),dp(24),dp(12));root.setBackgroundColor(0xFF121722);
  TextView heading=text(title,24,Color.WHITE);root.addView(heading,new LinearLayout.LayoutParams(-1,dp(38)));
  status=text(engine().status(),12,0xFFB8BFC5);status.setMaxLines(2);root.addView(status,new LinearLayout.LayoutParams(-1,dp(40)));
  body=new LinearLayout(this);root.addView(body,new LinearLayout.LayoutParams(-1,0,1));
  footer=new LinearLayout(this);root.addView(footer,new LinearLayout.LayoutParams(-1,dp(37)));setContentView(root);
 }
 private Button add(LinearLayout parent,String label,Runnable action){Button b=new Button(this);b.setAllCaps(false);b.setText(label);b.setTextSize(13);b.setTextColor(Color.WHITE);b.setBackground(selector());b.setPadding(dp(10),dp(3),dp(10),dp(3));b.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(43));p.topMargin=dp(5);parent.addView(b,p);b.setOnClickListener(v->safe(action));return b;}
 private void safe(Runnable action){try{action.run();}catch(RuntimeException e){Toast.makeText(this,e.getMessage()==null?"Acción no disponible":e.getMessage(),Toast.LENGTH_LONG).show();update();}}
 private void main(){
  page="main";learning=false;TvRemoteKeys.end(this);ui.removeCallbacks(expireKey);frame("Inicio y recuperación de TV");
  left=new LinearLayout(this);left.setOrientation(1);left.setPadding(0,0,dp(16),0);body.addView(left,new LinearLayout.LayoutParams(0,-1,4));
  right=new LinearLayout(this);right.setOrientation(1);body.addView(right,new LinearLayout.LayoutParams(0,-1,6));
  summary=text("",14,0xFFD5DADF);summary.setMaxLines(4);left.addView(summary,new LinearLayout.LayoutParams(-1,dp(91)));
  app=add(left,"Elegir aplicación",this::apps);
  channel=add(left,"Configurar canal · recorrido guiado",()->engine().request("configuración",true,null));
  test=add(left,"Probar recorrido guardado",()->{TvRecoveryConfig.Settings s=TvRecoveryConfig.get(this);if(!s.verified)throw new IllegalStateException("Configura primero el canal");engine().request("prueba",false,s);});
  access=add(left,"Habilitar Asistencia de TV",this::accessibility);
  TextView note=text("Si apagas solo el televisor y el TV Box sigue encendido, puede no informar un despertar. En ese caso usa el botón del control.",11,0xFF9DA8B2);note.setPadding(0,dp(9),0,0);left.addView(note);
  boot=add(right,"",()->engine().toggle("boot"));wake=add(right,"",()->engine().toggle("wake"));button=add(right,"",()->engine().toggle("button"));
  key=add(right,"Asignar botón del control",this::learnKey);
  run=add(right,"Recuperar ahora",()->engine().request("manual",false,null));
  activate=add(right,"Guardar y activar",()->engine().activate());
  add(right,"Desactivar todo",()->engine().disable());
  Button back=new Button(this);back.setText("Volver");back.setAllCaps(false);back.setBackground(selector());back.setTextColor(Color.WHITE);footer.addView(back,new LinearLayout.LayoutParams(-1,-1));back.setOnClickListener(v->finish());
  update();app.requestFocus();
 }
 private void update(){
  if(!page.equals("main")||summary==null)return;
  TvRecoveryConfig.Settings s=TvRecoveryConfig.get(this);boolean selected=!s.pkg.isEmpty(),nav=TvNavigationService.active!=null;
  summary.setText((selected?s.name:"1. Elige tu app de TV")+"\n"+(s.channel.isEmpty()?"2. Elige Televisión → Perú → canal":"Canal: "+s.channel)+"\n"+(s.verified?"Recorrido probado":"Recorrido pendiente de configurar o revisar"));
  if(!learning)status.setText(engine().status()+(s.button&&s.hardware()?" · "+TvRemoteKeys.availability():""));
  boot.setText("Al arrancar Android: "+(s.boot?"Activado":"Desactivado"));wake.setText("Al despertar: "+(s.wake?"Activado":"Desactivado"));button.setText("Botón de recuperación: "+(s.button?"Activado":"Desactivado"));
  key.setText(!s.hasBinding()?"Asignar una tecla libre del control":"Botón: "+(s.keyName.isEmpty()?"Asignado":s.keyName)+" · cambiar o quitar");
  access.setText(nav?"Asistencia de TV: disponible":"Habilitar Asistencia de TV para el canal");
  channel.setEnabled(selected&&nav&&!engine().busy());test.setEnabled(s.verified&&nav&&!engine().busy());
  activate.setEnabled(s.verified&&nav&&!engine().busy());run.setEnabled(selected);run.setText(engine().busy()?"Cancelar recuperación":s.verified?"Reabrir y ver mi canal ahora":"Reabrir app · canal pendiente");
  boot.setEnabled(s.boot||s.verified&&nav);wake.setEnabled(s.wake||s.verified&&nav);button.setEnabled(s.button||s.verified&&nav&&s.hasBinding());
 }
 private void apps(){
  page="apps";frame("Elegir aplicación de TV");ScrollView scroll=new ScrollView(this);LinearLayout list=new LinearLayout(this);list.setOrientation(1);scroll.addView(list);body.addView(scroll,new LinearLayout.LayoutParams(-1,-1));
  AppCatalog catalog=new AppCatalog(this);
  if(!catalog.ready()){status.setText("Cargando aplicaciones…");new Thread(()->{try{java.util.List<AppCatalog.Entry> loaded=catalog.scan();ui.post(()->{catalog.accept(loaded);if(!isDestroyed()&&page.equals("apps"))apps();});}catch(RuntimeException e){ui.post(()->status.setText("No se pudo cargar la lista de aplicaciones"));}},"Z6Tv-app-list").start();}
  else{
   int count=0;for(AppCatalog.Entry e:catalog.all())if(TvRecoveryConfig.eligible(this,e.pkg)){
    add(list,e.title,()->{engine().choose(e.pkg);main();channel.requestFocus();});count++;
   }
   if(count==0)status.setText("No hay apps compatibles instaladas. Instala tu app de TV y vuelve a esta pantalla.");
   else status.setText("Elige la app o variante instalada. Cambiarla desactiva la configuración anterior.");
  }
  Button back=new Button(this);back.setText("Volver");footer.addView(back,new LinearLayout.LayoutParams(-1,-1));back.setOnClickListener(v->main());
 }
 private void accessibility(){
  new AlertDialog.Builder(this).setTitle("Asistencia de TV").setMessage("Esta ayuda reconoce botones y etiquetas solo de la app de TV elegida mientras enseñas o ejecutas el recorrido al canal. No guarda pantallas ni credenciales. El servicio del panel sigue sin leer el contenido de otras apps.")
   .setPositiveButton("Abrir permisos",(d,w)->{Intent i=new Intent("android.settings.ACCESSIBILITY_DETAILS_SETTINGS").putExtra(Intent.EXTRA_COMPONENT_NAME,new ComponentName(this,TvNavigationService.class));if(i.resolveActivity(getPackageManager())==null)i=new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);startActivity(i);}).setNegativeButton("Volver",null).show();
 }
 private void learnKey(){
  TvRecoveryConfig.Settings s=TvRecoveryConfig.get(this);
  if(s.hasBinding()){new AlertDialog.Builder(this).setTitle("Botón de recuperación").setItems(new String[]{"Elegir otra tecla","Quitar asignación"},(d,w)->{if(w==0)nameKey();else engine().assignKey(0);}).setNegativeButton("Volver",null).show();}
  else nameKey();
 }
 private void nameKey(){String[] names={"Netflix","YouTube","Otro botón"};preparing=true;TvRemoteKeys.changed(this);AlertDialog d=new AlertDialog.Builder(this).setTitle("¿Qué botón vas a pulsar?").setItems(names,(dialog,w)->{pendingKeyName=names[w];beginLearning();}).setNegativeButton("Volver",null).show();keyDialog=d;d.setOnDismissListener(dialog->{preparing=false;if(keyDialog==d)keyDialog=null;TvRemoteKeys.changed(this);});}
 private void beginLearning(){learning=true;observedKey=-1;observedScan=0;keyResult="waiting";status.setText("Preparando la detección de "+pendingKeyName+"…");remoteSession=TvRemoteKeys.begin(this);ui.removeCallbacks(expireKey);ui.postDelayed(expireKey,60000);}
 static boolean isLearning(){TvRecoveryActivity a=current;return a!=null&&a.resumed&&a.learning;}
 static boolean keySetupActive(){TvRecoveryActivity a=current;return a!=null&&a.resumed&&(a.learning||a.preparing||a.confirming);}
 static void stopLearning(){TvRecoveryActivity a=current;if(a!=null){a.learning=a.preparing=a.confirming=false;a.remoteSession="";a.ui.removeCallbacks(a.expireKey);if(a.keyDialog!=null)a.keyDialog.dismiss();}}
 static void remoteReady(String token,boolean available){TvRecoveryActivity a=current;if(a!=null&&a.resumed&&a.learning&&token.equals(a.remoteSession))a.status.setText("Pulsa "+a.pendingKeyName+" ahora. Atrás cancela."+(available?" También se reconocen teclas especiales mediante el control auxiliar.":" Se usará la detección disponible en Android."));}
 static void remoteDetected(String token,String device,int code){TvRecoveryActivity a=current;if(a!=null&&a.resumed&&a.learning&&token.equals(a.remoteSession)){a.observedKey=0;a.observedScan=code;a.keyResult="detected_auxiliary";a.confirmKey(0,0,device,code);}}
 static boolean keyPromptReady(){TvRecoveryActivity a=current;return a!=null&&a.resumed&&a.preparing;}
 static org.json.JSONObject keyState()throws org.json.JSONException{TvRecoveryActivity a=current;return a==null?new org.json.JSONObject():new org.json.JSONObject().put("visible",a.resumed).put("result",a.keyResult).put("code",a.observedKey).put("scan",a.observedScan).put("message",a.status==null?"":a.status.getText());}
 static boolean capture(KeyEvent event){TvRecoveryActivity a=current;if(a==null||!a.resumed)return false;if(a.learning)return a.captureKey(event);if(a.preparing||a.confirming){if(event.getKeyCode()==KeyEvent.KEYCODE_HOME){if(a.keyDialog!=null)a.keyDialog.dismiss();return false;}return !TvRecoveryConfig.protectedKey(event.getKeyCode());}return false;}
 private boolean captureKey(KeyEvent e){
  int code=e.getKeyCode();if(e.getAction()==KeyEvent.ACTION_DOWN&&e.getRepeatCount()==0){observedKey=code;observedScan=e.getScanCode();}if(code==KeyEvent.KEYCODE_BACK||code==KeyEvent.KEYCODE_ESCAPE){if(e.getAction()==KeyEvent.ACTION_DOWN){learning=false;TvRemoteKeys.end(this);keyResult="cancelled";ui.removeCallbacks(expireKey);update();}return true;}if(code==KeyEvent.KEYCODE_HOME){learning=false;TvRemoteKeys.end(this);keyResult="cancelled";ui.removeCallbacks(expireKey);update();return false;}if(TvRecoveryConfig.protectedKey(code)){keyResult="reserved";return false;}
  if(e.getAction()!=KeyEvent.ACTION_DOWN||e.getRepeatCount()!=0)return true;
  int scan=e.getScanCode();if(!TvRecoveryConfig.allowedBinding(code,scan)){keyResult="reserved";status.setText("Android no identifica esa tecla de forma utilizable. Prueba otro botón.");return true;}
  keyResult="detected";confirmKey(code,scan,"",0);return true;
 }
 private void confirmKey(int code,int scan,String device,int raw){
  learning=false;confirming=true;ui.removeCallbacks(expireKey);TvRemoteKeys.end(this);
  AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Usar "+pendingKeyName).setMessage((raw==0?"Tecla detectada.":"Tecla especial detectada mediante el control auxiliar.")+" Mientras esté activada, recuperará tu app de TV en lugar de su acceso habitual. Puedes cambiarla o quitarla desde este menú. Mantenerla pulsada no acumula órdenes.")
   .setPositiveButton("Asignar",(d,w)->safe(()->{if(raw==0)engine().assignKey(code,scan,pendingKeyName);else engine().assignHardware(device,raw,pendingKeyName);})).setNegativeButton("Cancelar",null).show();keyDialog=dialog;dialog.setOnDismissListener(d->{confirming=false;if(keyDialog==dialog)keyDialog=null;TvRemoteKeys.changed(this);});dialog.getButton(AlertDialog.BUTTON_POSITIVE).requestFocus();
 }
 @Override public boolean onKeyDown(int key,KeyEvent event){if(capture(event))return true;return super.onKeyDown(key,event);}
 @Override public void onBackPressed(){if(learning){learning=false;TvRemoteKeys.end(this);ui.removeCallbacks(expireKey);update();return;}if(!page.equals("main")){main();return;}super.onBackPressed();}
}
