package dev.eugheist.tc4modern;

/** Original WarpEvents.checkWarpEvent roll; effects and player/gear adapters are separate. */
public final class WarpEventRoll {
    private WarpEventRoll() {}
    /** Original server player event gate, before any random draws or temporary decay. */
    public static boolean due(int playerTicks, boolean wussMode, boolean warded) {
        return !wussMode && !warded && playerTicks > 0 && playerTicks % 2000 == 0;
    }
    /** Apply low-level event setters; add() would incorrectly reset the reduced counter. */
    public static Result check(WarpState state, int gear, boolean protectiveMask, java.util.function.IntUnaryOperator random) {
        Result result = check(state.get(WarpState.Kind.PERMANENT), state.get(WarpState.Kind.STICKY),
            state.get(WarpState.Kind.TEMPORARY), state.counter(), gear, protectiveMask, random);
        state.setCounter(result.counter());
        state.set(WarpState.Kind.TEMPORARY, result.temporary());
        return result;
    }
    /** Read the equipped fortress mask at the event, never cache it in persistent warp state. */
    public static Result check(net.minecraft.world.entity.player.Player player, int gear, java.util.function.IntUnaryOperator random) {
        var state=PlayerWarp.load(player);var result=check(state,gear,FortressArmor.mask(player)==0,random);PlayerWarp.save(player,state);return result;
    }
    public record Result(boolean triggered,int strength,int effect,int counter,int temporary) {}
    public static Result check(int permanent,int sticky,int temporary,int counter,int gear,boolean protectiveMask,java.util.function.IntUnaryOperator random) {
        int chance=random.applyAsInt(100);
        int total=permanent+sticky+temporary;
        if(counter>0 && total+gear>0 && chance<=Math.sqrt(counter)) {
            int strength=Math.min(100,(2*(total+gear)+counter)/3);
            int remaining=Math.max(0,(int)(counter-Math.max(5,Math.sqrt(counter)*2)));
            int effect=random.applyAsInt(strength);
            if(protectiveMask) effect-=2+random.applyAsInt(4);
            return new Result(true,strength,effect,remaining,Math.max(0,temporary-1));
        }
        return new Result(false,0,0,counter,Math.max(0,temporary-1));
    }
}
