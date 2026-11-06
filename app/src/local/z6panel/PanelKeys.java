package local.z6panel;

import java.util.HashMap;
import java.util.Map;

/** Pairs consumed presses and releases, including releases after a panel closes. */
public final class PanelKeys {
 private final Map<Integer,Integer> pressed=new HashMap<>();
 public void down(int code,int generation){if(!pressed.containsKey(code))pressed.put(code,generation);}
 public Integer up(int code){return pressed.remove(code);}
 public void clear(){pressed.clear();}
}
