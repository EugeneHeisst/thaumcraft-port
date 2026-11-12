package thaumcraft.common.tiles.essentia;
import thaumcraft.api.aspects.Aspect;
/** Original TileAlembic container arithmetic; facing uses legacy side ids 2..5. */
public final class AlembicState implements AlchemyFurnaceState.Alembic {
    public Aspect aspect, aspectFilter;
    public int amount;
    public final int maxAmount = 32;
    public int facing = 2;
    public Aspect aspect() { return aspect; }
    public int amount() { return amount; }
    public int maxAmount() { return maxAmount; }
    public Aspect filter() { return aspectFilter; }
    public int addToContainer(Aspect tt, int am) {
        if (amount < maxAmount && tt == aspect || amount == 0) {
            aspect = tt;
            int added = Math.min(am, maxAmount - amount);
            amount += added;
            am -= added;
        }
        return am;
    }
    public boolean takeFromContainer(Aspect tt, int am) {
        if (amount == 0 || aspect == null) { aspect = null; amount = 0; }
        if (aspect != null && amount >= am && tt == aspect) {
            amount -= am;
            if (amount <= 0) { aspect = null; amount = 0; }
            return true;
        }
        return false;
    }
    /** Legacy side ids: 0 down, 1 up, 2..5 horizontal. */
    public boolean canOutputTo(int side) { return side != facing && side != 0; }
    public int comparator() { float r = (float)amount / (float)maxAmount; return (int)Math.floor(r * 14.0f) + (amount > 0 ? 1 : 0); }
    /** BlockMetalDevice knock messages tile.alembic.msg.1..5. */
    public int knockMessage() {
        if (aspect == null || amount == 0) return 1;
        if (amount < maxAmount * 0.4) return 2;
        if (amount < maxAmount * 0.8) return 3;
        if (amount < maxAmount) return 4;
        return 5;
    }
}
