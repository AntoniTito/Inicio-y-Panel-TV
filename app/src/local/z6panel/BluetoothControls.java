package local.z6panel;
import android.bluetooth.*;import android.content.*;import android.os.*;import java.util.*;
/** Public discovery plus firmware-checked Android 10 A2DP compatibility methods. */
final class BluetoothControls {
 interface PairingListener{void onPairingRequest(BluetoothDevice device);}
 final Context context;final Runnable changed;final PairingListener pairingListener;BluetoothAdapter adapter;final Map<String,BluetoothDevice> found=new LinkedHashMap<>();
 private String requestedAddress;
 private final Handler ui=new Handler(Looper.getMainLooper());private BluetoothProfile audio;private boolean closed=false;
 private final Runnable stop=()->{try{if(adapter!=null&&adapter.isDiscovering())adapter.cancelDiscovery();}catch(SecurityException ignored){}};
 private final BroadcastReceiver receiver=new BroadcastReceiver(){public void onReceive(Context c,Intent i){BluetoothDevice d=i.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);String action=i.getAction();if(d!=null&&BluetoothDevice.ACTION_FOUND.equals(action))found.put(d.getAddress(),d);if(d!=null&&BluetoothDevice.ACTION_PAIRING_REQUEST.equals(action)&&d.getAddress().equals(requestedAddress)){confirmPairing(d);if(pairingListener!=null)pairingListener.onPairingRequest(d);}if(d!=null&&BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)&&d.getAddress().equals(requestedAddress)&&d.getBondState()==BluetoothDevice.BOND_BONDED)ui.postDelayed(()->autoConnect(d,0),900);changed.run();}};
 BluetoothControls(Context c,Runnable callback,PairingListener listener){context=c;changed=callback;pairingListener=listener;adapter=BluetoothAdapter.getDefaultAdapter();IntentFilter f=new IntentFilter();f.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);f.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);f.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);f.addAction(BluetoothDevice.ACTION_FOUND);f.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);f.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);f.addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);f.addAction("android.bluetooth.a2dp.profile.action.ACTIVE_DEVICE_CHANGED");
  if(Build.VERSION.SDK_INT>=33)c.registerReceiver(receiver,f,Context.RECEIVER_EXPORTED);else c.registerReceiver(receiver,f);
  if(adapter!=null&&permissionsGranted())try{adapter.getProfileProxy(c,new BluetoothProfile.ServiceListener(){public void onServiceConnected(int p,BluetoothProfile proxy){if(closed){adapter.closeProfileProxy(p,proxy);return;}audio=proxy;changed.run();}public void onServiceDisconnected(int p){audio=null;changed.run();}},BluetoothProfile.A2DP);}catch(SecurityException ignored){}
 }
 boolean permissionsGranted(){return Build.VERSION.SDK_INT<31||context.checkSelfPermission("android.permission.BLUETOOTH_CONNECT")==android.content.pm.PackageManager.PERMISSION_GRANTED&&context.checkSelfPermission("android.permission.BLUETOOTH_SCAN")==android.content.pm.PackageManager.PERMISSION_GRANTED;}
 List<BluetoothDevice> devices(){Map<String,BluetoothDevice> all=new LinkedHashMap<>();if(!permissionsGranted())return new ArrayList<>();try{if(adapter!=null)for(BluetoothDevice d:adapter.getBondedDevices())all.put(d.getAddress(),d);}catch(SecurityException denied){return new ArrayList<>();}all.putAll(found);List<BluetoothDevice> result=new ArrayList<>(all.values());result.sort((a,b)->label(a).compareToIgnoreCase(label(b)));return result;}
 String label(BluetoothDevice d){String n=d.getName();if(n!=null&&!n.trim().isEmpty())return n;String address=d.getAddress();return "Dispositivo sin nombre · "+address.substring(Math.max(0,address.length()-8));}
 int audioState(BluetoothDevice d){try{return audio==null?BluetoothProfile.STATE_DISCONNECTED:audio.getConnectionState(d);}catch(SecurityException e){return BluetoothProfile.STATE_DISCONNECTED;}}
 boolean isActive(BluetoothDevice d){try{return audio!=null&&d.equals(audio.getClass().getMethod("getActiveDevice").invoke(audio));}catch(Exception e){return false;}}
 String useAudio(BluetoothDevice d){if(audioState(d)!=BluetoothProfile.STATE_CONNECTED)return "Conecta primero el parlante";try{return (Boolean)audio.getClass().getMethod("setActiveDevice",BluetoothDevice.class).invoke(audio,d)?"Salida seleccionada":"No se pudo seleccionar la salida";}catch(Exception e){return "Cambio de salida no disponible";}}
 String state(BluetoothDevice d){int s=audioState(d);if(s==BluetoothProfile.STATE_CONNECTED)return isActive(d)?"Salida de audio activa":"Audio conectado";if(s==BluetoothProfile.STATE_CONNECTING)return "Conectando…";if(s==BluetoothProfile.STATE_DISCONNECTING)return "Desconectando…";return d.getBondState()==BluetoothDevice.BOND_BONDING?"Emparejando…":d.getBondState()==BluetoothDevice.BOND_BONDED?"Emparejado":"Disponible";}
 boolean supportsAudio(BluetoothDevice d){if(audioState(d)!=BluetoothProfile.STATE_DISCONNECTED)return true;BluetoothClass c=d.getBluetoothClass();if(c!=null&&c.getMajorDeviceClass()==BluetoothClass.Device.Major.AUDIO_VIDEO)return true;ParcelUuid[] uuids=d.getUuids();if(uuids!=null)for(ParcelUuid u:uuids){String id=u.toString();if(id.startsWith("0000110b-")||id.startsWith("0000110d-"))return true;}return false;}
 boolean audioControlAvailable(){if(audio==null||Build.VERSION.SDK_INT!=29)return false;try{audio.getClass().getMethod("connect",BluetoothDevice.class);audio.getClass().getMethod("disconnect",BluetoothDevice.class);return true;}catch(Exception e){return false;}}
 String audioConnection(BluetoothDevice d,boolean connect){if(d.getBondState()!=BluetoothDevice.BOND_BONDED||!supportsAudio(d))return "Empareja primero un dispositivo de audio";if(!audioControlAvailable())return "Control de audio no disponible en este firmware";try{boolean accepted=(Boolean)audio.getClass().getMethod(connect?"connect":"disconnect",BluetoothDevice.class).invoke(audio,d);return accepted?"Solicitud enviada":"No se pudo iniciar el cambio";}catch(Exception e){return "Android no permite cambiar esta conexión";}}
 private void confirmPairing(BluetoothDevice d){try{d.getClass().getMethod("setPairingConfirmation",boolean.class).invoke(d,true);}catch(Exception ignored){}}
 private void autoConnect(BluetoothDevice d,int attempt){if(closed||d.getBondState()!=BluetoothDevice.BOND_BONDED)return;if(audioState(d)==BluetoothProfile.STATE_CONNECTED)return;if(audioControlAvailable()){audioConnection(d,true);changed.run();return;}if(attempt<8)ui.postDelayed(()->autoConnect(d,attempt+1),750);}
 boolean scan(){
  if(adapter==null||!adapter.isEnabled())return false;
  found.clear();stop.run();
  boolean ok;
  try{ok=adapter.startDiscovery();}catch(SecurityException e){return false;}
  if(!ok){ui.postDelayed(()->{try{if(adapter!=null&&adapter.isEnabled()&&!adapter.isDiscovering())adapter.startDiscovery();}catch(SecurityException ignored){}},350);ok=true;}
  ui.removeCallbacks(stop);ui.postDelayed(stop,15000);return ok;
 }
 boolean pair(BluetoothDevice d,Runnable result){
  stop.run();
  if(d==null)return false;
  if(d.getBondState()==BluetoothDevice.BOND_BONDED)return true;
  if(d.getBondState()==BluetoothDevice.BOND_BONDING)return true;
   requestedAddress=d.getAddress();
  ui.postDelayed(()->{
   boolean started=false;
   try{
    try{started=(Boolean)d.getClass().getMethod("createBond",int.class).invoke(d,BluetoothDevice.TRANSPORT_BREDR);}
    catch(Exception ignored){started=d.createBond();}
   }catch(SecurityException ignored){}
   if(!started){
    ui.postDelayed(()->{try{if(d.getBondState()==BluetoothDevice.BOND_NONE)d.createBond();}catch(SecurityException ignored){}},1200);
    if(result!=null)ui.postDelayed(result,1800);
    return;
   }
   ui.postDelayed(()->{if(result!=null)result.run();},1800);
  },700);
  return true;
 }
 boolean forget(BluetoothDevice d){
  stop.run();
  try{return (Boolean)d.getClass().getMethod("removeBond").invoke(d);}catch(Exception e){return false;}
 }
 void cancelPairing(BluetoothDevice d){
  try{d.getClass().getMethod("cancelBondProcess").invoke(d);}catch(Exception ignored){}
  requestedAddress=null;
 }
 void stopScan(){ui.removeCallbacks(stop);stop.run();}
 void close(){closed=true;stopScan();try{context.unregisterReceiver(receiver);}catch(Exception ignored){}if(adapter!=null&&audio!=null)adapter.closeProfileProxy(BluetoothProfile.A2DP,audio);audio=null;}
}
