package dev.eugheist.tc4modern;

/** Original general.wuss_mode; existing warp remains stored when the mode changes. */
public final class WarpConfig {
    private WarpConfig() {}
    public static final net.neoforged.neoforge.common.ModConfigSpec SPEC;
    private static final net.neoforged.neoforge.common.ModConfigSpec.BooleanValue WUSS_MODE, HARD_NODES, WARDED_STONE, MIRRORS;
    static {
        var builder=new net.neoforged.neoforge.common.ModConfigSpec.Builder();
        builder.push("general");
        WUSS_MODE=builder.comment("Disable warp from research and warp events, as in original Thaumcraft 4.")
            .define("wuss_mode",false);
        HARD_NODES=builder.comment("Allow hungry nodes to pull creatures, tainted nodes to spread fibres, and sinister nodes to spawn zombies.").define("hard_mode_nodes",true);
        WARDED_STONE=builder.comment("If set to false, warded stone, doors and glass will just be cosmetic in nature and not have its hardened properties (everyone will be able to break it with equal ease).").define("allow_warded_stone",true);
        MIRRORS=builder.comment("Setting this to false will disable arcane mirror research and crafting recipes.").define("allow_mirrors",true);
        builder.pop();
        WorldgenConfig.define(builder);
        TaintConfig.define(builder);
        GolemConfig.define(builder);
        RunicConfig.define(builder);
        ResearchConfig.define(builder);
        SpawnConfig.define(builder);
        PortableHoleConfig.define(builder);
        SPEC=builder.build();
    }
    public static boolean hardNodes() { return !SPEC.isLoaded() || HARD_NODES.get(); }
    /** Original Config.wardedStone; like the original it is read when research is registered, so research changes need a restart. */
    public static boolean wardedStone() { return !SPEC.isLoaded() || WARDED_STONE.get(); }
    /** Original Config.allowMirrors, read when research is registered. */
    public static boolean allowMirrors() { return !SPEC.isLoaded() || MIRRORS.get(); }
    public static boolean wussMode() { return SPEC.isLoaded() && WUSS_MODE.get(); }
}
