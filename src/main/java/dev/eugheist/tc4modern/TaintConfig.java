package dev.eugheist.tc4modern;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Original live general settings for biome spread and flux-caused taint. */
final class TaintConfig {
    static ModConfigSpec.IntValue SPREAD;
    static ModConfigSpec.BooleanValue FROM_FLUX;
    static void define(ModConfigSpec.Builder builder){
        builder.push("general");
        SPREAD=builder.comment("Original biome spread rate: one in N*5 per eligible update with at least two adjacent taint blocks. Zero stops biome spread, while taint inside existing biomes remains active.").defineInRange("biome_taint_spread",200,0,1000000);
        FROM_FLUX=builder.comment("Allow concentrated flux goo to create taint, as in original Thaumcraft. Does not disable existing taint or ordinary flux decay.").define("biome_taint_from_flux",true);
        builder.pop();
    }
    static int spreadRate(){return WarpConfig.SPEC.isLoaded()?SPREAD.get():200;}
    static boolean fromFlux(){return !WarpConfig.SPEC.isLoaded()||FROM_FLUX.get();}
}
