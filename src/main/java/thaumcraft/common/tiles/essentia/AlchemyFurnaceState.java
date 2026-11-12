package thaumcraft.common.tiles.essentia;
import java.util.Random;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
/**
 * Original TileAlchemyFurnace server tick (TC4 4.2.3.5) with the platform (inventory, fuel table,
 * world lookups) injected through {@link Host}.
 */
public final class AlchemyFurnaceState {
    public AspectList aspects = new AspectList();
    public int vis;
    public final int maxVis = 50;
    public int smeltTime = 100;
    public int bellows = -1;
    public boolean speedBoost;
    public int furnaceBurnTime, currentItemBurnTime, furnaceCookTime;
    int count;
    public interface Alembic {
        Aspect aspect(); int amount(); int maxAmount(); Aspect filter();
        int addToContainer(Aspect aspect, int amount);
    }
    public interface Host {
        /** TileAlembic at y+deep, or null. */
        Alembic alembic(int deep);
        /** getObjectTags + getBonusTags of slot 0, or null. */
        AspectList inputTags();
        /** TileEntityFurnace.getItemBurnTime of slot 1. */
        int fuelBurnTime();
        boolean fuelIsAlumentum();
        void consumeFuel();
        void consumeInput();
        int countBellows();
        Random random();
        void markForUpdate();
    }
    public boolean isBurning() { return furnaceBurnTime > 0; }
    public int getCookProgressScaled(int scale) { if (smeltTime <= 0) smeltTime = 1; return furnaceCookTime * scale / smeltTime; }
    public int getContentsScaled(int scale) { return vis * scale / maxVis; }
    public int getBurnTimeRemainingScaled(int scale) { if (currentItemBurnTime == 0) currentItemBurnTime = 200; return furnaceBurnTime * scale / currentItemBurnTime; }
    /** @return true when the burning state flipped or an item finished (original flag1). */
    public boolean serverTick(Host host) {
        boolean flag = furnaceBurnTime > 0;
        boolean flag1 = false;
        ++count;
        if (furnaceBurnTime > 0) --furnaceBurnTime;
        if (bellows < 0) bellows = host.countBellows();
        if (count % (speedBoost ? 20 : 40) == 0 && aspects.size() > 0) {
            AspectList exclude = new AspectList();
            int deep = 0;
            Alembic alembic;
            while (deep < 5 && (alembic = host.alembic(++deep)) != null) {
                if (alembic.aspect() != null && alembic.amount() < alembic.maxAmount() && aspects.getAmount(alembic.aspect()) > 0) {
                    takeFromContainer(alembic.aspect(), 1);
                    alembic.addToContainer(alembic.aspect(), 1);
                    exclude.merge(alembic.aspect(), 1);
                    host.markForUpdate();
                }
            }
            deep = 0;
            while (deep < 5 && (alembic = host.alembic(++deep)) != null) {
                if (alembic.aspect() != null && alembic.amount() != 0) continue;
                Aspect as = null;
                if (alembic.filter() == null) as = takeRandomAspect(exclude, host.random());
                else if (takeFromContainer(alembic.filter(), 1)) as = alembic.filter();
                if (as == null) continue;
                alembic.addToContainer(as, 1);
                host.markForUpdate();
                break;
            }
        }
        if (furnaceBurnTime == 0 && canSmelt(host)) {
            currentItemBurnTime = furnaceBurnTime = host.fuelBurnTime();
            if (furnaceBurnTime > 0) {
                flag1 = true;
                speedBoost = host.fuelIsAlumentum();
                host.consumeFuel();
            }
        }
        if (isBurning() && canSmelt(host)) {
            ++furnaceCookTime;
            if (furnaceCookTime >= smeltTime) {
                furnaceCookTime = 0;
                smeltItem(host);
                flag1 = true;
            }
        } else furnaceCookTime = 0;
        if (flag != furnaceBurnTime > 0) { flag1 = true; host.markForUpdate(); }
        return flag1;
    }
    boolean canSmelt(Host host) {
        AspectList al = host.inputTags();
        if (al == null || al.size() == 0) return false;
        int vs = al.visSize();
        if (vs > maxVis - vis) return false;
        smeltTime = (int)((float)(vs * 10) * (1.0f - 0.125f * (float)bellows));
        return true;
    }
    void smeltItem(Host host) {
        if (canSmelt(host)) {
            AspectList al = host.inputTags();
            for (Aspect a : al.getAspects()) aspects.add(a, al.getAmount(a));
            vis = aspects.visSize();
            host.consumeInput();
        }
    }
    public Aspect takeRandomAspect(AspectList exclude, Random random) {
        if (aspects.size() > 0) {
            AspectList temp = aspects.copy();
            if (exclude.size() > 0) for (Aspect a : exclude.getAspects()) temp.remove(a);
            if (temp.size() > 0) {
                Aspect tag = temp.getAspects()[random.nextInt(temp.getAspects().length)];
                aspects.remove(tag, 1);
                --vis;
                return tag;
            }
        }
        return null;
    }
    public boolean takeFromContainer(Aspect tag, int amount) {
        if (aspects != null && aspects.getAmount(tag) >= amount) { aspects.remove(tag, amount); vis -= amount; return true; }
        return false;
    }
}
