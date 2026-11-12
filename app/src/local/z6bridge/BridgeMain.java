package local.z6bridge;
import android.os.*;import android.content.*;import android.content.pm.*;import java.io.*;import java.util.*;import java.util.concurrent.*;
/** ADB shell helper. Binder authenticates the app UID. No network listener or arbitrary commands. */
public final class BridgeMain {
 static final String DESCRIPTOR="local.z6panel.system.v1";
 static final String VERSION="0.19.0-tv3";
 static final java.util.concurrent.atomic.AtomicLong attachAttempts=new java.util.concurrent.atomic.AtomicLong();
 static volatile boolean clientLinked;
 static final class Attacher implements Runnable {
  final Handler handler;final Object manager;final Class<?> managerType;final IBinder bridge;
  volatile IBinder client;int retries;
  Attacher(Handler h,Object m,Class<?> t,IBinder b){handler=h;manager=m;managerType=t;bridge=b;}
  void later(long delay){handler.removeCallbacks(this);handler.postDelayed(this,delay);}
  public void run(){
   if(client!=null&&client.isBinderAlive())return;
   Binder token=new Binder();boolean acquired=false;attachAttempts.incrementAndGet();
   try{
    Bundle extras=new Bundle();extras.putBinder("bridge",bridge);
    Object holder=managerType.getMethod("getContentProviderExternal",String.class,int.class,IBinder.class,String.class).invoke(manager,"local.z6panel.bridge",0,token,"z6panel");acquired=true;
    Object provider=holder.getClass().getField("provider").get(holder);
    Bundle answer=attachProvider(provider,extras);
    if(answer==null||!answer.getBoolean("attached",false))throw new IOException("Client did not acknowledge attachment");
    final IBinder next=answer.getBinder("client_life");
    retries=0;
    if(next==null){clientLinked=false;later(30000);return;} // Old client compatibility only.
    client=next;RemoteKeys.client(next);
    next.linkToDeath(()->{if(client==next){RemoteKeys.clientDied(next);client=null;clientLinked=false;later(500);}},0);
    clientLinked=true;
   }catch(Exception e){client=null;clientLinked=false;later(Math.min(15000,500L<<Math.min(retries++,5)));android.util.Log.w("Z6Bridge","Attach pending",e);}
   finally{if(acquired)try{managerType.getMethod("removeContentProviderExternalAsUser",String.class,IBinder.class,int.class).invoke(manager,"local.z6panel.bridge",token,0);}catch(Exception ignored){}}
  }
 }
 static Bundle attachProvider(Object provider,Bundle extras)throws Exception {
  Class<?> type=Class.forName("android.content.IContentProvider");
  if(Build.VERSION.SDK_INT>=31){
   Class<?> source=Class.forName("android.content.AttributionSource");
   Class<?> builderType=Class.forName("android.content.AttributionSource$Builder");
   Object builder=builderType.getConstructor(int.class).newInstance(android.os.Process.myUid());
   builderType.getMethod("setPackageName",String.class).invoke(builder,"com.android.shell");
   Object attribution=builderType.getMethod("build").invoke(builder);
   return (Bundle)type.getMethod("call",source,String.class,String.class,String.class,Bundle.class).invoke(provider,attribution,"local.z6panel.bridge","attach",null,extras);
  }
  if(Build.VERSION.SDK_INT==30)return (Bundle)type.getMethod("call",String.class,String.class,String.class,String.class,String.class,Bundle.class).invoke(provider,"com.android.shell",null,"local.z6panel.bridge","attach",null,extras);
  return (Bundle)type.getMethod("call",String.class,String.class,String.class,String.class,Bundle.class).invoke(provider,"com.android.shell","local.z6panel.bridge","attach",null,extras);
 }
 static final ThreadPoolExecutor COMMANDS=new ThreadPoolExecutor(0,1,30,TimeUnit.SECONDS,new ArrayBlockingQueue<Runnable>(1),r->{Thread t=new Thread(r,"Z6Bridge-command");t.setDaemon(true);return t;});
 static int allowedUid;
 static volatile boolean recoveryRequired;
 public static void main(String[] args)throws Exception {
  if(android.os.Process.myUid()!=2000||args.length!=1)throw new SecurityException("shell only");
  final int allowed=Integer.parseInt(args[0]);if(allowed<10000)throw new SecurityException("app UID required");
  allowedUid=allowed;
  Looper.prepareMainLooper();
  final Object manager=Class.forName("android.app.ActivityManager").getMethod("getService").invoke(null);
  final Class<?> managerType=Class.forName("android.app.IActivityManager");
  final Binder binder=new Binder(){protected boolean onTransact(int code,Parcel data,Parcel reply,int flags)throws RemoteException{
   if(Binder.getCallingUid()!=allowed)throw new SecurityException("unauthorized UID");
   if(code!=1)return false;data.enforceInterface(DESCRIPTOR);String op=data.readString();String result;
   long identity=Binder.clearCallingIdentity();try{result=executeBounded(op);}catch(Exception e){result="ERROR: "+e.getClass().getSimpleName();}finally{Binder.restoreCallingIdentity(identity);}
   reply.writeNoException();reply.writeString(result);return true;
  }};
  Handler handler=new Handler(Looper.getMainLooper());handler.post(new Attacher(handler,manager,managerType,binder));
  // Startup policy is attempted once per helper process, never on the 500 ms reattach path.
  Thread startup=new Thread(()->{for(String op:new String[]{"VOLUME_UI_OFF","VOLUME_KEYS_SILENT"})try{String status=executeBounded(op);if(!"OK".equals(status))android.util.Log.w("Z6Bridge",op+": "+status);}catch(Exception e){android.util.Log.w("Z6Bridge",op+": "+e);}},"Z6Bridge-startup");startup.setDaemon(true);startup.start();
  Looper.loop();
 }
 static String executeBounded(final String op)throws Exception {
  // Health checks remain read-only and responsive while a command is running.
  if("VERSION".equals(op)||"PING".equals(op)||"STATS".equals(op)||RemoteKeys.isStop(op))return execute(op);
  if(recoveryRequired)return "ERROR: RECOVERY_REQUIRED; restart local control";
  Future<String> result;
  try{result=COMMANDS.submit(()->execute(op));}catch(RejectedExecutionException e){return "ERROR: BUSY; retry";}
  try{return result.get(12,TimeUnit.SECONDS);}catch(TimeoutException e){recoveryRequired=true;result.cancel(true);COMMANDS.remove((Runnable)result);return "TIMEOUT: restart local control";}catch(InterruptedException e){result.cancel(true);COMMANDS.remove((Runnable)result);Thread.currentThread().interrupt();return "ERROR: interrupted";}catch(ExecutionException e){Throwable cause=e.getCause();while(cause instanceof java.lang.reflect.InvocationTargetException&&cause.getCause()!=null)cause=cause.getCause();return "ERROR: "+cause.getClass().getSimpleName();}
 }
 static String execute(String op)throws Exception {
  if("VERSION".equals(op))return VERSION;
  if("STATS".equals(op))return new org.json.JSONObject().put("attachAttempts",attachAttempts.get()).put("clientLinked",clientLinked).put("remote",RemoteKeys.stats()).toString();
  if("PING".equals(op))return recoveryRequired?"ERROR: RECOVERY_REQUIRED; restart local control":"OK";
  if(op!=null&&op.startsWith("REMOTE:"))return RemoteKeys.command(op.substring(7));
  if(op!=null&&op.startsWith("REMOTE_LAYOUT:"))return NativeRemoteLayout.command(op.substring(14));
  if("FOREGROUND_APP".equals(op))return foregroundApp();
  if(op!=null&&op.startsWith("TV_NAV:")){String[] a=op.substring(7).split(":",-1);return a.length==2?tvKey(a[0],Integer.parseInt(a[1])):"DENIED";}
  if(op!=null&&op.startsWith("TV_FOCUS:"))return tvFocus(op.substring(9));
  if(op!=null&&op.startsWith("RESTART_APP:"))return restartApp(op.substring(12));
  if(op!=null&&op.startsWith("RESUME_APP:"))return resumeApp(op.substring(11));
  if("RECENTS_TRIM".equals(op))return trimRecentTasks();
  if("VOLUME_UI_OFF".equals(op)){
   IBinder binder=(IBinder)Class.forName("android.os.ServiceManager").getMethod("getService",String.class).invoke(null,"audio");
   Object audio=Class.forName("android.media.IAudioService$Stub").getMethod("asInterface",IBinder.class).invoke(null,binder);
   Class.forName("android.media.IAudioService").getMethod("setVolumeController",Class.forName("android.media.IVolumeController")).invoke(audio,new Object[]{null});return "OK";
  }if("VOLUME_KEYS_SILENT".equals(op)){
   String first=runCommand(false,"/system/bin/settings","put","system","sysui_volume_up_silent","1");
   if(!"OK".equals(first))return first;
   return runCommand(false,"/system/bin/settings","put","system","sysui_volume_down_silent","1");
  }if("SLEEP".equals(op)){
   String paused=runCommand(false,"/system/bin/input","keyevent","127");
   if(!"OK".equals(paused))return "ERROR: media pause: "+paused;
   return runCommand(false,"/system/bin/input","keyevent","223");
  }String[] command;
  if(op!=null&&op.startsWith("UNINSTALL:")){
   String pkg=op.substring(10);if(pkg.length()>200||!pkg.matches("[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)+"))return "DENIED";
   for(String keep:new String[]{"local.z6panel","com.xuper.netxxus","io.gh.reisxd.tizentube.fortaleza","com.esaba.downloader","com.android.vending","com.google.android.gms"})if(pkg.equals(keep))return "DENIED";
   Object pm=Class.forName("android.app.ActivityThread").getMethod("getPackageManager").invoke(null);
   android.content.pm.ApplicationInfo info=(android.content.pm.ApplicationInfo)Class.forName("android.content.pm.IPackageManager").getMethod("getApplicationInfo",String.class,int.class,int.class).invoke(pm,pkg,0,0);
   if(info==null||(info.flags&(android.content.pm.ApplicationInfo.FLAG_SYSTEM|android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP))!=0||info.uid<10000)return "DENIED";
   command=new String[]{"/system/bin/pm","uninstall","--user","0",pkg};
  }else if(op!=null&&op.startsWith("TIMEZONE:")){
   String zone=op.substring(9);if(!java.util.Arrays.asList(java.util.TimeZone.getAvailableIDs()).contains(zone))return "DENIED";
   command=new String[]{"/system/bin/cmd","alarm","set-timezone",zone};
  }else {if(op==null)return "DENIED";switch(op){case "WIFI_ON":command=new String[]{"/system/bin/svc","wifi","enable"};break;case "WIFI_OFF":command=new String[]{"/system/bin/svc","wifi","disable"};break;case "REBOOT":command=new String[]{"/system/bin/reboot"};break;default:return "DENIED";}}
  return runCommand(op.startsWith("UNINSTALL:"),command);
 }
 /** Drains output concurrently so a full pipe cannot masquerade as a successful command. */
 static String runCommand(boolean uninstall,String... command)throws Exception {
  if(Thread.currentThread().isInterrupted())return "ERROR: interrupted";
  final java.lang.Process child=new ProcessBuilder(command).redirectErrorStream(true).start();
  final ByteArrayOutputStream output=new ByteArrayOutputStream();final IOException[] failure=new IOException[1];
  Thread reader=new Thread(()->{try(InputStream in=child.getInputStream()){byte[] bytes=new byte[512];int count;while((count=in.read(bytes))!=-1){int keep=Math.min(count,4096-output.size());if(keep>0)output.write(bytes,0,keep);}}catch(IOException e){failure[0]=e;}},"Z6Bridge-output");reader.setDaemon(true);reader.start();
  try{
   if(!child.waitFor(5,TimeUnit.SECONDS))return "TIMEOUT: command";
   reader.join(500);if(reader.isAlive())return "ERROR: incomplete command output";
   if(failure[0]!=null)return "ERROR: command output unavailable";
   String detail=output.toString("UTF-8").trim();int exit=child.exitValue();
   boolean success=uninstall?"Success".equals(detail):detail.isEmpty();
   return exit==0&&success?"OK":"ERROR ("+exit+"): "+detail;
  }finally{child.destroy();if(child.isAlive())child.destroyForcibly();try{child.getInputStream().close();}catch(IOException ignored){}}
 }
 static Object taskManager()throws Exception {return Class.forName("android.app.ActivityTaskManager").getMethod("getService").invoke(null);}
 static String topPackage(Object info)throws Exception {
  if(info==null)return null;
  ComponentName top=(ComponentName)info.getClass().getField("topActivity").get(info);
  return top!=null&&validPackage(top.getPackageName())?top.getPackageName():null;
 }
 /** Read-only: never infer foreground from baseIntent, process order or old recents. */
 static String foregroundApp(){
  Object manager=null;Class<?> type=null;
  try{manager=taskManager();type=Class.forName("android.app.IActivityTaskManager");String pkg=topPackage(type.getMethod(Build.VERSION.SDK_INT>=30?"getFocusedRootTaskInfo":"getFocusedStackInfo").invoke(manager));if(pkg!=null)return pkg;}catch(Exception ignored){}
  try{if(manager!=null&&type!=null&&Build.VERSION.SDK_INT>=30){List<?> tasks=(List<?>)type.getMethod("getTasks",int.class,boolean.class,boolean.class).invoke(manager,1,false,false);if(tasks!=null&&!tasks.isEmpty()){String pkg=topPackage(tasks.get(0));if(pkg!=null)return pkg;}}}catch(Exception ignored){}
  try{if(manager!=null&&type!=null){List<?> tasks=(List<?>)type.getMethod("getTasks",int.class).invoke(manager,1);if(tasks!=null&&!tasks.isEmpty()){String pkg=topPackage(tasks.get(0));if(pkg!=null)return pkg;}}}catch(Exception ignored){}
  // Some vendor builds retain the pre-Q task query on IActivityManager.
  try{Object am=Class.forName("android.app.ActivityManager").getMethod("getService").invoke(null);List<?> tasks=(List<?>)Class.forName("android.app.IActivityManager").getMethod("getTasks",int.class).invoke(am,1);if(tasks!=null&&!tasks.isEmpty()){String pkg=topPackage(tasks.get(0));if(pkg!=null)return pkg;}}catch(Exception ignored){}
  return "ERROR: FOREGROUND_UNAVAILABLE; retry";
 }
 static boolean validPackage(String pkg){return pkg!=null&&pkg.length()<=200&&pkg.matches("[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)+");}
 /** Bring an existing task forward, without force-stop or sending a launch intent. */
 static String resumeApp(String pkg)throws Exception {
  if(!validPackage(pkg)||pkg.equals("local.z6panel"))return "DENIED";
  Object manager=taskManager();Class<?> type=Class.forName("android.app.IActivityTaskManager");
  List<?> tasks=Build.VERSION.SDK_INT>=30?(List<?>)type.getMethod("getTasks",int.class,boolean.class,boolean.class).invoke(manager,50,false,false):(List<?>)type.getMethod("getTasks",int.class).invoke(manager,50);
  for(Object task:tasks){if(!pkg.equals(topPackage(task)))continue;int id=task.getClass().getField(Build.VERSION.SDK_INT>=30?"taskId":"id").getInt(task);
   type.getMethod("moveTaskToFront",Class.forName("android.app.IApplicationThread"),String.class,int.class,int.class,Bundle.class).invoke(manager,null,"com.android.shell",id,0,null);return "OK";
  }return "ERROR: TASK_NOT_FOUND";
 }
 static Object packageManager()throws Exception {return Class.forName("android.app.ActivityThread").getMethod("getPackageManager").invoke(null);}
 static ApplicationInfo applicationInfo(Object pm,String pkg)throws Exception {return (ApplicationInfo)Class.forName("android.content.pm.IPackageManager").getMethod("getApplicationInfo",String.class,int.class,int.class).invoke(pm,pkg,0,0);}
 static List<?> listSlice(Object value)throws Exception {if(value instanceof List)return (List<?>)value;return value==null?Collections.emptyList():(List<?>)value.getClass().getMethod("getList").invoke(value);}
 /** Contract: OK means force-stop completed. The app MUST launch its explicit MAIN
  * component afterwards (LAUNCHER/LEANBACK_LAUNCHER). No monkey or helper-side launch. */
 static boolean safeTarget(String pkg)throws Exception {
  if(!validPackage(pkg)||pkg.equals("local.z6panel")||pkg.startsWith("com.android.")||pkg.startsWith("com.google.android.")||pkg.startsWith("android."))return false;
  Object pm=packageManager();ApplicationInfo app=applicationInfo(pm,pkg);
  if(app==null||!app.enabled||app.uid<10000||app.uid==allowedUid||(app.flags&ApplicationInfo.FLAG_INSTALLED)==0||(app.flags&(ApplicationInfo.FLAG_SYSTEM|ApplicationInfo.FLAG_UPDATED_SYSTEM_APP|ApplicationInfo.FLAG_PERSISTENT|ApplicationInfo.FLAG_SUSPENDED))!=0)return false;
  Class<?> type=Class.forName("android.content.pm.IPackageManager");
  // Deny shared-UID packages: stopping one could also kill critical companions.
  String[] peers=(String[])type.getMethod("getPackagesForUid",int.class).invoke(pm,app.uid);
  if(peers==null||peers.length!=1||!pkg.equals(peers[0]))return false;
  // HOME-capable apps are critical even when installed by the user.
  Object homes=type.getMethod("queryIntentActivities",Intent.class,String.class,int.class,int.class).invoke(pm,new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).setPackage(pkg),null,0,0);
  if(!listSlice(homes).isEmpty())return false;
  boolean launchable=false;
  for(String category:new String[]{Intent.CATEGORY_LEANBACK_LAUNCHER,Intent.CATEGORY_LAUNCHER}){
   Intent intent=new Intent(Intent.ACTION_MAIN).addCategory(category).setPackage(pkg);
   for(Object item:listSlice(type.getMethod("queryIntentActivities",Intent.class,String.class,int.class,int.class).invoke(pm,intent,null,0,0))){
    ActivityInfo activity=((ResolveInfo)item).activityInfo;
    if(activity!=null&&pkg.equals(activity.packageName)&&activity.enabled&&activity.exported&&activity.permission==null){launchable=true;break;}
   }
  }
  return launchable;
 }
 static String restartApp(String pkg)throws Exception {
  return safeTarget(pkg)?runCommand(false,"/system/bin/am","force-stop","--user","0",pkg):"DENIED";
 }
 static String tvKey(String pkg,int key)throws Exception {
  if(key!=19&&key!=20&&key!=23||!safeTarget(pkg)||!pkg.equals(foregroundApp()))return "DENIED";
  Class<?> type=Class.forName("android.hardware.input.InputManager");Object input=type.getMethod("getInstance").invoke(null);
  java.lang.reflect.Method inject=type.getMethod("injectInputEvent",Class.forName("android.view.InputEvent"),int.class);
  long now=SystemClock.uptimeMillis();
  android.view.KeyEvent down=new android.view.KeyEvent(now,now,0,key,0,0,-1,0,android.view.KeyEvent.FLAG_FROM_SYSTEM,android.view.InputDevice.SOURCE_DPAD);
  android.view.KeyEvent up=android.view.KeyEvent.changeAction(down,1);
  boolean sent=Boolean.TRUE.equals(inject.invoke(input,down,2));
  boolean released=Boolean.TRUE.equals(inject.invoke(input,up,2));
  return sent&&released?"OK":"ERROR: TV key unavailable";
 }
 private static Object focusSession;
 static String tvFocus(String pkg)throws Exception {
  if(!safeTarget(pkg)||!pkg.equals(foregroundApp()))return "DENIED";
  Class<?> sessionType=Class.forName("android.view.IWindowSession");
  if(focusSession==null){
   IBinder service=(IBinder)Class.forName("android.os.ServiceManager").getMethod("getService",String.class).invoke(null,"window");
   Object manager=Class.forName("android.view.IWindowManager$Stub").getMethod("asInterface",IBinder.class).invoke(null,service);
   Class<?> callback=Class.forName("android.view.IWindowSessionCallback");final Binder life=new Binder();
   Object client=java.lang.reflect.Proxy.newProxyInstance(callback.getClassLoader(),new Class[]{callback},(proxy,method,args)->method.getName().equals("asBinder")?life:null);
   focusSession=Class.forName("android.view.IWindowManager").getMethod("openSession",callback).invoke(manager,client);
  }
  if(!Boolean.TRUE.equals(sessionType.getMethod("getInTouchMode").invoke(focusSession)))return "OK";
  if(!pkg.equals(foregroundApp()))return "DENIED";
  sessionType.getMethod("setInTouchMode",boolean.class).invoke(focusSession,false);
  return Boolean.FALSE.equals(sessionType.getMethod("getInTouchMode").invoke(focusSession))?"OK":"ERROR: remote focus unavailable";
 }

 static String trimRecentTasks()throws Exception {
  Object manager=taskManager();
  Class<?> type=Class.forName("android.app.IActivityTaskManager");
  List<?> tasks=listSlice(type.getMethod("getRecentTasks",int.class,int.class,int.class).invoke(manager,30,3,0));
  Set<String> keep=new HashSet<>(Arrays.asList("local.z6panel","com.xuper.netxxus","com.android.systemui","com.google.android.gms","com.android.settings"));
  int userApps=0,removed=0;
  for(Object task:tasks){
   android.content.Intent base=(android.content.Intent)task.getClass().getField("baseIntent").get(task);
   android.content.ComponentName component=base==null?null:base.getComponent();
   String pkg=component==null?null:component.getPackageName();
   int id=task.getClass().getField("id").getInt(task);
   if(pkg==null||keep.contains(pkg))continue;
   ApplicationInfo app=applicationInfo(packageManager(),pkg);
   if(app==null||app.uid<10000||app.uid==allowedUid||(app.flags&(ApplicationInfo.FLAG_SYSTEM|ApplicationInfo.FLAG_UPDATED_SYSTEM_APP|ApplicationInfo.FLAG_PERSISTENT))!=0)continue;
   if(Thread.currentThread().isInterrupted())return "ERROR: interrupted";
   if(userApps++>=2){if(!(Boolean)type.getMethod("removeTask",int.class).invoke(manager,id))return "ERROR: task removal failed after "+removed;removed++;}
  }
  return "OK";
 }
}
