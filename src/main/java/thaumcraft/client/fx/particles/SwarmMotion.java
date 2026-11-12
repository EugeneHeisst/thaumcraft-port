package thaumcraft.client.fx.particles;
import java.util.random.RandomGenerator;
/** Original FXSwarm steering, including its negative turn limit while circling a hurt/near target. */
public final class SwarmMotion {
    public float yaw,pitch;
    public double vx,vy,vz;
    public int deathTimer;
    public boolean alive=true;
    public void steer(double dx,double dy,double dz,double distanceSquared,double targetWidth,boolean hurt,float turn,float speed,RandomGenerator random){
        float yawLimit=turn/2+random.nextInt((int)(turn/2)),pitchLimit=turn/2+random.nextInt((int)(turn/2));
        if(distanceSquared<=targetWidth||hurt){yawLimit=-yawLimit;pitchLimit=-pitchLimit;}
        float wantedYaw=(float)(Math.atan2(dz,dx)*180/Math.PI)-90;
        float wantedPitch=(float)(-Math.atan2(dy,Math.sqrt(dx*dx+dz*dz))*180/Math.PI);
        pitch=rotate(pitch,wantedPitch,pitchLimit);yaw=rotate(yaw,wantedYaw,yawLimit);
        double x=-net.minecraft.util.Mth.sin(yaw/180*(float)Math.PI)*net.minecraft.util.Mth.cos(pitch/180*(float)Math.PI);
        double z=net.minecraft.util.Mth.cos(yaw/180*(float)Math.PI)*net.minecraft.util.Mth.cos(pitch/180*(float)Math.PI);
        double y=-net.minecraft.util.Mth.sin(pitch/180*(float)Math.PI);
        float length=net.minecraft.util.Mth.sqrt((float)(x*x+y*y+z*z));
        vx=(x/length+random.nextGaussian()*(random.nextBoolean()?-1:1)*(double).0075f*15)*speed;
        vy=(y/length+random.nextGaussian()*(random.nextBoolean()?-1:1)*(double).0075f*15)*speed;
        vz=(z/length+random.nextGaussian()*(random.nextBoolean()?-1:1)*(double).0075f*15)*speed;
    }
    public void targetMissing(float gravity){deathTimer++;vy-=gravity/2;if(deathTimer>50)alive=false;}
    public static float rotate(float current,float wanted,float limit){
        float delta=net.minecraft.util.Mth.wrapDegrees(wanted-current);
        if(delta>limit)delta=limit;
        if(delta< -limit)delta= -limit;
        return current+delta;
    }
    public static int sprite(int age){return 7+age%8;}
    public static float radius(int age,float scale){return .1f*scale*(net.minecraft.util.Mth.sin(age/3f)*.25f+1);}
    public float alpha(){return (50f-deathTimer)/50;}
}
