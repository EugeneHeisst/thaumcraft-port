package thaumcraft.common.blocks;
import java.util.random.RandomGenerator;
/** Original BlockFluxGoo.updateTick branch after the finite-fluid update. */
public final class FluxGooDecay {
    public interface World {
        int metadata();boolean airAbove();void remove();void slime(int size);void taint();void decrement();void gasAbove();
    }
    public static void tick(World world,boolean taintFromFlux,RandomGenerator random){
        int md=world.metadata();
        if(md>=2&&md<6&&world.airAbove()&&random.nextInt(25)==0){world.remove();world.slime(1);}
        else if(md>=6&&world.airAbove()){
            if(random.nextInt(25)==0){world.remove();world.slime(2);}
            else if(taintFromFlux&&random.nextInt(50)==0)world.taint();
        }else if(random.nextInt(30)==0){
            if(md==0)world.remove();
            else {world.decrement();if(random.nextBoolean()&&world.airAbove())world.gasAbove();}
        }
    }
    private FluxGooDecay(){}
}
