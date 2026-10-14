package thaumcraft.common.blocks;

import java.util.random.RandomGenerator;

/** Adapted Forge 1.7.10 BlockFluidFinite updateTick; quantities are metadata + 1. */
public final class FiniteFlux {
    public interface World {
        int quantity(int x,int y,int z); // air=0, another block=-1
        boolean displace(int x,int y,int z);
        boolean outside(int y);
        void set(int x,int y,int z,int quantity);
        void schedule(int x,int y,int z);
        boolean swapVertical(int x,int y,int z,int direction,int quantity);
    }
    private static int below(World w,int x,int y,int z,int threshold) {
        int n=w.quantity(x,y,z);return n>=threshold?-1:n;
    }
    public static void tick(World w,int x,int y,int z,int direction,RandomGenerator random) {
        tick(w,x,y,z,direction,8,random);
    }
    /** Original finite fluids select their own quantaPerBlock (death=4, flux=8). */
    public static void tick(World w,int x,int y,int z,int direction,int capacity,RandomGenerator random) {
        if(capacity<1)throw new IllegalArgumentException("capacity");
        int remaining=w.quantity(x,y,z);
        if(remaining<=0)return;
        int previous=remaining;
        int yy=y+direction;
        if(w.outside(yy)){w.set(x,y,z,0);return;}
        int other=below(w,x,yy,z,capacity);
        if(other>=0){
            int total=other+remaining;
            w.set(x,yy,z,Math.min(capacity,total));w.schedule(x,yy,z);
            remaining=Math.max(0,total-capacity);
            if(remaining==0){w.set(x,y,z,0);return;}
        } else if(w.displace(x,yy,z)) {
            w.set(x,yy,z,remaining);w.schedule(x,yy,z);w.set(x,y,z,0);return;
        } else if(w.swapVertical(x,y,z,direction,remaining))return;
        boolean changed=remaining!=previous;
        if(remaining==1){if(changed)w.set(x,y,z,1);return;}
        int[][] offsets={{0,-1},{0,1},{-1,0},{1,0}};
        int[] values=new int[4];int total=remaining,count=1;
        for(int i=0;i<4;i++){
            int xx=x+offsets[i][0],zz=z+offsets[i][1];
            if(w.displace(xx,y,zz))w.set(xx,y,zz,0);
            values[i]=below(w,xx,y,zz,remaining-1);
            if(values[i]>=0){total+=values[i];count++;}
        }
        if(count==1){if(changed)w.set(x,y,z,remaining);return;}
        int each=total/count,rem=total%count;
        for(int i=0;i<4;i++)if(values[i]>=0){
            int n=each;
            if(rem==count || rem>1 && random.nextInt(count-rem)!=0){n++;rem--;}
            if(n!=values[i]){
                int xx=x+offsets[i][0],zz=z+offsets[i][1];
                w.set(xx,y,zz,n);w.schedule(xx,y,zz);
            }
            count--;
        }
        if(rem>0)each++;
        w.set(x,y,z,each);
    }
    private FiniteFlux(){}
}
