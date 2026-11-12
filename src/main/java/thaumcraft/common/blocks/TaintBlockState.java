package thaumcraft.common.blocks;
/** Original BlockTaint metadata-specific physical and drop properties. */
public final class TaintBlockState {
    public static float hardness(int metadata){return switch(metadata){case 0->1.75f;case 1->1.5f;case 2->.2f;default->2f;};}
    public static int drops(int metadata){return metadata==2?9:metadata==1?1:0;}
    public static boolean flesh(int metadata){return metadata==2;}
    private TaintBlockState(){}
}
