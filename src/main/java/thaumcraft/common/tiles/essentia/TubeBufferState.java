package thaumcraft.common.tiles.essentia;

import java.util.Random;
import net.minecraft.core.Direction;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IEssentiaTransport;

/** TileTubeBuffer (TC4 4.2.3.5): eight mixed essentia, per-side choke (0 open, 1 suction 1, 2 none), bellows boost 32 each. */
public class TubeBufferState {
    public static final int MAXAMOUNT = 8;
    public final AspectList aspects = new AspectList();
    public boolean[] openSides = {true, true, true, true, true, true};
    public byte[] chokedSides = {0, 0, 0, 0, 0, 0};
    public int count;
    public int bellows = -1;
    public interface Host {
        IEssentiaTransport connectable(Direction face);
        Random random();
        int countBellows();
        void changed();
    }
    public void tick(Host host, boolean server) {
        ++count;
        if (bellows < 0 || count % 20 == 0) bellows = host.countBellows();
        if (server && count % 5 == 0 && aspects.visSize() < MAXAMOUNT) fillBuffer(host);
    }
    void fillBuffer(Host host) {
        for (Direction dir : Direction.values()) {
            IEssentiaTransport ic = host.connectable(dir);
            Direction back = dir.getOpposite();
            if (ic == null || ic.getEssentiaAmount(back) <= 0 || ic.getSuctionAmount(back) >= getSuctionAmount(dir) || getSuctionAmount(dir) < ic.getMinimumSuction()) continue;
            Aspect ta = ic.getEssentiaType(back);
            addToContainer(host, ta, ic.takeEssentia(ta, 1, back));
            return;
        }
    }
    /** Returns the amount that did not fit; only single essentia are accepted. */
    public int addToContainer(Host host, Aspect tag, int amount) {
        if (amount != 1) return amount;
        if (aspects.visSize() < MAXAMOUNT) { aspects.add(tag, amount); host.changed(); return 0; }
        return amount;
    }
    public boolean takeFromContainer(Host host, Aspect tag, int amount) {
        if (aspects.getAmount(tag) >= amount) { aspects.remove(tag, amount); host.changed(); return true; }
        return false;
    }
    public boolean isConnectable(Direction face) { return openSides[face.get3DDataValue()]; }
    public boolean canInputFrom(Direction face) { return openSides[face.get3DDataValue()]; }
    public boolean canOutputTo(Direction face) { return openSides[face.get3DDataValue()]; }
    public int getSuctionAmount(Direction loc) {
        int choke = chokedSides[loc.get3DDataValue()];
        return choke == 2 ? 0 : bellows <= 0 || choke == 1 ? 1 : bellows * 32;
    }
    /** Original quirk: a random stored aspect on every call. */
    public Aspect getEssentiaType(Random random) {
        return aspects.size() > 0 ? aspects.getAspects()[random.nextInt(aspects.getAspects().length)] : null;
    }
    /** Refuses while any other open neighbour has stronger matching suction than the taker. */
    public int takeEssentia(Host host, Aspect aspect, int amount, Direction face) {
        if (!canOutputTo(face)) return 0;
        int suction = 0;
        IEssentiaTransport ic = host.connectable(face);
        if (ic != null) suction = ic.getSuctionAmount(face.getOpposite());
        for (Direction dir : Direction.values()) {
            if (!canOutputTo(dir) || dir == face || (ic = host.connectable(dir)) == null) continue;
            int sa = ic.getSuctionAmount(dir.getOpposite());
            Aspect su = ic.getSuctionType(dir.getOpposite());
            if (su != aspect && su != null || suction >= sa || getSuctionAmount(dir) >= sa) continue;
            return 0;
        }
        if (amount > aspects.getAmount(aspect)) amount = aspects.getAmount(aspect);
        return takeFromContainer(host, aspect, amount) ? amount : 0;
    }
    public int addEssentia(Host host, Aspect aspect, int amount, Direction face) {
        return canInputFrom(face) ? amount - addToContainer(host, aspect, amount) : 0;
    }
}
