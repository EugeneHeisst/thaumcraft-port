package dev.eugheist.tc4modern;

/** Exact original PacketMiscEvent and ClientTickEventsFML fog envelope. */
public final class WarpFogState {
    private boolean active;
    private int duration;
    private float density;
    public boolean active() { return active; }
    public int duration() { return duration; }
    public float density() { return density; }
    public void mist() { active = true; duration = 2400; }
    public void guardianPulse() { active = true; if (duration < 200) duration = 200; }
    public void tick() {
        if (!active) return;
        if (duration < 100) density = 0.1f * (duration / 100f);
        else if (density < 0.1f) density += 0.001f;
        if (--duration < 0) active = false;
    }
}
