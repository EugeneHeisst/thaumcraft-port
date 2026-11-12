package thaumcraft.common.tiles.essentia;
import thaumcraft.api.aspects.Aspect;
/** TileJarFillableVoid: excess matching essentia is destroyed, even at capacity. */
public final class VoidJarState extends JarState {
    int count;
    @Override public int addToContainer(Aspect tt, int am) {
        if(am==0)return am;
        if(tt==aspect||amount==0){aspect=tt;amount=Math.min(maxAmount,amount+am);return 0;}
        return am;
    }
    @Override public int getMinimumSuction(){return aspectFilter!=null?48:32;}
    @Override public int getSuctionAmount(){return aspectFilter!=null&&amount<maxAmount?48:32;}
    @Override public boolean tick(thaumcraft.api.aspects.IEssentiaTransport above){
        return ++count%5==0&&fillJar(above);
    }
}
