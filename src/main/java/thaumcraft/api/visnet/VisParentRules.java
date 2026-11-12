package thaumcraft.api.visnet;
import java.util.*;
import java.util.function.BiPredicate;
/** Original VisNetHandler.addNode/findClosestNodes parent selection, independent of world access. */
public final class VisParentRules {
 public interface Node extends VisNearbyRules.Node {int attunement();}
 private record Candidate(Node node,float score){}
 private VisParentRules(){}
 private static float distance(Node a,Node b){double x=(double)a.x()-b.x(),y=(double)a.y()-b.y(),z=(double)a.z()-b.z();float d=(float)(x*x+y*y+z*z);return d>a.range()*a.range()?-1:d;}
 private static void children(Node target,Node parent,List<Candidate> out,Set<Node> visited){
  if(visited.size()>512||!visited.add(parent))return;
  for(var raw:parent.children()){
   if(!(raw instanceof Node child)||child==target||child==parent)continue;
   float d=distance(target,child);if(d>0)out.add(new Candidate(child,d));
   children(target,child,out,visited);
  }
 }
 public static Node select(Node target,Iterable<? extends Node> sources,BiPredicate<Node,Node> visible){
  List<Candidate> candidates=new ArrayList<>();
  for(Node source:sources){
   if(source==null||!source.valid())continue;
   float d=distance(target,source);if(d>0)candidates.add(new Candidate(source,d-target.range()*2));
   children(target,source,candidates,Collections.newSetFromMap(new IdentityHashMap<>()));
  }
  Node closest=null;float score=Float.MAX_VALUE;
  for(var candidate:candidates){Node node=candidate.node();
   if(candidate.score()<score&&(target.attunement()==-1||node.attunement()==-1||target.attunement()==node.attunement())&&visible.test(target,node)){closest=node;score=candidate.score();}
  }
  return closest;
 }
}
