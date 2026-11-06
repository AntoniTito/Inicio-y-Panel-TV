package local.z6qa;
import android.os.*;
import android.view.*;
import java.lang.reflect.*;
/** Real held DPAD_CENTER injection, restricted to shell and emulators. */
public final class ControlHold {
 public static void main(String[] args)throws Exception {
  String qemu=(String)Class.forName("android.os.SystemProperties").getMethod("get",String.class).invoke(null,"ro.kernel.qemu");
  if(android.os.Process.myUid()!=2000||!"1".equals(qemu))throw new SecurityException("Shell emulator only");
  Class<?> cls=Class.forName("android.hardware.input.InputManager");Object manager=cls.getMethod("getInstance").invoke(null);Method inject=cls.getMethod("injectInputEvent",InputEvent.class,int.class);long start=SystemClock.uptimeMillis();
  KeyEvent down=new KeyEvent(start,start,KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_CENTER,0,0,KeyCharacterMap.VIRTUAL_KEYBOARD,0,0,InputDevice.SOURCE_DPAD);
  if(!Boolean.TRUE.equals(inject.invoke(manager,down,2)))throw new IllegalStateException("Key down rejected");
  Thread.sleep(850);
  KeyEvent up=new KeyEvent(start,SystemClock.uptimeMillis(),KeyEvent.ACTION_UP,KeyEvent.KEYCODE_DPAD_CENTER,0,0,KeyCharacterMap.VIRTUAL_KEYBOARD,0,0,InputDevice.SOURCE_DPAD);
  if(!Boolean.TRUE.equals(inject.invoke(manager,up,2)))throw new IllegalStateException("Key up rejected");
  System.out.println("HELD_OK");
 }
}
