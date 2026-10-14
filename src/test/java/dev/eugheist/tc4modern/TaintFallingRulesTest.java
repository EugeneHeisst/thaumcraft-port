package dev.eugheist.tc4modern;
import thaumcraft.common.blocks.TaintFallingRules;
/** Original boundary behavior: unloaded descent restores the source, loaded descent defers removal. */
public final class TaintFallingRulesTest {
    static final class World implements TaintFallingRules.World {
        boolean loaded;int removed,spawned;String placed,spawn;
        public boolean canFallBelow(int x,int y,int z){return y>2;}
        public boolean loadedArea(int x,int y,int z,int radius){if(radius!=32)throw new AssertionError();return loaded;}
        public void spawn(int x,int y,int z,int md,int ox,int oy,int oz){spawned++;spawn=x+","+y+","+z+":"+md+":"+ox+","+oy+","+oz;}
        public void remove(int x,int y,int z){removed++;}
        public void place(int x,int y,int z,int md){placed=x+","+y+","+z+":"+md;}
    }
    public static void main(String[] args){
        World w=new World();
        if(TaintFallingRules.tryFall(w,4,12,6,5,12,6,0)||w.removed!=1||!"4,12,6:0".equals(w.placed)||w.spawned!=0)throw new AssertionError("Unloaded original source restoration changed");
        w=new World();w.loaded=true;
        if(!TaintFallingRules.tryFall(w,4,12,6,5,12,6,1)||w.removed!=0||w.placed!=null||!"5,12,6:1:4,12,6".equals(w.spawn))throw new AssertionError("Loaded falling source/target separation changed");
        w=new World();w.loaded=true;
        if(TaintFallingRules.tryFall(w,4,3,6,4,3,6,0)||w.spawned!=0||w.removed!=0)throw new AssertionError("Supported block fell");
        System.out.println("Original falling taint loaded/unloaded checks passed");
    }
}
