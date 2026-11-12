package thaumcraft.common.tiles;

/** Original TileNitor/FXWisp movement and color, for the two Nitor particle variants. */
public final class NitorWisp {
    public double x,y,z,previousX,previousY,previousZ,dx,dy,dz;
    public final float red,green,blue,scale,gravity;
    public final int lifetime;
    public int age;
    public NitorWisp(java.util.random.RandomGenerator random,double targetX,double targetZ,
            float size,int type,float gravity) {
        x=y=z=previousX=previousY=previousZ=.5;
        this.gravity=gravity;
        // Original EntityFX initial particleScale = (randomFloat*.5+.5)*2.
        scale=(random.nextFloat()*.5f+.5f)*2f*size;
        lifetime=(int)(36.0/(Math.random()*.3+.7));
        if(type==4){red=.7f+random.nextFloat()*.3f;green=.2f;blue=.2f;}
        else if(type==5){red=random.nextFloat()*.1f;green=random.nextFloat()*.1f;blue=random.nextFloat()*.1f;}
        else {red=.5f+random.nextFloat()*.3f;green=.5f+random.nextFloat()*.3f;blue=.2f;}
        dx=(targetX-x)/lifetime;dz=(targetZ-z)/lifetime;
    }
    public interface Movement { boolean move(NitorWisp wisp); }
    public boolean tick() { return tick(w->{w.x+=w.dx;w.y+=w.dy;w.z+=w.dz;return false;}); }
    public boolean tick(Movement movement) {
        previousX=x;previousY=y;previousZ=z;
        if(age++>=lifetime)return false;
        dy-=.04*(double)gravity;
        boolean onGround=movement.move(this);
        dx*=(double).98f;dy*=(double).98f;dz*=(double).98f;
        if(onGround){dx*=(double).7f;dz*=(double).7f;}
        return true;
    }
    public float radius(){return .5f*scale*((float)lifetime-age)/lifetime;}
}
