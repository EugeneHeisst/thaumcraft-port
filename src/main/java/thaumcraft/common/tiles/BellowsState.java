package thaumcraft.common.tiles;
/** Original TileBellows tick state; modern world/furnace/render bindings supplied separately. */
public final class BellowsState {
    public float inflation=1;
    public boolean direction,firstRun=true;
    public byte orientation;
    public boolean onVanillaFurnace;
    public int delay;
    public void clientTick(boolean powered,java.util.random.RandomGenerator random,java.util.function.DoubleConsumer sound){
        if(powered)return;
        if(firstRun)inflation=.35f+random.nextFloat()*.55f;
        firstRun=false;
        if(inflation>.35f&&!direction)inflation-=.075f;
        if(inflation<=.35f&&!direction)direction=true;
        if(inflation<1&&direction)inflation+=.025f;
        if(inflation>=1&&direction){direction=false;sound.accept(.5f+(random.nextFloat()-random.nextFloat())*.2f);}
    }
    public int furnaceTick(boolean powered,int progress){
        if(onVanillaFurnace&&!powered){
            if(++delay>=2){delay=0;if(progress>0&&progress<199)progress++;}
        }
        return progress;
    }
}
