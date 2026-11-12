package thaumcraft.common.tiles.essentia;
import thaumcraft.api.aspects.Aspect;
/** Original TileJarFillable container, suction and fill rules; transport neighbour injected. */
public class JarState {
    public Aspect aspect, aspectFilter;
    public int amount;
    public final int maxAmount = 64;
    public int facing = 2;
    int count;
    public int addToContainer(Aspect tt, int am) {
        if (am == 0) return am;
        if (amount < maxAmount && tt == aspect || amount == 0) {
            aspect = tt;
            int added = Math.min(am, maxAmount - amount);
            amount += added;
            am -= added;
        }
        return am;
    }
    public boolean takeFromContainer(Aspect tt, int am) {
        if (amount >= am && tt == aspect) {
            amount -= am;
            if (amount <= 0) { aspect = null; amount = 0; }
            return true;
        }
        return false;
    }
    public boolean doesContainerAccept(Aspect tag) { return aspectFilter != null ? tag.equals(aspectFilter) : true; }
    public int getMinimumSuction() { return aspectFilter != null ? 64 : 32; }
    public Aspect getSuctionType() { return aspectFilter != null ? aspectFilter : aspect; }
    public int getSuctionAmount() { return amount < maxAmount ? (aspectFilter != null ? 64 : 32) : 0; }
    /** updateEntity: every fifth tick while not full. @param above getConnectableTile(UP) or null. */
    public boolean tick(thaumcraft.api.aspects.IEssentiaTransport above) {
        if (++count % 5 != 0 || amount >= maxAmount) return false;
        return fillJar(above);
    }
    protected boolean fillJar(thaumcraft.api.aspects.IEssentiaTransport ic) {
        final var DOWN = net.minecraft.core.Direction.DOWN;
        if (ic == null || !ic.canOutputTo(DOWN)) return false;
        Aspect ta = null;
        if (aspectFilter != null) ta = aspectFilter;
        else if (aspect != null && amount > 0) ta = aspect;
        else if (ic.getEssentiaAmount(DOWN) > 0 && ic.getSuctionAmount(DOWN) < getSuctionAmount() && getSuctionAmount() >= ic.getMinimumSuction()) ta = ic.getEssentiaType(DOWN);
        if (ta != null && ic.getSuctionAmount(DOWN) < getSuctionAmount()) {
            int before = amount;
            addToContainer(ta, ic.takeEssentia(ta, 1, DOWN));
            return amount != before;
        }
        return false;
    }
}
