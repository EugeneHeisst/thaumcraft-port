package dev.eugheist.tc4modern;

/** Original presentation preferences, kept local to each client. */
public final class WarpClientConfig {
    private WarpClientConfig() {}
    public static final net.neoforged.neoforge.common.ModConfigSpec SPEC;
    private static final net.neoforged.neoforge.common.ModConfigSpec.BooleanValue SHADERS;
    static final net.neoforged.neoforge.common.ModConfigSpec.BooleanValue DISPLAY_ASPECTS, DIAL_BOTTOM, CROOKED, GLOWING_TAINT, BLUE_MAGICAL_FOREST, COLOR_BLIND;
    static final net.neoforged.neoforge.common.ModConfigSpec.IntValue NOTIFICATION_DELAY, NOTIFICATION_MAX, GOLEM_LINK_QUALITY;
    static {
        var builder = new net.neoforged.neoforge.common.ModConfigSpec.Builder();
        builder.push("general");
        SHADERS = builder.comment("Enable original Thaumcraft post-processing shaders. Disable to reduce rendering cost.")
            .define("shaders", true);
        DISPLAY_ASPECTS=builder.comment("Show scanned item aspects in container screens by default; holding Shift hides them instead of revealing them.").define("display_aspects",false);
        DIAL_BOTTOM=builder.comment("Place the original wand vis dial at the bottom-left instead of top-left.").define("wand_dial_bottom",false);
        CROOKED=builder.comment("Tilt original jar labels slightly; disable for straight labels.").define("crooked",true);
        NOTIFICATION_DELAY=builder.comment("Original research/aspect notification duration in milliseconds. Minimum four keeps the quarter-duration arrival animation defined.").defineInRange("notification_delay",5000,4,3600000);
        NOTIFICATION_MAX=builder.comment("Maximum visible research/aspect notifications; zero hides notifications without changing knowledge.").defineInRange("notification_max",15,0,100);
        GOLEM_LINK_QUALITY=builder.comment("Original flowing-link quality, shared by golem bell ribbons and cultist ritual beams. Values below four disable lines while keeping markers.").defineInRange("golem_link_quality",16,0,128);
        GLOWING_TAINT=builder.comment("Setting this to false will remove the glowing purple nodules from taint fibres. Applies when chunks are rebuilt.").define("glowing_taint",true);
        BLUE_MAGICAL_FOREST=builder.comment("Set this to true to get the old blue magical forest back. Applies when chunks are rebuilt.").define("blue_magical_forest",false);
        COLOR_BLIND=builder.comment("Setting this to true will make certain colors higher contrast or darker to prevent them from being 'invisible' to color blind people.").define("color_blind",false);
        builder.pop();
        SPEC = builder.build();
    }
    public static int golemLinkQuality(){int value=SPEC.isLoaded()?GOLEM_LINK_QUALITY.get():16;return value<4?0:value;}
    public static boolean dialBottom(){return SPEC.isLoaded()&&DIAL_BOTTOM.get();}
    public static boolean crooked(){return !SPEC.isLoaded()||CROOKED.get();}
    public static int notificationDelay(){return SPEC.isLoaded()?NOTIFICATION_DELAY.get():5000;}
    public static int notificationMax(){return SPEC.isLoaded()?NOTIFICATION_MAX.get():15;}
    public static boolean colorBlind(){return SPEC.isLoaded()&&COLOR_BLIND.get();}
    public static boolean blueMagicalForest(){return SPEC.isLoaded()&&BLUE_MAGICAL_FOREST.get();}
    public static boolean glowingTaint(){return !SPEC.isLoaded()||GLOWING_TAINT.get();}
    public static boolean displayAspects(){return SPEC.isLoaded()&&DISPLAY_ASPECTS.get();}
    public static boolean shaders() { return !SPEC.isLoaded() || SHADERS.get(); }
}
