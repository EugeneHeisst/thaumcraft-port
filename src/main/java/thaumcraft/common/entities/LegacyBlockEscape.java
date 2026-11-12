package thaumcraft.common.entities;
/** Original Entity.pushOutOfBlocks 1.7.10 nearest face selection, intentionally never selecting down. */
public final class LegacyBlockEscape {
    public interface World {boolean normalCube(int x,int y,int z);}
    public record Velocity(double x,double y,double z){}
    public static Velocity escape(World world,boolean colliding,double x,double y,double z,double vx,double vy,double vz,java.util.random.RandomGenerator random){
        int bx=(int)Math.floor(x),by=(int)Math.floor(y),bz=(int)Math.floor(z);
        if(!colliding&&!world.normalCube(bx,by,bz))return new Velocity(vx,vy,vz);
        boolean west=!world.normalCube(bx-1,by,bz),east=!world.normalCube(bx+1,by,bz);
        world.normalCube(bx,by-1,bz); // Original queries down but never chooses it.
        boolean up=!world.normalCube(bx,by+1,bz),north=!world.normalCube(bx,by,bz-1),south=!world.normalCube(bx,by,bz+1);
        double fx=x-bx,fy=y-by,fz=z-bz,closest=9999;int side=3;
        if(west&&fx<closest){closest=fx;side=0;}
        if(east&&1-fx<closest){closest=1-fx;side=1;}
        if(up&&1-fy<closest){closest=1-fy;side=3;}
        if(north&&fz<closest){closest=fz;side=4;}
        if(south&&1-fz<closest){side=5;}
        float speed=random.nextFloat()*.2f+.1f;
        return switch(side){case 0->new Velocity(-speed,vy,vz);case 1->new Velocity(speed,vy,vz);case 3->new Velocity(vx,speed,vz);case 4->new Velocity(vx,vy,-speed);case 5->new Velocity(vx,vy,speed);default->new Velocity(vx,vy,vz);};
    }
    private LegacyBlockEscape(){}
}
