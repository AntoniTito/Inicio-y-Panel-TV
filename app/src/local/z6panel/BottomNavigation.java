package local.z6panel;

import java.util.*;

/** In-memory focus for independent rows; editing and page navigation stay with the host. */
final class BottomNavigation {
 enum Section { ACTIONS, APPS }
 static final String ADD="add_application",SAVE="save_changes";
 private static final class Row {
  List<String> ids=Collections.emptyList();Map<String,Integer> indexes=Collections.emptyMap();int index;
  String id(){return ids.isEmpty()?"":ids.get(index);}
  void update(List<String> next){String previous=id();int old=index;ids=new ArrayList<>(next);indexes=new HashMap<>();for(int i=0;i<ids.size();i++){if(indexes.put(ids.get(i),i)!=null)throw new IllegalArgumentException("Duplicate row ID");}Integer kept=indexes.get(previous);index=kept!=null?kept:Math.max(0,Math.min(old,ids.size()-1));}
 }
 private final Row actions=new Row(),apps=new Row();private Section section=Section.ACTIONS;
 private Row row(Section s){return s==Section.ACTIONS?actions:apps;}
 void update(List<String> actionIds,List<String> appIds){actions.update(actionIds);apps.update(appIds);if(row(section).ids.isEmpty())section=actions.ids.isEmpty()?Section.APPS:Section.ACTIONS;}
 Section section(){return section;}
 String id(){return row(section).id();}
 int index(){return row(section).ids.isEmpty()?-1:row(section).index;}
 int appIndex(){return apps.ids.isEmpty()?-1:apps.index;}
 int flatIndex(){return index()<0?-1:(section==Section.ACTIONS?index():actions.ids.size()+index());}
 boolean choose(Section s,String id){Integer index=row(s).indexes.get(id);if(index==null)return false;section=s;row(s).index=index;return true;}
 void first(Section s){if(!row(s).ids.isEmpty()){section=s;row(s).index=0;}}
 boolean vertical(boolean down){Section next=down?Section.APPS:Section.ACTIONS;if(section==next||row(next).ids.isEmpty())return false;section=next;return true;}
 boolean move(int delta,boolean circular){Row r=row(section);int size=r.ids.size();if(size<2)return false;int next=circular?Math.floorMod(r.index+delta,size):Math.max(0,Math.min(size-1,r.index+delta));if(next==r.index)return false;r.index=next;return true;}
 List<String> ids(Section s){return Collections.unmodifiableList(row(s).ids);}
}
