package thaumcraft.common.entities.monster;
import java.util.random.RandomGenerator;
/** Original EntityTaintSwarm's free flight, pursuit and melee predicates. */
public final class TaintSwarmFlight {
    public record Target(int x,int y,int z){}
    public record Velocity(double x,double y,double z,float yaw){}
    public static Target choose(double x,double y,double z,RandomGenerator random){
        return new Target((int)x+random.nextInt(7)-random.nextInt(7),(int)y+random.nextInt(6)-2,(int)z+random.nextInt(7)-random.nextInt(7));
    }
    public static boolean needsTarget(Target target,double x,double y,double z,RandomGenerator random){
        if(target==null)return true;
        if(random.nextInt(30)==0)return true;
        // ChunkCoordinates.getDistanceSquared uses coordinates cast to int by the caller.
        float dx=target.x-(int)x,dy=target.y-(int)y,dz=target.z-(int)z;
        return dx*dx+dy*dy+dz*dz<4;
    }
    public static Velocity steer(double vx,double vy,double vz,double dx,double dy,double dz,boolean pursuit){
        vx+=(Math.signum(dx)*.5-vx)*(pursuit?.025000000149011613:.015000000014901161);
        vy+=(Math.signum(dy)*(double).7f-vy)*(double).1f;
        vz+=(Math.signum(dz)*.5-vz)*(pursuit?.02500000001490116:.015000000014901161);
        float yaw=(float)(Math.atan2(vz,vx)*180/Math.PI)-90;
        return new Velocity(vx,vy,vz,yaw);
    }
    public static boolean canAttack(int cooldown,float distance,double targetTop,double targetBottom,double ownTop,double ownBottom){return cooldown<=0&&distance<3&&targetTop>ownBottom&&targetBottom<ownTop;}
    public static byte summoned(byte flags,boolean summoned){return summoned?(byte)(flags|2):(byte)(flags&0xfffffffc);}
    private TaintSwarmFlight(){}
}
