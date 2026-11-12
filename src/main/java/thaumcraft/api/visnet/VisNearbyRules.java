package thaumcraft.api.visnet;
import java.util.ArrayList;
import java.util.List;
/** Original calculateNearbyNodes/getAllChildren selection; caller owns source and cache lifecycle. */
public final class VisNearbyRules {
    public interface Node extends VisDrainRules.Node {
        int x();int y();int z();int range();
        boolean valid();boolean loaded();
        Iterable<? extends Node> children();
    }
    private VisNearbyRules(){}
    private static float inRange(Node node,int x,int y,int z){
        float distance=(float)(((double)node.x()-x)*(node.x()-x)+((double)node.y()-y)*(node.y()-y)+((double)node.z()-z)*(node.z()-z));
        return distance>(float)(node.range()*node.range())?-1:distance;
    }
    private static void descendants(Node parent,List<Node> list){
        for(Node child:parent.children()){
            if(child==null||!child.loaded())continue;
            list.add(child);descendants(child,list);
        }
    }
    public static List<Node> select(Iterable<? extends Node> roots,int x,int y,int z){
        List<Node> result=new ArrayList<>();
        for(Node root:roots){
            if(root==null||!root.valid())continue;
            Node closest=null;float distance=Float.MAX_VALUE;
            float r=inRange(root,x,y,z);
            if(r>0){closest=root;distance=r;}
            List<Node> children=new ArrayList<>();descendants(root,children);
            for(Node child:children){
                float candidate=inRange(child,x,y,z);
                if(candidate>0&&candidate<distance){closest=child;distance=candidate;}
            }
            if(closest!=null)result.add(closest);
        }
        return result;
    }
}
