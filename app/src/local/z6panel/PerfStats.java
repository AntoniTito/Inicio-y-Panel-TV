package local.z6panel;
import java.util.concurrent.atomic.AtomicLong;
import org.json.*;
/** In-memory counters only. Access restricted to shell by BridgeProvider. */
final class PerfStats {
 static final AtomicLong volumeCalls=new AtomicLong(),volumeNanos=new AtomicLong(),volumeHudNanos=new AtomicLong();
 static final AtomicLong scans=new AtomicLong(),fullRenders=new AtomicLong(),binds=new AtomicLong(),focusUpdates=new AtomicLong(),foregroundQueries=new AtomicLong(),actions=new AtomicLong(),actionNanos=new AtomicLong(),maxActionNanos=new AtomicLong();
 static void action(long elapsed){actions.incrementAndGet();actionNanos.addAndGet(elapsed);long old;do{old=maxActionNanos.get();if(old>=elapsed)break;}while(!maxActionNanos.compareAndSet(old,elapsed));}
 static JSONObject snapshot()throws JSONException{JSONObject o=new JSONObject();o.put("catalogScans",scans.get());o.put("fullRenders",fullRenders.get());o.put("incrementalBinds",binds.get());o.put("focusUpdates",focusUpdates.get());o.put("foregroundQueries",foregroundQueries.get());o.put("actions",actions.get());o.put("actionNanos",actionNanos.get());o.put("maxActionNanos",maxActionNanos.get());o.put("volumeCalls",volumeCalls.get());o.put("volumeNanos",volumeNanos.get());o.put("volumeHudNanos",volumeHudNanos.get());return o;}
}
