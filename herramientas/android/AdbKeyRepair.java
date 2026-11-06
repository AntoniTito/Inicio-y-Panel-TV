package local.z6qa;
/** One-shot emulator repair: forget ONLY a previously approved local-panel key.
 * Android rewrites its key file atomically; no root and no arbitrary file writes.
 * The caller must verify the fingerprint against the approved Z6Panel key first.
 */
public final class AdbKeyRepair {
 public static void main(String[] args)throws Exception {
  if(android.os.Process.myUid()!=2000||android.os.Build.VERSION.SDK_INT!=31)throw new SecurityException("Android-12 shell required");
  Object qemu=Class.forName("android.os.SystemProperties").getMethod("get",String.class).invoke(null,"ro.kernel.qemu");
  if(!"1".equals(qemu)||args.length!=1||!args[0].matches("([0-9A-F]{2}:){15}[0-9A-F]{2}"))throw new SecurityException("Emulator and verified fingerprint required");
  Object binder=Class.forName("android.os.ServiceManager").getMethod("getService",String.class).invoke(null,"adb");
  Object manager=Class.forName("android.debug.IAdbManager$Stub").getMethod("asInterface",android.os.IBinder.class).invoke(null,binder);
  Class.forName("android.debug.IAdbManager").getMethod("unpairDevice",String.class).invoke(manager,args[0]);
  System.out.println("Only the specified panel key was forgotten. Approve that key again on Android.");
 }
}
