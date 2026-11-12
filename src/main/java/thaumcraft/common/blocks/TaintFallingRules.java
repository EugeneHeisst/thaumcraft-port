package thaumcraft.common.blocks;
/** Original BlockTaint.tryToFall, including its unloaded-area placement quirk. */
public final class TaintFallingRules {
    public interface World {
        boolean canFallBelow(int x,int y,int z);
        default boolean air(int x,int y,int z){return false;}
        default boolean taint(int x,int y,int z){return false;}
        boolean loadedArea(int x,int y,int z,int radius);
        void spawn(int x,int y,int z,int metadata,int ox,int oy,int oz);
        void remove(int x,int y,int z);
        void place(int x,int y,int z,int metadata);
    }
    public static boolean tryFall(World world,int x,int y,int z,int tx,int ty,int tz,int metadata){
        if(world.canFallBelow(tx,ty-1,tz)&&ty>=0){
            if(world.loadedArea(tx,ty,tz,32)){world.spawn(tx,ty,tz,metadata,x,y,z);return true;}
            else{
                world.remove(x,y,z);
                while(world.canFallBelow(tx,ty-1,tz)&&ty>0)--ty;
                // Original restores at the source position, rather than the calculated landing position.
                if(ty>0)world.place(x,y,z,metadata);
            }
        }
        return false;
    }
    public static boolean crustFall(World world,int x,int y,int z,java.util.random.RandomGenerator random){
        if(tryFall(world,x,y,z,x,y,z,0))return true;
        if(world.air(x,y+1,z)){
            int dir=2+random.nextInt(4);
            int dx=dir==4?-1:dir==5?1:0,dz=dir==2?-1:dir==3?1:0;
            boolean slide=true;
            for(int a=0;a<4;a++)if(!world.air(x+dx,y-a,z+dz)||!world.taint(x,y-a,z)){slide=false;break;}
            if(slide&&tryFall(world,x,y,z,x+dx,y,z+dz,0))return true;
        }
        return false;
    }
    private TaintFallingRules(){}
}
