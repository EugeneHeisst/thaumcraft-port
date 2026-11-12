package thaumcraft.client.fx.particles;
import java.util.random.RandomGenerator;
/** Original FXBubble motion, lifetime, scale and sprite progression with a modern submission bridge. */
public final class FXBubble {
    public double x,y,z,previousX,previousY,previousZ,dx,dy,dz,bubblespeed=.002;
    public float red=1,green=0,blue=.5f,scale;
    public int particle=16,remaining;
    public boolean dead;
    public FXBubble(double x,double y,double z,int age,RandomGenerator random){
        this.x=previousX=x;this.y=previousY=y;this.z=previousZ=z;
        scale=(random.nextFloat()*.5f+.5f)*2*(random.nextFloat()*.3f+.2f);
        dx=(random.nextDouble()*2-1)*.02;dy=random.nextDouble()*.02;dz=(random.nextDouble()*2-1)*.02;
        remaining=(int)(age+2+8/(random.nextDouble()*.8+.2));
    }
    public void froth(RandomGenerator random,boolean down){
        scale*=.75f;remaining=down?12+random.nextInt(12):4+random.nextInt(3);
        bubblespeed=down?-.005:-.001;dx/=5;dy/=10;dz/=5;
    }
    public void tick(RandomGenerator random){
        previousX=x;previousY=y;previousZ=z;dy+=bubblespeed;
        if(bubblespeed>0){dx+=(random.nextFloat()-random.nextFloat())*.01f;dz+=(random.nextFloat()-random.nextFloat())*.01f;}
        x+=dx;y+=dy;z+=dz;dx*=.85f;dy*=.85f;dz*=.85f;
        if(remaining--<=0)dead=true;else if(remaining<=2)particle++;
    }
}
