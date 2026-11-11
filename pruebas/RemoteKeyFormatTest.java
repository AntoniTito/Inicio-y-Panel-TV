import local.z6bridge.RemoteKeyFormat;
import java.nio.*;
import java.util.*;

public final class RemoteKeyFormatTest {
 private static int count;
 private static void check(boolean ok,String label){if(!ok)throw new AssertionError(label);count++;}
 public static void main(String[] args){
  for(int key:new int[]{59,68,87,88,183,194,398,401})check(RemoteKeyFormat.allowed(key),"function key allowed");
  for(int key:new int[]{0,1,14,28,30,102,103,105,106,108,113,114,115,116,142,158,172,402})check(!RemoteKeyFormat.allowed(key),"character and essential keys excluded");
  RemoteKeyFormat.Event event;ByteBuffer sample=ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN);sample.putShort(8,(short)1).putShort(10,(short)87).putInt(12,1);
  check(RemoteKeyFormat.binary(sample,0,16).code==87,"F11 binary event parsed");sample.putInt(12,2);check(RemoteKeyFormat.binary(sample,0,16).value==2,"held function key can be learned");sample.putShort(10,(short)30);check(RemoteKeyFormat.binary(sample,0,16)==null,"typed character not emitted");sample.putShort(8,(short)3).putShort(10,(short)87);check(RemoteKeyFormat.binary(sample,0,16)==null,"motion excluded");sample.putShort(8,(short)1).putInt(12,3);check(RemoteKeyFormat.binary(sample,0,16)==null,"invalid key state excluded");
  for(int size:new int[]{16,24})for(ByteOrder order:new ByteOrder[]{ByteOrder.LITTLE_ENDIAN,ByteOrder.BIG_ENDIAN}){
   ByteBuffer data=ByteBuffer.allocate(size*2).order(order);data.putShort(size-8,(short)1).putShort(size-6,(short)88).putInt(size-4,1);
   event=RemoteKeyFormat.binary(data,0,size);check(event!=null&&event.code==88&&event.value==1,"native record width and byte order");
   data.putShort(size-6,(short)115);check(RemoteKeyFormat.binary(data,0,size)==null,"volume record excluded");
   check(RemoteKeyFormat.binary(data,-1,size)==null,"negative offset rejected");
  }
  check(RemoteKeyFormat.binary(ByteBuffer.allocate(24),0,20)==null,"unknown ABI width rejected");
  String info="add device 1: /dev/input/event3\n  bus: 0005\n vendor 1d5a\n product c081\n name: \"Example remote\"\n id: \"private-device-id-must-not-be-kept\"\nadd device 2: /dev/input/event1\n name: \"IR\"\n";
  List<RemoteKeyFormat.Device> devices=RemoteKeyFormat.devices(info);check(devices.size()==2,"device metadata parsed");check(devices.get(0).identity().equals("0005|1d5a|c081|Example remote"),"stable coarse device identity");check(!devices.get(0).identity().contains("private-device"),"device serial omitted");
  check(RemoteKeyFormat.devices("add device 1: /sdcard/private\n name: \"Wrong\"\n").isEmpty(),"paths outside input rejected");
  System.out.println("Passed "+count+" remote metadata checks");
 }
}
