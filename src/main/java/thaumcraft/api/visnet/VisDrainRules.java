package thaumcraft.api.visnet;
import thaumcraft.api.aspects.Aspect;
/** Original VisNetHandler.drainVis loop for the preselected nearby network endpoints. */
public final class VisDrainRules {
    public interface Node {int consumeVis(Aspect aspect,int amount);}
    public interface Effect {void drained(Node node,int primalColor);}
    private VisDrainRules(){}
    public static int drain(Iterable<? extends Node> nodes,Aspect aspect,int amount,Effect effect){
        int drained=0;
        for(Node node:nodes){
            if(node==null)continue;
            int taken=node.consumeVis(aspect,amount);
            drained+=taken;amount-=taken;
            if(taken>0)effect.drained(node,Aspect.getPrimalAspects().indexOf(aspect));
            if(amount<=0)break;
        }
        return drained;
    }
}
