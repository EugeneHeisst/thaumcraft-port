package dev.eugheist.tc4modern;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

public final class BrainJarStateTest {
    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        var state = new BrainJarState();
        require(state.experience() == 0 && state.comparatorSignal() == 0, "Empty jar is silent to comparator");
        state.absorb(1);
        require(state.comparatorSignal() == 1, "One XP enables comparator");
        state.absorb(999);
        require(state.comparatorSignal() == 8, "Half capacity gives original comparator signal 8");
        state.absorb(999);
        require(state.canAttract() && state.collectThisTick(), "One remaining XP still permits a whole orb batch");
        state.absorb(7); state.absorb(11);
        require(state.experience() == 2017, "Original collects every orb in a batch before clamping");
        state.beginTick();
        require(state.experience() == 2000 && state.comparatorSignal() == 15, "Following tick clamps capacity");
        require(!state.canAttract() && !state.collectThisTick(), "Full jar leaves orbs alone");
        int before = state.experience();
        int released = state.release(RandomSource.create(1));
        require(released >= 0 && released < 64 && state.experience() == before - released, "Click emits at most63XP and preserves remainder");
        require(state.eatDelay() == 40 && !state.canAttract(), "Click pauses attraction");
        for (int tick = 0; tick < 40; tick++) require(!state.collectThisTick(), "Every delay tick skips collection, including transition to zero");
        require(state.eatDelay() == 0, "Forty ticks exhaust delay");
        var saved = new CompoundTag(); state.save(saved);
        require(saved.size() == 1 && saved.contains("XP"), "Original persists XP only");
        var loaded = new BrainJarState(); loaded.load(saved);
        require(loaded.experience() == state.experience() && loaded.eatDelay() == 0, "Experience survives reload without transient delay");
        var empty = new BrainJarState();
        require(empty.release(RandomSource.create(1)) == 0 && empty.eatDelay() == 40, "Empty click still pauses collection");
        Vec3 horizontal = BrainJarState.attraction(Vec3.ZERO, new Vec3(3.5, 0, 0));
        Vec3 vertical = BrainJarState.attraction(Vec3.ZERO, new Vec3(0, 3.5, 0));
        require(Math.abs(horizontal.x + 0.0375) < 1e-12 && horizontal.y == 0, "Original horizontal force at half radius");
        require(Math.abs(vertical.y + 0.0825) < 1e-12, "Original stronger vertical attraction");
        require(BrainJarState.attraction(Vec3.ZERO, new Vec3(7, 0, 0)).equals(Vec3.ZERO), "No attraction outside seven-unit force radius");
        require(BrainJarState.attraction(Vec3.ZERO, Vec3.ZERO).equals(Vec3.ZERO), "Overlapping centre stays finite for collection");
        System.out.println("Brain jar state tests passed: batches, capacity, release, delay, persistence, comparator, attraction.");
    }
}
