package thaumcraft.common.lib.vis;
/** Original GuiArcaneWorkbench aspect locations and insufficient-vis pulse. */
public final class ArcaneCostVisuals {
    public static final int[][] LOCATIONS={{72,21},{24,43},{24,102},{72,124},{120,102},{120,43}};
    public static float alpha(int ticks,int index,boolean enough){return enough?1:.5f+(net.minecraft.util.Mth.sin((ticks+index*10)/2f)*.2f-.2f);}
}
