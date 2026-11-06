package local.z6panel;

import android.content.Context;
import android.os.SystemClock;
import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;

/** Temporary LAN receiver. No exported service, background listener, or arbitrary file paths. */
final class PhotoReceiver implements AutoCloseable {
 interface Listener {void message(String text);default void received(String id,String name){message("Foto recibida: "+name);}}
 private final WallpaperStore store;private final Listener listener;private final ServerSocket server;private final String token;
 private final long deadline=SystemClock.elapsedRealtime()+15*60000L;private volatile boolean closed;private volatile Socket client;private Thread thread;
 PhotoReceiver(Context context,Listener l)throws IOException {store=new WallpaperStore(context);listener=l;byte[] b=new byte[16];new SecureRandom().nextBytes(b);StringBuilder s=new StringBuilder();for(byte v:b)s.append(String.format(Locale.ROOT,"%02x",v&255));token=s.toString();server=new ServerSocket();server.setReuseAddress(true);server.bind(new InetSocketAddress(0),1);server.setSoTimeout(1000);}
 int port(){return server.getLocalPort();}String path(){return "/"+token+"/";}boolean running(){return !closed;}
 void start(){thread=new Thread(()->{try{while(!closed&&SystemClock.elapsedRealtime()<deadline){try{Socket socket=server.accept();client=socket;socket.setSoTimeout(30000);try{serve(socket);}catch(Exception ignored){}finally{try{socket.close();}catch(IOException ignored){}client=null;}}catch(SocketTimeoutException idle){}}}catch(IOException ignored){}finally{close();listener.message("Recepción cerrada. Vuelve a abrirla para obtener otro QR.");}},"Z6-photo-receiver");thread.start();}
 public void close(){closed=true;try{server.close();}catch(IOException ignored){}Socket c=client;if(c!=null)try{c.close();}catch(IOException ignored){}if(thread!=null)thread.interrupt();}
 private String line(InputStream input)throws IOException {ByteArrayOutputStream out=new ByteArrayOutputStream();for(int ch;(ch=input.read())!=-1;){if(ch=='\n')return out.toString("US-ASCII").replace("\r","");if(out.size()>8192)throw new IOException("Header too large");out.write(ch);}throw new EOFException();}
 private void serve(Socket socket)throws Exception {
  InetAddress remote=socket.getInetAddress();if(!remote.isSiteLocalAddress()&&!remote.isLoopbackAddress()&&!remote.isLinkLocalAddress()){reply(socket,403,"text/plain","Solo se permiten conexiones locales");return;}
  InputStream in=new BufferedInputStream(socket.getInputStream());String[] request=line(in).split(" ");if(request.length!=3)return;Map<String,String> headers=new HashMap<>();int total=0;for(String h;!(h=line(in)).isEmpty();){total+=h.length();if(total>16384)throw new IOException("Headers too large");int colon=h.indexOf(':');if(colon>0)headers.put(h.substring(0,colon).toLowerCase(Locale.ROOT),h.substring(colon+1).trim());}
  String host=headers.get("host");if(host==null||!host.matches("(localhost|[0-9.]+)(:[0-9]+)?")){reply(socket,403,"text/plain","Dirección no permitida");return;}
  String origin=headers.get("origin");if(origin!=null&&!origin.equals("http://"+host)){reply(socket,403,"text/plain","Origen no permitido");return;}
  if(!request[1].startsWith(path())){reply(socket,403,"text/plain","Sesión no válida o caducada");return;}
  if(request[0].equals("GET")&&request[1].equals(path())){try(InputStream page=store.context.getResources().openRawResource(R.raw.photo_upload)){reply(socket,200,"text/html; charset=utf-8",new String(WallpaperStore.readLimited(page,65536),StandardCharsets.UTF_8));}return;}
  if(!request[0].equals("POST")||!request[1].startsWith(path()+"upload?name=")){reply(socket,404,"text/plain","No disponible");return;}
  File temp=null;WallpaperStore.Photo photo=null;
  try{String value=headers.get("content-length");long length=value==null?-1:Long.parseLong(value);if(length<=0||length>WallpaperStore.MAX_INPUT){reply(socket,413,"application/json",new JSONObject().put("error","Máximo 20 MB por imagen").toString());return;}
   String name=URLDecoder.decode(request[1].substring((path()+"upload?name=").length()),"UTF-8");if(name.length()>200)name=name.substring(0,200);listener.message("Recibiendo foto…");temp=store.receive(in,length);if(closed||SystemClock.elapsedRealtime()>=deadline)throw new IOException("Sesión caducada");photo=store.importImage(temp,name);
   if(closed||SystemClock.elapsedRealtime()>=deadline){store.remove(photo.id);photo=null;throw new IOException("Sesión cerrada; vuelve a enviar");}
   listener.received(photo.id,photo.name);reply(socket,200,"application/json",new JSONObject().put("ok",true).put("name",photo.name).toString());
  }catch(Exception e){String error=e.getMessage()==null?"No se pudo importar; usa JPG o PNG":e.getMessage();if(photo==null)listener.message("No se recibió la foto: "+error);reply(socket,400,"application/json",new JSONObject().put("error",error).toString());}finally{if(temp!=null)temp.delete();}
 }
 private void reply(Socket socket,int status,String type,String body)throws IOException {byte[] bytes=body.getBytes(StandardCharsets.UTF_8);OutputStream out=socket.getOutputStream();String reason=status==200?"OK":"Error";out.write(("HTTP/1.1 "+status+" "+reason+"\r\nContent-Type: "+type+"\r\nContent-Length: "+bytes.length+"\r\nConnection: close\r\nCache-Control: no-store\r\nReferrer-Policy: no-referrer\r\nX-Content-Type-Options: nosniff\r\nContent-Security-Policy: default-src 'none'; style-src 'unsafe-inline'; script-src 'nonce-Z6Upload'; connect-src 'self'; img-src 'self'\r\n\r\n").getBytes(StandardCharsets.US_ASCII));out.write(bytes);out.flush();}
}
