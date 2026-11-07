package local.z6panel;

import java.util.*;

public final class BottomNavigationTest {
 private static int checks;
 private static void check(boolean value,String name){if(!value)throw new AssertionError(name);checks++;}
 public static void main(String[] args){
  List<String> actions=Arrays.asList("audio","wifi","bluetooth","settings","home");
  BottomNavigation n=new BottomNavigation();
  n.update(actions,Collections.emptyList());
  check(n.id().equals("audio"),"cold start Audio");
  check(!n.vertical(true),"loading keeps actions accessible");
  n.move(-1,true);check(n.id().equals("home"),"actions left wrap");
  n.move(1,true);check(n.id().equals("audio"),"actions right wrap");
  n.update(actions,Arrays.asList(BottomNavigation.ADD));n.vertical(true);
  check(n.id().equals(BottomNavigation.ADD)&&!n.move(1,true),"empty loaded row Add");
  check(!n.vertical(true),"down stays in apps");
  n.update(actions,Arrays.asList("a","b","c"));
  n.move(-1,true);check(n.id().equals("c"),"apps left wrap");
  n.move(1,true);check(n.id().equals("a"),"apps right wrap");
  n.choose(BottomNavigation.Section.APPS,"b");n.vertical(false);n.move(1,true);n.vertical(true);
  check(n.id().equals("b"),"independent row memories");
  n.update(actions,Arrays.asList("c","a","b"));check(n.index()==2&&n.id().equals("b"),"stable ID after reorder");
  n.update(actions,Arrays.asList("c","a"));check(n.id().equals("a")&&n.index()==1,"nearest position after removal");
  check(!n.move(1,false)&&n.id().equals("a"),"editing has no right wrap");
  n.move(-1,false);check(!n.move(-1,false)&&n.id().equals("c"),"editing has no left wrap");
  n.update(actions,Arrays.asList(BottomNavigation.ADD,BottomNavigation.SAVE));n.choose(BottomNavigation.Section.APPS,BottomNavigation.SAVE);
  check(!n.move(1,false)&&n.flatIndex()==6,"last hidden app save position");
  n.update(actions,Arrays.asList(BottomNavigation.ADD));check(n.id().equals(BottomNavigation.ADD),"save removal keeps valid app focus");
  List<String> many=new ArrayList<>();for(int i=0;i<50;i++)many.add("pkg"+i);
  n.update(actions,many);n.first(BottomNavigation.Section.APPS);
  for(int i=0;i<503;i++)n.move(1,true);
  check(n.index()==3,"many rapid movements");
  n.update(actions,Collections.singletonList("only"));check(n.id().equals("only")&&!n.move(-1,true),"single app stable");
  n.update(actions,Collections.emptyList());check(n.section()==BottomNavigation.Section.ACTIONS&&n.id().equals("wifi"),"catalog empty fallback retains action ID");
  check(new BottomNavigation().id().equals(""),"new process does not persist selection");
  System.out.println(checks+" navigation checks passed");
 }
}
