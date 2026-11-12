package thaumcraft.client.fx.particles;
/** Original FXSparkle type 6, size 1.5, multiplier 6, zero velocity and gravity. */
public final class AlumentumSparkle {
    public final float scale,red,green,blue;
    public int age;
    public AlumentumSparkle(java.util.random.RandomGenerator random){
        scale=(random.nextFloat()*.5f+.5f)*2*1.5f;
        red=.8f+random.nextFloat()*.2f;green=.8f+random.nextFloat()*.2f;blue=.8f+random.nextFloat()*.2f;
    }
    public boolean tick(){return age++<18;}
    public float radius(){return .1f*scale*(18-age+1)/18f;}
    public float u(){return ((16+age/6)%4)/16f;}
}
