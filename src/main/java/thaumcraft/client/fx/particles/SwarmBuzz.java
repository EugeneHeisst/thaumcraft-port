package thaumcraft.client.fx.particles;
import java.util.ArrayList;
import java.util.random.RandomGenerator;
/** Original FXSwarm global buzz limiter, including its 1.5 millisecond expiry. */
public final class SwarmBuzz {
    private static final ArrayList<Long> BUZZCOUNT=new ArrayList<>();
    private SwarmBuzz(){}
    public static void tick(RandomGenerator random,java.util.function.BooleanSupplier playerNearby,java.util.function.DoubleConsumer play){
        if(BUZZCOUNT.size()<3&&random.nextInt(50)==0&&playerNearby.getAsBoolean()){
            play.accept(.5f+random.nextFloat()*.4f);
            BUZZCOUNT.add(System.nanoTime()+1500000L);
        }
        if(BUZZCOUNT.size()>=3&&BUZZCOUNT.get(0)<System.nanoTime())BUZZCOUNT.remove(0);
    }
}
