package thaumcraft.common.lib.vis;
/** Original BlockCustomOreItem palette and BlockCustomOre shard drop count. */
public final class ShardPalette {
    public static final int[] COLORS={0xFFFFFF,0xFFFF7E,16727041,37119,40960,0xEECCFF,0x555577};
    public static int color(int variant){return COLORS[variant+1];}
    public static int drops(java.util.Random random,int fortune){return 1+random.nextInt(2+fortune);}
}
