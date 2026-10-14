package dev.eugheist.tc4modern;

/** Original PlayerKnowledge warp buckets and Thaumcraft addWarpToPlayer rules. */
public final class WarpState {
    public enum Kind { PERMANENT, STICKY, TEMPORARY }
    private int permanent, sticky, temporary, counter;
    public int get(Kind kind) {
        return switch (kind) { case PERMANENT -> permanent; case STICKY -> sticky; case TEMPORARY -> temporary; };
    }
    public int total() { return permanent + sticky + temporary; }
    public int counter() { return counter; }
    public void setCounter(int value) { counter = value; }
    /** Lower-level PlayerKnowledge setters clamp every bucket to zero. */
    public void set(Kind kind, int value) {
        value = Math.max(0, value);
        switch (kind) { case PERMANENT -> permanent = value; case STICKY -> sticky = value; case TEMPORARY -> temporary = value; }
    }
    public net.minecraft.nbt.CompoundTag save() {
        var tag = new net.minecraft.nbt.CompoundTag();
        tag.putInt("Thaumcraft.eldritch", permanent);
        tag.putInt("Thaumcraft.eldritch.sticky", sticky);
        tag.putInt("Thaumcraft.eldritch.temp", temporary);
        tag.putInt("Thaumcraft.eldritch.counter", counter);
        return tag;
    }
    public static WarpState load(net.minecraft.nbt.CompoundTag tag) {
        var state = new WarpState();
        state.set(Kind.PERMANENT, tag.getInt("Thaumcraft.eldritch"));
        state.set(Kind.STICKY, tag.getInt("Thaumcraft.eldritch.sticky"));
        state.set(Kind.TEMPORARY, tag.getInt("Thaumcraft.eldritch.temp"));
        state.setCounter(tag.getInt("Thaumcraft.eldritch.counter"));
        return state;
    }
    /** True means the original API would synchronize, notify, and reset its event counter. */
    public boolean add(Kind kind, int amount) {
        if (amount == 0 || (amount < 0 && (kind == Kind.PERMANENT || get(kind) <= 0))) return false;
        set(kind, get(kind) + amount);
        counter = total();
        return true;
    }
}
