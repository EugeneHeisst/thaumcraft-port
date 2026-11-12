package thaumcraft.common.tiles.essentia;
import java.util.Random;
import net.minecraft.core.Direction;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.IEssentiaTransport;
/** Original TileTube (TC4 4.2.3.5) suction, venting and one-essentia transfer; Direction.from3DDataValue == ForgeDirection ordinal. */
public class TubeState implements IEssentiaTransport {
    public Direction facing = Direction.NORTH;
    public boolean[] openSides = {true, true, true, true, true, true};
    public Aspect essentiaType;
    public int essentiaAmount;
    public Aspect suctionType;
    public int suction;
    public int venting;
    public int count;
    public interface Host {
        /** ThaumcraftApiHelper.getConnectableTile(world, x, y, z, face). */
        IEssentiaTransport connectable(Direction face);
        Random random();
        /** world.addBlockEvent(blockTube, 1, colourIndex): venting. */
        void vent(int aspectOrderIndex);
        /** world.addBlockEvent(blockTube, 0, 0): creak. */
        void creak();
        void changed();
    }
    public void serverTick(Host host) {
        if (venting > 0) --venting;
        if (count == 0) count = host.random().nextInt(10);
        if (venting <= 0) {
            if (++count % 2 == 0) {
                calculateSuction(host, null, false, false);
                checkVenting(host);
                if (essentiaType != null && essentiaAmount == 0) essentiaType = null;
            }
            if (count % 5 == 0 && suction > 0) equalizeWithNeighbours(host, false);
        }
    }
    protected void calculateSuction(Host host, Aspect filter, boolean restrict, boolean directional) {
        suction = 0;
        suctionType = null;
        for (Direction loc : Direction.values()) {
            IEssentiaTransport ic;
            if (directional && facing != loc.getOpposite() || !isConnectable(loc) || (ic = host.connectable(loc)) == null) continue;
            Direction back = loc.getOpposite();
            int suck;
            if (filter != null && ic.getSuctionType(back) != null && ic.getSuctionType(back) != filter
                || filter == null && getEssentiaAmount(loc) > 0 && ic.getSuctionType(back) != null && getEssentiaType(loc) != ic.getSuctionType(back)
                || filter != null && getEssentiaAmount(loc) > 0 && getEssentiaType(loc) != null && ic.getSuctionType(back) != null && getEssentiaType(loc) != ic.getSuctionType(back)
                || (suck = ic.getSuctionAmount(back)) <= 0 || suck <= suction + 1) continue;
            Aspect st = ic.getSuctionType(back);
            if (st == null) st = filter;
            setSuction(st, restrict ? suck / 2 : suck - 1);
        }
    }
    protected void checkVenting(Host host) {
        for (Direction loc : Direction.values()) {
            IEssentiaTransport ic;
            if (!isConnectable(loc) || (ic = host.connectable(loc)) == null) continue;
            int suck = ic.getSuctionAmount(loc.getOpposite());
            if (suction <= 0 || suck != suction && suck != suction - 1 || suctionType == ic.getSuctionType(loc.getOpposite())) continue;
            host.vent(suctionType == null ? -1 : AspectOrder.indexOf(suctionType));
            venting = 40;
        }
    }
    protected void equalizeWithNeighbours(Host host, boolean directional) {
        if (essentiaAmount > 0) return;
        for (Direction loc : Direction.values()) {
            IEssentiaTransport ic;
            Direction back = loc.getOpposite();
            if (directional && facing == back || !isConnectable(loc) || (ic = host.connectable(loc)) == null || !ic.canOutputTo(back)
                || getSuctionType(null) != null && getSuctionType(null) != ic.getEssentiaType(back) && ic.getEssentiaType(back) != null
                || getSuctionAmount(null) <= ic.getSuctionAmount(back) || getSuctionAmount(null) < ic.getMinimumSuction()) continue;
            Aspect a = getSuctionType(null);
            if (a == null && (a = ic.getEssentiaType(back)) == null) a = ic.getEssentiaType(null);
            int am = addEssentia(a, ic.takeEssentia(a, 1, back), loc);
            if (am <= 0) continue;
            host.changed();
            if (host.random().nextInt(100) == 0) host.creak();
            return;
        }
    }
    public boolean isConnectable(Direction face) { return face != null && openSides[face.get3DDataValue()]; }
    public boolean canInputFrom(Direction face) { return face != null && openSides[face.get3DDataValue()]; }
    public boolean canOutputTo(Direction face) { return face != null && openSides[face.get3DDataValue()]; }
    public void setSuction(Aspect aspect, int amount) { suctionType = aspect; suction = amount; }
    public Aspect getSuctionType(Direction loc) { return suctionType; }
    public int getSuctionAmount(Direction loc) { return suction; }
    public Aspect getEssentiaType(Direction loc) { return essentiaType; }
    public int getEssentiaAmount(Direction loc) { return essentiaAmount; }
    public int takeEssentia(Aspect aspect, int amount, Direction face) {
        if (canOutputTo(face) && essentiaType == aspect && essentiaAmount > 0 && amount > 0) {
            --essentiaAmount;
            if (essentiaAmount <= 0) essentiaType = null;
            return 1;
        }
        return 0;
    }
    public int addEssentia(Aspect aspect, int amount, Direction face) {
        if (canInputFrom(face) && essentiaAmount == 0 && amount > 0) { essentiaType = aspect; ++essentiaAmount; return 1; }
        return 0;
    }
    public int getMinimumSuction() { return 0; }
    public boolean renderExtendedTube() { return false; }
}
