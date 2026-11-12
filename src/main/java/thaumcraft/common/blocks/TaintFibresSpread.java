package thaumcraft.common.blocks;
import java.util.random.RandomGenerator;
/** Original BlockTaintFibres spreading rules, with legacy material queries delegated to the modern world. */
public final class TaintFibresSpread {
    public interface World {
        boolean taintedBiome(int x,int z);
        void taintBiome(int x,int z);
        int adjacentTaint(int x,int y,int z);
        boolean onlyAdjacentToTaint(int x,int y,int z);
        boolean adjacentSolid(int x,int y,int z);
        boolean liquid(int x,int y,int z);
        boolean fibreReplaceable(int x,int y,int z);
        boolean air(int x,int y,int z);
        boolean solidTop(int x,int y,int z);
        boolean woodOrCactusOrGourd(int x,int y,int z);
        boolean groundOrSandOrClayOrPlant(int x,int y,int z);
        void fibres(int x,int y,int z,int variant);
        void solidTaint(int x,int y,int z,int variant);
        void remove(int x,int y,int z);
        void schedule(int x,int y,int z);
        void spore(int x,int y,int z);
        boolean hasSpore(int x,int y,int z);
    }
    public static boolean spread(World world,int x,int y,int z,RandomGenerator random){
        if(world.adjacentSolid(x,y,z)&&!world.onlyAdjacentToTaint(x,y,z)&&!world.liquid(x,y,z)&&world.fibreReplaceable(x,y,z)){
            int variant=0;
            if(random.nextInt(10)==0&&world.air(x,y+1,z)&&world.solidTop(x,y-1,z)){
                if(random.nextInt(10)<9)variant=1;
                else if(random.nextInt(12)<10)variant=2;
                else variant=3;
            }
            world.fibres(x,y,z,variant);world.schedule(x,y,z);return true;
        }
        return false;
    }
    public static void biomeSpread(World world,int x,int y,int z,int rate,RandomGenerator random){
        if(rate>0){
            int xx=random.nextInt(3)-1,zz=random.nextInt(3)-1;
            if(!world.taintedBiome(x+xx,z+zz)&&random.nextInt(rate*5)==0&&world.adjacentTaint(x,y,z)>=2){
                world.taintBiome(x+xx,z+zz);world.schedule(x,y,z);
            }
        }
    }
    public static void tick(World world,int x,int y,int z,int variant,int rate,boolean spawnSpores,RandomGenerator random){
        biomeSpread(world,x,y,z,rate,random);
        if((variant==0&&world.onlyAdjacentToTaint(x,y,z))||!world.taintedBiome(x,z)){
            world.remove(x,y,z);return;
        }
        int xx=x+random.nextInt(3)-1,yy=y+random.nextInt(5)-3,zz=z+random.nextInt(3)-1;
        if(world.taintedBiome(xx,zz)&&!spread(world,xx,yy,zz,random)){
            int adjacent=world.adjacentTaint(xx,yy,zz);
            boolean woody=world.woodOrCactusOrGourd(xx,yy,zz);
            boolean earthy=!world.air(xx,yy,zz)&&world.groundOrSandOrClayOrPlant(xx,yy,zz);
            if(adjacent>=2&&woody){
                world.solidTaint(xx,yy,zz,0);world.schedule(xx,yy,zz);
            }
            if(adjacent>=3&&earthy){
                world.solidTaint(xx,yy,zz,1);world.schedule(xx,yy,zz);
            }
            if(variant==3&&spawnSpores&&random.nextInt(10)==0&&world.air(x,y+1,z)){
                world.fibres(x,y,z,4);world.spore(x,y+1,z);
            }else if(variant==4&&!world.hasSpore(x,y+1,z))world.fibres(x,y,z,3);
        }
    }
    private TaintFibresSpread(){}
}
