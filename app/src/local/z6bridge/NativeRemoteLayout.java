package local.z6bridge;

import android.view.InputDevice;
import org.json.*;

/** One-time Android configuration. Only a verified two-function remote layout, never Generic.kl. */
public final class NativeRemoteLayout {
 static final String LAYOUT="local.z6panel/local.z6panel.RemoteLayoutReceiver/z6remote";
 public static String command(String operation)throws Exception {
  if(android.os.Process.myUid()!=2000)throw new SecurityException("Shell only");
  Class<?> managerType=Class.forName("android.hardware.input.InputManager"),identifierType=Class.forName("android.hardware.input.InputDeviceIdentifier");
  Object manager=managerType.getMethod("getInstance").invoke(null);
  JSONObject result=new JSONObject();JSONArray devices=new JSONArray();
  for(int id:InputDevice.getDeviceIds()){
   InputDevice d=InputDevice.getDevice(id);if(d==null||d.getVendorId()!=7514||d.getProductId()!=49281||!d.getName().equals("Remoter ATV3")||d.getKeyboardType()!=2)continue;
   devices.put(id);
  }
  boolean restore=operation.equals("restore")||operation.startsWith("restore:");
  if(!restore&&!operation.equals("apply_saved")&&devices.length()!=1)return result.put("status",devices.length()==0?"NO_DEVICE":"AMBIGUOUS_DEVICE").toString();
  Object identifier=identifierType.getConstructor(String.class,int.class,int.class).newInstance("z6-remote-vendor",7514,49281);
  Object previous=managerType.getMethod("getCurrentKeyboardLayoutForInputDevice",identifierType).invoke(manager,identifier);
  if(operation.equals("apply")||operation.equals("apply_saved"))managerType.getMethod("setCurrentKeyboardLayoutForInputDevice",identifierType,String.class).invoke(manager,identifier,LAYOUT);
  else if(restore){
   String original="";if(operation.startsWith("restore:")){JSONObject backup=new JSONObject(new String(android.util.Base64.decode(operation.substring(8),android.util.Base64.DEFAULT),java.nio.charset.StandardCharsets.UTF_8));original=backup.optString("previous","");if(original.equals("null"))original="";if(!original.isEmpty()&&!original.matches("[A-Za-z0-9_.]+/[A-Za-z0-9_.$]+/[A-Za-z0-9_-]+"))throw new IllegalArgumentException("Invalid original layout");}
   if(LAYOUT.equals(previous)){managerType.getMethod("removeKeyboardLayoutForInputDevice",identifierType,String.class).invoke(manager,identifier,LAYOUT);if(!original.isEmpty()&&!original.equals(LAYOUT))managerType.getMethod("setCurrentKeyboardLayoutForInputDevice",identifierType,String.class).invoke(manager,identifier,original);}
  }
  else if(!operation.equals("inspect"))throw new IllegalArgumentException("Unsupported layout operation");
  Object current=managerType.getMethod("getCurrentKeyboardLayoutForInputDevice",identifierType).invoke(manager,identifier);
  return result.put("status","OK").put("previous",previous==null?JSONObject.NULL:previous).put("current",current==null?JSONObject.NULL:current).put("vendor",7514).put("product",49281).toString();
 }
 public static void main(String[] args)throws Exception {if(args.length!=1)throw new IllegalArgumentException();System.out.println(command(args[0]));}
}
