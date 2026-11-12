package thaumcraft.common.blocks;

import java.util.random.RandomGenerator;

/** Original TileCrucible.spill coordinate and metadata decisions. */
public final class CrucibleFlux {
    public interface World {
        boolean air(int x,int y,int z);
        int fluxMetadata(int x,int y,int z); // -1 unless either flux block
        void increment(int x,int y,int z,int metadata);
        void place(int x,int y,int z,boolean gas);
    }
    public static void spill(World w,int x,int y,int z,RandomGenerator random){
        if(random.nextInt(4)!=0)return;
        if(w.air(x,y+1,z)){w.place(x,y+1,z,random.nextBoolean());return;}
        int md=w.fluxMetadata(x,y+1,z);
        if(md>=0 && md<7){w.increment(x,y+1,z,md+1);return;}
        int xx=x-1+random.nextInt(3),yy=y-1+random.nextInt(3),zz=z-1+random.nextInt(3);
        if(w.air(xx,yy,zz))w.place(xx,yy,zz,random.nextBoolean());
    }
    private CrucibleFlux(){}
}
