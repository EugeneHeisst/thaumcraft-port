package thaumcraft.common.entities.monster;

/** Original EntityThaumicSlime size-dependent behavior; client and server sizes intentionally differ. */
public final class ThaumicSlimeState {
    private ThaumicSlimeState(){}
    public static float serverWidth(int size){return .25f*(float)Math.sqrt(size)+.25f;}
    public static float clientWidth(int size){return .6f*(float)Math.sqrt(size);}
    public static int experience(int size){return (int)Math.sqrt(size);}
    public static int attack(int size){return size;}
    public static float volume(int size){return .1f*(float)Math.sqrt(size);}
    public static boolean jumpSound(int size){return size>3;}
    public static boolean landSound(int size){return size>5;}
    public static int merge(int receiver,int donor){return Math.min(100,receiver+donor);}
    public static int deathChildren(int size){int n=(int)Math.sqrt(size);return n>1?n:0;}
    public static float childX(int index,int size){return ((index%2)-.5f)*(int)Math.sqrt(size)/4f;}
    public static float childZ(int index,int size){return ((index/2)-.5f)*(int)Math.sqrt(size)/4f;}
    public static double attackDistanceSquared(int size,int launched){
        int n=(int)Math.max(1,Math.sqrt(size));if(launched>0&&n==2)n=3;return .8*n*.8*n;
    }
}
