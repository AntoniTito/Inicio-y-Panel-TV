package local.z6bridge;

import android.os.*;
import android.system.*;
import android.util.Base64;
import org.json.*;
import java.io.*;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** Reads selected function-key metadata. Blocking poll is woken explicitly on cancellation. */
final class RemoteKeys {
 private static volatile IBinder client;
 private static volatile Watcher current;
 private static volatile String mode="stop",binding="";
 private static final Handler ui=new Handler(Looper.getMainLooper());
 private static Runnable expiry;
 static boolean isStop(String operation){if(operation==null||!operation.startsWith("REMOTE:")||operation.length()>4103)return false;try{return new JSONObject(new String(Base64.decode(operation.substring(7),Base64.DEFAULT),StandardCharsets.UTF_8)).optString("mode").equals("stop");}catch(Exception e){return false;}}
 static void client(IBinder binder){client=binder;}
 static synchronized void clientDied(IBinder binder){if(client==binder){client=null;stop();}}
 static JSONObject stats()throws JSONException{Watcher watcher=current;return new JSONObject().put("mode",mode).put("watcherAlive",watcher!=null&&!watcher.closed).put("devices",watcher==null?0:watcher.devices.size());}
 static synchronized String command(String encoded)throws Exception{
  if(encoded==null||encoded.length()>4096)return "DENIED";
  JSONObject request=new JSONObject(new String(Base64.decode(encoded,Base64.DEFAULT),StandardCharsets.UTF_8));String next=request.getString("mode");
  if(next.equals("stop")){stop();return "OK";}if(!next.equals("learn")&&!next.equals("bind"))return "DENIED";
  String token=request.optString("token"),device=request.optString("device");int code=request.optInt("code");
  if(!token.matches("[A-Za-z0-9-]{1,64}")||device.length()>256||next.equals("bind")&&(!RemoteKeyFormat.allowed(code)||device.isEmpty()))return "DENIED";
  String desired=next+":"+token+":"+device+":"+code;if(!request.optBoolean("refresh")&&desired.equals(binding)&&current!=null&&!current.closed)return "OK";
  stop();List<RemoteKeyFormat.Device> available=RemoteKeyFormat.devices(deviceInfo()),devices=new ArrayList<>();
  for(RemoteKeyFormat.Device d:available)if(next.equals("learn")||device.equals(d.identity()))devices.add(d);
  if(next.equals("bind")&&devices.size()>1)return "AMBIGUOUS_DEVICE";if(devices.isEmpty())return "NO_DEVICE";
  if(devices.size()>16)return "TOO_MANY_DEVICES";
  Watcher watcher=new Watcher(next,token,code,devices);if(watcher.devices.isEmpty()){watcher.close();return "INPUT_UNAVAILABLE";}
  current=watcher;mode=next;binding=desired;watcher.start();
  if(next.equals("learn")){expiry=()->{synchronized(RemoteKeys.class){if(current==watcher){send("expired",token,"",0);stop();}}};ui.postDelayed(expiry,65000);}
  return "OK";
 }
 private static String deviceInfo()throws Exception{
  final java.lang.Process process=new ProcessBuilder("/system/bin/getevent","-i","-p").redirectErrorStream(true).start();final ByteArrayOutputStream bytes=new ByteArrayOutputStream();final boolean[] overflow={false};
  Thread reader=new Thread(()->{try(InputStream in=process.getInputStream()){byte[] b=new byte[2048];int n;while((n=in.read(b))!=-1){if(bytes.size()+n>131072)overflow[0]=true;else bytes.write(b,0,n);}}catch(IOException ignored){}},"Z6Remote-info");reader.setDaemon(true);reader.start();
  try{if(!process.waitFor(4,TimeUnit.SECONDS))throw new IOException("Input metadata timeout");reader.join(500);if(reader.isAlive()||overflow[0]||process.exitValue()!=0)throw new IOException("Input unavailable");return bytes.toString("UTF-8");}finally{process.destroy();if(process.isAlive())process.destroyForcibly();}
 }
 private static final class Watcher extends Thread {
  final String kind,token;final int wanted;final ArrayList<RemoteKeyFormat.Device> devices=new ArrayList<>();
  final ArrayList<FileDescriptor> inputs=new ArrayList<>();final FileDescriptor[] wake;
  volatile boolean closed;
  Watcher(String kind,String token,int wanted,List<RemoteKeyFormat.Device> candidates)throws Exception{
   super("Z6Remote-key");setDaemon(true);this.kind=kind;this.token=token;this.wanted=wanted;wake=Os.pipe();
   for(RemoteKeyFormat.Device device:candidates)try{inputs.add(Os.open(device.path,OsConstants.O_RDONLY|OsConstants.O_NONBLOCK|OsConstants.O_CLOEXEC,0));devices.add(device);}catch(ErrnoException ignored){}
  }
  void close(){closed=true;try{Os.write(wake[1],new byte[]{1},0,1);}catch(Exception ignored){}if(getState()==State.NEW)release();}
  private void release(){for(FileDescriptor fd:inputs)try{Os.close(fd);}catch(Exception ignored){}for(FileDescriptor fd:wake)try{Os.close(fd);}catch(Exception ignored){}}
  public void run(){
   StructPollfd[] polls=new StructPollfd[inputs.size()+1];for(int i=0;i<polls.length;i++){polls[i]=new StructPollfd();polls[i].fd=i<inputs.size()?inputs.get(i):wake[0];polls[i].events=(short)OsConstants.POLLIN;}
   int size=android.os.Process.is64Bit()?24:16;byte[] buffer=new byte[size*32];ByteBuffer bytes=ByteBuffer.wrap(buffer).order(ByteOrder.nativeOrder());
   try{while(!closed){
    try{Os.poll(polls,-1);}catch(ErrnoException e){if(e.errno==OsConstants.EINTR)continue;throw e;}
    if(closed||polls[inputs.size()].revents!=0)break;
    for(int i=0;i<inputs.size()&&!closed;i++){
     if((polls[i].revents&(OsConstants.POLLHUP|OsConstants.POLLERR|OsConstants.POLLNVAL))!=0)throw new IOException("Remote disconnected");
     if((polls[i].revents&OsConstants.POLLIN)==0)continue;int n;
     try{n=Os.read(inputs.get(i),buffer,0,buffer.length);}catch(ErrnoException e){if(e.errno==OsConstants.EAGAIN)continue;throw e;}
     if(n<=0||n%size!=0)throw new IOException("Invalid input record");bytes.limit(n);
     for(int at=0;at<n;at+=size){RemoteKeyFormat.Event event=RemoteKeyFormat.binary(bytes,at,size);if(event==null||event.value==0||kind.equals("bind")&&(event.value!=1||event.code!=wanted))continue;
      synchronized(RemoteKeys.class){if(current!=this||closed)return;send(kind.equals("learn")?"learn":"key",token,devices.get(i).identity(),event.code);if(kind.equals("learn")){stop();return;}}
     }
    }
   }}catch(Exception e){android.util.Log.w("Z6Remote","Input watcher unavailable: "+e.getClass().getSimpleName());}
   finally{closed=true;release();synchronized(RemoteKeys.class){if(current==this){send("unavailable",token,"",0);current=null;mode="stop";binding="";}}}
  }
 }
 private static void send(String type,String token,String device,int code){
  IBinder target=client;if(target==null||!target.isBinderAlive())return;Parcel data=Parcel.obtain();
  try{data.writeInterfaceToken("local.z6panel.remote.v1");data.writeString(new JSONObject().put("type",type).put("token",token).put("device",device).put("code",code).toString());if(!target.transact(2,data,null,IBinder.FLAG_ONEWAY))stop();}catch(Exception ignored){stop();}finally{data.recycle();}
 }
 private static synchronized void stop(){if(expiry!=null)ui.removeCallbacks(expiry);expiry=null;mode="stop";binding="";Watcher watcher=current;current=null;if(watcher!=null)watcher.close();}
}
