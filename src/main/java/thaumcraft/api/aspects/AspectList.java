/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.nbt.Tag
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.nbt.ListTag
 *  thaumcraft.api.ThaumcraftApiHelper
 */
package thaumcraft.api.aspects;

import java.io.Serializable;
import java.util.LinkedHashMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;

public class AspectList
implements Serializable {
    public LinkedHashMap<Aspect, Integer> aspects = new LinkedHashMap();

    public AspectList(ItemStack stack) {
        try {
            AspectList temp = ThaumcraftApiHelper.getObjectAspects((ItemStack)stack);
            if (temp != null) {
                for (Aspect tag : temp.getAspects()) {
                    this.add(tag, temp.getAmount(tag));
                }
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    public AspectList() {
    }

    public AspectList copy() {
        AspectList out = new AspectList();
        for (Aspect a : this.getAspects()) {
            out.add(a, this.getAmount(a));
        }
        return out;
    }

    public int size() {
        return this.aspects.size();
    }

    public int visSize() {
        int q = 0;
        for (Aspect as : this.aspects.keySet()) {
            q += this.getAmount(as);
        }
        return q;
    }

    public Aspect[] getAspects() {
        Aspect[] q = new Aspect[0];
        return this.aspects.keySet().toArray(q);
    }

    public Aspect[] getPrimalAspects() {
        AspectList t = new AspectList();
        for (Aspect as : this.aspects.keySet()) {
            if (!as.isPrimal()) continue;
            t.add(as, 1);
        }
        Aspect[] q = new Aspect[0];
        return t.aspects.keySet().toArray(q);
    }

    public Aspect[] getAspectsSorted() {
        try {
            Aspect[] out = this.aspects.keySet().toArray(new Aspect[0]);
            boolean change = false;
            block2: do {
                change = false;
                for (int a = 0; a < out.length - 1; ++a) {
                    Aspect e1 = out[a];
                    Aspect e2 = out[a + 1];
                    if (e1 == null || e2 == null || e1.getTag().compareTo(e2.getTag()) <= 0) continue;
                    out[a] = e2;
                    out[a + 1] = e1;
                    change = true;
                    continue block2;
                }
            } while (change);
            return out;
        }
        catch (Exception e) {
            return this.getAspects();
        }
    }

    public Aspect[] getAspectsSortedAmount() {
        try {
            Aspect[] out = this.aspects.keySet().toArray(new Aspect[0]);
            boolean change = false;
            block2: do {
                change = false;
                for (int a = 0; a < out.length - 1; ++a) {
                    Aspect eb;
                    int e1 = this.getAmount(out[a]);
                    int e2 = this.getAmount(out[a + 1]);
                    if (e1 <= 0 || e2 <= 0 || e2 <= e1) continue;
                    Aspect ea = out[a];
                    out[a] = eb = out[a + 1];
                    out[a + 1] = ea;
                    change = true;
                    continue block2;
                }
            } while (change);
            return out;
        }
        catch (Exception e) {
            return this.getAspects();
        }
    }

    public int getAmount(Aspect key) {
        return this.aspects.get(key) == null ? 0 : this.aspects.get(key);
    }

    public boolean reduce(Aspect key, int amount) {
        if (this.getAmount(key) >= amount) {
            int am = this.getAmount(key) - amount;
            this.aspects.put(key, am);
            return true;
        }
        return false;
    }

    public AspectList remove(Aspect key, int amount) {
        int am = this.getAmount(key) - amount;
        if (am <= 0) {
            this.aspects.remove(key);
        } else {
            this.aspects.put(key, am);
        }
        return this;
    }

    public AspectList remove(Aspect key) {
        this.aspects.remove(key);
        return this;
    }

    public AspectList add(Aspect aspect, int amount) {
        if (this.aspects.containsKey(aspect)) {
            int oldamount = this.aspects.get(aspect);
            amount += oldamount;
        }
        this.aspects.put(aspect, amount);
        return this;
    }

    public AspectList merge(Aspect aspect, int amount) {
        int oldamount;
        if (this.aspects.containsKey(aspect) && amount < (oldamount = this.aspects.get(aspect).intValue())) {
            amount = oldamount;
        }
        this.aspects.put(aspect, amount);
        return this;
    }

    public AspectList add(AspectList in) {
        for (Aspect a : in.getAspects()) {
            this.add(a, in.getAmount(a));
        }
        return this;
    }

    public AspectList merge(AspectList in) {
        for (Aspect a : in.getAspects()) {
            this.merge(a, in.getAmount(a));
        }
        return this;
    }

    public void readFromNBT(CompoundTag nbttagcompound) {
        this.aspects.clear();
        ListTag tlist = nbttagcompound.getList("Aspects", 10);
        for (int j = 0; j < tlist.size(); ++j) {
            CompoundTag rs = tlist.getCompound(j);
            if (!rs.contains("key")) continue;
            this.add(Aspect.getAspect(rs.getString("key")), rs.getInt("amount"));
        }
    }

    public void readFromNBT(CompoundTag nbttagcompound, String label) {
        this.aspects.clear();
        ListTag tlist = nbttagcompound.getList(label, 10);
        for (int j = 0; j < tlist.size(); ++j) {
            CompoundTag rs = tlist.getCompound(j);
            if (!rs.contains("key")) continue;
            this.add(Aspect.getAspect(rs.getString("key")), rs.getInt("amount"));
        }
    }

    public void writeToNBT(CompoundTag nbttagcompound) {
        ListTag tlist = new ListTag();
        nbttagcompound.put("Aspects", (Tag)tlist);
        for (Aspect aspect : this.getAspects()) {
            if (aspect == null) continue;
            CompoundTag f = new CompoundTag();
            f.putString("key", aspect.getTag());
            f.putInt("amount", this.getAmount(aspect));
            tlist.add((Tag)f);
        }
    }

    public void writeToNBT(CompoundTag nbttagcompound, String label) {
        ListTag tlist = new ListTag();
        nbttagcompound.put(label, (Tag)tlist);
        for (Aspect aspect : this.getAspects()) {
            if (aspect == null) continue;
            CompoundTag f = new CompoundTag();
            f.putString("key", aspect.getTag());
            f.putInt("amount", this.getAmount(aspect));
            tlist.add((Tag)f);
        }
    }
}
