package local.z6panel;
import android.os.*;
final class SystemBridge {
 static volatile IBinder remote;
 private static final java.util.concurrent.ThreadPoolExecutor READS=new java.util.concurrent.ThreadPoolExecutor(0,2,20,java.util.concurrent.TimeUnit.SECONDS,new java.util.concurrent.ArrayBlockingQueue<Runnable>(1),r->{Thread t=new Thread(r,"Z6Bridge-read");t.setDaemon(true);return t;});
 static String read(String operation,long timeout){
  if(!"PING".equals(operation)&&!"VERSION".equals(operation)&&!"FOREGROUND_APP".equals(operation)&&!"STATS".equals(operation))return "DENIED";
  java.util.concurrent.Future<String> result;
  try{result=READS.submit(()->Thread.currentThread().isInterrupted()?"TIMEOUT":call(operation));}catch(java.util.concurrent.RejectedExecutionException e){return "TIMEOUT: control ocupado";}
  try{return result.get(timeout,java.util.concurrent.TimeUnit.MILLISECONDS);}catch(Exception e){return "TIMEOUT: control sin respuesta";}finally{result.cancel(true);}
 }
 static String call(String operation){IBinder bridge=remote;if(bridge==null||!bridge.isBinderAlive())return "Servicio de sistema desconectado";
  Parcel data=Parcel.obtain(),reply=Parcel.obtain();try{data.writeInterfaceToken("local.z6panel.system.v1");data.writeString(operation);if(!bridge.transact(1,data,reply,0))return "Operación no disponible";reply.readException();return reply.readString();}catch(Exception e){return "Servicio de sistema desconectado";}finally{data.recycle();reply.recycle();}
 }
}
