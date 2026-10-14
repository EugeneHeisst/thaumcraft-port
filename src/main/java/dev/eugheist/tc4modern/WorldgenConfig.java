package dev.eugheist.tc4modern;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Original World_Generation, node rarity and biome weights in the existing common config file. */
final class WorldgenConfig {
    static ModConfigSpec.BooleanValue AURA,STRUCTURES,CINNABAR,AMBER,INFUSED,TREES,TAINT;
    static ModConfigSpec.BooleanValue REGEN_AURA,REGEN_STRUCTURES,REGEN_CINNABAR,REGEN_AMBER,REGEN_INFUSED,REGEN_TREES,REGEN_TAINT;
    static ModConfigSpec.ConfigValue<String> REGEN_KEY;
    static ModConfigSpec.IntValue NODE_RARITY,SPECIAL_RARITY,FOREST_WEIGHT,TAINT_WEIGHT;
    static void define(ModConfigSpec.Builder builder) {
        builder.push("World_Generation");
        AURA=builder.define("generate_aura_nodes",true);
        STRUCTURES=builder.define("generate_structures",true);
        CINNABAR=builder.comment("Legacy spelling retained for compatibility with original Thaumcraft configuration.").define("generate_cinnibar_ore",true);
        AMBER=builder.define("generate_amber_ore",true);
        INFUSED=builder.define("generate_infused_stone",true);
        TREES=builder.comment("Natural greatwood, silverwood and cinderpearls; intrinsic biome decoration and planted saplings remain active.").define("generate_trees",true);
        TAINT=builder.comment("Allow naturally occurring tainted biomes; existing taint and biome mutation remain active. Restart required.").worldRestart().define("generate_taint",true);
        builder.pop().comment("If a chunk is encountered that skipped TC worldgen, then the game will attempt to regenerate certain world features if they are set to true. CAUTION: Best used for worlds created before you added this mod, and only if you know what you are doing. Backups are advised.").push("World_Regeneration");
        REGEN_KEY=builder.comment("This key is used to keep track of which chunk have been generated/regenerated. Changing it will cause the regeneration code to run again, so only change it if you want it to happen. Useful to regen only one world feature at a time.").define("regen_key","DEFAULT");
        REGEN_AURA=builder.define("aura_nodes",false);
        REGEN_STRUCTURES=builder.define("structures",false);
        REGEN_CINNABAR=builder.define("cinnibar_ore",false);
        REGEN_AMBER=builder.define("amber_ore",false);
        REGEN_INFUSED=builder.define("infused_stone",false);
        REGEN_TREES=builder.define("trees",false);
        REGEN_TAINT=builder.comment("Kept from the original configuration; the original never read it, so it does not queue or regenerate anything.").define("taint",false);
        builder.pop().push("general");
        NODE_RARITY=builder.comment("One wild aura node per N chunks on average; totems use ten times this rarity.").defineInRange("node_rarity",36,1,1000000);
        SPECIAL_RARITY=builder.comment("One in N nodes may have a special type; modifier chance is one in floor(N/2).").defineInRange("special_node_rarity",18,3,1000000);
        builder.pop().push("Biomes");
        FOREST_WEIGHT=builder.comment("Original relative weight five, adapted to modern TerraBlender regions. Zero disables natural magical forest. Restart required.").worldRestart().defineInRange("magical_forest_biome_weight",5,0,1000);
        TAINT_WEIGHT=builder.comment("Original relative weight two, adapted to modern TerraBlender regions. Zero disables natural tainted land. Restart required.").worldRestart().defineInRange("taint_biome_weight",2,0,1000);
        builder.pop();
    }
    private static boolean loaded(){return WarpConfig.SPEC.isLoaded();}
    /** Set while Retrogen re-runs features on an old chunk: each generator then also needs its World_Regeneration switch (original newGen||Config.regenX). */
    static final ThreadLocal<Boolean> REGENERATING=ThreadLocal.withInitial(()->false);
    private static boolean gate(ModConfigSpec.BooleanValue gen,ModConfigSpec.BooleanValue regen){return (!loaded()||gen.get())&&(!REGENERATING.get()||loaded()&&regen.get());}
    static boolean aura(){return gate(AURA,REGEN_AURA);}
    static boolean structures(){return gate(STRUCTURES,REGEN_STRUCTURES);}
    static boolean cinnabar(){return gate(CINNABAR,REGEN_CINNABAR);}
    static boolean amber(){return gate(AMBER,REGEN_AMBER);}
    static boolean infused(){return gate(INFUSED,REGEN_INFUSED);}
    static boolean trees(){return gate(TREES,REGEN_TREES);}
    static String regenKey(){return loaded()?REGEN_KEY.get():"DEFAULT";}
    /** EventHandlerWorld.chunkLoad: regen_taint is deliberately absent, as in the original. */
    static boolean anyRegen(){return loaded()&&(REGEN_AMBER.get()||REGEN_AURA.get()||REGEN_CINNABAR.get()||REGEN_INFUSED.get()||REGEN_STRUCTURES.get()||REGEN_TREES.get());}
    static boolean taint(){return !loaded()||TAINT.get();}
    static int nodeRarity(){return loaded()?NODE_RARITY.get():36;}
    static int specialRarity(){return loaded()?SPECIAL_RARITY.get():18;}
    static int forestWeight(){return loaded()?FOREST_WEIGHT.get():5;}
    static int taintWeight(){return loaded()?TAINT_WEIGHT.get():2;}
}
