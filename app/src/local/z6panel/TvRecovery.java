package local.z6panel;

import android.app.KeyguardManager;
import android.content.*;
import android.net.*;
import android.os.*;
import android.provider.Settings;
import android.view.KeyEvent;
import android.widget.Toast;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;

/** One event-driven recovery at a time. No foreground polling or idle UI inspection. */
final class TvRecovery {
 static TvRecovery instance;
 final Context context;final Handler ui=new Handler(Looper.getMainLooper());
 private final ThreadPoolExecutor work=new ThreadPoolExecutor(1,1,20,TimeUnit.SECONDS,new ArrayBlockingQueue<Runnable>(1));
 private final SharedPreferences runtime;
 private final Set<String> consumed=new HashSet<>();
 private volatile long generation;
 private boolean working,listening,launched,guide,testing,targetSeen,automaticRetried;
 private String phase="idle",reason="",status="Sin configurar",learnSource="";
 private int routeIndex,scrolls,walkDirection=1;
 private long lastClick,lastShortcutEvent,prepareAfter,navigateAfter;
 private TvRecoveryConfig.Settings job,draft;
 private Runnable deadline;
 private ConnectivityManager.NetworkCallback network;
 private final Runnable probe=()->probe();
 private final BroadcastReceiver power=new BroadcastReceiver(){public void onReceive(Context c,Intent i){power(i.getAction());}};

