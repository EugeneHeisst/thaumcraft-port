package thaumcraft.common.tiles.essentia;

import net.minecraft.core.Direction;
import thaumcraft.api.aspects.Aspect;

/** TileTubeRestrict, TileTubeOneway, TileTubeFilter and TileTubeValve (TC4 4.2.3.5) as TubeState overrides. */
public final class TubeVariants {
    private TubeVariants() {}
    /** TileTubeRestrict: always halves the suction it passes on. */
    public static class Restrict extends TubeState {
        @Override protected void calculateSuction(Host host, Aspect filter, boolean restrict, boolean directional) { super.calculateSuction(host, filter, true, directional); }
    }
    /** TileTubeOneway: suction only from the facing side, equalizing never pulls from it. */
    public static class Oneway extends TubeState {
        @Override protected void calculateSuction(Host host, Aspect filter, boolean restrict, boolean directional) { super.calculateSuction(host, filter, restrict, true); }
        @Override protected void equalizeWithNeighbours(Host host, boolean directional) { super.equalizeWithNeighbours(host, true); }
    }
    /** TileTubeFilter: suction is filtered by the aspect set with a phial (getAspects reports it as amount -1). */
    public static class Filter extends TubeState {
        public Aspect aspectFilter;
        @Override protected void calculateSuction(Host host, Aspect filter, boolean restrict, boolean directional) { super.calculateSuction(host, aspectFilter, restrict, directional); }
    }
    /** TileTubeValve: the handle side never connects; a redstone edge toggles flow and closed valves ignore suction. */
    public static class Valve extends TubeState {
        public boolean allowFlow = true, wasPoweredLastTick;
        public float rotation;
        public interface PowerHost extends Host {
            boolean powered();
            void squeek();
            void sync();
        }
        /** updateEntity before super: checked every fifth tick with the count of the previous tick. */
        public void serverTick(PowerHost host) {
            if (count % 5 == 0) {
                boolean power = host.powered();
                if (wasPoweredLastTick && !power && !allowFlow) { allowFlow = true; host.squeek(); host.sync(); }
                if (!wasPoweredLastTick && power && allowFlow) { allowFlow = false; host.squeek(); host.sync(); }
                wasPoweredLastTick = power;
            }
            super.serverTick(host);
        }
        public void clientTick() {
            if (!allowFlow && rotation < 360) rotation += 20;
            else if (allowFlow && rotation > 0) rotation -= 20;
        }
        @Override public boolean isConnectable(Direction face) { return face != facing && super.isConnectable(face); }
        @Override public void setSuction(Aspect aspect, int amount) { if (allowFlow) super.setSuction(aspect, amount); }
    }
}
