package local.z6bridge;

import java.util.*;
import java.util.regex.*;
import java.nio.*;

/** Metadata only. Never returns character, navigation, power or volume events. */
public final class RemoteKeyFormat {
 private RemoteKeyFormat(){}
 public static boolean allowed(int code){return code>=59&&code<=68||code==87||code==88||code>=183&&code<=194||code>=398&&code<=401;}
 public static final class Device {
  public String path="",name="",bus="",vendor="",product="";
  public String identity(){return bus+"|"+vendor+"|"+product+"|"+name;}
 }
 public static final class Event {
  public final int code,value;
  Event(int code,int value){this.code=code;this.value=value;}
 }
 private static final Pattern HEADER=Pattern.compile("^add device [0-9]+: (/dev/input/event[0-9]+)$"),FIELD=Pattern.compile("^(bus:|vendor|product)\\s+([0-9a-fA-F]{4})$");
 public static Event binary(ByteBuffer bytes,int offset,int size){if(size!=16&&size!=24||offset<0||offset>bytes.limit()-size)return null;int at=offset+size-8,type=bytes.getShort(at)&65535,code=bytes.getShort(at+2)&65535,value=bytes.getInt(at+4);return type==1&&allowed(code)&&(value==0||value==1||value==2)?new Event(code,value):null;}
 public static List<Device> devices(String info){
  ArrayList<Device> devices=new ArrayList<>();Device current=null;
  for(String raw:info.split("\\r?\\n")){
   String line=raw.trim();Matcher header=HEADER.matcher(line);
   if(header.matches()){current=new Device();current.path=header.group(1);devices.add(current);continue;}if(current==null)continue;
   Matcher field=FIELD.matcher(line);
   if(field.matches()){String v=field.group(2).toLowerCase(Locale.ROOT);if(field.group(1).equals("bus:"))current.bus=v;else if(field.group(1).equals("vendor"))current.vendor=v;else current.product=v;}
   if(line.startsWith("name:")&&line.contains("\"")){int first=line.indexOf('"'),last=line.lastIndexOf('"');if(last>first&&last-first<=161)current.name=line.substring(first+1,last);}
  }
  devices.removeIf(d->d.name.isEmpty());return devices;
 }
}
