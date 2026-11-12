package thaumcraft.common.blocks;
import java.util.random.RandomGenerator;
/** Original BlockTaint branches when the sampled target is outside the tainted biome. */
public final class TaintBlockDecay {
    public interface World {void goo(int x,int y,int z);void dirt(int x,int y,int z);}
    public interface InfestedWorld extends World {
        boolean airAbove();boolean nearbySwarmer();boolean encased();void swarmer();
    }
    public static void insideTargetBiome(InfestedWorld world,int x,int y,int z,int variant,boolean spawnSpores,RandomGenerator random){
        if(variant==0){
            if(spawnSpores&&world.airAbove()&&random.nextInt(200)==0){
                if(!world.nearbySwarmer())world.swarmer();
            }else if(world.encased())world.goo(x,y,z);
        }
    }
    public static void outsideTargetBiome(World world,int x,int y,int z,int variant,RandomGenerator random){
        if(variant==0&&random.nextInt(20)==0)world.goo(x,y,z);
        else if(variant==1&&random.nextInt(10)==0)world.dirt(x,y,z);
    }
    private TaintBlockDecay(){}
}
