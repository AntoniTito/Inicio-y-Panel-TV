package local.z6panel;
import android.content.Context;import android.os.Process;import android.security.keystore.*;import android.util.Base64;
import java.io.*;import java.net.*;import java.nio.*;import java.nio.charset.StandardCharsets;import java.math.BigInteger;import java.security.*;import java.security.interfaces.RSAPublicKey;
import java.util.concurrent.*;
/** Loopback-only ADB bootstrap, fixed command, private key stays in Android Keystore. */
final class LocalAdb {
 private static final String ALIAS="z6panel.local.adb.v1";
 private static final String HELPER_VERSION="0.19.0-tv3";
 private static final ThreadPoolExecutor BRIDGE_READS=new ThreadPoolExecutor(0,1,30,TimeUnit.SECONDS,new ArrayBlockingQueue<Runnable>(1),r->{Thread t=new Thread(r,"Z6LocalAdb-version");t.setDaemon(true);return t;});
 private static final int CNXN=0x4e584e43,AUTH=0x48545541,OPEN=0x4e45504f,OKAY=0x59414b4f,WRTE=0x45545257,CLSE=0x45534c43;
 static final class Packet {int command,a,b;byte[] data;}
 static synchronized String start(Context context,boolean authorize)throws Exception {
  if(HELPER_VERSION.equals(bridgeRead("VERSION"))&&"OK".equals(bridgeRead("PING")))return "OK";
  if(android.os.Build.VERSION.SDK_INT<30)DebuggingRecovery.restorePending(context);
  // Restarting adbd can kill the privileged helper and the shell intended to
  // restore it. Availability events schedule a bounded retry instead.
  return startOnce(context,authorize);
 }
 private static String startOnce(Context context,boolean authorize)throws Exception {
  KeyStore store=KeyStore.getInstance("AndroidKeyStore");store.load(null);
  if(!store.containsAlias(ALIAS)){if(!authorize)return "Autoriza primero el inicio autónomo";KeyPairGenerator gen=KeyPairGenerator.getInstance("RSA","AndroidKeyStore");gen.initialize(new KeyGenParameterSpec.Builder(ALIAS,KeyProperties.PURPOSE_SIGN).setKeySize(2048).setDigests(KeyProperties.DIGEST_NONE).setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1).build());gen.generateKeyPair();store.load(null);}
  PrivateKey key=(PrivateKey)store.getKey(ALIAS,null);RSAPublicKey pub=(RSAPublicKey)store.getCertificate(ALIAS).getPublicKey();
  if(authorize){byte[] digest=MessageDigest.getInstance("MD5").digest(publicBytes(pub));StringBuilder fingerprint=new StringBuilder();for(byte b:digest){if(fingerprint.length()>0)fingerprint.append(":");fingerprint.append(String.format(java.util.Locale.ROOT,"%02X",b&255));}android.util.Log.i("Z6LocalAdb","Public key fingerprint "+fingerprint);}
  String apk=context.getApplicationInfo().sourceDir;if(!apk.matches("/[A-Za-z0-9_./=+~-]+"))throw new SecurityException("APK path");
  if(!HELPER_VERSION.equals(bridgeRead("VERSION")))TvRemoteKeys.stopAndWait();
  String replace=HELPER_VERSION.equals(bridgeRead("VERSION"))&&"OK".equals(bridgeRead("PING"))?"":"for p in $(pidof z6panel_bridge); do kill $p; done; sleep 1; ";
  String command=replace+"if ! pidof z6panel_bridge >/dev/null; then CLASSPATH='"+apk+"' nohup app_process /system/bin --nice-name=z6panel_bridge local.z6bridge.BridgeMain "+Process.myUid()+" >/data/local/tmp/z6-bridge.log 2>&1 </dev/null & fi; for n in 1 2 3 4 5; do pidof z6panel_bridge && break; sleep 1; done";
  try(Socket socket=new Socket()){
   // A peer sending partial packets cannot extend recovery indefinitely.
   java.util.Timer deadline=new java.util.Timer("Z6LocalAdb-deadline",true);
   deadline.schedule(new java.util.TimerTask(){public void run(){try{socket.close();}catch(IOException ignored){}}},authorize?75000:25000);
   try{socket.connect(new InetSocketAddress("127.0.0.1",5555),2000);socket.setSoTimeout(authorize?60000:8000);InputStream in=socket.getInputStream();OutputStream out=socket.getOutputStream();send(out,CNXN,0x01000000,4096,"host::z6panel\0".getBytes(StandardCharsets.UTF_8));
   boolean signed=false,offered=false;for(int attempt=0;attempt<4;attempt++){Packet p=read(in);if(p.command==CNXN){if(p.a<0x01000000)throw new IOException("ADB version");break;}if(p.command!=AUTH||p.a!=1||p.data.length!=20)throw new IOException("ADB authentication");if(!signed){Signature signature=Signature.getInstance("NONEwithRSA");signature.initSign(key);signature.update(new byte[]{0x30,0x21,0x30,0x09,0x06,0x05,0x2b,0x0e,0x03,0x02,0x1a,0x05,0x00,0x04,0x14});signature.update(p.data);byte[] signedToken=signature.sign();Signature verify=Signature.getInstance("NONEwithRSA");verify.initVerify(pub);verify.update(new byte[]{0x30,0x21,0x30,0x09,0x06,0x05,0x2b,0x0e,0x03,0x02,0x1a,0x05,0x00,0x04,0x14});verify.update(p.data);if(!verify.verify(signedToken))throw new GeneralSecurityException("ADB signature self-check failed");android.util.Log.i("Z6LocalAdb","RSA signature self-check OK");send(out,AUTH,2,0,signedToken);signed=true;}else if(authorize&&!offered){send(out,AUTH,3,0,(Base64.encodeToString(publicBytes(pub),Base64.NO_WRAP)+" Z6Panel@localhost\0").getBytes(StandardCharsets.UTF_8));offered=true;}else return "Falta autorizar la clave local en la Z6";if(attempt==3)throw new IOException("Authentication incomplete");}
   socket.setSoTimeout(10000);send(out,OPEN,1,0,("exec:"+command+"\0").getBytes(StandardCharsets.UTF_8));int remote=0,total=0;ByteArrayOutputStream response=new ByteArrayOutputStream();for(int i=0;i<100;i++){Packet p=read(in);if(p.b!=1)throw new IOException("ADB stream");if(p.command==OKAY&&p.a!=0&&remote==0){remote=p.a;}else if(p.command==WRTE&&remote!=0&&p.a==remote){total+=p.data.length;response.write(p.data);if(total>16384)throw new IOException("ADB output limit");send(out,OKAY,1,remote,new byte[0]);}else if(p.command==CLSE){send(out,CLSE,1,p.a,new byte[0]);String result=response.toString("UTF-8").trim();android.util.Log.i("Z6LocalAdb","Bootstrap: "+result);if(remote==0||p.a!=remote||!result.matches("[0-9]+(\\s+[0-9]+)*"))return "No se inició el servicio local; reintenta";return "STARTED";}else throw new IOException("ADB response");}throw new IOException("ADB message limit");
   }finally{deadline.cancel();}
  }catch(SocketTimeoutException e){return "Conexión o autorización pendiente; reintenta";}catch(ConnectException e){return "ADB local no disponible; reintenta la recuperación";}catch(IOException e){android.util.Log.w("Z6LocalAdb","Bootstrap: "+e);return "Inicio local interrumpido; reintenta";}
 }
 private static String bridgeRead(final String operation){
  Future<String> result;
  try{result=BRIDGE_READS.submit(()->SystemBridge.call(operation));}catch(RejectedExecutionException e){return "";}
  try{return result.get(1500,TimeUnit.MILLISECONDS);}catch(InterruptedException e){result.cancel(true);BRIDGE_READS.remove((Runnable)result);Thread.currentThread().interrupt();return "";}catch(Exception e){result.cancel(true);BRIDGE_READS.remove((Runnable)result);return "";}
 }
 private static String awaitBridge()throws InterruptedException {
  long until=android.os.SystemClock.elapsedRealtime()+6000;
  do{if(HELPER_VERSION.equals(bridgeRead("VERSION"))&&"OK".equals(bridgeRead("PING")))return "OK";Thread.sleep(200);}while(android.os.SystemClock.elapsedRealtime()<until);
  return "Servicio local pendiente o versión distinta de "+HELPER_VERSION+"; reintenta";
 }
 static byte[] publicBytes(RSAPublicKey pub){BigInteger n=pub.getModulus(),word=BigInteger.ONE.shiftLeft(32);ByteBuffer b=ByteBuffer.allocate(524).order(ByteOrder.LITTLE_ENDIAN);b.putInt(64);b.putInt(n.modInverse(word).negate().mod(word).intValue());little(b,n,256);little(b,BigInteger.ONE.shiftLeft(4096).mod(n),256);b.putInt(pub.getPublicExponent().intValue());return b.array();}
 private static void little(ByteBuffer b,BigInteger n,int count){for(int i=0;i<count;i++)b.put(n.shiftRight(i*8).byteValue());}
 private static void send(OutputStream out,int command,int a,int b,byte[] data)throws IOException{int sum=0;for(byte value:data)sum+=value&255;ByteBuffer h=ByteBuffer.allocate(24).order(ByteOrder.LITTLE_ENDIAN);h.putInt(command).putInt(a).putInt(b).putInt(data.length).putInt(sum).putInt(command^-1);out.write(h.array());out.write(data);out.flush();}
 private static Packet read(InputStream in)throws IOException{DataInputStream input=new DataInputStream(in);byte[] header=new byte[24];input.readFully(header);ByteBuffer b=ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN);Packet p=new Packet();p.command=b.getInt();p.a=b.getInt();p.b=b.getInt();int size=b.getInt(),checksum=b.getInt(),magic=b.getInt();if(magic!=(p.command^-1)||size<0||size>4096)throw new IOException("ADB header");p.data=new byte[size];input.readFully(p.data);int sum=0;for(byte value:p.data)sum+=value&255;if(sum!=checksum)throw new IOException("ADB checksum");return p;}
}
