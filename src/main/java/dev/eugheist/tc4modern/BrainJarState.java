package dev.eugheist.tc4modern;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/** Original TileJarBrain storage and BlockJar interaction rules. */
public final class BrainJarState {
    public static final int CAPACITY = 2000;
    private int experience;
    private int eatDelay;

    public int experience() { return experience; }
    public int eatDelay() { return eatDelay; }
    public boolean canAttract() { return experience < CAPACITY && eatDelay == 0; }

    /** Called before attraction, once per tick; original clamps overflow on the next tick. */
    public void beginTick() {
        if (experience > CAPACITY) experience = CAPACITY;
    }

    /** Original delay decrement takes the branch instead of consuming orbs this tick. */
    public boolean collectThisTick() {
        if (eatDelay > 0) { --eatDelay; return false; }
        return experience < CAPACITY;
    }

    /** The original consumes the whole overlapping batch, even if it exceeds capacity. */
    public void absorb(int orbValue) { experience += orbValue; }

    public int release(RandomSource random) {
        eatDelay = 40;
        int released = random.nextInt(Math.min(experience + 1, 64));
        experience -= released;
        return released;
    }

    public int comparatorSignal() {
        return (int)Math.floor((float)experience / CAPACITY * 14) + (experience > 0 ? 1 : 0);
    }

    public static Vec3 attraction(Vec3 center, Vec3 orb) {
        Vec3 delta = center.subtract(orb).scale(1.0 / 7.0);
        double distance = delta.length();
        double force = 1.0 - distance;
        if (force <= 0 || distance == 0) return Vec3.ZERO;
        force *= force;
        return new Vec3(delta.x / distance * force * 0.15,
            delta.y / distance * force * 0.33, delta.z / distance * force * 0.15);
    }

    public void load(CompoundTag tag) { experience = tag.getInt("XP"); eatDelay = 0; }
    public void save(CompoundTag tag) { tag.putInt("XP", experience); }
}