 private TvRecovery(Context c){context=c.getApplicationContext();runtime=context.getSharedPreferences("tv_recovery_runtime_v1",0);status=runtime.getString("status","Sin configurar");work.allowCoreThreadTimeOut(true);}
 static TvRecovery get(Context c){if(instance==null)instance=new TvRecovery(c);return instance;}
 static void attach(Context c){TvRecovery t=get(c);TvRemoteKeys.resume(c);t.watch();if(t.runtime.getBoolean("inflight",false)){t.runtime.edit().putBoolean("inflight",false).apply();t.setStatus("La recuperación anterior se interrumpió; puedes usar el botón");}t.checkBoot();}
 static void boot(Context c){TvRecovery t=get(c);if(PanelService.active!=null)t.ui.post(t::checkBoot);}
 private int bootCount(){return Settings.Global.getInt(context.getContentResolver(),Settings.Global.BOOT_COUNT,-1);}
 private void checkBoot(){
  TvRecoveryConfig.Settings s=TvRecoveryConfig.get(context);int boot=bootCount();
  boolean pending=runtime.getInt("last_boot",-1)!=boot;
  runtime.edit().putInt("last_boot",boot).apply();
  if(s.boot&&pending&&boot>=0)request("arranque",false,null);
 }
 private boolean interactive(){return ((PowerManager)context.getSystemService(Context.POWER_SERVICE)).isInteractive();}
 private boolean unlocked(){return !((KeyguardManager)context.getSystemService(Context.KEYGUARD_SERVICE)).isKeyguardLocked();}
 private void watch(){
  boolean needed=TvRecoveryConfig.get(context).wake||busy();
  if(needed&&!listening){IntentFilter f=new IntentFilter(Intent.ACTION_SCREEN_OFF);f.addAction(Intent.ACTION_SCREEN_ON);f.addAction(Intent.ACTION_USER_PRESENT);context.registerReceiver(power,f);listening=true;}
  else if(!needed&&listening){context.unregisterReceiver(power);listening=false;}
 }
 private void power(String action){
  if(Intent.ACTION_SCREEN_OFF.equals(action)){
   runtime.edit().putBoolean("asleep",true).putInt("sleep_boot",bootCount()).apply();cancel("Recuperación detenida al apagar la pantalla");
  }else if(Intent.ACTION_SCREEN_ON.equals(action)){
   boolean wake=runtime.getBoolean("asleep",false)&&runtime.getInt("sleep_boot",-2)==bootCount();
   runtime.edit().putBoolean("asleep",false).apply();
   if(wake&&TvRecoveryConfig.get(context).wake&&!busy()){
    final long token=generation;ui.postDelayed(()->{if(token==generation&&interactive()&&TvRecoveryConfig.get(context).wake)request("despertar",false,null);},1500);
   }
  }else if(Intent.ACTION_USER_PRESENT.equals(action))ready();
 }
 static void readyChanged(){if(instance!=null)instance.ui.post(instance::ready);}
 void changed(){watch();TvRemoteKeys.changed(context);TvRecoveryActivity.refresh();}
 void restored(){runtime.edit().putInt("last_boot",bootCount()).putBoolean("asleep",false).putBoolean("inflight",false).apply();changed();}
 void suspend(){cancel("");TvRemoteKeys.suspend();if(listening){context.unregisterReceiver(power);listening=false;}}
 boolean busy(){return !phase.equals("idle");}
 String status(){return status;}
 String phase(){return phase;}
 private void setStatus(String value){status=value;runtime.edit().putString("status",value).apply();TvRecoveryActivity.refresh();}
 private void notice(String value){setStatus(value);Toast.makeText(context,value,Toast.LENGTH_LONG).show();}
 void choose(String pkg){
  if(!TvRecoveryConfig.eligible(context,pkg))throw new IllegalArgumentException("Esta app no se puede reiniciar con seguridad");
  cancel("");TvRecoveryConfig.Settings old=TvRecoveryConfig.get(context),s=new TvRecoveryConfig.Settings();
  s.pkg=pkg;s.key=old.key;s.scan=old.scan;s.keyName=old.keyName;s.remoteDevice=old.remoteDevice;s.remoteCode=old.remoteCode;s.signature=TvRecoveryConfig.signature(context,pkg);
  try{s.name=context.getPackageManager().getApplicationLabel(context.getPackageManager().getApplicationInfo(pkg,0)).toString();}catch(Exception e){s.name=pkg;}
  TvRecoveryConfig.save(context,s);runtime.edit().putInt("last_boot",bootCount()).putBoolean("asleep",false).apply();
  setStatus("App elegida. Configura el recorrido al canal");changed();
 }
 void toggle(String kind){
  TvRecoveryConfig.Settings s=TvRecoveryConfig.get(context).copy();
  if(kind.equals("boot"))s.boot=!s.boot;else if(kind.equals("wake"))s.wake=!s.wake;else if(kind.equals("button"))s.button=!s.button;else throw new IllegalArgumentException();
  boolean enabling=kind.equals("boot")?s.boot:kind.equals("wake")?s.wake:s.button;
  if(enabling&&(!TvRecoveryConfig.sameApp(context,s)||!s.verified||TvNavigationService.active==null))throw new IllegalStateException("Primero habilita la ayuda y prueba el recorrido al canal");
  if(enabling&&kind.equals("button")&&!s.hasBinding())throw new IllegalStateException("Asigna una tecla libre primero");
  if(!enabling)cancel("");TvRecoveryConfig.save(context,s);runtime.edit().putInt("last_boot",bootCount()).apply();changed();
 }
 void assignKey(int key){
  assignKey(key,0,"");
 }
 void assignKey(int key,int scan,String name){
  if((key!=0||scan!=0)&&!TvRecoveryConfig.allowedBinding(key,scan))throw new IllegalArgumentException("Esa tecla está reservada; prueba un acceso a una app u otra tecla libre");
  cancel("");lastShortcutEvent=0;TvRecoveryConfig.Settings s=TvRecoveryConfig.get(context).copy();s.key=key;s.scan=scan;s.keyName=name;s.remoteCode=0;s.remoteDevice="";s.button=false;TvRecoveryConfig.save(context,s);changed();
 }
 void assignHardware(String device,int code,String name){if(device.isEmpty()||device.length()>256||!local.z6bridge.RemoteKeyFormat.allowed(code))throw new IllegalArgumentException("Tecla física no admitida");cancel("");lastShortcutEvent=0;TvRecoveryConfig.Settings s=TvRecoveryConfig.get(context).copy();s.key=s.scan=0;s.remoteDevice=device;s.remoteCode=code;s.keyName=name;s.button=false;TvRecoveryConfig.save(context,s);changed();}
 void activate(){
  TvRecoveryConfig.Settings s=TvRecoveryConfig.get(context).copy();
  if(!s.verified||!TvRecoveryConfig.sameApp(context,s)||TvNavigationService.active==null)throw new IllegalStateException("Prueba primero el recorrido y habilita Asistencia de TV");
  s.boot=true;s.wake=true;s.button=s.hasBinding();TvRecoveryConfig.save(context,s);
  runtime.edit().putInt("last_boot",bootCount()).putBoolean("asleep",false).apply();setStatus("Activado: se reabrirá la app al arrancar y al despertar");changed();
 }
 void disable(){cancel("");TvRecoveryConfig.Settings s=TvRecoveryConfig.get(context).copy();s.boot=s.wake=s.button=false;TvRecoveryConfig.save(context,s);TvRemoteKeys.disable(context);TvRecoveryActivity.stopLearning();setStatus("Inicio automático y botón desactivados");changed();}
 void request(String why,boolean teach,TvRecoveryConfig.Settings trial){
  if(busy()){cancel("Recuperación cancelada");return;}
  if(!interactive()){setStatus("La pantalla está apagada; no se abrirá la app");return;}
  job=(trial==null?TvRecoveryConfig.get(context):trial).copy();
  if(job.pkg.isEmpty()){notice("Elige primero la aplicación de TV");return;}
  if((teach||trial!=null)&&TvNavigationService.active==null){notice("Activa Asistencia de TV para configurar el canal");return;}
  generation++;guide=teach;testing=trial!=null;draft=teach?job.copy():trial;reason=why;phase="waiting";
  launched=targetSeen=automaticRetried=false;routeIndex=scrolls=0;walkDirection=1;
  setStatus("Preparando "+job.name+"…");watch();final long token=generation;
  // Let Android finish restoring HOME/input services before a cold-boot launch.
  // This is one cancellable preparation callback, never a timed navigation sequence.
  prepareAfter=why.equals("arranque")?SystemClock.elapsedRealtime()+5000:0;
  if(prepareAfter!=0)ui.postDelayed(()->{if(token==generation)ready();},5000);
  deadline=()->{if(token==generation)fail("No se pudo preparar la app: revisa Internet y el control auxiliar",false);};ui.postDelayed(deadline,90000);
  if(why.equals("arranque")||why.equals("despertar")){
   network=new ConnectivityManager.NetworkCallback(){public void onAvailable(Network n){ui.post(TvRecovery.this::ready);}public void onCapabilitiesChanged(Network n,NetworkCapabilities c){ui.post(TvRecovery.this::ready);}};
   try{((ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE)).registerDefaultNetworkCallback(network);}catch(RuntimeException e){network=null;}
  }
  ready();
 }
 private boolean internet(){ConnectivityManager m=(ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);NetworkCapabilities n=m.getNetworkCapabilities(m.getActiveNetwork());return n!=null&&n.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);}
 private void ready(){
  if(!phase.equals("waiting")||working||!interactive()||!unlocked()||SystemClock.elapsedRealtime()<prepareAfter)return;
  final boolean automatic=reason.equals("arranque")||reason.equals("despertar");
  if(automatic&&!internet())return;
  if(automatic&&job.verified&&TvNavigationService.active==null)return;
  if(SystemBridge.remote==null||!SystemBridge.remote.isBinderAlive()){if(!automatic)fail("Recupera el control auxiliar en Equipo → Diagnóstico",false);return;}
  final long token=generation;final TvRecoveryConfig.Settings taskJob=job;working=true;
  submit(()->{
   String result;
   if(token!=generation)result="CANCELLED";
   else if(!TvRecoveryConfig.sameApp(context,taskJob))result="APP_CHANGED";
   else if(!"OK".equals(SystemBridge.read("PING",1200)))result="NO_HELPER";
   else if(token!=generation||!interactive())result="CANCELLED";
   else{
    runtime.edit().putBoolean("inflight",true).commit();
    result=SystemBridge.call("RESTART_APP:"+taskJob.pkg);
   }
   final String outcome=result;ui.post(()->{
    working=false;if(token!=generation){ready();return;}
    if(!"OK".equals(outcome)){
     if(automatic&&(outcome.equals("NO_HELPER")||outcome.contains("BUSY"))){
      // Startup capability checks share the bounded helper queue. No app restart
      // was accepted: retry preparation only, within the original 90 s deadline.
      ui.postDelayed(()->{if(token==generation)ready();},750);return;
     }
     fail(outcome.equals("APP_CHANGED")?"La app cambió o ya no está disponible; vuelve a elegirla":"No se pudo cerrar la app: "+outcome,false);return;
    }
    launch(token);
   });
  });
 }
 private void submit(Runnable task){try{work.execute(task);}catch(RejectedExecutionException e){working=false;fail("El control está ocupado; vuelve a intentarlo",false);}}
 private void launch(long token){
  if(token!=generation||!interactive())return;
  clearDeadline();unwatchNetwork();Intent intent=TvRecoveryConfig.launchIntent(context,job.pkg);
  if(intent==null){fail("La app ya no tiene un acceso de inicio disponible",false);return;}
  try{
   if(PanelService.active!=null)PanelService.active.hideForTvRecovery();
   if(TvNavigationService.active!=null)TvNavigationService.active.begin(job.pkg);
   context.startActivity(intent);launched=true;phase=guide?"teaching":"navigating";
   // Cold boot can restore the player's own session asynchronously. Wait for
   // that initial restoration before inspecting and activating named controls.
   navigateAfter=SystemClock.elapsedRealtime()+(reason.equals("arranque")?5000:500);
   runtime.edit().putBoolean("inflight",false).apply();
   if(!guide&&(!job.verified||job.steps.length()==0)){finish("App reabierta. El canal necesita configuración",true);return;}
   if(TvNavigationService.active==null){finish("App reabierta. Activa Asistencia de TV para llegar al canal",true);return;}
   if(guide){draft.steps=new JSONArray();draft.verified=false;setStatus("Elige Televisión, Perú y tu canal en el panel de ayuda");}
   deadline=()->{if(token==generation)fail(guide?"Terminó el tiempo de configuración; vuelve a intentarlo":"App abierta; no se pudo confirmar el canal. Puedes continuar manualmente",false);};
   ui.postDelayed(deadline,guide?300000:60000);scheduleProbe(500);
  }catch(RuntimeException e){fail("Android no permitió abrir la app: "+e.getClass().getSimpleName(),false);}
 }
 void foreground(String pkg){
  if(!launched||!busy()||pkg==null)return;
  if(pkg.equals(job.pkg)){targetSeen=true;return;}
  if(targetSeen&&!pkg.equals(context.getPackageName())&&!pkg.equals("android")&&!pkg.equals("com.android.systemui")){
   final long token=generation;ui.postDelayed(()->{
    TvNavigationService nav=TvNavigationService.active;if(token!=generation||!busy()||nav==null)return;
    nav.isTargetForeground(yes->{if(token==generation&&Boolean.FALSE.equals(yes))cancel("Navegación detenida al cambiar de aplicación");});
   },200);
  }
 }
 void navigationChanged(){if(phase.equals("teaching")||phase.equals("navigating"))scheduleProbe(160);}
 private void scheduleProbe(long ms){ui.removeCallbacks(probe);ui.postDelayed(probe,Math.max(ms,navigateAfter-SystemClock.elapsedRealtime()));}
 private void probe(){
  if(!phase.equals("teaching")&&!phase.equals("navigating"))return;
  TvNavigationService nav=TvNavigationService.active;
  if(nav==null){fail("La ayuda de navegación se desconectó; la app sigue abierta",false);return;}
  final long token=generation;nav.snapshot(snapshot->{
   if(token!=generation||!busy())return;
   if(snapshot==null)return;
   if(guide){nav.showGuide(snapshot);return;}
   if(routeIndex>=job.steps.length()){
    if(SystemClock.elapsedRealtime()-lastClick<400){scheduleProbe(450);return;}
    if(nav.matchesEndpoint(snapshot,job.endpoint)){
     if(testing){clearDeadline();phase="confirming";deadline=()->{if(token==generation)fail("La prueba quedó sin confirmar; el recorrido no se guardó",false);};ui.postDelayed(deadline,300000);setStatus("Comprueba que se reproduce el canal elegido");nav.showConfirmation(job.channel);}
     else finish("Canal preparado: "+job.channel,false);
    }
    return;
   }
   try{
    JSONObject step=job.steps.getJSONObject(routeIndex);
    if(step.optString("op","click").equals("back")){
     if(!step.optString("activity").isEmpty()&&!step.optString("activity").equals(snapshot.activity))return;
     nav.back(ok->{if(token!=generation)return;if(ok)advanced();else fail("No se pudo volver dentro de la app",true);});return;
    }
    TvNavigationService.Selector selector=TvNavigationService.Selector.parse(step.getJSONObject("target"));
    int matches=snapshot.matches(selector);
    // Some players expose their channel in a full list or a compact player
    // list. Only the final, explicitly taught channel may use this fallback.
    if(matches==0&&routeIndex==job.steps.length()-1&&selector.label().equalsIgnoreCase(job.channel)){
     for(TvNavigationService.Selector candidate:snapshot.controls)if(candidate.nativeRow&&candidate.label().equalsIgnoreCase(job.channel)){selector=candidate;matches++;}
    }
    if(matches>1){fail("El menú cambió: hay controles ambiguos. Revisa el recorrido",true);return;}
    if(matches==0&&routeIndex>0&&!testing&&!automaticRetried&&(reason.equals("arranque")||reason.equals("despertar"))){
     long remaining=4000-(SystemClock.elapsedRealtime()-lastClick);
     if(remaining>0){scheduleProbe(remaining);return;}
     retryAutomatic();return;
    }
    if(matches==1){nav.click(selector,ok->{if(token!=generation)return;if(ok)advanced();else fail(nav.failureMessage(),false);});}
    else if(snapshot.scrollable&&scrolls<12){
     scrolls++;nav.scroll(walkDirection>0,ok->{if(token!=generation)return;if(!ok&&walkDirection>0){walkDirection=-1;scrolls=0;scheduleProbe(180);}else if(ok)scheduleProbe(180);});
    }
   }catch(JSONException e){fail("El recorrido necesita configurarse otra vez",true);}
  });
 }
 private void retryAutomatic(){
  automaticRetried=true;generation++;launched=targetSeen=false;phase="waiting";routeIndex=scrolls=0;walkDirection=1;prepareAfter=0;
  ui.removeCallbacks(probe);clearDeadline();if(TvNavigationService.active!=null)TvNavigationService.active.end();
  setStatus("Reintentando la apertura de "+job.name+"…");final long token=generation;
  deadline=()->{if(token==generation)fail("No se pudo preparar la app; puedes usar el botón",false);};ui.postDelayed(deadline,30000);ready();
 }
 private void advanced(){routeIndex++;scrolls=0;walkDirection=1;lastClick=SystemClock.elapsedRealtime();scheduleProbe(400);}
 void guideClick(TvNavigationService.Selector selector){
  if(!phase.equals("teaching")||draft.steps.length()>=24)return;
  final long token=generation;TvNavigationService nav=TvNavigationService.active;if(nav==null)return;
  final String source=nav.activity();nav.click(selector,ok->{if(token!=generation)return;
   if(!ok){notice(nav.failureMessage());return;}
   try{draft.steps.put(new JSONObject().put("op","click").put("activity",source).put("target",selector.json()));learnSource=source;draft.channel=selector.label();}
   catch(JSONException e){fail("No se pudo guardar el paso",false);return;}
   scheduleProbe(350);
  });
 }
 void guideScroll(boolean forward){TvNavigationService nav=TvNavigationService.active;if(!phase.equals("teaching")||nav==null)return;final long token=generation;nav.scroll(forward,ok->{if(token==generation)scheduleProbe(200);});}
 void guideBack(){
  TvNavigationService nav=TvNavigationService.active;if(!phase.equals("teaching")||nav==null)return;
  final String source=nav.activity();final long token=generation;nav.back(ok->{if(token!=generation)return;if(ok)try{draft.steps.put(new JSONObject().put("op","back").put("activity",source));scheduleProbe(350);}catch(JSONException e){fail("No se pudo registrar el regreso",false);}});
 }
 void taught(){
  if(!phase.equals("teaching")||draft.steps.length()==0){notice("Elige primero los controles hasta llegar a tu canal");return;}
  TvNavigationService nav=TvNavigationService.active;if(nav==null)return;
  final long token=generation;nav.snapshot(snapshot->{
   if(token!=generation||!phase.equals("teaching")||snapshot==null)return;
   JSONObject endpoint=nav.endpoint(snapshot,draft.channel,learnSource);
   if(endpoint==null){notice("No se puede verificar esta pantalla; prueba cuando el canal ya se vea");return;}
   draft.endpoint=endpoint;draft.verified=true;TvRecoveryConfig.Settings trial=draft.copy();
   finish("Probando el recorrido desde cero…",false);request("prueba",false,trial);
  });
 }
 void confirmTest(boolean yes){
  if(!phase.equals("confirming"))return;
  if(!yes){finish("Recorrido sin guardar. Puedes configurarlo otra vez",true);return;}
  TvRecoveryConfig.save(context,job);finish("Recorrido guardado: "+job.channel,false);
  context.startActivity(new Intent(context,TvRecoveryActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP));changed();
 }
 void cancel(String message){if(!busy()){generation++;return;}finish(message,false);}
 private void fail(String message,boolean invalidRoute){
  if(invalidRoute&&!testing&&!guide){TvRecoveryConfig.Settings s=TvRecoveryConfig.get(context).copy();s.verified=false;TvRecoveryConfig.save(context,s);}
  finish(message,true);
 }
 private void clearDeadline(){if(deadline!=null)ui.removeCallbacks(deadline);deadline=null;}
 private void unwatchNetwork(){if(network!=null){try{((ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE)).unregisterNetworkCallback(network);}catch(RuntimeException ignored){}network=null;}}
 private void finish(String message,boolean toast){
  generation++;phase="idle";guide=testing=launched=targetSeen=false;job=draft=null;
  ui.removeCallbacks(probe);clearDeadline();unwatchNetwork();runtime.edit().putBoolean("inflight",false).apply();
  if(TvNavigationService.active!=null)TvNavigationService.active.end();watch();
  if(!message.isEmpty()){if(toast)notice(message);else setStatus(message);}else TvRecoveryActivity.refresh();
 }
 static boolean handleKey(KeyEvent event){return instance!=null&&instance.key(event);}
 private boolean key(KeyEvent e){
  int code=e.getKeyCode();boolean down=e.getAction()==KeyEvent.ACTION_DOWN;
  TvRecoveryConfig.Settings s=TvRecoveryConfig.get(context);
  if(!busy()&&!TvRecoveryActivity.keySetupActive()&&!(s.button&&s.matches(e))&&consumed.isEmpty())return false;
  String identity=code+":"+e.getScanCode()+":"+e.getDeviceId();
  if(!down&&consumed.remove(identity))return true;
  if(TvRecoveryActivity.capture(e)){if(down)consumed.add(identity);return true;}
  if(s.button&&s.matches(e)){if(down){consumed.add(identity);long now=SystemClock.elapsedRealtime();boolean first=e.getRepeatCount()==0&&now-lastShortcutEvent>350;lastShortcutEvent=now;if(first){if(busy())cancel("Recuperación cancelada");else request("botón",false,null);}}return true;}
  if(!busy())return false;
  if(code==KeyEvent.KEYCODE_HOME){if(down)cancel("Navegación cancelada");return false;}
  if(code==KeyEvent.KEYCODE_BACK||code==KeyEvent.KEYCODE_ESCAPE){if(down){consumed.add(identity);cancel("Navegación cancelada");}return true;}
  if(TvNavigationService.active!=null&&TvNavigationService.active.guideKey(e))return true;
  if(down&&(code>=19&&code<=23||code==KeyEvent.KEYCODE_ENTER))cancel("Puedes continuar con el control");
  return false;
 }
 void hardwareShortcut(){long now=SystemClock.elapsedRealtime();boolean first=now-lastShortcutEvent>350;lastShortcutEvent=now;if(first){if(busy())cancel("Recuperación cancelada");else request("botón",false,null);}}
 JSONObject inspect()throws JSONException{
  TvRecoveryConfig.Settings s=TvRecoveryConfig.get(context);return new JSONObject().put("phase",phase).put("status",status).put("reason",reason)
   .put("package",s.pkg).put("channel",s.channel).put("verified",s.verified).put("boot",s.boot).put("wake",s.wake)
   .put("button",s.button).put("key",s.key).put("scan",s.scan).put("keyName",s.keyName).put("learningKey",TvRecoveryActivity.isLearning()).put("keyPromptReady",TvRecoveryActivity.keyPromptReady()).put("navigationService",TvNavigationService.active!=null).put("step",routeIndex).put("automaticRetried",automaticRetried)
   .put("steps",s.steps.length()).put("draftSteps",draft==null?0:draft.steps.length()).put("keyLearning",TvRecoveryActivity.keyState()).put("remoteCode",s.remoteCode).put("remoteInput",TvRemoteKeys.inspect()).put("screenListener",listening).put("networkListener",network!=null).put("workerActive",work.getActiveCount());
 }
 void testEvent(String event){
  if(!("ranchu".equals(Build.HARDWARE)||"goldfish".equals(Build.HARDWARE)))throw new SecurityException("Emulator only");
  if(event.equals("boot")){checkBoot();}
  else if(event.equals("wake_cancel")){power(Intent.ACTION_SCREEN_OFF);power(Intent.ACTION_SCREEN_ON);disable();}
  else if(event.equals("held_button")){TvRecoveryConfig.Settings s=TvRecoveryConfig.get(context);if(!s.button||!s.hasBinding())throw new IllegalStateException();for(int i=0;i<8;i++){long now=SystemClock.uptimeMillis();key(new KeyEvent(now,now,0,s.key,0,0,0,s.scan));key(new KeyEvent(now,now,1,s.key,0,0,0,s.scan));}}
  else if(event.equals("off"))power(Intent.ACTION_SCREEN_OFF);else if(event.equals("on"))power(Intent.ACTION_SCREEN_ON);else throw new IllegalArgumentException();
 }
}
