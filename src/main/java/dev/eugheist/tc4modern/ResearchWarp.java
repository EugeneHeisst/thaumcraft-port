package dev.eugheist.tc4modern;

/** Original ConfigResearch warp registrations and ResearchManager completion split. */
public final class ResearchWarp {
    private ResearchWarp() {}
    private static final java.util.Map<String,Integer> VALUES=java.util.Map.ofEntries(
        java.util.Map.entry("FOCUSHELLBAT", 2),
        java.util.Map.entry("ROD_bone", 1),
        java.util.Map.entry("ROD_bone_staff", 1),
        java.util.Map.entry("SINSTONE", 2),
        java.util.Map.entry("INFERNALFURNACE", 2),
        java.util.Map.entry("JARBRAIN", 3),
        java.util.Map.entry("MASKANGRYGHOST", 1),
        java.util.Map.entry("MASKSIPPINGFIEND", 1),
        java.util.Map.entry("LIQUIDDEATH", 3),
        java.util.Map.entry("BOTTLETAINT", 2),
        java.util.Map.entry("GOLEMFLESH", 3),
        java.util.Map.entry("COREBUTCHER", 1),
        java.util.Map.entry("ADVANCEDGOLEM", 5),
        java.util.Map.entry("RESEARCHER2", 1),
        java.util.Map.entry("CRIMSON", 3),
        java.util.Map.entry("OCULUS", 6),
        java.util.Map.entry("PRIMNODE", 1),
        java.util.Map.entry("CAP_void", 1),
        java.util.Map.entry("FOCUSPRIMAL", 2),
        java.util.Map.entry("ROD_primal_staff", 3));
    public static int amount(String key) { return VALUES.getOrDefault(key,0); }
    public static int sticky(String key) { return amount(key)/2; }
    public static int permanent(String key) { return amount(key)-sticky(key); }
    public static void award(net.minecraft.world.entity.player.Player player,String key) {
        if (WarpConfig.wussMode()) return;
        int permanent=permanent(key),sticky=sticky(key);
        if(permanent>0) PlayerWarp.add(player,WarpState.Kind.PERMANENT,permanent);
        if(sticky>0) PlayerWarp.add(player,WarpState.Kind.STICKY,sticky);
    }
}
