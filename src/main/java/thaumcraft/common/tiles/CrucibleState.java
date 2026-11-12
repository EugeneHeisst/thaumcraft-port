package thaumcraft.common.tiles;

import java.util.random.RandomGenerator;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

/** Original TileCrucible state calculations, extracted from TC4 4.2.3.5.
 * Platform supplies heat-source/bellows detection, randomness and flux spill callbacks.
 * Recipe matching and item entities remain in the platform bridge.
 */
public final class CrucibleState {
    public short heat;
    public int water;
    public AspectList aspects = new AspectList();
    public long counter = -100L;
    public static final int CAPACITY = 1000;
    public boolean boiling() { return heat > 150; }
    public int tagAmount() {
        int count = 0;
        for (Aspect aspect : aspects.getAspects()) count += aspects.getAmount(aspect);
        return count;
    }
    public int comparator(){return (int)Math.floor(tagAmount()/100f*14)+(tagAmount()>0?1:0);}
    public int fill(int amount) {
        int accepted = Math.max(0, Math.min(amount, CAPACITY - water));
        water += accepted;
        return accepted;
    }
    public void tick(boolean heatSource, int bellows, RandomGenerator random, Runnable spill) {
        ++counter;
        if (water > 0) {
            if (heatSource) {
                if (heat < 200) heat = (short)(heat + 1 + bellows * 2);
            } else if (heat > 0) --heat;
        } else if (heat > 0) --heat;
        if (tagAmount() > 100 && counter % 5L == 0) {
            Aspect[] available = aspects.getAspects();
            aspects.remove(available[random.nextInt(available.length)], 1);
            spill.run();
        }
        if (counter > 100 && boiling()) {
            counter = 0;
            if (tagAmount() > 0) {
                Aspect[] available = aspects.getAspects();
                Aspect aspect = available[random.nextInt(available.length)];
                if (aspect.isPrimal()) aspect = available[random.nextInt(available.length)];
                water = Math.max(0, water - 2);
                aspects.remove(aspect, 1);
                if (!aspect.isPrimal()) {
                    aspects.add(aspect.getComponents()[random.nextBoolean() ? 0 : 1], 1);
                } else spill.run();
            }
        }
    }
    public void dissolve(AspectList tags) {
        for (Aspect aspect : tags.getAspects()) aspects.add(aspect, tags.getAmount(aspect));
        counter = -150L;
    }
    /** Called only after the bridge has validated catalyst, research and recipe aspects. */
    public void crafted(AspectList remaining) {
        aspects = remaining;
        water = Math.max(0, water - 50);
        counter = -250L;
    }
    /** Original TileCrucibleRenderer RGB recoloring by aspect concentration. */
    public float[] fluidColor() {
        float recolor = (float)tagAmount() / 100f;
        if (recolor > 0f) recolor = .5f + recolor / 2f;
        return new float[]{1f-recolor/3f,1f-recolor,1f-recolor/2f};
    }
    public float fluidHeight() {
        float base = .3f + .5f * ((float)water / CAPACITY);
        float out = base + (float)tagAmount() / 100f * (1f - base);
        if (out > 1f) out = 1.001f;
        if (out == 1f) out = .9999f;
        return out;
    }
    public void empty(Runnable spill) {
        if (water > 0 || aspects.visSize() > 0) {
            water = 0;
            for (int a = 0; a < aspects.visSize() / 2; ++a) spill.run();
            aspects = new AspectList();
        }
    }
}
