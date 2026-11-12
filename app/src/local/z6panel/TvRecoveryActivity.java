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
import org.json.*;

/** Independent shortcuts and startup options; no idle refresh loop. */
public final class TvRecoveryActivity extends Activity {
 static TvRecoveryActivity current;
 private final Handler ui=new Handler(Looper.getMainLooper());
 private boolean resumed,learning,preparing,confirming,needsRender;
 private String page="main",editing="",pendingKeyName="Otro botón",remoteSession="",keyResult="idle";
 private int observedKey=-1,observedScan;
 private AlertDialog keyDialog;
 private LinearLayout body,footer;private TextView status;
 private final Map<String,Button> controls=new LinkedHashMap<>();
 private final Runnable expireKey=()->{learning=false;TvRemoteKeys.end(this);keyResult="expired";message("Android no entregó esa tecla. Prueba otra o prepara los botones especiales.");};
 private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
 private TvRecovery engine(){return TvRecovery.get(this);}
 @Override public void onCreate(Bundle b){super.onCreate(b);current=this;getWindow().setFlags(1024,1024);if(b!=null){page=b.getString("page","main");editing=b.getString("editing","");}render();}
 @Override protected void onSaveInstanceState(Bundle b){super.onSaveInstanceState(b);b.putString("page",page);b.putString("editing",editing);}
 @Override protected void onResume(){super.onResume();resumed=true;current=this;if(needsRender){needsRender=false;render();}else update();maybeLearn();}
 @Override protected void onNewIntent(Intent i){super.onNewIntent(i);setIntent(i);if(resumed)maybeLearn();}
 @Override protected void onPause(){resumed=false;needsRender=true;endLearning();super.onPause();}
 @Override protected void onDestroy(){if(current==this)current=null;endLearning();ui.removeCallbacksAndMessages(null);super.onDestroy();}
 static void refresh(){TvRecoveryActivity a=current;if(a!=null&&a.resumed)a.ui.post(a::update);}
 private TextView text(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);return t;}
 private Drawable fill(int color,boolean border){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(8));if(border)d.setStroke(dp(2),0xFFE5F0F3);return d;}
 private Drawable selector(){StateListDrawable d=new StateListDrawable();d.addState(new int[]{android.R.attr.state_focused},fill(0x70333F49,true));d.addState(new int[]{android.R.attr.state_pressed},fill(0x70333F49,true));d.addState(new int[]{},fill(0x301A2630,false));return d;}
 private void frame(String title,String help){
  controls.clear();LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setPadding(dp(24),dp(12),dp(24),dp(12));root.setBackgroundColor(0xFF121722);
  root.addView(text(title,24,Color.WHITE),new LinearLayout.LayoutParams(-1,dp(38)));
  status=text(help,12,0xFFBBC4CC);status.setMaxLines(2);root.addView(status,new LinearLayout.LayoutParams(-1,dp(39)));
  body=new LinearLayout(this);root.addView(body,new LinearLayout.LayoutParams(-1,0,1));footer=new LinearLayout(this);root.addView(footer,new LinearLayout.LayoutParams(-1,dp(49)));setContentView(root);
  add(footer,"back","Volver",this::back).setGravity(Gravity.CENTER);
 }
 private LinearLayout column(int weight,boolean scroll){LinearLayout c=new LinearLayout(this);c.setOrientation(1);View outer=c;if(scroll){ScrollView s=new ScrollView(this);s.addView(c);outer=s;}LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-1,weight);p.rightMargin=dp(12);body.addView(outer,p);return c;}
 private Button add(LinearLayout parent,String id,String label,Runnable action){Button b=new Button(this);b.setAllCaps(false);b.setText(label);b.setTextSize(13);b.setTextColor(Color.WHITE);b.setBackground(selector());b.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);b.setPadding(dp(12),dp(3),dp(12),dp(3));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(44));p.topMargin=dp(5);parent.addView(b,p);controls.put(id,b);b.setOnClickListener(v->safe(action));return b;}
 private void safe(Runnable r){try{r.run();}catch(RuntimeException e){message(e.getMessage()==null?"Acción no disponible":e.getMessage());}}
 private void message(String s){if(status!=null)status.setText(s);Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
 private String focused(){View v=getCurrentFocus();for(Map.Entry<String,Button> e:controls.entrySet())if(e.getValue()==v)return e.getKey();return "";}
 private void render(){String focus=focused();if(page.equals("edit")&&RemoteActions.find(this,editing)==null)page="main";if(page.equals("edit"))editor();else if(page.equals("startup"))startup();else main();Button b=controls.get(focus);if(b==null)b=controls.get(page.equals("edit")?"action":page.equals("startup")?"boot":"add");if(b!=null)b.requestFocus();}
 private void main(){
  page="main";frame("Mi control y arranque","Cada botón tiene su propia acción. Arranque y despertar se configuran por separado.");
  LinearLayout left=column(6,true),right=column(4,false);left.addView(text("Botones del control",15,0xFFE5F0F3));
  for(RemoteActions.Action a:RemoteActions.get(this))add(left,"button:"+a.id,a.label()+" · "+(a.enabled?"Activo":"Desactivado")+"\n"+a.description(),()->{editing=a.id;page="edit";render();});
  if(RemoteActions.get(this).isEmpty())left.addView(text("Añade un botón libre para abrir o recuperar una app.",13,0xFFBBC4CC));
  add(left,"add","＋ Añadir botón",this::newButton);
  right.addView(text("Encendido del TV Box",15,0xFFE5F0F3));TvRecoveryConfig.Settings s=TvRecoveryConfig.get(this);
  add(right,"startup","Arranque y despertar\n"+(s.boot||s.wake?s.name+" · "+s.channel:"Desactivados"),()->{page="startup";render();});
  add(right,"prepare","Preparar Netflix y YouTube",this::prepareSpecial);
  add(right,"access","Acceso para reabrir apps",this::usageAccess);
  add(right,"help","Cómo funciona y permisos",this::help);
  TextView note=text("Home, Atrás, flechas, OK, volumen y encendido conservan sus funciones.",11,0xFF9DA8B2);note.setPadding(0,dp(12),0,0);right.addView(note);update();
 }
 private RemoteActions.Action action(){RemoteActions.Action a=RemoteActions.find(this,editing);if(a==null)throw new IllegalStateException("El botón ya no existe");return a.copy();}
 private void editor(){
  RemoteActions.Action a=action();page="edit";frame(a.label(),a.description());LinearLayout left=column(6,false),right=column(4,false);
  add(left,"action","Qué hace: "+kindName(a.kind),()->new AlertDialog.Builder(this).setTitle("Acción del botón").setItems(new String[]{"Abrir aplicación","Cerrar y volver a abrir aplicación","Reabrir y llegar a un canal o pantalla","Inicio de Z6","Ajustes de Android","Silenciar / activar sonido"},(d,w)->safe(()->{RemoteActions.Action v=action();v.kind=new String[]{"open","reopen","route","home","settings","mute"}[w];RemoteActions.save(this,v);render();})).setNegativeButton("Volver",null).show());
  if(Arrays.asList("open","reopen","route").contains(a.kind)){
   add(left,"app","Aplicación: "+(a.target.name.isEmpty()?"Elegir":a.target.name),()->apps(false));
   if(a.kind.equals("route")){
    add(left,"destination",a.target.verified?"Cambiar destino · "+a.target.channel:"Elegir destino en la app",()->teach(false));
    left.addView(text("Navega directamente en tu app. Home termina y permite probar antes de guardar.",12,0xFFBBC4CC));
   }
  }
  add(left,"test","Probar ahora",()->{RemoteActions.Action v=action();if(v.target.pkg.isEmpty()&&Arrays.asList("open","reopen","route").contains(v.kind))apps(false);else engine().execute(v);});
  add(right,"enabled",a.enabled?"✓ Botón activado":"Botón desactivado",()->{RemoteActions.Action v=action();v.enabled=!v.enabled;RemoteActions.save(this,v);render();});
  add(right,"binding","Tecla: "+(a.target.hasBinding()?a.label():"Sin asignar")+" · cambiar",()->chooseName(false));
  add(right,"delete","Quitar este botón",()->new AlertDialog.Builder(this).setTitle("¿Quitar "+a.label()+"?").setMessage("Se elimina su acción de Z6. Las otras asignaciones se conservan.").setPositiveButton("Quitar",(d,w)->{RemoteActions.remove(this,editing);page="main";render();}).setNegativeButton("Cancelar",null).show());
  right.addView(text(a.target.hardware()?"Esta tecla requiere el auxiliar. Preparar los botones especiales permite usar Android directamente en controles compatibles.":"La tecla se recibe mediante Android. No necesita una escucha de archivos de entrada.",11,0xFF9DA8B2));
 }
 private String kindName(String k){switch(k){case "open":return "Abrir app";case "reopen":return "Cerrar y volver a abrir";case "route":return "Reabrir y llegar al destino";case "home":return "Inicio de Z6";case "settings":return "Ajustes de Android";default:return "Silencio";}}
 private void startup(){
  page="startup";TvRecoveryConfig.Settings s=TvRecoveryConfig.get(this);frame("Arranque y despertar",s.name.isEmpty()?"Elige una app y configura su destino.":s.name+" · "+s.channel);LinearLayout left=column(6,false),right=column(4,false);
  add(left,"app","Aplicación: "+(s.name.isEmpty()?"Elegir":s.name),()->apps(true));
  add(left,"destination",s.verified?"Cambiar destino · "+s.channel:"Elegir destino en la app",()->teach(true));
  add(left,"test","Probar desde cero",()->engine().request("prueba",false,TvRecoveryConfig.get(this)));
  add(right,"boot","Al arrancar Android: "+(s.boot?"Activado":"Desactivado"),()->{engine().toggle("boot");render();});
  add(right,"wake","Al despertar: "+(s.wake?"Activado":"Desactivado"),()->{engine().toggle("wake");render();});
  right.addView(text("Apagar solo el televisor puede dejar el TV Box encendido. Si Android no informa un despertar, usa tu botón personalizado.",12,0xFFBBC4CC));
 }
 private void teach(boolean startup){TvRecoveryConfig.Settings s=startup?TvRecoveryConfig.get(this):action().target;if(s.pkg.isEmpty()){apps(startup);return;}if(TvNavigationService.active==null){accessibility();return;}new AlertDialog.Builder(this).setTitle("Elige tu destino en "+s.name).setMessage("1. Navega con las flechas y OK como siempre.\n2. Cuando veas el canal o pantalla que deseas, pulsa Home.\n3. Prueba desde cero y confirma para guardar.\n\nLa etiqueta pequeña no bloquea el control.").setPositiveButton("Comenzar",(d,w)->{if(startup)engine().request("configuración",true,null);else engine().requestProfile(action(),true,false);}).setNegativeButton("Volver",null).show();}
 private void apps(boolean startup){
  AppCatalog catalog=new AppCatalog(this);if(!catalog.ready()){message("Cargando aplicaciones…");new Thread(()->{try{java.util.List<AppCatalog.Entry> list=catalog.scan();ui.post(()->{catalog.accept(list);if(resumed)apps(startup);});}catch(RuntimeException e){ui.post(()->message("No se pudo consultar las aplicaciones"));}},"Z6Remote-apps").start();return;}
  boolean opening=!startup&&action().kind.equals("open");
  List<AppCatalog.Entry> entries=new ArrayList<>();for(AppCatalog.Entry e:catalog.all())if(opening?TvRecoveryConfig.openable(this,e.pkg):TvRecoveryConfig.eligible(this,e.pkg))entries.add(e);
  if(entries.isEmpty()){message(opening?"No hay aplicaciones disponibles para abrir":"No hay aplicaciones instaladas que Android permita reiniciar");return;}
  String[] names=new String[entries.size()];for(int i=0;i<names.length;i++)names[i]=entries.get(i).title;
  new AlertDialog.Builder(this).setTitle("Elegir aplicación").setItems(names,(d,w)->safe(()->{AppCatalog.Entry e=entries.get(w);if(startup)engine().choose(e.pkg);else{RemoteActions.Action a=action();TvRecoveryConfig.Settings s=new TvRecoveryConfig.Settings();s.pkg=e.pkg;s.name=e.title;s.signature=TvRecoveryConfig.signature(this,e.pkg);s.key=a.target.key;s.scan=a.target.scan;s.keyName=a.target.keyName;s.remoteCode=a.target.remoteCode;s.remoteDevice=a.target.remoteDevice;a.target=s;RemoteActions.save(this,a);}render();})).setNegativeButton("Volver",null).show();
 }
 private void newButton(){if(RemoteActions.get(this).size()>=32){message("Máximo 32 botones");return;}chooseName(true);}
 private void chooseName(boolean create){new AlertDialog.Builder(this).setTitle("¿Qué botón quieres configurar?").setItems(new String[]{"Netflix","YouTube","Otro botón libre"},(d,w)->{pendingKeyName=new String[]{"Netflix","YouTube","Otro botón"}[w];if(create){RemoteActions.Action a=new RemoteActions.Action();a.kind="open";a.target.keyName=pendingKeyName;RemoteActions.save(this,a);editing=a.id;page="edit";render();}prepareLearning();}).setNegativeButton("Volver",null).show();}
 private void prepareLearning(){preparing=true;AlertDialog d=new AlertDialog.Builder(this).setTitle("Asignar "+pendingKeyName).setMessage("Pulsa Comenzar cuando tengas el control. Después pulsa solo la tecla elegida. Home y Atrás cancelan; las teclas esenciales no se reasignan.").setPositiveButton("Comenzar",(v,w)->beginLearning()).setNegativeButton("Volver",null).show();keyDialog=d;d.setOnDismissListener(v->{preparing=false;if(keyDialog==d)keyDialog=null;});}
 private void maybeLearn(){String prepare=getIntent().getStringExtra("prepare_remote"),learn=getIntent().getStringExtra("learn_remote");getIntent().removeExtra("prepare_remote");getIntent().removeExtra("learn_remote");String name=prepare!=null?prepare:learn;if(name==null)return;pendingKeyName=name.length()>40?"Otro botón":name;RemoteActions.Action found=null;for(RemoteActions.Action a:RemoteActions.get(this))if(a.label().equals(pendingKeyName))found=a;if(found==null){found=new RemoteActions.Action();found.target.keyName=pendingKeyName;found.kind="open";RemoteActions.save(this,found);}editing=found.id;page="edit";render();if(prepare!=null)prepareLearning();else beginLearning();}
 private void beginLearning(){learning=true;observedKey=-1;observedScan=0;keyResult="waiting";remoteSession=TvRemoteKeys.begin(this);ui.removeCallbacks(expireKey);ui.postDelayed(expireKey,60000);if(status!=null)status.setText("Pulsa únicamente "+pendingKeyName+". Atrás cancela.");}
 private void endLearning(){learning=preparing=confirming=false;ui.removeCallbacks(expireKey);TvRemoteKeys.end(this);}
 static boolean isLearning(){return current!=null&&current.resumed&&current.learning;}
 static boolean keySetupActive(){return current!=null&&current.resumed&&(current.learning||current.preparing||current.confirming);}
 static boolean keyPromptReady(){return current!=null&&current.resumed&&current.preparing;}
 static void stopLearning(){TvRecoveryActivity a=current;if(a!=null){a.endLearning();if(a.keyDialog!=null)a.keyDialog.dismiss();}}
 static void remoteReady(String token,boolean ok){TvRecoveryActivity a=current;if(a!=null&&a.learning&&token.equals(a.remoteSession))a.status.setText("Pulsa únicamente "+a.pendingKeyName+". Atrás cancela."+(ok?" Detección ampliada disponible.":" Detección de Android."));}
 static void remoteDetected(String token,String device,int code){TvRecoveryActivity a=current;if(a!=null&&a.learning&&token.equals(a.remoteSession))a.confirmKey(0,0,device,code);}
 static JSONObject keyState()throws JSONException{TvRecoveryActivity a=current;return a==null?new JSONObject():new JSONObject().put("visible",a.resumed).put("result",a.keyResult).put("code",a.observedKey).put("scan",a.observedScan).put("message",a.status==null?"":a.status.getText());}
 static boolean capture(KeyEvent e){TvRecoveryActivity a=current;if(a==null||!a.resumed)return false;if(a.learning)return a.captureKey(e);if((a.preparing||a.confirming)&&e.getKeyCode()==KeyEvent.KEYCODE_HOME){if(e.getAction()==KeyEvent.ACTION_DOWN){a.endLearning();if(a.keyDialog!=null)a.keyDialog.dismiss();}return false;}return (a.preparing||a.confirming)&&!TvRecoveryConfig.protectedKey(e.getKeyCode());}
 private boolean captureKey(KeyEvent e){int k=e.getKeyCode();if(k==KeyEvent.KEYCODE_BACK||k==KeyEvent.KEYCODE_HOME){if(e.getAction()==0){endLearning();keyResult="cancelled";}return k!=KeyEvent.KEYCODE_HOME;}if(TvRecoveryConfig.protectedKey(k)){keyResult="reserved";if(e.getAction()==0)status.setText("Esa tecla conserva su función. Pulsa otra tecla libre.");return false;}if(e.getAction()==0&&e.getRepeatCount()==0){observedKey=k;observedScan=e.getScanCode();confirmKey(k,e.getScanCode(),"",0);}return true;}
 private void confirmKey(int code,int scan,String device,int raw){
  learning=false;confirming=true;ui.removeCallbacks(expireKey);TvRemoteKeys.end(this);keyResult="detected";observedKey=code;observedScan=raw==0?scan:raw;
  AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Asignar "+pendingKeyName).setMessage(raw==0?"Android detectó la tecla. Se utilizará la acción de este botón.":"Esta tecla depende del auxiliar. Para Netflix y YouTube, usa Preparar botones especiales si tu control es compatible.").setPositiveButton("Asignar",(d,w)->safe(()->{
   if(raw!=0)throw new IllegalStateException("Prepara los botones especiales o elige una tecla que Android entregue. La asignación anterior se conserva.");
   RemoteActions.Action a=action();a.target.key=code;a.target.scan=scan;a.target.keyName=pendingKeyName.equals("Otro botón")?KeyEvent.keyCodeToString(code).replace("KEYCODE_",""):pendingKeyName;a.target.remoteCode=0;a.target.remoteDevice="";RemoteActions.save(this,a);render();
  })).setNegativeButton("Cancelar",null).show();keyDialog=dialog;dialog.setOnDismissListener(d->{confirming=false;keyDialog=null;});dialog.getButton(AlertDialog.BUTTON_POSITIVE).requestFocus();
 }
 private void prepareSpecial(){new AlertDialog.Builder(this).setTitle("Preparar botones especiales").setMessage("En controles comprobados, Android guardará una configuración para Netflix y YouTube. Solo cambia esas dos teclas; no modifica archivos del sistema. Esta preparación inicial necesita el auxiliar conectado. Después la recepción de las teclas no depende de depuración.\n\nEn otro control, usa Añadir botón para comprobar sus teclas.").setPositiveButton("Preparar",(d,w)->{status.setText("Preparando configuración de Android…");new Thread(()->{String reply=SystemBridge.call("REMOTE_LAYOUT:apply");ui.post(()->safe(()->{try{JSONObject result=new JSONObject(reply);if(!result.optString("status").equals("OK")){message("Activa el control con una flecha y vuelve a preparar. Este mapa requiere Remoter ATV3 compatible.");return;}RemoteActions.nativePrepared(this,result);render();message("Netflix y YouTube preparados en Android");}catch(JSONException e){throw new IllegalStateException("Conecta el acceso inicial desde el PC. No se cambió la asignación.");}}));},"Z6Remote-setup").start();}).setNeutralButton("Restaurar teclas originales",(d,w)->restoreSpecial()).setNegativeButton("Volver",null).show();}
 private void restoreSpecial(){String layout=getSharedPreferences(RemoteActions.STORE,0).getString("layout","{}");if(layout.equals("{}")){message("No hay una configuraci\u00f3n nativa de Z6 guardada.");return;}String encoded=android.util.Base64.encodeToString(layout.getBytes(java.nio.charset.StandardCharsets.UTF_8),android.util.Base64.NO_WRAP);new Thread(()->{String reply=SystemBridge.call("REMOTE_LAYOUT:restore:"+encoded);ui.post(()->safe(()->{try{JSONObject o=new JSONObject(reply);if(!o.optString("status").equals("OK"))throw new JSONException("Not restored");List<RemoteActions.Action> list=new ArrayList<>();for(RemoteActions.Action a:RemoteActions.get(this)){a=a.copy();if(a.target.key==188&&a.target.scan==87||a.target.key==189&&a.target.scan==88)a.enabled=false;list.add(a);}RemoteActions.persist(this,list);render();message("Teclas originales restauradas; acciones de esos botones desactivadas.");}catch(JSONException e){message("Hace falta el acceso inicial. Puedes restaurar con herramientas/mando.ps1 -Restaurar desde el PC.");}}));},"Z6Remote-restore").start();}
 private void usageAccess(){if(NativeAppRestart.available(this)){new AlertDialog.Builder(this).setTitle("Acceso disponible").setMessage("Z6 puede comprobar que una app se cerró antes de volver a abrirla. Solo consulta la app elegida durante esa acción. Si Android impide cerrarla, se usa el auxiliar disponible o se informa el fallo.").setPositiveButton("Entendido",null).show();return;}new AlertDialog.Builder(this).setTitle("Permitir comprobar el cierre").setMessage("Autoriza Acceso de uso para Z6. Permite verificar que la app dejó de ejecutarse. Es una preparación inicial; no requiere activar depuración cada vez.").setPositiveButton("Abrir acceso",(d,w)->{Intent i=new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).setData(android.net.Uri.parse("package:"+getPackageName()));if(i.resolveActivity(getPackageManager())!=null)startActivity(i);else message("Este firmware no ofrece esa pantalla. El asistente del PC puede preparar el permiso una vez.");}).setNegativeButton("Volver",null).show();}
 private void accessibility(){new AlertDialog.Builder(this).setTitle("Asistencia para el destino").setMessage("Lee únicamente los controles de la app elegida durante la configuración o ejecución del recorrido. No lee contraseñas. El panel principal mantiene su permiso de lectura desactivado.").setPositiveButton("Abrir permiso",(d,w)->{Intent i=new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);if(i.resolveActivity(getPackageManager())!=null)startActivity(i);}).setNegativeButton("Volver",null).show();}
 private void help(){new AlertDialog.Builder(this).setTitle("Control y permisos").setMessage("Estas funciones forman parte de Z6 Panel.\n\nAndroid recibe las teclas configuradas. Z6 ejecuta su acción. Reabrir usa primero el cierre normal de Android y comprueba el resultado. Algunas apps o firmwares necesitan el auxiliar para acciones restringidas.\n\nDiagnóstico y permisos revisa esas capacidades y permite reparar el acceso avanzado. No debería ser un paso de uso diario. No instalamos root ni modificamos la partición del sistema.\n\nLas teclas disponibles dependen del control y del fabricante.").setPositiveButton("Entendido",null).show();}
 private void update(){if(!resumed||learning||preparing||confirming)return;Button access=controls.get("access");if(access!=null)access.setText(NativeAppRestart.available(this)?"✓ Reapertura con Android disponible":"Preparar acceso para reabrir apps");if(status!=null&&(page.equals("main")||page.equals("startup")))status.setText((page.equals("main")?"Cada botón tiene su propia acción. Arranque y despertar se configuran por separado.\n":"")+"Última acción: "+engine().status());}
 static JSONObject inspect()throws JSONException{TvRecoveryActivity a=current;JSONObject o=new JSONObject();if(a==null)return o;JSONArray rows=new JSONArray();for(Map.Entry<String,Button> e:a.controls.entrySet())rows.put(new JSONObject().put("id",e.getKey()).put("label",e.getValue().getText()).put("focused",e.getValue().hasFocus()));return o.put("visible",a.resumed).put("page",a.page).put("editing",a.editing).put("controls",rows);}
 static boolean shellChoose(String id){TvRecoveryActivity a=current;if(a==null||!a.resumed)return false;Button b=a.controls.get(id);return b!=null&&b.performClick();}
 private void back(){if(learning){endLearning();render();return;}if(!page.equals("main")){page="main";render();return;}finish();}
 @Override public void onBackPressed(){back();}
 @Override public boolean onKeyDown(int key,KeyEvent e){if(capture(e))return true;return super.onKeyDown(key,e);}
}
